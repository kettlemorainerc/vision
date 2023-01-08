#include "./main.hpp"
#include <opencv2/opencv.hpp>
#include <Properties.h>
#include <PropertiesParser.h>
#include <iostream>
#include <chrono>
#include <thread>
#include "./run.hpp"
#include "./process.hpp"
#include <gst/gst.h>
#include <gst/app/gstappsink.h>

using namespace cv;

static const auto window_name = "Video";
static GMainLoop *loop = nullptr;
static int frame_count = 0;
static auto prev_start = std::chrono::high_resolution_clock::now();

static bool updated = false;

static const auto target_seconds = 5;
static const auto target_wait = std::chrono::seconds(target_seconds);

void process_sample(GstSample *sample) {
	static GstBuffer *prev_buf = nullptr;
	static GstMapInfo *prev_info = nullptr;
	static GstBuffer *buffer = nullptr;
	static GstMapInfo *info = nullptr;

	static Mat frame(1080, 1920, CV_8UC4);

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
		printerr("Failed to map buffer");
		std::exit(1);
	}

	if(getWindowProperty(window_name, WindowPropertyFlags::WND_PROP_VISIBLE) == 0) {
		print("Window shutdown, stopping");
		std::exit(0);
	}

	frame.data = info->data;
	imshow(window_name, frame);
	updated = true;

	frame_count++;

	if(std::chrono::high_resolution_clock::now() - prev_start > target_wait) {
		print((frame_count / target_seconds) << " FPS");
		frame_count = 0;
		prev_start = std::chrono::high_resolution_clock::now();
	}
}

cppproperties::Properties readProperties(int argCount, char* argv[]) {
	print("Open cv version " << getVersionString());

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
        printerr("The first non-flag is not a reference to a properties file!");
        std::exit(1);
    }

    cppproperties::PropertiesParser parser;
    return parser.Read(propertiesFile);
}

static const auto wait_for = 1000 / 60;

int main(int argCount, char* argv[]) {
	gst_init(&argCount, &argv);

    cppproperties::Properties props = readProperties(argCount, argv);
	auto pipelineStr = props.GetProperty("pipeline");
	print("using pipeline '" << pipelineStr << "'");

	GError *err = NULL;
	GstElement *pipeline = gst_parse_launch(pipelineStr.c_str(), &err);

	print("Searching for appsink");
	GstAppSink *appsink = GST_APP_SINK(gst_bin_get_by_name(GST_BIN(pipeline), "sink"));

	gst_app_sink_set_drop(appsink, true);
	gst_app_sink_set_emit_signals(appsink, false);

	if(!appsink) {
		printerr("Failed to find appsink!");
		return 1;
	}

	print("binding sample listener");

	print("Starting");
	namedWindow(window_name, WINDOW_KEEPRATIO);

	gst_element_set_state(pipeline, GstState::GST_STATE_PLAYING);
	GstSample *sample = nullptr;
	while(getWindowProperty(window_name, WindowPropertyFlags::WND_PROP_VISIBLE) != 0) {
		sample = gst_app_sink_try_pull_sample(appsink, 1);
		if(sample) {
			process_sample(sample);
			pollKey();
		}
	}
}