#include <opencv2/core.hpp>
#include <opencv2/imgcodecs.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/highgui.hpp>
#include <Properties.h>
#include <PropertiesParser.h>
#include <iostream>
#include <chrono>
#include <thread>
#include <opencv2/aruco.hpp>
#include <algorithm>
#include <vector>

#include "./main.hpp"
#include "./run.hpp"
#include "./process.hpp"
#include "./sample_to_mat.hpp"
#include "./calibration.hpp"

using namespace cv;

template <class MatType>
void process_sample(GstSample *sample, RunState *state) {
	static GstBuffer *prev_buf = nullptr;
	static GstMapInfo *prev_info = nullptr;
	static GstBuffer *buffer = nullptr;
	static GstMapInfo *info = new GstMapInfo;

	static MatType *frame = nullptr;

	mat_convert::to_mat<MatType>(sample, &frame, buffer, info, prev_buf, prev_info);

	if(getWindowProperty(window_name, WindowPropertyFlags::WND_PROP_VISIBLE) == 0) {
		team2077_print("Window shutdown, stopping");
		return;
	}

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
	int i = 1;
    for(; i < argCount; i++) {
		std::string arg;
		arg.append(argv[i]);
		if(arg.empty()) continue;

        if(arg[0] == '-') {
            // ignore --whatver=<value> flags
            // skip --whatever <value> styles
            if(arg.find('=', 0) < 0) {
                i++;
            }
        } else {
            // The first non-flag argument should be the properties file
            propertiesFile = arg;
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

template <class MatType>
static void main_loop(GstAppSink * appsink, RunState *state) {
	gst_element_set_state(GST_ELEMENT(appsink), GstState::GST_STATE_PLAYING);

	GstSample *sample  = gst_app_sink_try_pull_sample(appsink, GST_SECOND);
	GstSample *prev_sample = nullptr;

	do {
		prev_sample = sample;
		sample = gst_app_sink_try_pull_sample(appsink, 1);

		if(sample) {
			process_sample<MatType>(sample, state);
			pollKey();
		}

		if(prev_sample) {
			gst_sample_unref(prev_sample);
			prev_sample = nullptr;
		}
	} while(getWindowProperty(window_name, WindowPropertyFlags::WND_PROP_VISIBLE) != 0);
}

void gst_start(RunState &state) {
	auto info = run::gst_prepare(state.props);

	team2077_print("Starting");
	namedWindow(window_name, WINDOW_KEEPRATIO);
	setWindowProperty(window_name, WindowPropertyFlags::WND_PROP_VISIBLE, 1);
	// cv:imshow(window_name, cv::Mat(1080, 1920, expected_mat_type));

	gst_element_set_state(GST_ELEMENT(info.pipeline), GstState::GST_STATE_PLAYING);

	// loop = g_main_loop_new(NULL, false);
	// g_main_loop_run(loop);

	if(cv::cuda::getCudaEnabledDeviceCount() > 0) {
		team2077_print("Cuda detected, using GpuMats");
		main_loop<cv::cuda::GpuMat>(info.appsink, &state);
	} else {
		if(cv::cuda::getCudaEnabledDeviceCount() == -1) {
			team2077_print("Cuda not enabled, or not compatible, using normal Mat");
		} else {
			team2077_print("No Cuda devices detected, using normal Mat");
		}
		main_loop<cv::Mat>(info.appsink, &state);
	}
}

// static const auto wait_for = 1000 / 60;

template <class MatType>
void cv_start(std::string pipelineStr, RunState &state) {
	cv::VideoCapture capture(pipelineStr, CAP_GSTREAMER);
	capture.open(0);

	MatType *buff = new MatType();
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

	auto props_names = props.GetPropertyNames();

	std::vector<std::string>::iterator iter;
	bool calibrate = false;
	if(std::find(props_names.begin(), props_names.end(), "calibrate") != props_names.end()) {
		std::string calib = props.GetProperty("calibrate");
		std::transform(calib.begin(), calib.end(), calib.begin(), [](unsigned char c){return std::tolower(c);});

		calibrate = calib == "true";
	}

	if(calibrate) {
		calibration::calibrate(props);
	} else {
		team2077_print("Building run state");
		RunState state {
			.props = props,
			.prev_start=std::chrono::high_resolution_clock::now().time_since_epoch(),
			.frame_count = 0,
			.detector = getDetector(),
			.width = 1920,
			.height = 1080,
		};

		gst_start(state);
	}
}