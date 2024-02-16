package org.usfirst.frc.team2077.view.processors;

import org.opencv.core.Mat;

public interface FrameProcessor {
    void processFrame(Mat frame, Mat overlay) throws Exception;
}
