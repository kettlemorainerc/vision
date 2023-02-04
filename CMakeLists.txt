cmake_minimum_required(VERSION 3.20)

project(Vision
    VERSION 0.0.1
    DESCRIPTION "Team 2077"
	LANGUAGES CXX
)

set(MAP_IMPORTED_CONFIG_MinSizeRel Release)
set(MAP_IMPORTED_CONFIG_RelWithDebInfo Debug)
set(EXTERN_PATH ${PROJECT_SOURCE_DIR}/external)
set(OPENCV_PATH ${PROJECT_SOURCE_DIR}/opencv)
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
	${OPENCV_PATH}/standard/include${OPENCV_PATH}/modules/modules/alphamat
	${OPENCV_PATH}/modules/modules/aruco/include
	${OPENCV_PATH}/modules/modules/barcode/include
	${OPENCV_PATH}/modules/modules/bgsegm/include
	${OPENCV_PATH}/modules/modules/bioinspired/include
	${OPENCV_PATH}/modules/modules/ccalib/include
	${OPENCV_PATH}/modules/modules/cnn_3dobj/include
	${OPENCV_PATH}/modules/modules/cudaarithm/include
	${OPENCV_PATH}/modules/modules/cudabgsegm/include
	${OPENCV_PATH}/modules/modules/cudacodec/include
	${OPENCV_PATH}/modules/modules/cudafeatures2d/include
	${OPENCV_PATH}/modules/modules/cudafilters/include
	${OPENCV_PATH}/modules/modules/cudaimgproc/include
	${OPENCV_PATH}/modules/modules/cudalegacy/include
	${OPENCV_PATH}/modules/modules/cudaobjdetect/include
	${OPENCV_PATH}/modules/modules/cudaoptflow/include
	${OPENCV_PATH}/modules/modules/cudastereo/include
	${OPENCV_PATH}/modules/modules/cudawarping/include
	${OPENCV_PATH}/modules/modules/cudev/include
	${OPENCV_PATH}/modules/modules/cvv/include
	${OPENCV_PATH}/modules/modules/datasets/include
	${OPENCV_PATH}/modules/modules/dnns_easily_fooled/include
	${OPENCV_PATH}/modules/modules/dnn_objdetect/include
	${OPENCV_PATH}/modules/modules/dnn_superres/include
	${OPENCV_PATH}/modules/modules/dpm/include
	${OPENCV_PATH}/modules/modules/face/include
	${OPENCV_PATH}/modules/modules/freetype/include
	${OPENCV_PATH}/modules/modules/fuzzy/include
	${OPENCV_PATH}/modules/modules/hdf/include
	${OPENCV_PATH}/modules/modules/hfs/include
	${OPENCV_PATH}/modules/modules/img_hash/include
	${OPENCV_PATH}/modules/modules/intensity_transform/include
	${OPENCV_PATH}/modules/modules/julia/include
	${OPENCV_PATH}/modules/modules/line_descriptor/include
	${OPENCV_PATH}/modules/modules/matlab/include
	${OPENCV_PATH}/modules/modules/mcc/include
	${OPENCV_PATH}/modules/modules/optflow/include
	${OPENCV_PATH}/modules/modules/ovis/include
	${OPENCV_PATH}/modules/modules/phase_unwrapping/include
	${OPENCV_PATH}/modules/modules/plot/include
	${OPENCV_PATH}/modules/modules/quality/include
	${OPENCV_PATH}/modules/modules/rapid/include
	${OPENCV_PATH}/modules/modules/reg/include
	${OPENCV_PATH}/modules/modules/rgbd/include
	${OPENCV_PATH}/modules/modules/saliency/include
	${OPENCV_PATH}/modules/modules/sfm/include
	${OPENCV_PATH}/modules/modules/shape/include
	${OPENCV_PATH}/modules/modules/stereo/include
	${OPENCV_PATH}/modules/modules/structured_light/include
	${OPENCV_PATH}/modules/modules/superres/include
	${OPENCV_PATH}/modules/modules/surface_matching/include
	${OPENCV_PATH}/modules/modules/text/include
	${OPENCV_PATH}/modules/modules/tracking/include
	${OPENCV_PATH}/modules/modules/videostab/include
	${OPENCV_PATH}/modules/modules/viz/include
	${OPENCV_PATH}/modules/modules/wechat_qrcode/include
	${OPENCV_PATH}/modules/modules/xfeatures2d/include
	${OPENCV_PATH}/modules/modules/ximgproc/include
	${OPENCV_PATH}/modules/modules/xobjdetect/include
	${OPENCV_PATH}/modules/modules/xphoto/include
    ${OpenCV_INCLUDE_DIRS}
	${GSTREAMER_INCLUDE_DIRS}
	${GLIB2_INCLUDE_DIRS}
	${GLIB2_MAIN_INCLUDE_DIR}
	${GLIB2_INTERNAL_INCLUDE_DIR}
	${GOBJECT_INCLUDE_DIR}
)

add_executable(vision
	# ${CPP_PATH}/src/sample_to_mat.cpp
	${CPP_PATH}/src/calibration.cpp
    ${CPP_PATH}/src/main.cpp
    ${CPP_PATH}/src/process.cpp
    ${CPP_PATH}/src/run.cpp

    # cpp-parser
    ${EXTERN_PATH}/cpp-properties/src/Properties.cpp
    ${EXTERN_PATH}/cpp-properties/src/PropertiesParser.cpp
    ${EXTERN_PATH}/cpp-properties/src/PropertiesUtils.cpp
)

set_property(TARGET vision PROPERTY CXX_STANDARD 20)

get_cmake_property(NAMES VARIABLES)
list(SORT NAMES)
foreach(var ${NAMES})
	if(var MATCHES "^G(LIB).*$")
		message(STATUS ${var}=${${var}})
	endif()
endforeach()
# list(SORT NAMES)
# foreach(var ${NAMES})
# 	if(var MATCHES "^O[pP][eE][nN][Cc][Vv].*$")
# 		message(STATUS ${var}=${${var}})
# 	endif()
# endforeach()

target_link_directories(vision
	PUBLIC ${OpenCV_LIB_PATH}
	PUBLIC ${OpenCV_CONFIG_PATH}
)

set_property(TARGET ${OpenCV_LIBRARIES} PROPERTY MAP_IMPORTED_CONFIG_MINSIZEREL Debug)
set_property(TARGET ${OpenCV_LIBRARIES} PROPERTY MAP_IMPORTED_CONFIG_RELWITHDEBINFO Debug)


target_link_libraries(vision
	${OpenCV_LIBRARIES}
	${GSTREAMER_LIBRARY}
	${GLIB2_LIBRARIES}
	${GOBJECT_LIBRARIES}
	${GSTREAMER_APP_LIBRARY}
)