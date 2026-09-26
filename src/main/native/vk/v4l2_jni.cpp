#include <jni.h>

#include <stdexcept>

#include "v4l2_stream.h"

namespace {

void throwJava(JNIEnv *env, const char *message, const char *type = "java/io/IOException") {
    jclass cls = env->FindClass(type);
    if (cls != nullptr) {
        env->ThrowNew(cls, message);
    }
}

V4l2Stream &streamOf(jlong handle) {
    return **reinterpret_cast<std::shared_ptr<V4l2Stream> *>(handle);
}

}

extern "C" {

JNIEXPORT jlong JNICALL Java_org_openpnp_machine_reference_camera_V4l2Stream_open(JNIEnv *env, jclass,
        jstring uniqueId, jint width, jint height, jint fps) {
    const char *id = env->GetStringUTFChars(uniqueId, nullptr);
    std::string idString(id);
    env->ReleaseStringUTFChars(uniqueId, id);
    try {
        return reinterpret_cast<jlong>(new std::shared_ptr<V4l2Stream>(V4l2Stream::open(idString, width, height,
                fps)));
    }
    catch (const std::exception &e) {
        throwJava(env, e.what(), "java/lang/RuntimeException");
        return 0;
    }
}

JNIEXPORT void JNICALL Java_org_openpnp_machine_reference_camera_V4l2Stream_close(JNIEnv *, jclass,
        jlong handle) {
    auto *holder = reinterpret_cast<std::shared_ptr<V4l2Stream> *>(handle);
    (*holder)->close();
    delete holder;
}

JNIEXPORT jint JNICALL Java_org_openpnp_machine_reference_camera_V4l2Stream_bytesPerLine(JNIEnv *, jclass,
        jlong handle) {
    return streamOf(handle).bytesPerLine();
}

JNIEXPORT jboolean JNICALL Java_org_openpnp_machine_reference_camera_V4l2Stream_isZeroCopy(JNIEnv *, jclass,
        jlong handle) {
    return streamOf(handle).zeroCopy() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_org_openpnp_machine_reference_camera_V4l2Stream_hasNewFrame(JNIEnv *, jclass,
        jlong handle, jlong after) {
    return streamOf(handle).hasNewFrame(after) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL Java_org_openpnp_machine_reference_camera_V4l2Stream_acquire(JNIEnv *env, jclass,
        jlong handle, jlong after, jlong notBeforeNs, jint timeoutMs, jlongArray info) {
    try {
        uint64_t sequence = 0;
        int64_t timestamp = 0;
        int slot = streamOf(handle).acquire(after, notBeforeNs, timeoutMs, &sequence, &timestamp);
        if (slot >= 0) {
            jlong values[2] = {(jlong) sequence, timestamp};
            env->SetLongArrayRegion(info, 0, 2, values);
        }
        return slot;
    }
    catch (const std::exception &e) {
        throwJava(env, e.what());
        return -1;
    }
}

// Returns a handle in the same form GpuBuffer owns.
JNIEXPORT jlong JNICALL Java_org_openpnp_machine_reference_camera_V4l2Stream_slotBuffer(JNIEnv *, jclass,
        jlong handle, jint slot) {
    return reinterpret_cast<jlong>(new std::shared_ptr<gpu::Resource>(streamOf(handle).slotBuffer(slot)));
}

JNIEXPORT void JNICALL Java_org_openpnp_machine_reference_camera_V4l2Stream_release(JNIEnv *, jclass,
        jlong handle, jint slot, jlong gpuValue) {
    streamOf(handle).release(slot, gpuValue);
}

JNIEXPORT jboolean JNICALL Java_org_openpnp_machine_reference_camera_V4l2Stream_queryControl(JNIEnv *env,
        jclass, jlong handle, jint id, jintArray limits) {
    int32_t values[3];
    if (!streamOf(handle).queryControl(id, &values[0], &values[1], &values[2])) {
        return JNI_FALSE;
    }
    env->SetIntArrayRegion(limits, 0, 3, values);
    return JNI_TRUE;
}

JNIEXPORT jint JNICALL Java_org_openpnp_machine_reference_camera_V4l2Stream_getControl(JNIEnv *env,
        jclass, jlong handle, jint id) {
    try {
        return streamOf(handle).getControl(id);
    }
    catch (const std::exception &e) {
        throwJava(env, e.what(), "java/lang/RuntimeException");
        return 0;
    }
}

JNIEXPORT void JNICALL Java_org_openpnp_machine_reference_camera_V4l2Stream_setControl(JNIEnv *env,
        jclass, jlong handle, jint id, jint value) {
    try {
        streamOf(handle).setControl(id, value);
    }
    catch (const std::exception &e) {
        throwJava(env, e.what(), "java/lang/RuntimeException");
    }
}

}
