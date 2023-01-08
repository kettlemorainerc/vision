cmake_minimum_required(VERSION 3.20)

project(Vision
    VERSION 0.0.1
    DESCRIPTION "Team 2077"
)

set(EXTERN_PATH ${PROJECT_SOURCE_DIR}/external)
set(CPP_PATH ${PROJECT_SOURCE_DIR}/cpp)

list(APPEND CMAKE_MODULE_PATH ${EXTERN_PATH}/qt-gstreamer/cmake/modules)
list(APPEND CMAKE_MODULE_PATH ${EXTERN_PATH})

find_package(OpenCV REQUIRED)
find_package(GLIB2 MODULE REQUIRED)
find_package(GObject MODULE REQUIRED)
find_package(GStreamer MODULE REQUIRED)
find_package(GStreamerPluginsBase MODULE REQUIRED "app")

include_directories(
    ${EXTERN_PATH}/gstreamer/gst
    ${CPP_PATH}/src/
    ${EXTERN_PATH}/cpp-properties/include
    ${OpenCV_INCLUDE_DIRS}
	${GSTREAMER_INCLUDE_DIRS}
	${GLIB2_INCLUDE_DIRS}
	${GLIB2_MAIN_INCLUDE_DIR}
	${GLIB2_INTERNAL_INCLUDE_DIR}
	${GOBJECT_INCLUDE_DIR}
)

add_executable(vision
    ${CPP_PATH}/src/main.cpp
    ${CPP_PATH}/src/process.cpp

    # cpp-parser
    ${EXTERN_PATH}/cpp-properties/src/Properties.cpp
    ${EXTERN_PATH}/cpp-properties/src/PropertiesParser.cpp
    ${EXTERN_PATH}/cpp-properties/src/PropertiesUtils.cpp
)

get_cmake_property(NAMES VARIABLES)
list(SORT NAMES)
foreach(var ${NAMES})
	if(var MATCHES "^G.*LIBRAR(Y|IES)$")
		message(STATUS ${var}=${${var}})
	endif()
endforeach()

target_link_libraries(vision
	${OpenCV_LIBS}
	${GSTREAMER_LIBRARY}
	${GLIB2_LIBRARIES}
	${GOBJECT_LIBRARIES}
	${GSTREAMER_APP_LIBRARY}
)