#include <opencv2/aruco.hpp>
#include <opencv2/cudaimgproc.hpp>
#include <opencv2/cudaobjdetect.hpp>
#include "./process.hpp"

static void display(cv::InputArray &mat, RunState *state) {
    static auto difference = std::chrono::seconds(10);
    cv::imshow(window_name, mat);
    state->frame_count ++;

    if(std::chrono::high_resolution_clock::now().time_since_epoch() - state->prev_start >= difference) {
        state->prev_start = std::chrono::high_resolution_clock::now().time_since_epoch();
        team2077_print((state->frame_count / 10) << " frames per second");
        state->frame_count = 0;
    }
}

template <class MatType>
void searchAndMarkImage(cv::Mat &gray_scale, RunState *state, cv::Mat &draw_on) {
    std::vector<std::vector<cv::Point2f>> corners;
    std::vector<int> ids;

    state->detector.detectMarkers(gray_scale, corners, ids);
	static auto green = cv::Scalar(0, 255, 0, 255);
    cv::aruco::drawDetectedMarkers(draw_on, corners, ids, green);
}

void process::processFrame(cv::Mat *frame, RunState *state) {
	cv::Mat gray;
	cv::cvtColor(*frame, gray, cv::COLOR_BGRA2GRAY);

    searchAndMarkImage<cv::Mat>(gray, state, *frame);

    
    display(*frame, state);
}

void process::processFrame(cv::cuda::GpuMat *frame, RunState *state) {
	cv::cuda::GpuMat gray;
	cv::cuda::cvtColor(*frame, gray, cv::COLOR_BGR2GRAY);

	cv::Mat processed(gray);
	cv::Mat img(*frame);

	searchAndMarkImage<cv::Mat>(processed, state, img);

    display(img, state);
}
