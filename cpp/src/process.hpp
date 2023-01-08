#pragma once

#include <opencv2/opencv.hpp>
#include "./run.hpp"

namespace process {
    void processFrame(cv::Mat *frame, RunState *state);
}