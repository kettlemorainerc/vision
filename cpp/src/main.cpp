#include "./main.hpp"
#include <opencv2/opencv.hpp>
#include <Properties.h>
#include <PropertiesParser.h>
#include <iostream>
#include <chrono>
#include <thread>
#include "./run.hpp"
#include "./process.hpp"

using namespace cv;

cppproperties::Properties readProperties(int argCount, char* argv[]) {
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

int main(int argCount, char* argv[]) {
    cppproperties::Properties props = readProperties(argCount, argv);

    VideoCapture capture(
        "mfvideosrc device-index=1 ! video/x-raw,format=NV12,width=1920,height=1080 ! glupload ! glcolorconvert ! gldownload ! videoscale ! video/x-raw,format=ABGR ! appsink",
        CAP_GSTREAMER
    );
    if(!capture.open(0)) {
        printerr("Failed to start video capture");
        std::exit(1);
    }

    Mat frame;
    int count = 0;
    namedWindow("Video", WINDOW_KEEPRATIO);
    auto prev = std::chrono::high_resolution_clock::now().time_since_epoch();

    RunState state {
        props,
        prev,
        0
    };

    // While window visible
    while(getWindowProperty("Video", WindowPropertyFlags::WND_PROP_VISIBLE) != 0.) {
        if(capture.retrieve(frame)) {
            process::processFrame(frame, state);
        }

        waitKey(1);
    }
}