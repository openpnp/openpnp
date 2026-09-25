#include <jni.h>

#include <mutex>
#include <string>

#include <opencv2/core.hpp>
#include <opencv2/core/ocl.hpp>
#include <opencv2/imgproc.hpp>

namespace {

struct CameraTransform {
    std::mutex lock;
    cv::UMat map1;
    cv::UMat map2;
    cv::UMat lut;
    cv::Mat src;
    cv::Mat dst;
    cv::UMat gpuSrc;
    cv::UMat gpuBalanced;
    cv::UMat gpuDst;
};

void throwJava(JNIEnv *env, const char *message) {
    jclass cls = env->FindClass("java/lang/RuntimeException");
    if (cls != nullptr) {
        env->ThrowNew(cls, message);
    }
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
        // Same fixed-point map format as OpenCV's CPU remap uses internally.
        cv::Mat fixedXY, fixedFraction;
        cv::convertMaps(*reinterpret_cast<cv::Mat *>(mapX), *reinterpret_cast<cv::Mat *>(mapY),
                fixedXY, fixedFraction, CV_16SC2, false);
        std::lock_guard<std::mutex> guard(t->lock);
        fixedXY.copyTo(t->map1);
        fixedFraction.copyTo(t->map2);
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
        int type = CV_8UC(channels);
        t->src.create(height, width, type);
        env->GetByteArrayRegion(src, 0, static_cast<jsize>(t->src.total() * channels),
                reinterpret_cast<jbyte *>(t->src.data));
        if (env->ExceptionCheck()) {
            return;
        }
        t->src.copyTo(t->gpuSrc);
        const cv::UMat *input = &t->gpuSrc;
        if (!t->lut.empty() && channels == 3) {
            cv::LUT(t->gpuSrc, t->lut, t->gpuBalanced);
            input = &t->gpuBalanced;
        }
        cv::remap(*input, t->gpuDst, t->map1, t->map2, cv::INTER_LINEAR, cv::BORDER_CONSTANT);
        t->gpuDst.copyTo(t->dst);
        jsize size = static_cast<jsize>(t->dst.total() * t->dst.elemSize());
        if (env->GetArrayLength(dst) != size) {
            throwJava(env, "destination size mismatch");
            return;
        }
        env->SetByteArrayRegion(dst, 0, size, reinterpret_cast<const jbyte *>(t->dst.data));
    }
    catch (const std::exception &e) {
        throwJava(env, e.what());
    }
}

}
