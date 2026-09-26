#include "runtime.h"

#include <dlfcn.h>
#include <sys/epoll.h>
#include <sys/eventfd.h>
#include <unistd.h>

#include <cerrno>
#include <cstring>
#include <stdexcept>

VkApi vk;

namespace gpu {

namespace {

struct ShaderSource {
    const char *name;
    const uint32_t *code;
    size_t words;
};

#include "shaders.inc"

Runtime *runtime = nullptr;
std::mutex initLock;

void check(VkResult result, const char *what) {
    if (result != VK_SUCCESS) {
        throw std::runtime_error(std::string(what) + " failed: VkResult " + std::to_string(result));
    }
}

int deviceRank(VkPhysicalDeviceType type) {
    switch (type) {
        case VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU:
            return 3;
        case VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU:
            return 2;
        case VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU:
            return 1;
        default:
            return 0;
    }
}

}

Buffer::~Buffer() {
    VkDevice device = runtime->device();
    vk.vkDestroyBuffer(device, buffer, nullptr);
    vk.vkFreeMemory(device, memory, nullptr);
}

Pipeline::~Pipeline() {
    VkDevice device = runtime->device();
    vk.vkDestroyPipeline(device, pipeline, nullptr);
    vk.vkDestroyPipelineLayout(device, layout, nullptr);
    vk.vkDestroyDescriptorSetLayout(device, setLayout, nullptr);
    vk.vkDestroyShaderModule(device, module, nullptr);
}

Program::~Program() {
    VkDevice device = runtime->device();
    vk.vkDestroyDescriptorPool(device, pool, nullptr);
}

Runtime &Runtime::init() {
    std::lock_guard<std::mutex> guard(initLock);
    if (runtime == nullptr) {
        // A failed init leaks its half-created state; it happens at most once per process.
        auto *created = new Runtime();
        created->create();
        runtime = created;
    }
    return *runtime;
}

Runtime *Runtime::get() {
    return runtime;
}

void Runtime::create() {
    void *lib = dlopen("libvulkan.so.1", RTLD_NOW | RTLD_LOCAL);
    if (lib == nullptr) {
        throw std::runtime_error("libvulkan.so.1 not found");
    }
    auto getInstanceProcAddr = reinterpret_cast<PFN_vkGetInstanceProcAddr>(dlsym(lib, "vkGetInstanceProcAddr"));
    if (getInstanceProcAddr == nullptr) {
        throw std::runtime_error("vkGetInstanceProcAddr not found");
    }
#define LOAD_GLOBAL(name) vk.name = reinterpret_cast<PFN_##name>(getInstanceProcAddr(nullptr, #name));
    VK_GLOBAL_FUNCS(LOAD_GLOBAL)
#undef LOAD_GLOBAL
    uint32_t loaderVersion = VK_API_VERSION_1_0;
    if (vk.vkEnumerateInstanceVersion != nullptr) {
        vk.vkEnumerateInstanceVersion(&loaderVersion);
    }
    if (loaderVersion < VK_API_VERSION_1_2) {
        throw std::runtime_error("Vulkan 1.2 loader required");
    }

    VkApplicationInfo app{VK_STRUCTURE_TYPE_APPLICATION_INFO};
    app.pApplicationName = "OpenPnP";
    app.apiVersion = VK_API_VERSION_1_2;
    VkInstanceCreateInfo instanceInfo{VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO};
    instanceInfo.pApplicationInfo = &app;
    check(vk.vkCreateInstance(&instanceInfo, nullptr, &instance_), "vkCreateInstance");
#define LOAD_INSTANCE(name) vk.name = reinterpret_cast<PFN_##name>(getInstanceProcAddr(instance_, #name));
    VK_INSTANCE_FUNCS(LOAD_INSTANCE)
#undef LOAD_INSTANCE

    uint32_t count = 0;
    vk.vkEnumeratePhysicalDevices(instance_, &count, nullptr);
    std::vector<VkPhysicalDevice> devices(count);
    vk.vkEnumeratePhysicalDevices(instance_, &count, devices.data());
    int bestRank = -1;
    for (VkPhysicalDevice candidate : devices) {
        VkPhysicalDeviceProperties props;
        vk.vkGetPhysicalDeviceProperties(candidate, &props);
        if (props.apiVersion < VK_API_VERSION_1_2) {
            continue;
        }
        VkPhysicalDeviceVulkan12Features features12{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES};
        VkPhysicalDeviceFeatures2 features{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2};
        features.pNext = &features12;
        vk.vkGetPhysicalDeviceFeatures2(candidate, &features);
        if (!features12.timelineSemaphore) {
            continue;
        }
        uint32_t families = 0;
        vk.vkGetPhysicalDeviceQueueFamilyProperties(candidate, &families, nullptr);
        std::vector<VkQueueFamilyProperties> familyProps(families);
        vk.vkGetPhysicalDeviceQueueFamilyProperties(candidate, &families, familyProps.data());
        for (uint32_t i = 0; i < families; i++) {
            if ((familyProps[i].queueFlags & VK_QUEUE_COMPUTE_BIT) && deviceRank(props.deviceType) > bestRank) {
                bestRank = deviceRank(props.deviceType);
                physical_ = candidate;
                queueFamily_ = i;
                deviceName_ = props.deviceName;
                break;
            }
        }
    }
    if (physical_ == VK_NULL_HANDLE) {
        throw std::runtime_error("no Vulkan 1.2 device with timeline semaphores");
    }
    vk.vkGetPhysicalDeviceMemoryProperties(physical_, &memory_);

    float priority = 1.0f;
    VkDeviceQueueCreateInfo queueInfo{VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO};
    queueInfo.queueFamilyIndex = queueFamily_;
    queueInfo.queueCount = 1;
    queueInfo.pQueuePriorities = &priority;
    VkPhysicalDeviceVulkan12Features enable12{VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES};
    enable12.timelineSemaphore = VK_TRUE;
    VkDeviceCreateInfo deviceInfo{VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO};
    deviceInfo.pNext = &enable12;
    deviceInfo.queueCreateInfoCount = 1;
    deviceInfo.pQueueCreateInfos = &queueInfo;
    check(vk.vkCreateDevice(physical_, &deviceInfo, nullptr, &device_), "vkCreateDevice");
#define LOAD_DEVICE(name) \
    vk.name = reinterpret_cast<PFN_##name>(vk.vkGetDeviceProcAddr(device_, #name)); \
    if (vk.name == nullptr) { \
        throw std::runtime_error(#name " not found"); \
    }
    VK_DEVICE_FUNCS(LOAD_DEVICE)
#undef LOAD_DEVICE
    vk.vkGetDeviceQueue(device_, queueFamily_, 0, &queue_);

    VkSemaphoreTypeCreateInfo typeInfo{VK_STRUCTURE_TYPE_SEMAPHORE_TYPE_CREATE_INFO};
    typeInfo.semaphoreType = VK_SEMAPHORE_TYPE_TIMELINE;
    VkSemaphoreCreateInfo semaphoreInfo{VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO};
    semaphoreInfo.pNext = &typeInfo;
    check(vk.vkCreateSemaphore(device_, &semaphoreInfo, nullptr, &timeline_), "vkCreateSemaphore");

    VkCommandPoolCreateInfo poolInfo{VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO};
    poolInfo.queueFamilyIndex = queueFamily_;
    check(vk.vkCreateCommandPool(device_, &poolInfo, nullptr, &commandPool_), "vkCreateCommandPool");

    eventFd_ = eventfd(0, EFD_NONBLOCK | EFD_CLOEXEC);
    epollFd_ = epoll_create1(EPOLL_CLOEXEC);
    if (eventFd_ < 0 || epollFd_ < 0) {
        throw std::runtime_error(std::string("eventfd/epoll: ") + strerror(errno));
    }
    epoll_event event{};
    event.events = EPOLLIN;
    event.data.fd = eventFd_;
    epoll_ctl(epollFd_, EPOLL_CTL_ADD, eventFd_, &event);

    submitThread_ = std::thread(&Runtime::submitLoop, this);
    submitThread_.detach();
    completionThread_ = std::thread(&Runtime::completionLoop, this);
    completionThread_.detach();
}

uint32_t Runtime::memoryType(uint32_t allowed, VkMemoryPropertyFlags required, VkMemoryPropertyFlags preferred,
        int skip) {
    for (int pass = 0; pass < 2; pass++) {
        VkMemoryPropertyFlags wanted = pass == 0 ? required | preferred : required;
        for (uint32_t i = 0; i < memory_.memoryTypeCount; i++) {
            if ((allowed & (1u << i)) && (memory_.memoryTypes[i].propertyFlags & wanted) == wanted
                    && skip-- == 0) {
                return i;
            }
        }
    }
    return UINT32_MAX;
}

std::shared_ptr<Buffer> Runtime::createBuffer(VkDeviceSize size, bool hostVisible) {
    auto buffer = std::make_shared<Buffer>();
    buffer->size = size;
    VkBufferCreateInfo info{VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO};
    info.size = size;
    info.usage = VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT
            | VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_SRC_BIT
            | VK_BUFFER_USAGE_TRANSFER_DST_BIT;
    check(vk.vkCreateBuffer(device_, &info, nullptr, &buffer->buffer), "vkCreateBuffer");
    VkMemoryRequirements req;
    vk.vkGetBufferMemoryRequirements(device_, buffer->buffer, &req);
    VkMemoryPropertyFlags required = hostVisible
            ? VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT : 0;
    // Discrete GPUs may only expose a small host-visible device-local heap, so fall back when it is full.
    VkResult result = VK_ERROR_OUT_OF_DEVICE_MEMORY;
    for (int skip = 0; result != VK_SUCCESS; skip++) {
        uint32_t type = memoryType(req.memoryTypeBits, required, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, skip);
        if (type == UINT32_MAX) {
            check(result, "vkAllocateMemory");
        }
        VkMemoryAllocateInfo alloc{VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO};
        alloc.allocationSize = req.size;
        alloc.memoryTypeIndex = type;
        result = vk.vkAllocateMemory(device_, &alloc, nullptr, &buffer->memory);
    }
    check(vk.vkBindBufferMemory(device_, buffer->buffer, buffer->memory, 0), "vkBindBufferMemory");
    if (hostVisible) {
        check(vk.vkMapMemory(device_, buffer->memory, 0, VK_WHOLE_SIZE, 0, &buffer->mapped), "vkMapMemory");
    }
    return buffer;
}

std::shared_ptr<Pipeline> Runtime::createPipeline(const std::string &shader, const std::vector<int32_t> &spec,
        const std::vector<VkDescriptorType> &bindings) {
    const ShaderSource *source = nullptr;
    for (const ShaderSource &s : shaderSources) {
        if (s.name != nullptr && shader == s.name) {
            source = &s;
        }
    }
    if (source == nullptr) {
        throw std::runtime_error("unknown shader " + shader);
    }
    auto pipeline = std::make_shared<Pipeline>();
    pipeline->bindings = bindings;
    VkShaderModuleCreateInfo moduleInfo{VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO};
    moduleInfo.codeSize = source->words * 4;
    moduleInfo.pCode = source->code;
    check(vk.vkCreateShaderModule(device_, &moduleInfo, nullptr, &pipeline->module), "vkCreateShaderModule");

    std::vector<VkDescriptorSetLayoutBinding> layoutBindings(bindings.size());
    for (size_t i = 0; i < bindings.size(); i++) {
        layoutBindings[i].binding = i;
        layoutBindings[i].descriptorType = bindings[i];
        layoutBindings[i].descriptorCount = 1;
        layoutBindings[i].stageFlags = VK_SHADER_STAGE_COMPUTE_BIT;
    }
    VkDescriptorSetLayoutCreateInfo setInfo{VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO};
    setInfo.bindingCount = layoutBindings.size();
    setInfo.pBindings = layoutBindings.data();
    check(vk.vkCreateDescriptorSetLayout(device_, &setInfo, nullptr, &pipeline->setLayout),
            "vkCreateDescriptorSetLayout");
    VkPipelineLayoutCreateInfo layoutInfo{VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO};
    layoutInfo.setLayoutCount = 1;
    layoutInfo.pSetLayouts = &pipeline->setLayout;
    check(vk.vkCreatePipelineLayout(device_, &layoutInfo, nullptr, &pipeline->layout), "vkCreatePipelineLayout");

    std::vector<VkSpecializationMapEntry> entries(spec.size());
    for (size_t i = 0; i < spec.size(); i++) {
        entries[i].constantID = i;
        entries[i].offset = i * sizeof(int32_t);
        entries[i].size = sizeof(int32_t);
    }
    VkSpecializationInfo specInfo{};
    specInfo.mapEntryCount = entries.size();
    specInfo.pMapEntries = entries.data();
    specInfo.dataSize = spec.size() * sizeof(int32_t);
    specInfo.pData = spec.data();
    VkComputePipelineCreateInfo info{VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO};
    info.stage.sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
    info.stage.stage = VK_SHADER_STAGE_COMPUTE_BIT;
    info.stage.module = pipeline->module;
    info.stage.pName = "main";
    info.stage.pSpecializationInfo = spec.empty() ? nullptr : &specInfo;
    info.layout = pipeline->layout;
    check(vk.vkCreateComputePipelines(device_, VK_NULL_HANDLE, 1, &info, nullptr, &pipeline->pipeline),
            "vkCreateComputePipelines");
    return pipeline;
}

std::shared_ptr<Program> Runtime::createProgram(std::vector<Step> steps) {
    auto program = std::make_shared<Program>();
    uint32_t sets = 0;
    uint32_t uniforms = 0;
    uint32_t storages = 0;
    for (const Step &step : steps) {
        if (step.dispatch) {
            const Dispatch &d = *step.dispatch;
            if (d.buffers.size() != d.pipeline->bindings.size()) {
                throw std::runtime_error("dispatch buffer count does not match the pipeline bindings");
            }
            sets++;
            for (VkDescriptorType type : d.pipeline->bindings) {
                (type == VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER ? uniforms : storages)++;
            }
        }
    }
    std::vector<VkDescriptorPoolSize> sizes;
    if (uniforms > 0) {
        sizes.push_back({VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, uniforms});
    }
    if (storages > 0) {
        sizes.push_back({VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, storages});
    }

    std::lock_guard<std::mutex> guard(recordLock_);
    if (sets > 0) {
        VkDescriptorPoolCreateInfo poolInfo{VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO};
        poolInfo.maxSets = sets;
        poolInfo.poolSizeCount = sizes.size();
        poolInfo.pPoolSizes = sizes.data();
        check(vk.vkCreateDescriptorPool(device_, &poolInfo, nullptr, &program->pool), "vkCreateDescriptorPool");
    }
    VkCommandBufferAllocateInfo allocInfo{VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO};
    allocInfo.commandPool = commandPool_;
    allocInfo.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
    allocInfo.commandBufferCount = 1;
    check(vk.vkAllocateCommandBuffers(device_, &allocInfo, &program->cmd), "vkAllocateCommandBuffers");
    VkCommandBuffer cmd = program->cmd;
    VkCommandPool pool = commandPool_;
    std::mutex *lock = &recordLock_;
    // The command buffer goes back to the pool with the program; freeing needs the pool lock.
    auto freeCmd = std::shared_ptr<Resource>(new Resource(), [this, cmd, pool, lock](Resource *r) {
        std::lock_guard<std::mutex> g(*lock);
        vk.vkFreeCommandBuffers(device_, pool, 1, &cmd);
        delete r;
    });
    program->refs.push_back(freeCmd);

    VkCommandBufferBeginInfo begin{VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO};
    check(vk.vkBeginCommandBuffer(cmd, &begin), "vkBeginCommandBuffer");
    VkMemoryBarrier barrier{VK_STRUCTURE_TYPE_MEMORY_BARRIER};
    barrier.srcAccessMask = VK_ACCESS_SHADER_WRITE_BIT | VK_ACCESS_TRANSFER_WRITE_BIT;
    barrier.dstAccessMask = VK_ACCESS_SHADER_READ_BIT | VK_ACCESS_SHADER_WRITE_BIT | VK_ACCESS_UNIFORM_READ_BIT
            | VK_ACCESS_INDIRECT_COMMAND_READ_BIT | VK_ACCESS_TRANSFER_READ_BIT | VK_ACCESS_TRANSFER_WRITE_BIT;
    VkPipelineStageFlags stages = VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT | VK_PIPELINE_STAGE_TRANSFER_BIT;
    for (size_t s = 0; s < steps.size(); s++) {
        if (s > 0) {
            vk.vkCmdPipelineBarrier(cmd, stages, stages | VK_PIPELINE_STAGE_DRAW_INDIRECT_BIT, 0, 1, &barrier, 0,
                    nullptr, 0, nullptr);
        }
        if (steps[s].copy) {
            const Copy &c = *steps[s].copy;
            VkBufferCopy region{c.srcOffset, c.dstOffset, c.size};
            vk.vkCmdCopyBuffer(cmd, c.src->buffer, c.dst->buffer, 1, &region);
            program->refs.push_back(c.src);
            program->refs.push_back(c.dst);
            continue;
        }
        const Dispatch &d = *steps[s].dispatch;
        VkDescriptorSet set;
        VkDescriptorSetAllocateInfo setInfo{VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO};
        setInfo.descriptorPool = program->pool;
        setInfo.descriptorSetCount = 1;
        setInfo.pSetLayouts = &d.pipeline->setLayout;
        check(vk.vkAllocateDescriptorSets(device_, &setInfo, &set), "vkAllocateDescriptorSets");
        std::vector<VkDescriptorBufferInfo> infos(d.buffers.size());
        std::vector<VkWriteDescriptorSet> writes(d.buffers.size());
        for (size_t i = 0; i < d.buffers.size(); i++) {
            infos[i] = {d.buffers[i]->buffer, 0, VK_WHOLE_SIZE};
            writes[i] = {VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET};
            writes[i].dstSet = set;
            writes[i].dstBinding = i;
            writes[i].descriptorCount = 1;
            writes[i].descriptorType = d.pipeline->bindings[i];
            writes[i].pBufferInfo = &infos[i];
            program->refs.push_back(d.buffers[i]);
        }
        vk.vkUpdateDescriptorSets(device_, writes.size(), writes.data(), 0, nullptr);
        vk.vkCmdBindPipeline(cmd, VK_PIPELINE_BIND_POINT_COMPUTE, d.pipeline->pipeline);
        vk.vkCmdBindDescriptorSets(cmd, VK_PIPELINE_BIND_POINT_COMPUTE, d.pipeline->layout, 0, 1, &set, 0,
                nullptr);
        if (d.indirect) {
            vk.vkCmdDispatchIndirect(cmd, d.indirect->buffer, d.indirectOffset);
            program->refs.push_back(d.indirect);
        }
        else {
            vk.vkCmdDispatch(cmd, d.groups[0], d.groups[1], d.groups[2]);
        }
        program->refs.push_back(d.pipeline);
    }
    VkMemoryBarrier toHost{VK_STRUCTURE_TYPE_MEMORY_BARRIER};
    toHost.srcAccessMask = VK_ACCESS_SHADER_WRITE_BIT | VK_ACCESS_TRANSFER_WRITE_BIT;
    toHost.dstAccessMask = VK_ACCESS_HOST_READ_BIT;
    vk.vkCmdPipelineBarrier(cmd, stages, VK_PIPELINE_STAGE_HOST_BIT, 0, 1, &toHost, 0, nullptr, 0, nullptr);
    check(vk.vkEndCommandBuffer(cmd), "vkEndCommandBuffer");
    return program;
}

uint64_t Runtime::submit(const std::shared_ptr<Program> &program) {
    // A command buffer may not be pending twice.
    uint64_t previous = program->lastUse;
    if (previous > 0) {
        wait(previous, UINT64_MAX);
    }
    uint64_t value;
    {
        std::lock_guard<std::mutex> guard(submitLock_);
        value = ++lastValue_;
        program->lastUse = value;
        for (auto &ref : program->refs) {
            ref->lastUse = value;
        }
        pending_.emplace_back(value, program->cmd);
    }
    onComplete(value, [program] {});
    uint64_t one = 1;
    if (write(eventFd_, &one, sizeof(one)) < 0) {
        throw std::runtime_error(std::string("eventfd write: ") + strerror(errno));
    }
    return value;
}

uint64_t Runtime::completed() {
    uint64_t value = 0;
    vk.vkGetSemaphoreCounterValue(device_, timeline_, &value);
    return value;
}

bool Runtime::wait(uint64_t value, uint64_t timeoutNs) {
    VkSemaphoreWaitInfo info{VK_STRUCTURE_TYPE_SEMAPHORE_WAIT_INFO};
    info.semaphoreCount = 1;
    info.pSemaphores = &timeline_;
    info.pValues = &value;
    VkResult result = vk.vkWaitSemaphores(device_, &info, timeoutNs);
    if (failed_) {
        throw std::runtime_error("GPU device failed");
    }
    if (result == VK_TIMEOUT) {
        return false;
    }
    check(result, "vkWaitSemaphores");
    return true;
}

void Runtime::retire(std::shared_ptr<Resource> resource) {
    uint64_t value = resource->lastUse;
    if (value <= completed()) {
        return;
    }
    onComplete(value, [resource] {});
}

void Runtime::onComplete(uint64_t value, std::function<void()> action) {
    {
        std::lock_guard<std::mutex> guard(actionLock_);
        actions_.emplace(value, std::move(action));
    }
    actionSignal_.notify_one();
}

void Runtime::watchFd(int fd, std::function<void()> handler) {
    {
        std::lock_guard<std::mutex> guard(fdLock_);
        fdHandlers_[fd] = std::move(handler);
    }
    epoll_event event{};
    event.events = EPOLLIN;
    event.data.fd = fd;
    if (epoll_ctl(epollFd_, EPOLL_CTL_ADD, fd, &event) < 0) {
        throw std::runtime_error(std::string("epoll_ctl: ") + strerror(errno));
    }
}

void Runtime::unwatchFd(int fd) {
    epoll_ctl(epollFd_, EPOLL_CTL_DEL, fd, nullptr);
    std::lock_guard<std::mutex> guard(fdLock_);
    fdHandlers_.erase(fd);
}

void Runtime::submitLoop() {
    std::vector<std::pair<uint64_t, VkCommandBuffer>> batch;
    epoll_event events[16];
    while (true) {
        int n = epoll_wait(epollFd_, events, 16, -1);
        for (int i = 0; i < n; i++) {
            int fd = events[i].data.fd;
            if (fd == eventFd_) {
                uint64_t ignored;
                while (read(eventFd_, &ignored, sizeof(ignored)) > 0) {
                }
                continue;
            }
            std::function<void()> handler;
            {
                std::lock_guard<std::mutex> guard(fdLock_);
                auto it = fdHandlers_.find(fd);
                if (it != fdHandlers_.end()) {
                    handler = it->second;
                }
            }
            if (handler) {
                try {
                    handler();
                }
                catch (const std::exception &) {
                    // Handlers report their own failures; one must not stop GPU submission.
                }
            }
        }
        {
            std::lock_guard<std::mutex> guard(submitLock_);
            batch.swap(pending_);
        }
        for (auto &job : batch) {
            VkTimelineSemaphoreSubmitInfo timelineInfo{VK_STRUCTURE_TYPE_TIMELINE_SEMAPHORE_SUBMIT_INFO};
            timelineInfo.signalSemaphoreValueCount = 1;
            timelineInfo.pSignalSemaphoreValues = &job.first;
            VkSubmitInfo info{VK_STRUCTURE_TYPE_SUBMIT_INFO};
            info.pNext = &timelineInfo;
            info.commandBufferCount = 1;
            info.pCommandBuffers = &job.second;
            info.signalSemaphoreCount = 1;
            info.pSignalSemaphores = &timeline_;
            if (failed_ || vk.vkQueueSubmit(queue_, 1, &info, VK_NULL_HANDLE) != VK_SUCCESS) {
                // Waiters must not hang on a value the GPU will never signal.
                failed_ = true;
                VkSemaphoreSignalInfo signal{VK_STRUCTURE_TYPE_SEMAPHORE_SIGNAL_INFO};
                signal.semaphore = timeline_;
                signal.value = job.first;
                vk.vkSignalSemaphore(device_, &signal);
            }
        }
        batch.clear();
    }
}

void Runtime::completionLoop() {
    std::vector<std::function<void()>> ready;
    while (true) {
        uint64_t target;
        {
            std::unique_lock<std::mutex> lock(actionLock_);
            actionSignal_.wait(lock, [this] { return !actions_.empty(); });
            target = actions_.begin()->first;
        }
        VkSemaphoreWaitInfo info{VK_STRUCTURE_TYPE_SEMAPHORE_WAIT_INFO};
        info.semaphoreCount = 1;
        info.pSemaphores = &timeline_;
        info.pValues = &target;
        vk.vkWaitSemaphores(device_, &info, UINT64_MAX);
        uint64_t done = completed();
        {
            std::lock_guard<std::mutex> guard(actionLock_);
            auto end = actions_.upper_bound(done);
            for (auto it = actions_.begin(); it != end; ++it) {
                ready.push_back(std::move(it->second));
            }
            actions_.erase(actions_.begin(), end);
        }
        for (auto &action : ready) {
            try {
                action();
            }
            catch (const std::exception &) {
            }
        }
        ready.clear();
    }
}

}
