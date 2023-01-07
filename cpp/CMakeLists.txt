cmake_minimum_required(VERSION 3.23.3)

list(APPEND CMAKE_PREFIX_PATH "C:/tools/opencv/build")

project(Vision
    VERSION 0.0.1
    DESCRIPTION "Team 2077"
)

find_package(OpenCV REQUIRED)

include_directories(
    ${PROJECT_SOURCE_DIR}/../external/gstreamer/gst
    ${PROJECT_SOURCE_DIR}/src/
    ${PROJECT_SOURCE_DIR}/../external/cpp-properties/include
    ${OpenCV_INCLUDE_DIRS}
)

add_executable(vision
    ${PROJECT_SOURCE_DIR}/src/main.cpp
    # cpp-parser
    ${PROJECT_SOURCE_DIR}/../external/cpp-properties/src/Properties.cpp
    ${PROJECT_SOURCE_DIR}/../external/cpp-properties/src/PropertiesParser.cpp
    ${PROJECT_SOURCE_DIR}/../external/cpp-properties/src/PropertiesUtils.cpp
)

target_link_libraries(vision ${OpenCV_LIBS})