#pragma once

#include <gst/gst.h>
#include <opencv2/imgproc.hpp>
#include <opencv2/core/cuda.hpp>

static inline const auto expected_mat_type = CV_8UC4;

namespace mat_convert {

	static void to_mat(int width, int height, GstMapInfo *sample, cv::Mat **ret) {
		using namespace cv;
		Mat *prev = *ret;
		*ret = new Mat(height, width, expected_mat_type, sample->data);

		if(prev) {
			delete prev;
		}
	}

	static void to_mat(int width, int height, GstMapInfo *sample, cv::cuda::GpuMat **ret) {
		using namespace cv;

		cuda::GpuMat *prev = *ret;
		Mat base(height, width, expected_mat_type, sample->data);
		*ret = new cuda::GpuMat(base);

		if(prev) {
			delete prev;
		}
	}

	template <class MatType>
	void to_mat(GstSample *sample, MatType **target, GstBuffer *buffer, GstMapInfo *info, GstBuffer *prev_buf, GstMapInfo *prev_info) {
		prev_buf = buffer;
		buffer = gst_sample_get_buffer(sample);

		int width, height;
		if(*target) {
			width = (*target)->cols;
			height = (*target)->rows;
		} else {
			auto caps = gst_sample_get_caps(sample);
			auto structure = gst_caps_get_structure(caps, 0);

			gst_structure_get_int(structure, "width", &width);
			gst_structure_get_int(structure, "height", &height);
		}
		
		auto sample_info = gst_sample_get_info(sample);
		
		
		if(gst_buffer_map(buffer, info, GstMapFlags::GST_MAP_READ)) {
			if(prev_buf) {
				gst_buffer_unmap(prev_buf, prev_info);

				prev_buf = nullptr;
			}
		} else {
			team2077_printerr("Failed to map buffer");
			std::exit(1);
		}

		to_mat(width, height, info, target);
	}
}