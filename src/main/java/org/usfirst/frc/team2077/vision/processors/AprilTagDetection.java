package org.usfirst.frc.team2077.vision.processors;

// import org.opencv.aruco.ArucoDetector;
// import org.opencv.aruco.Dictionary;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.usfirst.frc.team2077.video.NTMain;
import org.usfirst.frc.team2077.vision.Main;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class AprilTagDetection implements Main.FrameProcessor {
    Mat camera;
    Mat distance;
    private long lastMillis;

    public AprilTagDetection() {
        Properties properties = NTMain.getProperties();
        camera = new Mat(3, 3, CvType.CV_64FC1);
        distance = new Mat(1, 5, CvType.CV_64FC1);
        buildMat(properties, camera, "camera");
        buildMat(properties, distance, "distance");
    }

    static void buildMat(Properties properties, Mat mat, String matId) {
        for (int row = 0; row < mat.rows(); row++) {
            for (int col = 0; col < mat.cols(); col++) {
                String[] stringvalues = properties.get("AprilTag." + matId + "." + row + "." + col).toString().split(", *");
                double values = Double.parseDouble(stringvalues[0]);
                mat.put(row, col, values);


            }
        }

    }

    static {

        try {
            System.out.println(new File(".").getAbsolutePath());

        } catch (Exception e) {
            System.out.println("does dll exist: " + new File("./april_tags_2077.dll").exists());
            System.out.println("does lib exist: " + new File("./apriltagd.lib").exists());
            throw new RuntimeException(e);
        }

    }

    // private final ArucoDetector detector = new ArucoDetector(Dictionary.get(20));

    Mat undistorted = new Mat();
    Mat grayscale = new Mat();

    private static final Scalar BRIGHT_GREEN = new Scalar(50, 255, 50, 255);

    @Override
    public void processFrame(Mat input, Mat overlayMat) {
        long startMillis = System.currentTimeMillis();

        Calib3d.undistort(input, undistorted, camera, distance);
//        undistorted.copyTo(overlayMat);
        Imgproc.cvtColor(undistorted, grayscale, Imgproc.COLOR_BGR2GRAY);
//        undistorted.copyTo(overlayMat);

        List<Mat> corners = new LinkedList<>();
        Mat ids = new Mat();
        // detector.detectMarkers(grayscale, corners, ids);

        {
            Point topLeft, bottomRight;
            for(Mat cornerGrouping : corners) {
                topLeft = new Point(cornerGrouping.get(0, 0));
                bottomRight = new Point(cornerGrouping.get(0, 2));

                Imgproc.rectangle(overlayMat, topLeft, bottomRight, BRIGHT_GREEN, 2, Imgproc.FILLED);

            }
        }

        long end = System.currentTimeMillis();
//        System.out.printf("Process Frame took: %s s%n", (end - startMillis) / ONE_SECOND);
    }

    private static final float ONE_SECOND = TimeUnit.SECONDS.toMillis(1);
}
