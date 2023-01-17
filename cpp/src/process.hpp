#pragma once

#include <opencv2/opencv.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/objdetect.hpp>
#include "./run.hpp"

namespace process {
    void processFrame(cv::Mat *frame, RunState *state);
    void processFrame(cv::cuda::GpuMat *frame, RunState *state);
}