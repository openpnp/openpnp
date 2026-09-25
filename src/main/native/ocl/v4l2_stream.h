#pragma once

#include <cstdint>
#include <string>
#include <vector>

#include <opencv2/core.hpp>

struct V4l2Stream {
    int fd = -1;
    int width = 0;
    int height = 0;
    size_t bytesPerLine = 0;
    std::vector<std::pair<void *, size_t>> buffers;
    int current = -1;
};

// All functions throw std::runtime_error on failure.
V4l2Stream *v4l2Open(const std::string &uniqueId, int width, int height, int fps);
void v4l2Close(V4l2Stream *stream);
bool v4l2HasNewFrame(V4l2Stream *stream);
// Takes the newest frame, waiting up to timeoutMs for one. The returned YUYV view stays valid
// until the next grab or close.
bool v4l2Grab(V4l2Stream *stream, int timeoutMs, cv::Mat &yuyv);
bool v4l2QueryControl(V4l2Stream *stream, uint32_t id, int32_t *min, int32_t *max, int32_t *def);
int32_t v4l2GetControl(V4l2Stream *stream, uint32_t id);
void v4l2SetControl(V4l2Stream *stream, uint32_t id, int32_t value);
