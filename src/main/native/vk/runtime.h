#pragma once

#include <atomic>
#include <condition_variable>
#include <cstdint>
#include <functional>
#include <map>
#include <memory>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

#include "vk_api.h"

namespace gpu {

// Every GPU object records the last timeline value that used it; it is destroyed only once the
// GPU has passed that value.
struct Resource {
    std::atomic<uint64_t> lastUse{0};
    virtual ~Resource() = default;
};

struct Buffer : Resource {
    VkBuffer buffer = VK_NULL_HANDLE;
    VkDeviceMemory memory = VK_NULL_HANDLE;
    void *mapped = nullptr;
    VkDeviceSize size = 0;
    ~Buffer() override;
};

struct Pipeline : Resource {
    VkShaderModule module = VK_NULL_HANDLE;
    VkDescriptorSetLayout setLayout = VK_NULL_HANDLE;
    VkPipelineLayout layout = VK_NULL_HANDLE;
    VkPipeline pipeline = VK_NULL_HANDLE;
    std::vector<VkDescriptorType> bindings;
    ~Pipeline() override;
};

struct Dispatch {
    std::shared_ptr<Pipeline> pipeline;
    std::vector<std::shared_ptr<Buffer>> buffers;
    uint32_t groups[3] = {1, 1, 1};
    std::shared_ptr<Buffer> indirect;
    VkDeviceSize indirectOffset = 0;
};

struct Copy {
    std::shared_ptr<Buffer> src;
    std::shared_ptr<Buffer> dst;
    VkDeviceSize srcOffset = 0;
    VkDeviceSize dstOffset = 0;
    VkDeviceSize size = 0;
};

// A prerecorded command buffer; the steps run in order with full barriers between them.
struct Program : Resource {
    VkCommandBuffer cmd = VK_NULL_HANDLE;
    VkDescriptorPool pool = VK_NULL_HANDLE;
    std::vector<std::shared_ptr<Resource>> refs;
    ~Program() override;
};

struct Step {
    std::unique_ptr<Dispatch> dispatch;
    std::unique_ptr<Copy> copy;
};

class Runtime {
public:
    // Throws std::runtime_error when no usable device exists.
    static Runtime &init();
    static Runtime *get();

    std::string deviceName() const { return deviceName_; }
    bool int64() const { return int64_; }
    VkDevice device() const { return device_; }

    std::shared_ptr<Buffer> createBuffer(VkDeviceSize size, bool hostVisible);
    // Wraps a dma-buf without copying and takes ownership of fd; nullptr when that isn't possible.
    std::shared_ptr<Buffer> importDmaBuf(int fd, VkDeviceSize size);
    std::shared_ptr<Pipeline> createPipeline(const std::string &shader, const std::vector<int32_t> &spec,
            const std::vector<VkDescriptorType> &bindings);
    std::shared_ptr<Program> createProgram(std::vector<Step> steps);

    // Queues the program and returns the timeline value it signals.
    uint64_t submit(const std::shared_ptr<Program> &program);
    uint64_t completed();
    // Sleeps until the GPU passed value; false on timeout. Throws once the device failed.
    bool wait(uint64_t value, uint64_t timeoutNs);
    // Drops a reference once the GPU no longer uses the resource.
    void retire(std::shared_ptr<Resource> resource);
    // Runs action on the completion thread once the GPU passed value.
    void onComplete(uint64_t value, std::function<void()> action);
    // Calls handler on the submit thread whenever fd is readable.
    void watchFd(int fd, std::function<void()> handler);
    void unwatchFd(int fd);

private:
    Runtime() = default;
    void create();
    void submitLoop();
    void completionLoop();
    uint32_t memoryType(uint32_t allowed, VkMemoryPropertyFlags required, VkMemoryPropertyFlags preferred,
            int skip);

    VkInstance instance_ = VK_NULL_HANDLE;
    VkPhysicalDevice physical_ = VK_NULL_HANDLE;
    VkDevice device_ = VK_NULL_HANDLE;
    VkQueue queue_ = VK_NULL_HANDLE;
    uint32_t queueFamily_ = 0;
    VkPhysicalDeviceMemoryProperties memory_{};
    std::string deviceName_;
    VkSemaphore timeline_ = VK_NULL_HANDLE;
    std::atomic<bool> failed_{false};
    bool dmaBuf_ = false;
    bool int64_ = false;

    std::mutex recordLock_;
    VkCommandPool commandPool_ = VK_NULL_HANDLE;

    std::mutex submitLock_;
    uint64_t lastValue_ = 0;
    std::vector<std::pair<uint64_t, VkCommandBuffer>> pending_;
    int eventFd_ = -1;
    int epollFd_ = -1;
    std::mutex fdLock_;
    std::map<int, std::function<void()>> fdHandlers_;
    std::thread submitThread_;

    std::mutex actionLock_;
    std::condition_variable actionSignal_;
    std::multimap<uint64_t, std::function<void()>> actions_;
    std::thread completionThread_;
};

}
