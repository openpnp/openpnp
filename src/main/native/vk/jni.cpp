#include <jni.h>

#include <stdexcept>

#include "runtime.h"

using gpu::Buffer;
using gpu::Pipeline;
using gpu::Program;
using gpu::Resource;
using gpu::Runtime;

namespace {

void throwJava(JNIEnv *env, const char *message) {
    jclass cls = env->FindClass("java/lang/RuntimeException");
    if (cls != nullptr) {
        env->ThrowNew(cls, message);
    }
}

// Java holds each object as a heap-allocated shared_ptr, so native users can outlive its handle.
template <typename T>
jlong toHandle(std::shared_ptr<T> object) {
    return reinterpret_cast<jlong>(new std::shared_ptr<Resource>(std::move(object)));
}

template <typename T>
std::shared_ptr<T> fromHandle(jlong handle) {
    if (handle == 0) {
        throw std::runtime_error("null GPU handle");
    }
    auto object = std::dynamic_pointer_cast<T>(*reinterpret_cast<std::shared_ptr<Resource> *>(handle));
    if (!object) {
        throw std::runtime_error("wrong GPU handle type");
    }
    return object;
}

std::vector<jlong> longs(JNIEnv *env, jlongArray array) {
    std::vector<jlong> values(array == nullptr ? 0 : env->GetArrayLength(array));
    if (!values.empty()) {
        env->GetLongArrayRegion(array, 0, values.size(), values.data());
    }
    return values;
}

std::vector<jint> ints(JNIEnv *env, jintArray array) {
    std::vector<jint> values(array == nullptr ? 0 : env->GetArrayLength(array));
    if (!values.empty()) {
        env->GetIntArrayRegion(array, 0, values.size(), values.data());
    }
    return values;
}

}

extern "C" {

JNIEXPORT jstring JNICALL Java_org_openpnp_vision_gpu_GpuNative_init(JNIEnv *env, jclass) {
    try {
        return env->NewStringUTF(Runtime::init().deviceName().c_str());
    }
    catch (const std::exception &e) {
        throwJava(env, e.what());
        return nullptr;
    }
}

JNIEXPORT jboolean JNICALL Java_org_openpnp_vision_gpu_GpuNative_hasInt64(JNIEnv *, jclass) {
    return Runtime::get()->int64() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_org_openpnp_vision_gpu_GpuNative_hasByteStorage(JNIEnv *, jclass) {
    return Runtime::get()->byteStorage() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jlong JNICALL Java_org_openpnp_vision_gpu_GpuNative_completed(JNIEnv *, jclass) {
    return Runtime::get()->completed();
}

JNIEXPORT jboolean JNICALL Java_org_openpnp_vision_gpu_GpuNative_await(JNIEnv *env, jclass, jlong value,
        jlong timeoutNs) {
    try {
        return Runtime::get()->wait(value, timeoutNs) ? JNI_TRUE : JNI_FALSE;
    }
    catch (const std::exception &e) {
        throwJava(env, e.what());
        return JNI_FALSE;
    }
}

JNIEXPORT void JNICALL Java_org_openpnp_vision_gpu_GpuNative_release(JNIEnv *, jclass, jlong handle) {
    auto *holder = reinterpret_cast<std::shared_ptr<Resource> *>(handle);
    Runtime::get()->retire(std::move(*holder));
    delete holder;
}

JNIEXPORT jlong JNICALL Java_org_openpnp_vision_gpu_GpuNative_createBuffer(JNIEnv *env, jclass, jlong size,
        jboolean hostVisible) {
    try {
        return toHandle(Runtime::get()->createBuffer(size, hostVisible));
    }
    catch (const std::exception &e) {
        throwJava(env, e.what());
        return 0;
    }
}

JNIEXPORT jobject JNICALL Java_org_openpnp_vision_gpu_GpuNative_map(JNIEnv *env, jclass, jlong handle) {
    try {
        auto buffer = fromHandle<Buffer>(handle);
        if (buffer->mapped == nullptr) {
            throw std::runtime_error("buffer is not host visible");
        }
        return env->NewDirectByteBuffer(buffer->mapped, buffer->size);
    }
    catch (const std::exception &e) {
        throwJava(env, e.what());
        return nullptr;
    }
}

JNIEXPORT jlong JNICALL Java_org_openpnp_vision_gpu_GpuNative_createPipeline(JNIEnv *env, jclass,
        jstring shader, jintArray spec, jintArray bindings) {
    try {
        const char *chars = env->GetStringUTFChars(shader, nullptr);
        std::string name(chars);
        env->ReleaseStringUTFChars(shader, chars);
        std::vector<jint> specValues = ints(env, spec);
        std::vector<VkDescriptorType> types;
        for (jint uniform : ints(env, bindings)) {
            types.push_back(uniform ? VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER : VK_DESCRIPTOR_TYPE_STORAGE_BUFFER);
        }
        return toHandle(Runtime::get()->createPipeline(name,
                std::vector<int32_t>(specValues.begin(), specValues.end()), types));
    }
    catch (const std::exception &e) {
        throwJava(env, e.what());
        return 0;
    }
}

// Each step is a dispatch (pipelines[i] != 0) or a buffer copy. Dispatch buffers are flattened
// into buffers, bufferCounts[i] per step; a copy uses two buffers and copies[3 * i ..] as
// srcOffset, dstOffset, size. groups holds 3 counts per step, or an indirect buffer and offset.
JNIEXPORT jlong JNICALL Java_org_openpnp_vision_gpu_GpuNative_createProgram(JNIEnv *env, jclass,
        jlongArray pipelines, jlongArray buffers, jintArray bufferCounts, jintArray groups,
        jlongArray indirect, jlongArray offsets) {
    try {
        std::vector<jlong> pipelineHandles = longs(env, pipelines);
        std::vector<jlong> bufferHandles = longs(env, buffers);
        std::vector<jint> counts = ints(env, bufferCounts);
        std::vector<jint> groupCounts = ints(env, groups);
        std::vector<jlong> indirectHandles = longs(env, indirect);
        std::vector<jlong> offsetValues = longs(env, offsets);
        std::vector<gpu::Step> steps;
        size_t next = 0;
        for (size_t i = 0; i < pipelineHandles.size(); i++) {
            gpu::Step step;
            if (pipelineHandles[i] == 0) {
                step.copy = std::make_unique<gpu::Copy>();
                step.copy->src = fromHandle<Buffer>(bufferHandles.at(next++));
                step.copy->dst = fromHandle<Buffer>(bufferHandles.at(next++));
                step.copy->srcOffset = offsetValues.at(3 * i);
                step.copy->dstOffset = offsetValues.at(3 * i + 1);
                step.copy->size = offsetValues.at(3 * i + 2);
            }
            else {
                step.dispatch = std::make_unique<gpu::Dispatch>();
                step.dispatch->pipeline = fromHandle<Pipeline>(pipelineHandles[i]);
                for (jint b = 0; b < counts.at(i); b++) {
                    step.dispatch->buffers.push_back(fromHandle<Buffer>(bufferHandles.at(next++)));
                }
                if (indirectHandles.at(i) != 0) {
                    step.dispatch->indirect = fromHandle<Buffer>(indirectHandles[i]);
                    step.dispatch->indirectOffset = offsetValues.at(3 * i);
                }
                else {
                    for (int g = 0; g < 3; g++) {
                        step.dispatch->groups[g] = groupCounts.at(3 * i + g);
                    }
                }
            }
            steps.push_back(std::move(step));
        }
        return toHandle(Runtime::get()->createProgram(std::move(steps)));
    }
    catch (const std::exception &e) {
        throwJava(env, e.what());
        return 0;
    }
}

JNIEXPORT jlong JNICALL Java_org_openpnp_vision_gpu_GpuNative_submit(JNIEnv *env, jclass, jlong handle) {
    try {
        return Runtime::get()->submit(fromHandle<Program>(handle));
    }
    catch (const std::exception &e) {
        throwJava(env, e.what());
        return 0;
    }
}

}
