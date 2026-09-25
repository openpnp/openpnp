#include <jni.h>

#include <dlfcn.h>
#include <unistd.h>

#include <mutex>
#include <string>

#include <opencv2/core.hpp>
#include <opencv2/core/ocl.hpp>
#include <opencv2/imgproc.hpp>

#include "v4l2_stream.h"

namespace {

struct CameraTransform {
    std::mutex lock;
    cv::UMat map1;
    cv::UMat map2;
    cv::UMat lut;
    cv::UMat gpuSrc;
    cv::UMat gpuBalanced;
    cv::UMat gpuDst;
};

void throwJava(JNIEnv *env, const char *message, const char *type = "java/lang/RuntimeException") {
    jclass cls = env->FindClass(type);
    if (cls != nullptr) {
        env->ThrowNew(cls, message);
    }
}

// OpenCV (GPU) failures stay RuntimeExceptions; anything else comes from V4L2.
void throwCapture(JNIEnv *env) {
    try {
        throw;
    }
    catch (const cv::Exception &e) {
        throwJava(env, e.what());
    }
    catch (const std::exception &e) {
        throwJava(env, e.what(), "java/io/IOException");
    }
}

typedef int32_t cl_int;
typedef uint32_t cl_uint;
typedef struct _cl_command_queue *cl_command_queue;
typedef struct _cl_event *cl_event;
const cl_uint CL_EVENT_COMMAND_EXECUTION_STATUS = 0x11D3;

struct ClApi {
    cl_int (*enqueueMarker)(cl_command_queue, cl_uint, const cl_event *, cl_event *) = nullptr;
    cl_int (*flush)(cl_command_queue) = nullptr;
    cl_int (*getEventInfo)(cl_event, cl_uint, size_t, void *, size_t *) = nullptr;
    cl_int (*releaseEvent)(cl_event) = nullptr;
};

// OpenCV has already loaded the OpenCL library; borrow the few calls it doesn't wrap.
const ClApi &clApi() {
    static ClApi api = [] {
        ClApi a;
        void *lib = dlopen("libOpenCL.so.1", RTLD_NOW | RTLD_NOLOAD);
        if (lib == nullptr) {
            lib = dlopen("libOpenCL.so", RTLD_NOW | RTLD_NOLOAD);
        }
        if (lib != nullptr) {
            a.enqueueMarker = reinterpret_cast<decltype(a.enqueueMarker)>(dlsym(lib, "clEnqueueMarkerWithWaitList"));
            a.flush = reinterpret_cast<decltype(a.flush)>(dlsym(lib, "clFlush"));
            a.getEventInfo = reinterpret_cast<decltype(a.getEventInfo)>(dlsym(lib, "clGetEventInfo"));
            a.releaseEvent = reinterpret_cast<decltype(a.releaseEvent)>(dlsym(lib, "clReleaseEvent"));
        }
        return a;
    }();
    return api;
}

// Waits for the queued GPU work by sleeping; Intel's clFinish() spins a CPU core meanwhile.
void waitForGpu() {
    const ClApi &api = clApi();
    auto queue = static_cast<cl_command_queue>(cv::ocl::Queue::getDefault().ptr());
    cl_event event;
    if (!api.enqueueMarker || !api.flush || !api.getEventInfo || !api.releaseEvent || queue == nullptr
            || api.enqueueMarker(queue, 0, nullptr, &event) != 0) {
        return;
    }
    api.flush(queue);
    cl_int status = 1;
    while (api.getEventInfo(event, CL_EVENT_COMMAND_EXECUTION_STATUS, sizeof(status), &status, nullptr) == 0
            && status > 0) {
        usleep(100);
    }
    api.releaseEvent(event);
}

// Wraps a Java byte[] without copying; the array must be released before returning to Java.
class CriticalBytes {
public:
    CriticalBytes(JNIEnv *env, jbyteArray array) : env(env), array(array),
            size(env->GetArrayLength(array)),
            data(static_cast<uchar *>(env->GetPrimitiveArrayCritical(array, nullptr))) {}
    ~CriticalBytes() {
        if (data != nullptr) {
            env->ReleasePrimitiveArrayCritical(array, data, 0);
        }
    }
    JNIEnv *env;
    jbyteArray array;
    jsize size;
    uchar *data;
};

// Runs white balance and the composite remap on a BGR (or gray) GPU image and downloads the
// result straight into dst.
void finish(JNIEnv *env, CameraTransform *t, const cv::UMat &input, jbyteArray dst) {
    const cv::UMat *image = &input;
    if (!t->lut.empty() && input.channels() == 3) {
        cv::LUT(input, t->lut, t->gpuBalanced);
        image = &t->gpuBalanced;
    }
    if (!t->map1.empty()) {
        cv::remap(*image, t->gpuDst, t->map1, t->map2, cv::INTER_LINEAR, cv::BORDER_CONSTANT);
        image = &t->gpuDst;
    }
    waitForGpu();
    CriticalBytes out(env, dst);
    if (out.data == nullptr) {
        return;
    }
    if ((size_t) out.size != image->total() * image->elemSize()) {
        throw std::runtime_error("destination size mismatch");
    }
    cv::Mat wrapped(image->rows, image->cols, image->type(), out.data);
    image->copyTo(wrapped);
}

V4l2Stream *streamOf(jlong handle) {
    return reinterpret_cast<V4l2Stream *>(handle);
}

const char circularSymmetrySource[] =
#include "circular_symmetry.cl.inc"
;

cv::UMat upload(JNIEnv *env, jarray array, int type) {
    jsize length = env->GetArrayLength(array);
    cv::UMat result;
    void *data = env->GetPrimitiveArrayCritical(array, nullptr);
    if (data == nullptr) {
        throw std::runtime_error("out of memory");
    }
    try {
        cv::Mat(1, length, type, data).copyTo(result);
    }
    catch (...) {
        env->ReleasePrimitiveArrayCritical(array, data, JNI_ABORT);
        throw;
    }
    env->ReleasePrimitiveArrayCritical(array, data, JNI_ABORT);
    return result;
}

}

extern "C" {

JNIEXPORT jstring JNICALL Java_org_openpnp_vision_gpu_OclSupport_deviceInfo(JNIEnv *env, jclass) {
    try {
        if (!cv::ocl::haveOpenCL()) {
            return env->NewStringUTF("");
        }
        cv::ocl::setUseOpenCL(true);
        if (!cv::ocl::useOpenCL()) {
            return env->NewStringUTF("");
        }
        const cv::ocl::Device &device = cv::ocl::Device::getDefault();
        if (!device.available() || !(device.type() & cv::ocl::Device::TYPE_GPU)) {
            return env->NewStringUTF("");
        }
        std::string info = device.name() + " (" + device.vendorName() + ", " + device.version() + ")";
        return env->NewStringUTF(info.c_str());
    }
    catch (const std::exception &e) {
        throwJava(env, e.what());
        return nullptr;
    }
}

JNIEXPORT jlong JNICALL Java_org_openpnp_vision_gpu_OclCameraTransform_create(JNIEnv *, jclass) {
    return reinterpret_cast<jlong>(new CameraTransform());
}

JNIEXPORT void JNICALL Java_org_openpnp_vision_gpu_OclCameraTransform_release(JNIEnv *, jclass, jlong handle) {
    delete reinterpret_cast<CameraTransform *>(handle);
}

JNIEXPORT void JNICALL Java_org_openpnp_vision_gpu_OclCameraTransform_setTransform(JNIEnv *env, jclass,
        jlong handle, jlong mapX, jlong mapY, jlong lut) {
    auto *t = reinterpret_cast<CameraTransform *>(handle);
    try {
        std::lock_guard<std::mutex> guard(t->lock);
        if (mapX != 0) {
            // Same fixed-point map format as OpenCV's CPU remap uses internally.
            cv::Mat fixedXY, fixedFraction;
            cv::convertMaps(*reinterpret_cast<cv::Mat *>(mapX), *reinterpret_cast<cv::Mat *>(mapY),
                    fixedXY, fixedFraction, CV_16SC2, false);
            fixedXY.copyTo(t->map1);
            fixedFraction.copyTo(t->map2);
        }
        else {
            t->map1.release();
            t->map2.release();
        }
        if (lut != 0) {
            reinterpret_cast<cv::Mat *>(lut)->copyTo(t->lut);
        }
        else {
            t->lut.release();
        }
    }
    catch (const std::exception &e) {
        throwJava(env, e.what());
    }
}

JNIEXPORT void JNICALL Java_org_openpnp_vision_gpu_OclCameraTransform_apply(JNIEnv *env, jclass,
        jlong handle, jbyteArray src, jint width, jint height, jint channels, jbyteArray dst) {
    auto *t = reinterpret_cast<CameraTransform *>(handle);
    try {
        std::lock_guard<std::mutex> guard(t->lock);
        {
            CriticalBytes in(env, src);
            if (in.data == nullptr) {
                return;
            }
            if ((size_t) in.size < (size_t) width * height * channels) {
                throw std::runtime_error("source size mismatch");
            }
            cv::Mat(height, width, CV_8UC(channels), in.data).copyTo(t->gpuSrc);
        }
        finish(env, t, t->gpuSrc, dst);
    }
    catch (const std::exception &e) {
        throwJava(env, e.what());
    }
}

JNIEXPORT jboolean JNICALL Java_org_openpnp_vision_gpu_OclCameraTransform_applyV4l2(JNIEnv *env, jclass,
        jlong handle, jlong streamHandle, jint timeoutMs, jbyteArray dst) {
    auto *t = reinterpret_cast<CameraTransform *>(handle);
    try {
        std::lock_guard<std::mutex> guard(t->lock);
        cv::Mat yuyv;
        if (!v4l2Grab(streamOf(streamHandle), timeoutMs, yuyv)) {
            return JNI_FALSE;
        }
        {
            // The V4L2 buffer is page aligned, so the driver can often map it instead of copying.
            cv::UMat input = yuyv.getUMat(cv::ACCESS_READ);
            cv::cvtColor(input, t->gpuSrc, cv::COLOR_YUV2BGR_YUYV);
        }
        finish(env, t, t->gpuSrc, dst);
        return JNI_TRUE;
    }
    catch (const std::exception &) {
        throwCapture(env);
        return JNI_FALSE;
    }
}

JNIEXPORT void JNICALL Java_org_openpnp_vision_gpu_OclCircularSymmetry_score(JNIEnv *env, jclass,
        jbyteArray pixels, jintArray binStart, jintArray offsets, jintArray params, jfloatArray scores,
        jintArray radii) {
    try {
        static cv::ocl::ProgramSource source(circularSymmetrySource);
        cv::ocl::Kernel kernel("circular_symmetry", source);
        if (kernel.empty()) {
            throw std::runtime_error("circular_symmetry kernel failed to build");
        }
        jint p[14];
        env->GetIntArrayRegion(params, 0, 14, p);
        int cols = p[4];
        int rows = p[5];
        cv::UMat gpuPixels = upload(env, pixels, CV_8U);
        cv::UMat gpuBinStart = upload(env, binStart, CV_32S);
        cv::UMat gpuOffsets = upload(env, offsets, CV_32S);
        cv::UMat gpuScores(1, cols * rows, CV_32F);
        cv::UMat gpuRadii(1, cols * rows, CV_32S);
        kernel.args(cv::ocl::KernelArg::PtrReadOnly(gpuPixels), cv::ocl::KernelArg::PtrReadOnly(gpuBinStart),
                cv::ocl::KernelArg::PtrReadOnly(gpuOffsets), cv::ocl::KernelArg::PtrWriteOnly(gpuScores),
                cv::ocl::KernelArg::PtrWriteOnly(gpuRadii), p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7],
                p[8], p[9], p[10], p[11], p[12], p[13]);
        size_t global[2] = {(size_t) cols, (size_t) rows};
        if (!kernel.run(2, global, nullptr, false)) {
            throw std::runtime_error("circular_symmetry kernel failed to run");
        }
        waitForGpu();
        cv::Mat hostScores = gpuScores.getMat(cv::ACCESS_READ);
        env->SetFloatArrayRegion(scores, 0, cols * rows, hostScores.ptr<jfloat>());
        hostScores.release();
        cv::Mat hostRadii = gpuRadii.getMat(cv::ACCESS_READ);
        env->SetIntArrayRegion(radii, 0, cols * rows, hostRadii.ptr<jint>());
    }
    catch (const std::exception &e) {
        throwJava(env, e.what());
    }
}

JNIEXPORT jlong JNICALL Java_org_openpnp_machine_reference_camera_V4l2Stream_open(JNIEnv *env, jclass,
        jstring uniqueId, jint width, jint height, jint fps) {
    const char *id = env->GetStringUTFChars(uniqueId, nullptr);
    std::string idString(id);
    env->ReleaseStringUTFChars(uniqueId, id);
    try {
        return reinterpret_cast<jlong>(v4l2Open(idString, width, height, fps));
    }
    catch (const std::exception &e) {
        throwJava(env, e.what());
        return 0;
    }
}

JNIEXPORT void JNICALL Java_org_openpnp_machine_reference_camera_V4l2Stream_close(JNIEnv *, jclass,
        jlong handle) {
    v4l2Close(streamOf(handle));
}

JNIEXPORT jboolean JNICALL Java_org_openpnp_machine_reference_camera_V4l2Stream_hasNewFrame(JNIEnv *,
        jclass, jlong handle) {
    return v4l2HasNewFrame(streamOf(handle)) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_org_openpnp_machine_reference_camera_V4l2Stream_captureBgr(JNIEnv *env,
        jclass, jlong handle, jint timeoutMs, jbyteArray dst) {
    try {
        cv::Mat yuyv;
        if (!v4l2Grab(streamOf(handle), timeoutMs, yuyv)) {
            return JNI_FALSE;
        }
        CriticalBytes out(env, dst);
        if (out.data == nullptr) {
            return JNI_FALSE;
        }
        if ((size_t) out.size != yuyv.total() * 3) {
            throw std::runtime_error("destination size mismatch");
        }
        cv::Mat bgr(yuyv.rows, yuyv.cols, CV_8UC3, out.data);
        cv::cvtColor(yuyv, bgr, cv::COLOR_YUV2BGR_YUYV);
        return JNI_TRUE;
    }
    catch (const std::exception &) {
        throwCapture(env);
        return JNI_FALSE;
    }
}

JNIEXPORT jboolean JNICALL Java_org_openpnp_machine_reference_camera_V4l2Stream_queryControl(JNIEnv *env,
        jclass, jlong handle, jint id, jintArray limits) {
    int32_t values[3];
    if (!v4l2QueryControl(streamOf(handle), id, &values[0], &values[1], &values[2])) {
        return JNI_FALSE;
    }
    env->SetIntArrayRegion(limits, 0, 3, values);
    return JNI_TRUE;
}

JNIEXPORT jint JNICALL Java_org_openpnp_machine_reference_camera_V4l2Stream_getControl(JNIEnv *env,
        jclass, jlong handle, jint id) {
    try {
        return v4l2GetControl(streamOf(handle), id);
    }
    catch (const std::exception &e) {
        throwJava(env, e.what());
        return 0;
    }
}

JNIEXPORT void JNICALL Java_org_openpnp_machine_reference_camera_V4l2Stream_setControl(JNIEnv *env,
        jclass, jlong handle, jint id, jint value) {
    try {
        v4l2SetControl(streamOf(handle), id, value);
    }
    catch (const std::exception &e) {
        throwJava(env, e.what());
    }
}

}
