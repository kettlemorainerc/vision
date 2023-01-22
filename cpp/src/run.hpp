#pragma once

#include <Properties.h>
#include <chrono>
#include <opencv2/aruco.hpp>
#include <gst/gst.h>
#include <glib.h>
#include <gst/app/gstappsink.h>
#include <iostream>

#define team2077_print(arg) std::cout << arg << std::endl
#define team2077_printerr(arg) std::cerr << arg << std::endl

static const auto window_name = cv::String("Video");

struct RunState {
    cppproperties::Properties props;
    std::chrono::nanoseconds prev_start;
    int frame_count;
    cv::aruco::ArucoDetector detector;
	int width;
	int height;
};

namespace run {
	struct GstreamerInfo {
		GstBin *pipeline;
		GstAppSink *appsink;
		int width;
		int height;
	};

	GstreamerInfo gst_prepare(cppproperties::Properties &props);
}