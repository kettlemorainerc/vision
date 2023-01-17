#pragma once

#include <Properties.h>
#include <chrono>
#include <opencv2/aruco.hpp>

#define team2077_print(arg) std::cout << arg << std::endl
#define team2077_printerr(arg) std::cerr << arg << std::endl
static const auto window_name = cv::String("Video");

struct RunState {
    cppproperties::Properties props;
    std::chrono::nanoseconds prev_start;
    int frame_count;
    cv::aruco::ArucoDetector detector;
};