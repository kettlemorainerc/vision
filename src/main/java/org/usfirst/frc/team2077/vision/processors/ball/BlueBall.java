package org.usfirst.frc.team2077.vision.processors.ball;

import org.opencv.core.*;
import org.opencv.imgproc.*;
import org.usfirst.frc.team2077.vision.processors.*;

import java.util.*;

/**
 * Container class for constant information related to finding blue balls
 */
public class BlueBall implements BallDetector {
    public static final Scalar HUE_UPPER = new Scalar(230);
    public static final Scalar HUE_LOWER = new Scalar(220);

    public static final Scalar SATURATION_UPPER = new Scalar(100);
    public static final Scalar SATURATION_LOWER = new Scalar(70);

    public static final Scalar VALUE_UPPER = new Scalar(100);
    public static final Scalar VALUE_LOWER = new Scalar(60);

    public static final double IMAGE_TO_ACCUMULATOR_RESOLUTION_RATIO = 2;
    public static final double MIN_DISTANCE_BETWEEN_CENTERS = 15;

    public static final double CANNY_EDGE_UPPER = 10;
    public static final double ACCUMULATOR_THRESHOLD = 65;
    public static final int MIN_RADIUS = 30;
    public static final int MAX_RADIUS = 130;

    @Override
    public Optional<Ball> detectNearestBall(Mat bgrImage) {
        Mat hsv = ColorConversions.toHsv(bgrImage);

        List<Mat> hsvLayers = new ArrayList<>(4);
        Core.split(hsv, hsvLayers);

        Mat correctHue = rangeMask(hsvLayers.get(0), HUE_LOWER, HUE_UPPER);
        Mat correctSaturation = rangeMask(hsvLayers.get(1), SATURATION_LOWER, SATURATION_UPPER);
        Mat correctValue = rangeMask(hsvLayers.get(2), VALUE_LOWER, VALUE_UPPER);

        Mat circleMask = joinedMasks(correctHue, correctValue, correctSaturation);

        Mat circles = new Mat();
        Imgproc.HoughCircles(
              circleMask,
              circles,
              Imgproc.HOUGH_GRADIENT,
              IMAGE_TO_ACCUMULATOR_RESOLUTION_RATIO,
              MIN_DISTANCE_BETWEEN_CENTERS,
              CANNY_EDGE_UPPER,
              ACCUMULATOR_THRESHOLD,
              MIN_RADIUS,
              MAX_RADIUS
        );

        if(circles.cols() == 0) return Optional.empty();

        double[] largest = new double[] {0, 0, 0};
        for(int i = 0; i < circles.cols(); i++) {
            double[] currentBall = circles.get(0, i);
            if(currentBall[2] > largest[2]) largest = currentBall;
        }

        return Optional.of(new Ball((int) largest[0], (int) largest[1] + bgrImage.cols(), (int) largest[2]));
    }

    private static Mat rangeMask(Mat mat, Scalar lower, Scalar upper) {
        Mat m = new Mat();
        Core.inRange(mat, lower, upper, m);
        return m;
    }

    private static Mat joinedMasks(Mat h, Mat s, Mat v) {
        Mat m = new Mat();
        Core.bitwise_and(h, s, m);
        Core.bitwise_and(m, v, m);

        return m;
    }
}
