#include <process.hpp>
#include <opencv2/aruco.hpp>

static void display(cv::Mat mat, RunState *state) {
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
MatType searchImage(MatType *frame, RunState *state) {
    MatType ret;
    cv::cvtColor(*frame, ret, cv::COLOR_BGRA2GRAY);

    std::vector<std::vector<cv::Point2f>> corners;
    std::vector<int> ids;

    state->detector.detectMarkers(ret, corners, ids);
    cv::aruco::drawDetectedMarkers(ret, corners, ids);

    return ret;
}

void process::processFrame(cv::Mat *frame, RunState *state) {
    cv::Mat toDisplay = searchImage<cv::Mat>(frame, state);
    
    display(toDisplay, state);
}

void process::processFrame(cv::cuda::GpuMat *frame, RunState *state) {
    static const auto toDisplay = cv::Mat();
    cv::cuda::GpuMat processed = searchImage<cv::cuda::GpuMat>(frame, state);

    processed.download(toDisplay);
    display(toDisplay, state);
}
