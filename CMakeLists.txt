cmake_minimum_required(VERSION 3.20)

project(Vision
    VERSION 0.0.1
    DESCRIPTION "Team 2077"
)

set(MAP_IMPORTED_CONFIG_MinSizeRel Release)
set(MAP_IMPORTED_CONFIG_RelWithDebInfo Debug)
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