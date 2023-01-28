package org.usfirst.frc.team2077.vision.processors;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.usfirst.frc.team2077.vision.Main.FrameProcessor;

public class BallBirdseyeOverlay extends BirdseyeVisionOverlay implements FrameProcessor {

    public BallBirdseyeOverlay() {
        super( "Ball Target Data", 240 );
    }
    
    @Override
    public void processFrame( Mat frameMat, Mat overlayMat ) {

    }
}
