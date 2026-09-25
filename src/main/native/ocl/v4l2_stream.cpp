#include "v4l2_stream.h"

#include <cerrno>
#include <cstring>
#include <stdexcept>

#include <dirent.h>
#include <fcntl.h>
#include <linux/videodev2.h>
#include <poll.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <unistd.h>

namespace {

const int BUFFER_COUNT = 4;

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
        int fd = open(path.c_str(), O_RDWR | O_NONBLOCK);
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
        close(fd);
    }
    closedir(dir);
    if (found < 0) {
        throw std::runtime_error("no V4L2 capture device with ID " + uniqueId);
    }
    return found;
}

void queue(V4l2Stream *stream, int index) {
    v4l2_buffer buf{};
    buf.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
    buf.memory = V4L2_MEMORY_MMAP;
    buf.index = index;
    if (xioctl(stream->fd, VIDIOC_QBUF, &buf) == -1) {
        fail("VIDIOC_QBUF");
    }
}

// Keeps only the newest ready frame so captures never return stale, queued-up images.
bool dequeueNewest(V4l2Stream *stream) {
    bool got = false;
    while (true) {
        v4l2_buffer buf{};
        buf.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
        buf.memory = V4L2_MEMORY_MMAP;
        if (xioctl(stream->fd, VIDIOC_DQBUF, &buf) == -1) {
            if (errno == EAGAIN) {
                return got;
            }
            fail("VIDIOC_DQBUF");
        }
        if ((buf.flags & V4L2_BUF_FLAG_ERROR) || buf.bytesused < stream->bytesPerLine * stream->height) {
            queue(stream, buf.index);
            continue;
        }
        if (stream->current >= 0) {
            queue(stream, stream->current);
        }
        stream->current = buf.index;
        got = true;
    }
}

}

V4l2Stream *v4l2Open(const std::string &uniqueId, int width, int height, int fps) {
    auto *stream = new V4l2Stream();
    try {
        stream->fd = openDevice(uniqueId);

        v4l2_format fmt{};
        fmt.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
        fmt.fmt.pix.width = width;
        fmt.fmt.pix.height = height;
        fmt.fmt.pix.pixelformat = V4L2_PIX_FMT_YUYV;
        fmt.fmt.pix.field = V4L2_FIELD_NONE;
        if (xioctl(stream->fd, VIDIOC_S_FMT, &fmt) == -1) {
            fail("VIDIOC_S_FMT");
        }
        if (fmt.fmt.pix.pixelformat != V4L2_PIX_FMT_YUYV || (int) fmt.fmt.pix.width != width
                || (int) fmt.fmt.pix.height != height) {
            throw std::runtime_error("device refused YUYV " + std::to_string(width) + "x"
                    + std::to_string(height));
        }
        stream->width = width;
        stream->height = height;
        stream->bytesPerLine = fmt.fmt.pix.bytesperline;

        if (fps > 0) {
            v4l2_streamparm parm{};
            parm.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
            parm.parm.capture.timeperframe.numerator = 1;
            parm.parm.capture.timeperframe.denominator = fps;
            if (xioctl(stream->fd, VIDIOC_S_PARM, &parm) == -1) {
                fail("VIDIOC_S_PARM");
            }
        }

        v4l2_requestbuffers req{};
        req.count = BUFFER_COUNT;
        req.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
        req.memory = V4L2_MEMORY_MMAP;
        if (xioctl(stream->fd, VIDIOC_REQBUFS, &req) == -1) {
            fail("VIDIOC_REQBUFS");
        }
        for (uint32_t i = 0; i < req.count; i++) {
            v4l2_buffer buf{};
            buf.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
            buf.memory = V4L2_MEMORY_MMAP;
            buf.index = i;
            if (xioctl(stream->fd, VIDIOC_QUERYBUF, &buf) == -1) {
                fail("VIDIOC_QUERYBUF");
            }
            void *data = mmap(nullptr, buf.length, PROT_READ | PROT_WRITE, MAP_SHARED, stream->fd,
                    buf.m.offset);
            if (data == MAP_FAILED) {
                fail("mmap");
            }
            stream->buffers.emplace_back(data, buf.length);
            queue(stream, i);
        }
        v4l2_buf_type type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
        if (xioctl(stream->fd, VIDIOC_STREAMON, &type) == -1) {
            fail("VIDIOC_STREAMON");
        }
        return stream;
    }
    catch (...) {
        v4l2Close(stream);
        throw;
    }
}

void v4l2Close(V4l2Stream *stream) {
    if (stream->fd >= 0) {
        v4l2_buf_type type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
        xioctl(stream->fd, VIDIOC_STREAMOFF, &type);
    }
    for (auto &buffer : stream->buffers) {
        munmap(buffer.first, buffer.second);
    }
    if (stream->fd >= 0) {
        close(stream->fd);
    }
    delete stream;
}

bool v4l2HasNewFrame(V4l2Stream *stream) {
    pollfd p{stream->fd, POLLIN, 0};
    return poll(&p, 1, 0) > 0 && (p.revents & POLLIN);
}

bool v4l2Grab(V4l2Stream *stream, int timeoutMs, cv::Mat &yuyv) {
    bool got = dequeueNewest(stream);
    if (!got && timeoutMs > 0) {
        pollfd p{stream->fd, POLLIN, 0};
        int r;
        do {
            r = poll(&p, 1, timeoutMs);
        } while (r == -1 && errno == EINTR);
        if (r == -1) {
            fail("poll");
        }
        got = r > 0 && dequeueNewest(stream);
    }
    if (got) {
        yuyv = cv::Mat(stream->height, stream->width, CV_8UC2, stream->buffers[stream->current].first,
                stream->bytesPerLine);
    }
    return got;
}

bool v4l2QueryControl(V4l2Stream *stream, uint32_t id, int32_t *min, int32_t *max, int32_t *def) {
    v4l2_queryctrl ctrl{};
    ctrl.id = id;
    if (xioctl(stream->fd, VIDIOC_QUERYCTRL, &ctrl) == -1 || (ctrl.flags & V4L2_CTRL_FLAG_DISABLED)) {
        return false;
    }
    *min = ctrl.minimum;
    *max = ctrl.maximum;
    *def = ctrl.default_value;
    return true;
}

int32_t v4l2GetControl(V4l2Stream *stream, uint32_t id) {
    v4l2_control ctrl{};
    ctrl.id = id;
    if (xioctl(stream->fd, VIDIOC_G_CTRL, &ctrl) == -1) {
        fail("VIDIOC_G_CTRL");
    }
    return ctrl.value;
}

void v4l2SetControl(V4l2Stream *stream, uint32_t id, int32_t value) {
    v4l2_control ctrl{};
    ctrl.id = id;
    ctrl.value = value;
    if (xioctl(stream->fd, VIDIOC_S_CTRL, &ctrl) == -1) {
        fail("VIDIOC_S_CTRL");
    }
}
