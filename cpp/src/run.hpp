#pragma once

#include <Properties.h>
#include <chrono>

#define print(arg) std::cout << arg << std::endl
#define printerr(arg) std::cerr << arg << std::endl

struct RunState {
    cppproperties::Properties props;
    std::chrono::nanoseconds prev_start;
    int frame_count;
};