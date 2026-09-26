#include <jni.h>

#include <dlfcn.h>
#include <unistd.h>

#include <mutex>
#include <string>

#include <opencv2/core.hpp>
#include <opencv2/core/ocl.hpp>
#include <opencv2/imgproc.hpp>

namespace {

void throwJava(JNIEnv *env, const char *message, const char *type = "java/lang/RuntimeException") {
    jclass cls = env->FindClass(type);
    if (cls != nullptr) {
        env->ThrowNew(cls, message);
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

const char circularSymmetrySource[] =
#include "circular_symmetry.cl.inc"
;

const char rectlinearSymmetrySource[] =
#include "rectlinear_symmetry.cl.inc"
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

JNIEXPORT void JNICALL Java_org_openpnp_vision_gpu_OclRectlinearSymmetry_crossSections(JNIEnv *env, jclass,
        jbyteArray pixels, jfloatArray gammaLut, jfloatArray sines, jfloatArray cosines, jintArray params,
        jfloatArray floatParams, jfloatArray sums, jfloatArray weights, jfloatArray maskedWeights) {
    try {
        static cv::ocl::ProgramSource source(rectlinearSymmetrySource);
        cv::ocl::Kernel kernel("rectlinear_cross_sections", source);
        if (kernel.empty()) {
            throw std::runtime_error("rectlinear_cross_sections kernel failed to build");
        }
        jint p[11];
        env->GetIntArrayRegion(params, 0, 11, p);
        jfloat f[3];
        env->GetFloatArrayRegion(floatParams, 0, 3, f);
        int channels = p[1];
        int bins = p[8] + p[9];
        int angles = p[10];
        cv::UMat gpuPixels = upload(env, pixels, CV_8U);
        cv::UMat gpuLut = upload(env, gammaLut, CV_32F);
        cv::UMat gpuSines = upload(env, sines, CV_32F);
        cv::UMat gpuCosines = upload(env, cosines, CV_32F);
        cv::UMat gpuSums(1, angles * bins * channels, CV_32F);
        cv::UMat gpuWeights(1, angles * bins, CV_32F);
        cv::UMat gpuMasked(1, angles * bins, CV_32F);
        kernel.args(cv::ocl::KernelArg::PtrReadOnly(gpuPixels), cv::ocl::KernelArg::PtrReadOnly(gpuLut),
                cv::ocl::KernelArg::PtrReadOnly(gpuSines), cv::ocl::KernelArg::PtrReadOnly(gpuCosines),
                cv::ocl::KernelArg::PtrWriteOnly(gpuSums), cv::ocl::KernelArg::PtrWriteOnly(gpuWeights),
                cv::ocl::KernelArg::PtrWriteOnly(gpuMasked), p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7],
                p[8], p[9], f[0], f[1], f[2], p[10]);
        size_t global[2] = {(size_t) bins, (size_t) angles};
        if (!kernel.run(2, global, nullptr, false)) {
            throw std::runtime_error("rectlinear_cross_sections kernel failed to run");
        }
        waitForGpu();
        {
            cv::Mat host = gpuSums.getMat(cv::ACCESS_READ);
            env->SetFloatArrayRegion(sums, 0, angles * bins * channels, host.ptr<jfloat>());
        }
        {
            cv::Mat host = gpuWeights.getMat(cv::ACCESS_READ);
            env->SetFloatArrayRegion(weights, 0, angles * bins, host.ptr<jfloat>());
        }
        {
            cv::Mat host = gpuMasked.getMat(cv::ACCESS_READ);
            env->SetFloatArrayRegion(maskedWeights, 0, angles * bins, host.ptr<jfloat>());
        }
    }
    catch (const std::exception &e) {
        throwJava(env, e.what());
    }
}

}
