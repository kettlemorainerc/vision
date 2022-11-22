package org.usfirst.frc.team2077.processor;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.usfirst.frc.team2077.util.SuperProperties;

public abstract class Processor {
    protected Mat overlay = new Mat(0, 0, CvType.CV_8UC4);
    protected static final Scalar none = new Scalar(0, 0, 0, 0);

    public Processor(SuperProperties properties) {}

    public abstract Mat process(Mat frame);

    protected final void updateOverlayMat(Mat of) {
        if(!overlay.size().equals(of.size())) overlay = new Mat(of.size(), CvType.CV_8UC4);
        overlay.setTo(none);
    }
}
