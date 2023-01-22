#include "./run.hpp"

run::GstreamerInfo run::gst_prepare(cppproperties::Properties &props) {
	std::string pipelineStr = props.GetProperty("pipeline");

	team2077_print("Parsing pipeline");
	GError *err = NULL;
	GstElement *pipeline = gst_parse_launch(pipelineStr.c_str(), &err);

	team2077_print("Searching for appsink");
	GstAppSink *appsink = GST_APP_SINK(gst_bin_get_by_name(GST_BIN(pipeline), "sink"));

	if(!appsink) {
		throw new std::runtime_error("Failed to find appsink!");
	}

	gst_app_sink_set_drop(appsink, true);
	gst_app_sink_set_emit_signals(appsink, false);
	gst_app_sink_set_max_buffers(appsink, 1);

	auto caps = gst_app_sink_get_caps(appsink);
	auto structure = gst_caps_get_structure(caps, 0);

	int width, height;
	gst_structure_get_int(structure, "width", &width);
	gst_structure_get_int(structure, "height", &height);

	// team2077_print("binding sample listener");
	// g_signal_connect(appsink, "new-sample", G_CALLBACK(new_sample), &state);

	return run::GstreamerInfo {
		.pipeline = GST_BIN(pipeline),
		.appsink = appsink,
		.width = width,
		.height = height,
	};
}