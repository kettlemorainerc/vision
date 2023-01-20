#include <opencv2/core.hpp>
#include <opencv2/imgcodecs.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/highgui.hpp>
#include <Properties.h>
#include <PropertiesParser.h>
#include <iostream>
#include <chrono>
#include <thread>
#include <gst/gst.h>
#include <glib.h>
#include <gst/app/gstappsink.h>
#include <opencv2/aruco.hpp>

#include "./main.hpp"
#include "./run.hpp"
#include "./process.hpp"

using namespace cv;

static inline const auto expected_mat_type = CV_8UC3;

template <int width, int height>
const inline static void to_mat(GstMapInfo *sample, Mat **ret) {
	Mat *prev = *ret;
	*ret = new Mat(height, width, expected_mat_type, sample->data);

	if(prev) {
		delete prev;
	}
}

template <std::size_t width, std::size_t height>
const inline static void to_mat(GstMapInfo *sample, cuda::GpuMat **ret) {
	cuda::GpuMat *prev = *ret;
	Mat base(height, width, expected_mat_type, sample->data);
	*ret = new cuda::GpuMat(base);

	if(prev) {
		delete prev;
	}
}

template <class MatType, int width, int height>
void process_sample(GstSample *sample, RunState *state) {
	static GstBuffer *prev_buf = nullptr;
	static GstMapInfo *prev_info = nullptr;
	static GstBuffer *buffer = nullptr;
	static GstMapInfo *info = nullptr;

	static MatType *frame = nullptr;

	prev_buf = buffer;
	buffer = gst_sample_get_buffer(sample);
	prev_info = info;
	info = new GstMapInfo;
	if(gst_buffer_map(buffer, info, GstMapFlags::GST_MAP_READ)) {
		if(prev_buf) {
			gst_buffer_unmap(prev_buf, prev_info);

			prev_buf = nullptr;
			prev_info = nullptr;
		}
	} else {
		team2077_printerr("Failed to map buffer");
		std::exit(1);
	}

	if(getWindowProperty(window_name, WindowPropertyFlags::WND_PROP_VISIBLE) == 0) {
		team2077_print("Window shutdown, stopping");
		return;
	}


	to_mat<width, height>(info, &frame);
	process::processFrame(frame, state);
}

static const cv::Ptr<cv::aruco::DetectorParameters> getParams() {
    using namespace cv;

	auto ptr = std::make_shared<aruco::DetectorParameters>(aruco::DetectorParameters());
    Ptr<aruco::DetectorParameters> params(ptr);
    params->aprilTagMinClusterPixels = 20;
    params->aprilTagMinWhiteBlackDiff = 125;
    params->cornerRefinementMaxIterations = 5;

    return params;
}

static const cv::Ptr<cv::aruco::RefineParameters> getRefinementParams() {
    using namespace cv;

	auto ptr = std::make_shared<aruco::RefineParameters>(aruco::RefineParameters());
    Ptr<aruco::RefineParameters> params(ptr);
	params->checkAllOrders = false;

    return params;
}

static cv::aruco::ArucoDetector getDetector() {
    using namespace cv;
    const auto detectParams = getParams();
	const auto refineParams = getRefinementParams();
	const auto dict = aruco::getPredefinedDictionary(aruco::DICT_APRILTAG_36h11);

	team2077_print("Building detector");
    return aruco::ArucoDetector(dict, detectParams, refineParams);
}

cppproperties::Properties readProperties(int argCount, char* argv[]) {
	team2077_print("Open cv version " << getVersionString());

    // Start at 1 as the first argument is the executable (.../vision.exe)
    std::string propertiesFile;
    for(int i = 1; i < argCount; i++) {
        if(argv[i][0] == '-') {
            std::string str;
            str.append(argv[i]);
            // ignore --whatver=<value> flags
            // skip --whatever <value> styles
            if(str.find('=', 0) < 0) {
                i++;
            }
        } else {
            // The first non-flag argument should be the properties file
            propertiesFile.append(argv[i]);
            break;
        }
    }

    if(propertiesFile.empty()) {
        team2077_printerr("The first non-flag is not a reference to a properties file!");
        std::exit(1);
    }

    cppproperties::PropertiesParser parser;
    return parser.Read(propertiesFile);
}

template <class MatType, int width, int height>
static void main_loop(GstAppSink * appsink, RunState *state) {
	GstSample *sample = nullptr;
	GstSample *prev_sample = nullptr;
	do {
		prev_sample = sample;
		sample = gst_app_sink_try_pull_sample(appsink, 1);

		if(sample) {
			process_sample<MatType, width, height>(sample, state);
			pollKey();
		}

		if(prev_sample) {
			gst_sample_unref(prev_sample);
			prev_sample = nullptr;
		}
	} while(getWindowProperty(window_name, WindowPropertyFlags::WND_PROP_VISIBLE) != 0);
}

int gst_start(std::string pipelineStr, RunState &state) {
	team2077_print("Parsing pipeline");
	GError *err = NULL;
	GstElement *pipeline = gst_parse_launch(pipelineStr.c_str(), &err);

	team2077_print("Searching for appsink");
	GstAppSink *appsink = GST_APP_SINK(gst_bin_get_by_name(GST_BIN(pipeline), "sink"));
	if(!appsink) {
		team2077_printerr("Failed to find appsink!");
		return 1;
	}

	gst_app_sink_set_drop(appsink, true);
	gst_app_sink_set_emit_signals(appsink, false);
	gst_app_sink_set_max_buffers(appsink, 1);

	// team2077_print("binding sample listener");
	// g_signal_connect(appsink, "new-sample", G_CALLBACK(new_sample), &state);

	team2077_print("Starting");
	namedWindow(window_name, WINDOW_KEEPRATIO);
	setWindowProperty(window_name, WindowPropertyFlags::WND_PROP_VISIBLE, 1);
	// cv:imshow(window_name, cv::Mat(1080, 1920, expected_mat_type));

	gst_element_set_state(pipeline, GstState::GST_STATE_PLAYING);

	// loop = g_main_loop_new(NULL, false);
	// g_main_loop_run(loop);

	team2077_print("Trying to pull first sample");
	GstSample *sample =nullptr;
	GstSample *prev_sample = nullptr;

	static const int width = 1920;
	static const int height = 1080;

	if(cv::cuda::getCudaEnabledDeviceCount() > 0) {
		team2077_print("Cuda detected, using GpuMats");
		main_loop<cv::cuda::GpuMat, width, height>(appsink, &state);
	} else {
		if(cv::cuda::getCudaEnabledDeviceCount() == -1) {
			team2077_print("Cuda not enabled, or not compatible, using normal Mat");
		} else {
			team2077_print("No Cuda devices detected, using normal Mat");
		}
		main_loop<cv::Mat, width, height>(appsink, &state);
	}
}

// static const auto wait_for = 1000 / 60;

template <class MatType, int width, int height>
int cv_start(std::string pipelineStr, RunState &state) {
	cv::VideoCapture capture(pipelineStr, CAP_GSTREAMER);
	capture.open(0);

	MatType *buff = new MatType(height, width, expected_mat_type);
	MatType *prev_buff = nullptr;
	while(capture.grab()) {
		prev_buff = buff;
		capture.read(*buff);

		if(buff) {
			process::processFrame(buff, &state);
		}

		if(prev_buff) {
			delete prev_buff;
		}
	}
}

int main(int argCount, char* argv[]) {
	gst_init(&argCount, &argv);

    cppproperties::Properties props = readProperties(argCount, argv);
	auto pipelineStr = props.GetProperty("pipeline");
	team2077_print("using pipeline '" << pipelineStr << "'");

	team2077_print("Building run state");
	RunState state {
		.props = props,
		.prev_start=std::chrono::high_resolution_clock::now().time_since_epoch(),
		.frame_count = 0,
		.detector = getDetector(),
	};

	return gst_start(pipelineStr, state);
}