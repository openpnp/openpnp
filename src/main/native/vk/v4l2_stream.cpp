#include "v4l2_stream.h"

#include <cerrno>
#include <cstring>
#include <stdexcept>

#include <dirent.h>
#include <fcntl.h>
#include <linux/videodev2.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <unistd.h>

namespace {

// One frame held for the newest, a couple in flight on the GPU, the rest queued for the driver.
const int BUFFER_COUNT = 6;

int xioctl(int fd, unsigned long request, void *arg) {
    int r;
    do {
        r = ioctl(fd, request, arg);
    } while (r == -1 && errno == EINTR);
    return r;
}

[[noreturn]] void fail(const std::string &what) {
    throw std::runtime_error(what + ": " + strerror(errno));
}

// Same identifier openpnp-capture uses, so stored camera configurations keep matching.
std::string uniqueIdOf(const v4l2_capability &cap) {
    return std::string(reinterpret_cast<const char *>(cap.card)) + " "
            + reinterpret_cast<const char *>(cap.bus_info);
}

int openDevice(const std::string &uniqueId) {
    DIR *dir = opendir("/dev");
    if (dir == nullptr) {
        fail("opendir /dev");
    }
    int found = -1;
    while (dirent *entry = readdir(dir)) {
        if (strncmp(entry->d_name, "video", 5) != 0) {
            continue;
        }
        std::string path = std::string("/dev/") + entry->d_name;
        int fd = ::open(path.c_str(), O_RDWR | O_NONBLOCK | O_CLOEXEC);
        if (fd < 0) {
            continue;
        }
        v4l2_capability cap{};
        uint32_t required = V4L2_CAP_VIDEO_CAPTURE | V4L2_CAP_STREAMING;
        if (xioctl(fd, VIDIOC_QUERYCAP, &cap) == 0 && (cap.device_caps & required) == required
                && uniqueIdOf(cap) == uniqueId) {
            found = fd;
            break;
        }
        ::close(fd);
    }
    closedir(dir);
    if (found < 0) {
        throw std::runtime_error("no V4L2 capture device with ID " + uniqueId);
    }
    return found;
}

gpu::Runtime &runtime() {
    gpu::Runtime *rt = gpu::Runtime::get();
    if (rt == nullptr) {
        throw std::runtime_error("GPU runtime not initialized");
    }
    return *rt;
}

}

std::shared_ptr<V4l2Stream> V4l2Stream::open(const std::string &uniqueId, int width, int height, int fps) {
    gpu::Runtime &rt = runtime();
    auto stream = std::shared_ptr<V4l2Stream>(new V4l2Stream());
    try {
        stream->fd_ = openDevice(uniqueId);
        int fd = stream->fd_;

        v4l2_format fmt{};
        fmt.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
        fmt.fmt.pix.width = width;
        fmt.fmt.pix.height = height;
        fmt.fmt.pix.pixelformat = V4L2_PIX_FMT_YUYV;
        fmt.fmt.pix.field = V4L2_FIELD_NONE;
        if (xioctl(fd, VIDIOC_S_FMT, &fmt) == -1) {
            fail("VIDIOC_S_FMT");
        }
        if (fmt.fmt.pix.pixelformat != V4L2_PIX_FMT_YUYV || (int) fmt.fmt.pix.width != width
                || (int) fmt.fmt.pix.height != height || fmt.fmt.pix.bytesperline % 4 != 0) {
            throw std::runtime_error("device refused YUYV " + std::to_string(width) + "x" + std::to_string(height));
        }
        stream->width_ = width;
        stream->height_ = height;
        stream->bytesPerLine_ = fmt.fmt.pix.bytesperline;

        if (fps > 0) {
            v4l2_streamparm parm{};
            parm.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
            parm.parm.capture.timeperframe.numerator = 1;
            parm.parm.capture.timeperframe.denominator = fps;
            if (xioctl(fd, VIDIOC_S_PARM, &parm) == -1) {
                fail("VIDIOC_S_PARM");
            }
        }

        v4l2_requestbuffers req{};
        req.count = BUFFER_COUNT;
        req.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
        req.memory = V4L2_MEMORY_MMAP;
        if (xioctl(fd, VIDIOC_REQBUFS, &req) == -1) {
            fail("VIDIOC_REQBUFS");
        }
        stream->slots_.resize(req.count);
        stream->zeroCopy_ = true;
        for (uint32_t i = 0; i < req.count; i++) {
            Slot &slot = stream->slots_[i];
            v4l2_buffer buf{};
            buf.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
            buf.memory = V4L2_MEMORY_MMAP;
            buf.index = i;
            if (xioctl(fd, VIDIOC_QUERYBUF, &buf) == -1) {
                fail("VIDIOC_QUERYBUF");
            }
            slot.length = buf.length;
            slot.data = mmap(nullptr, buf.length, PROT_READ | PROT_WRITE, MAP_SHARED, fd, buf.m.offset);
            if (slot.data == MAP_FAILED) {
                slot.data = nullptr;
                fail("mmap");
            }
            v4l2_exportbuffer exp{};
            exp.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
            exp.index = i;
            exp.flags = O_RDONLY | O_CLOEXEC;
            if (stream->zeroCopy_ && xioctl(fd, VIDIOC_EXPBUF, &exp) == 0) {
                slot.gpu = rt.importDmaBuf(exp.fd, buf.length);
            }
            stream->zeroCopy_ = stream->zeroCopy_ && slot.gpu;
        }
        if (!stream->zeroCopy_) {
            stream->useStagingBuffers();
        }
        for (uint32_t i = 0; i < req.count; i++) {
            stream->queue(i);
        }
        v4l2_buf_type type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
        if (xioctl(fd, VIDIOC_STREAMON, &type) == -1) {
            fail("VIDIOC_STREAMON");
        }
        std::weak_ptr<V4l2Stream> weak = stream;
        rt.watchFd(fd, [weak] {
            if (auto self = weak.lock()) {
                self->onReadable();
            }
        });
        return stream;
    }
    catch (...) {
        stream->close();
        throw;
    }
}

void V4l2Stream::close() {
    if (fd_ >= 0) {
        runtime().unwatchFd(fd_);
    }
    std::lock_guard<std::mutex> guard(lock_);
    if (closed_) {
        return;
    }
    closed_ = true;
    if (fd_ >= 0) {
        v4l2_buf_type type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
        xioctl(fd_, VIDIOC_STREAMOFF, &type);
    }
    // Imported GPU buffers keep their dma-buf alive on their own.
    for (Slot &slot : slots_) {
        if (slot.data != nullptr) {
            munmap(slot.data, slot.length);
            slot.data = nullptr;
        }
    }
    if (fd_ >= 0) {
        ::close(fd_);
        fd_ = -1;
    }
    frameArrived_.notify_all();
}

void V4l2Stream::useStagingBuffers() {
    zeroCopy_ = false;
    for (Slot &slot : slots_) {
        slot.gpu = runtime().createBuffer(slot.length, true);
        slot.copied = false;
    }
}

void V4l2Stream::queue(int index) {
    v4l2_buffer buf{};
    buf.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
    buf.memory = V4L2_MEMORY_MMAP;
    buf.index = index;
    if (xioctl(fd_, VIDIOC_QBUF, &buf) == -1) {
        fail("VIDIOC_QBUF");
    }
    slots_[index].queued = true;
}

void V4l2Stream::maybeRequeue(int index) {
    Slot &slot = slots_[index];
    if (closed_ || slot.users > 0 || index == newest_ || slot.queued) {
        return;
    }
    gpu::Runtime &rt = runtime();
    if (slot.lastGpuUse > rt.completed()) {
        std::weak_ptr<V4l2Stream> weak = shared_from_this();
        rt.onComplete(slot.lastGpuUse, [weak, index] {
            if (auto self = weak.lock()) {
                std::lock_guard<std::mutex> guard(self->lock_);
                self->maybeRequeue(index);
            }
        });
        return;
    }
    queue(index);
}

void V4l2Stream::onReadable() {
    std::lock_guard<std::mutex> guard(lock_);
    if (closed_) {
        return;
    }
    bool arrived = false;
    while (true) {
        v4l2_buffer buf{};
        buf.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
        buf.memory = V4L2_MEMORY_MMAP;
        if (xioctl(fd_, VIDIOC_DQBUF, &buf) == -1) {
            if (errno != EAGAIN) {
                // Unplugged or broken; stop polling instead of spinning on the error.
                runtime().unwatchFd(fd_);
                closed_ = true;
                arrived = true;
            }
            break;
        }
        Slot &slot = slots_[buf.index];
        slot.queued = false;
        if ((buf.flags & V4L2_BUF_FLAG_ERROR) || buf.bytesused < (uint32_t) bytesPerLine_ * height_) {
            queue(buf.index);
            continue;
        }
        slot.sequence = ++frames_;
        monotonic_ = (buf.flags & V4L2_BUF_FLAG_TIMESTAMP_MASK) == V4L2_BUF_FLAG_TIMESTAMP_MONOTONIC;
        slot.timestampNs = (int64_t) buf.timestamp.tv_sec * 1000000000 + (int64_t) buf.timestamp.tv_usec * 1000;
        slot.copied = false;
        int previous = newest_;
        newest_ = buf.index;
        if (previous >= 0 && previous != newest_) {
            maybeRequeue(previous);
        }
        arrived = true;
    }
    if (arrived) {
        frameArrived_.notify_all();
    }
}

bool V4l2Stream::hasNewFrame(uint64_t after) {
    std::lock_guard<std::mutex> guard(lock_);
    return newest_ >= 0 && slots_[newest_].sequence > after;
}

int V4l2Stream::acquire(uint64_t after, int64_t notBeforeNs, int timeoutMs, uint64_t *sequence,
        int64_t *timestampNs) {
    int index;
    {
        std::unique_lock<std::mutex> lock(lock_);
        bool ready = frameArrived_.wait_for(lock, std::chrono::milliseconds(timeoutMs), [&] {
            return closed_ || (newest_ >= 0 && slots_[newest_].sequence > after
                    && (!monotonic_ || slots_[newest_].timestampNs >= notBeforeNs));
        });
        if (closed_) {
            throw std::runtime_error("V4L2 stream closed");
        }
        if (!ready) {
            return -1;
        }
        index = newest_;
        Slot &slot = slots_[index];
        slot.users++;
        *sequence = slot.sequence;
        *timestampNs = slot.timestampNs;
    }
    // Never waits on the GPU while holding lock_: the submit thread needs it to dequeue frames.
    std::lock_guard<std::mutex> guard(validateLock_);
    try {
        if (!validated_) {
            validateZeroCopy(index);
            validated_ = true;
        }
        Slot &slot = slots_[index];
        if (!zeroCopy_ && !slot.copied) {
            memcpy(slot.gpu->mapped, slot.data, (size_t) bytesPerLine_ * height_);
            slot.copied = true;
        }
    }
    catch (...) {
        release(index, 0);
        throw;
    }
    return index;
}

// Some drivers export buffers the GPU doesn't see coherently; compare one frame before trusting it.
void V4l2Stream::validateZeroCopy(int index) {
    if (!zeroCopy_) {
        return;
    }
    gpu::Runtime &rt = runtime();
    Slot &slot = slots_[index];
    size_t size = (size_t) bytesPerLine_ * height_;
    auto readback = rt.createBuffer(size, true);
    std::vector<gpu::Step> steps(1);
    steps[0].copy = std::make_unique<gpu::Copy>();
    steps[0].copy->src = slot.gpu;
    steps[0].copy->dst = readback;
    steps[0].copy->size = size;
    auto program = rt.createProgram(std::move(steps));
    uint64_t value = rt.submit(program);
    rt.wait(value, UINT64_MAX);
    if (memcmp(readback->mapped, slot.data, size) != 0) {
        useStagingBuffers();
    }
}

std::shared_ptr<gpu::Buffer> V4l2Stream::slotBuffer(int index) {
    std::lock_guard<std::mutex> guard(lock_);
    return slots_.at(index).gpu;
}

void V4l2Stream::release(int index, uint64_t gpuValue) {
    std::lock_guard<std::mutex> guard(lock_);
    Slot &slot = slots_.at(index);
    slot.users--;
    if (gpuValue > slot.lastGpuUse) {
        slot.lastGpuUse = gpuValue;
    }
    maybeRequeue(index);
}

bool V4l2Stream::queryControl(uint32_t id, int32_t *min, int32_t *max, int32_t *def) {
    v4l2_queryctrl ctrl{};
    ctrl.id = id;
    if (xioctl(fd_, VIDIOC_QUERYCTRL, &ctrl) == -1 || (ctrl.flags & V4L2_CTRL_FLAG_DISABLED)) {
        return false;
    }
    *min = ctrl.minimum;
    *max = ctrl.maximum;
    *def = ctrl.default_value;
    return true;
}

int32_t V4l2Stream::getControl(uint32_t id) {
    v4l2_control ctrl{};
    ctrl.id = id;
    if (xioctl(fd_, VIDIOC_G_CTRL, &ctrl) == -1) {
        fail("VIDIOC_G_CTRL");
    }
    return ctrl.value;
}

void V4l2Stream::setControl(uint32_t id, int32_t value) {
    v4l2_control ctrl{};
    ctrl.id = id;
    ctrl.value = value;
    if (xioctl(fd_, VIDIOC_S_CTRL, &ctrl) == -1) {
        fail("VIDIOC_S_CTRL");
    }
}
