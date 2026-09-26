#pragma once

#include <condition_variable>
#include <cstdint>
#include <memory>
#include <mutex>
#include <string>
#include <vector>

#include "runtime.h"

// Streams YUYV frames from a V4L2 device into GPU buffers. The submit thread dequeues frames as
// they arrive and keeps only the newest; a buffer goes back to the driver once no consumer holds
// it and the GPU has finished reading it.
class V4l2Stream : public std::enable_shared_from_this<V4l2Stream> {
public:
    // All functions throw std::runtime_error on failure.
    static std::shared_ptr<V4l2Stream> open(const std::string &uniqueId, int width, int height, int fps);
    void close();

    int width() const { return width_; }
    int height() const { return height_; }
    int bytesPerLine() const { return bytesPerLine_; }
    bool zeroCopy() const { return zeroCopy_; }

    bool hasNewFrame(uint64_t after);
    // Holds the newest frame with a sequence number above after, waiting up to timeoutMs for one.
    // Returns its slot, or -1 on timeout.
    int acquire(uint64_t after, int timeoutMs, uint64_t *sequence, int64_t *timestampNs);
    std::shared_ptr<gpu::Buffer> slotBuffer(int slot);
    // Gives a slot back once the GPU has passed gpuValue.
    void release(int slot, uint64_t gpuValue);

    bool queryControl(uint32_t id, int32_t *min, int32_t *max, int32_t *def);
    int32_t getControl(uint32_t id);
    void setControl(uint32_t id, int32_t value);

private:
    struct Slot {
        void *data = nullptr;
        size_t length = 0;
        std::shared_ptr<gpu::Buffer> gpu;
        uint64_t sequence = 0;
        int64_t timestampNs = 0;
        uint64_t lastGpuUse = 0;
        int users = 0;
        bool queued = false;
        bool copied = false;
    };

    void onReadable();
    void queue(int slot);
    void maybeRequeue(int slot);
    void validateZeroCopy(int slot);
    void useStagingBuffers();

    int fd_ = -1;
    int width_ = 0;
    int height_ = 0;
    int bytesPerLine_ = 0;
    bool zeroCopy_ = false;
    bool validated_ = false;
    bool closed_ = false;
    std::vector<Slot> slots_;
    int newest_ = -1;
    uint64_t frames_ = 0;
    std::mutex lock_;
    std::condition_variable frameArrived_;
    std::mutex validateLock_;
};
