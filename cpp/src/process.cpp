#include <process.hpp>


void process::processFrame(cv::Mat &frame, RunState &state)
{
    static auto difference = std::chrono::seconds(10);
    imshow("Video", frame);
    state.frame_count ++;

    if(std::chrono::high_resolution_clock::now().time_since_epoch() - state.prev_start >= difference) {
        state.prev_start = std::chrono::high_resolution_clock::now().time_since_epoch();
        print((state.frame_count / 10) << " frames per second");
        state.frame_count = 0;
    }
}
