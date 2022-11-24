package org.usfirst.frc.team2077.processor;

import org.bytedeco.opencv.opencv_core.*;
import org.usfirst.frc.team2077.util.SuperProperties;

import static org.bytedeco.opencv.global.opencv_core.CV_8UC4;

public abstract class Processor {
    protected Mat overlay = new Mat(0, 0, CV_8UC4);
    protected GpuMat gpuOverlay;
    protected static final Scalar none = new Scalar(0, 0, 0, 0);

    public Processor(SuperProperties properties) {}

    public abstract Mat process(Mat frame);

    public GpuMat process(GpuMat frame) {
        org.bytedeco.opencv.opencv_core.Mat local = new org.bytedeco.opencv.opencv_core.Mat();
        frame.download(local);

        frame.upload(process(local));
        return frame;
    }

    protected final void updateOverlayMat(Mat of) {
        if(!overlay.size().equals(of.size())) overlay = new Mat(of.size(), CV_8UC4);
        overlay.put(none);
    }

    protected final void updateOverlayMat(GpuMat of) {
        if(gpuOverlay == null || !gpuOverlay.size().equals(of.size())) gpuOverlay = new GpuMat(of.size(), CV_8UC4);
        gpuOverlay.put(none);
    }
}
