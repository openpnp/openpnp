#include <jni.h>

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
