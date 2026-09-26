#pragma once

#define VK_NO_PROTOTYPES
#include <vulkan/vulkan.h>

#define VK_GLOBAL_FUNCS(X) \
    X(vkCreateInstance) \
    X(vkEnumerateInstanceVersion)

#define VK_INSTANCE_FUNCS(X) \
    X(vkDestroyInstance) \
    X(vkEnumeratePhysicalDevices) \
    X(vkGetPhysicalDeviceProperties) \
    X(vkGetPhysicalDeviceQueueFamilyProperties) \
    X(vkGetPhysicalDeviceMemoryProperties) \
    X(vkGetPhysicalDeviceFeatures2) \
    X(vkEnumerateDeviceExtensionProperties) \
    X(vkCreateDevice) \
    X(vkGetDeviceProcAddr)

#define VK_DEVICE_FUNCS(X) \
    X(vkDestroyDevice) \
    X(vkGetDeviceQueue) \
    X(vkQueueSubmit) \
    X(vkCreateSemaphore) \
    X(vkDestroySemaphore) \
    X(vkWaitSemaphores) \
    X(vkSignalSemaphore) \
    X(vkGetSemaphoreCounterValue) \
    X(vkCreateBuffer) \
    X(vkDestroyBuffer) \
    X(vkGetBufferMemoryRequirements) \
    X(vkAllocateMemory) \
    X(vkFreeMemory) \
    X(vkBindBufferMemory) \
    X(vkMapMemory) \
    X(vkCreateShaderModule) \
    X(vkDestroyShaderModule) \
    X(vkCreateDescriptorSetLayout) \
    X(vkDestroyDescriptorSetLayout) \
    X(vkCreatePipelineLayout) \
    X(vkDestroyPipelineLayout) \
    X(vkCreateComputePipelines) \
    X(vkDestroyPipeline) \
    X(vkCreateDescriptorPool) \
    X(vkDestroyDescriptorPool) \
    X(vkAllocateDescriptorSets) \
    X(vkUpdateDescriptorSets) \
    X(vkCreateCommandPool) \
    X(vkDestroyCommandPool) \
    X(vkAllocateCommandBuffers) \
    X(vkFreeCommandBuffers) \
    X(vkBeginCommandBuffer) \
    X(vkEndCommandBuffer) \
    X(vkCmdBindPipeline) \
    X(vkCmdBindDescriptorSets) \
    X(vkCmdDispatch) \
    X(vkCmdDispatchIndirect) \
    X(vkCmdPipelineBarrier) \
    X(vkCmdCopyBuffer)

// Present only when the device supports importing dma-bufs.
#define VK_DMABUF_FUNCS(X) \
    X(vkGetMemoryFdPropertiesKHR)

struct VkApi {
#define VK_API_MEMBER(name) PFN_##name name = nullptr;
    VK_GLOBAL_FUNCS(VK_API_MEMBER)
    VK_INSTANCE_FUNCS(VK_API_MEMBER)
    VK_DEVICE_FUNCS(VK_API_MEMBER)
    VK_DMABUF_FUNCS(VK_API_MEMBER)
#undef VK_API_MEMBER
};

extern VkApi vk;
