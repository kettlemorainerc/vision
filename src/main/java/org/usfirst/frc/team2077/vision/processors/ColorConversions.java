package org.usfirst.frc.team2077.vision.processors;

import org.opencv.core.*;
import org.opencv.imgproc.*;

public abstract class ColorConversions {
    private ColorConversions(){}

    public static Mat toHsv(Mat nonHsv) {
        Mat ret = new Mat();
        Imgproc.cvtColor(nonHsv, ret, Imgproc.COLOR_BGR2HSV);
        return ret;
    }
}
