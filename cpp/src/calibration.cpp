#include <opencv2/opencv.hpp>
#include <string>
#include <format>

#include "./calibration.hpp"
#include "./run.hpp"
#include "./sample_to_mat.hpp"

std::string get_save_dir(cppproperties::Properties &properties) {
	try {
		auto dir = properties.GetProperty("calibration.save.dir");
		if(dir[dir.length() - 1] != '/') dir += '/';

		return dir;
	} catch(cppproperties::PropertyNotFoundException e) {

	}

	team2077_print("Saving calibration images to './calibration-images/' as no 'calibration.save.dir' property was provided");
	return "calibration-images/";
}

void calibration::calibrate(cppproperties::Properties &properties) {
	auto info = run::gst_prepare(properties);

	team2077_print("Starting");
	{
		using namespace cv;
		namedWindow(window_name, WINDOW_KEEPRATIO);
		setWindowProperty(window_name, WindowPropertyFlags::WND_PROP_VISIBLE, 1);
		// cv:imshow(window_name, cv::Mat(1080, 1920, expected_mat_type));
	}

	gst_element_set_state(GST_ELEMENT(info.pipeline), GstState::GST_STATE_PLAYING);
	GstSample *sample = nullptr;
	GstSample *prev_sample = nullptr;

	cv::Mat *mat;
	GstBuffer *buffer = nullptr;
	GstMapInfo *buf_info = nullptr;
	GstBuffer *prev_buffer = nullptr;
	GstMapInfo *prev_info = nullptr;
	int save_idx = 0;

	auto save_path = properties.GetProperty("calibration.save.dir");

	do {
		prev_sample = sample;
		sample = gst_app_sink_try_pull_sample(info.appsink, 1);

		if(sample) {
			mat_convert::to_mat<cv::Mat>(sample, &mat, buffer, buf_info, prev_buffer, prev_info);
			cv::imshow(window_name, *mat);
		}

		if(prev_sample) {
			gst_sample_unref(prev_sample);
			prev_sample = nullptr;
		}

		auto key = cv::pollKey();
		if(key == 10) { // Enter / New Line
			cv::imwrite(std::format<int>("calib-img.{}.png", save_idx++), *mat);
		} else if(key == 27) { // Escape

		}
	} while(cv::getWindowProperty(window_name, cv::WindowPropertyFlags::WND_PROP_VISIBLE) != 0);
}