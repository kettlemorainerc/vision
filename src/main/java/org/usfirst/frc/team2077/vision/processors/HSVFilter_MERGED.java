package org.usfirst.frc.team2077.vision.processors;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import org.opencv.core.Point;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.usfirst.frc.team2077.vision.Main.FrameProcessor;
import org.usfirst.frc.team2077.vision.NTMain;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;


public class HSVFilter_MERGED implements FrameProcessor {

    public static final int VISION_WIDTH = 1_000;
    public static final int VISION_DEGREES = 90;

    private Map<String, NetworkTableEntry> nte_ = new TreeMap<>();

    private final Map<String, Setting> settings_ = Setting.initializeSettings("HSVFilter Settings",

            new Setting("H min", 0, 255, 4),
            new Setting("H max", 0, 255, 49),
            new Setting("S min", 0, 255, 148),
            new Setting("S max", 0, 255, 255),
            new Setting("V min", 0, 255, 106),
            new Setting("V max", 0, 255, 255),
            new Setting("R min", 0, 255, 12),
            new Setting("R max", 0, 255, 95),
            new Setting("Threshold", 0, 255, 60));

    Mat rgb = new Mat();
    Mat hsv = new Mat();
    Mat gray = new Mat();
    Mat tmp = new Mat();

    @Override
    public void processFrame(Mat frameMat, Mat overlayMat) {

        Scalar red = new Scalar(0, 0, 255, 255);
        Scalar green = new Scalar(0, 255, 0, 255);
        Scalar white = new Scalar(255, 255, 255, 255);
        Scalar black = new Scalar(0, 0, 0, 255);

        int rows = overlayMat.rows();
        int cols = overlayMat.cols();

        Imgproc.line(overlayMat, new Point(cols / 2, 0), new Point(cols / 2, rows), green, 1);

        NetworkTableEntry nte;
        if ((nte = getNTE("Crosshairs")) != null) {
            double[] crosshairs = nte.getDoubleArray(new double[0]);
            if (crosshairs != null && crosshairs.length >= 4) {
                // draw crosshairs
                double x = crosshairs[0];
                double y = crosshairs[1];
                double w = crosshairs[2];
                double h = crosshairs[3];
                //System.out.println("{" + x + "," + y + "}");
                x += w / 2;
                y += h / 2;
                x = Math.round(x);
                y = Math.round(y);
                y = rows - y;
                Imgproc.line(overlayMat, new Point((int) x, 0), new Point((int) x, rows), white, 1);
                Imgproc.line(overlayMat, new Point(0, (int) y), new Point(cols, (int) y), white, 1);
            }
        }
        if ((nte = getNTE("Position")) != null) {
            double[] position = nte.getDoubleArray(new double[0]);
            if (position != null && position.length >= 3) {
                String location = "Location:" + (Math.round(position[0] * 10.) / 10.) + "N " + (Math.round(position[1] * 10.) / 10.) + "E";
                String heading = "Heading:" + (Math.round(position[2] * 10.) / 10.);
                drawText(overlayMat, location, 20, rows / 2 - 80);
                drawText(overlayMat, heading, 20, rows / 2 - 40);
            }
        }
        if ((nte = getNTE("Target")) != null) {
            double[] target = nte.getDoubleArray(new double[0]);
            if (target != null && target.length >= 4) {
                String range = "Range:" + (Math.round(target[3] * 10.) / 10.);
                String angle = "Target Angle:" + (Math.round(target[2] * 10.) / 10.);
                drawText(overlayMat, range, 20, rows / 2 + 0);
                drawText(overlayMat, angle, 20, rows / 2 + 40);
            }
        }

        if ((nte = getNTE("RangeAV")) != null) {
            double[] rangeAV = nte.getDoubleArray(new double[]{0, 0});

            String angle = "Angles:" + (Math.round(rangeAV[0] * 100.) / 100.);
            drawText(overlayMat, angle, 20, rows / 2 + 35);

            String velocity = "Velocity:" + Math.round(rangeAV[1]);
            drawText(overlayMat, velocity, 20, rows / 2 - 6);
        }

        if ((nte = getNTE("LauncherSpeed")) != null) {
            double[] launcherSpeed = nte.getDoubleArray(new double[0]);
            if (launcherSpeed != null && launcherSpeed.length >= 1) {
                String LeftVel = "LeftVel:" + (Math.round(launcherSpeed[0] * 10.) / 10.);
                String RightVel = "RightVel:" + (Math.round(launcherSpeed[1] * 10.) / 10.);
                drawText(overlayMat, LeftVel, 20, rows / 2 + 80);
                drawText(overlayMat, RightVel, 20, rows / 2 + 120);
            }
        }

        if ((nte = getNTE("ReadyShoot")) != null) {
            boolean ready = nte.getBoolean(false);
            drawText(overlayMat, "Shoot?: ", 20, rows / 2 + 240);
            if (ready) {
                Imgproc.circle(overlayMat, new Point(155, rows / 2 + 230), 5, green, 9);
            } else {
                Imgproc.circle(overlayMat, new Point(155, rows / 2 + 230), 5, red, 9);
            }
        }
//  /\//\//\\\/\\\\/\\\\\/\\\\\\/\\\\\\\/\\\\\\\\/\\\\\\\\/\\\\\\\\\\/
        long t0 = System.currentTimeMillis();

        Imgproc.cvtColor(frameMat, rgb, Imgproc.COLOR_BGRA2RGB);
        Imgproc.cvtColor(rgb, hsv, Imgproc.COLOR_RGB2HSV);

        Imgproc.GaussianBlur(hsv, hsv, new Size(5, 5), 1);
        // Core.inRange(hsv, new Scalar(27, 75, 120), new Scalar(31, 255, 255), hsv);
        Core.inRange(hsv, new Scalar(settings_.get("H min").value(), settings_.get("S min").value(), settings_.get("V min").value()),
                new Scalar(settings_.get("H max").value(), settings_.get("S max").value(), settings_.get("V max").value()), gray);

        // Imgproc.GaussianBlur(gray, gray, new Size(20, 20), 1);
        Imgproc.GaussianBlur(gray, gray, new Size(25, 25), 0, 0);

        // Imgproc.cvtColor(hsv, rgb, Imgproc.COLOR_HSV2RGB);
        // Imgproc.cvtColor(rgb, tmp, Imgproc.COLOR_RGB2BGRA);
        // tmp.copyTo( frameMat, gray );

        Imgproc.cvtColor(gray, frameMat, Imgproc.COLOR_GRAY2BGRA);

        Mat circles = new Mat();

        double cannyThreshhold = 10;
        int[] circleSizeRange = {settings_.get("R min").value(), settings_.get("R max").value()};
        int circleProximity = 2 * circleSizeRange[0];
        double circleThreshold = settings_.get("Threshold").value();
        int accumulatorScale = 2;


        Imgproc.HoughCircles(gray, circles, Imgproc.CV_HOUGH_GRADIENT, accumulatorScale, circleProximity, cannyThreshhold, circleThreshold, circleSizeRange[0], circleSizeRange[1]);

        // HoughCircles processing
        Imgproc.cvtColor(gray, frameMat, Imgproc.COLOR_GRAY2BGRA);


        double[] radius = new double[3];
        double[] ballsX = new double[3];
        double[] ballsY = new double[3];

        for (int i = 0; i < Math.min(5, circles.cols()); i++) {
            double[] presentCiricleData = circles.get(0, i);

            double x = presentCiricleData[0];
            double y = presentCiricleData[1];
            double r = (int) presentCiricleData[2];

//            SUPER QUICK CODE - WILL BE CHANGED LATER
            if (r > radius[0]) {
                radius[2] = radius[1];
                radius[1] = radius[0];
                radius[0] = r;

                ballsX[2] = ballsX[1];
                ballsX[1] = ballsX[0];
                ballsX[0] = x;

                ballsY[2] = ballsY[1];
                ballsY[1] = ballsY[0];
                ballsY[0] = y;
            } else if (r > radius[1]) {
                radius[2] = radius[1];
                radius[1] = r;

                ballsX[2] = ballsX[1];
                radius[1] = x;

                ballsY[2] = ballsY[1];
                ballsY[1] = y;
            } else if (r > radius[2]) {
                radius[2] = r;

                ballsX[2] = x;

                ballsY[2] = y;
            }
        }
//SEND THE THREE CLOSEST BALLS
//Will compute angle from North center
        double[] angles = {
            getAngle(ballsX[0], ballsY[0]),
            getAngle(ballsX[1], ballsY[1]),
            getAngle(ballsX[2], ballsY[2])
        };

        NetworkTableInstance.getDefault().getEntry("ball1").setDoubleArray(new double[]{ballsX[0], ballsY[0], radius[0], angles[0]});
        NetworkTableInstance.getDefault().getEntry("ball2").setDoubleArray(new double[]{ballsX[1], ballsY[1], radius[1], angles[1]});
        NetworkTableInstance.getDefault().getEntry("ball3").setDoubleArray(new double[]{ballsX[2], ballsY[2], radius[2], angles[2]});

        for (int i = 0; i < Math.min(3, circles.cols()); i++) {
            double[] data = circles.get(0, i);
//                847.0, 681.0
            double x = data[0];
            double y = data[1];
            int r = (int) data[2];

            Point center = new Point(x, y);

            NetworkTableInstance.getDefault().getEntry("ball").setDoubleArray(new double[]{x, y, (double) r});

            if (y > cols / 2) {
                // circle center
                Imgproc.circle(frameMat, center, 3, new Scalar(0, 255, 0, 128), 3);
                Imgproc.circle(overlayMat, center, 3, new Scalar(0, 255, 0, 128), 3);
                // circle outline
                Imgproc.circle(frameMat, center, r, new Scalar(0, 0, 255, 64), 5 - i);
                Imgproc.circle(overlayMat, center, r, new Scalar(0, 0, 255, 128), 10 - (2 * i));

                drawText(overlayMat, angles[i] + "", ballsX[i] - (radius[i] / 2), ballsY[i]);
            }
        }


//        drawText(overlayMat, "[3]", ballsX[1] - (radius[1] / 2), ballsY[1]);
//
//        drawText(overlayMat, "[3]", ballsX[2] - (radius[2] / 2), ballsY[2]);

        Imgproc.line(frameMat, new Point(VISION_WIDTH, 0), new Point(VISION_WIDTH, VISION_WIDTH), new Scalar(0, 0, 0, 150), 3);
        Imgproc.line(frameMat, new Point(0, 0), new Point(VISION_WIDTH, VISION_WIDTH), new Scalar(0, 0, 0, 150), 3);
//        Imgproc.line( frameMat, new Point(VISION_WIDTH,VISION_WIDTH), new Point(VISION_WIDTH,VISION_WIDTH), new Scalar(0,0,0,150), 3);
//        Imgproc.line( frameMat, new Point(VISION_WIDTH,0), new Point(VISION_WIDTH,VISION_WIDTH), new Scalar(0,0,0,150), 3);

        System.out.println(" processFrame time " + (System.currentTimeMillis() - t0) + "ms");
    }

    static public double getAngle(double x_, double y_) {
//        x_ -= (VISION_WIDTH/2);
//        y_ -= (VISION_WIDTH/2);
        x_ -= 500;

//        return x_;// /500*45;
//        return (int)(Math.toDegrees(Math.atan(x_/y_)));
        return (int) Math.toDegrees(Math.atan(x_ / (y_ / 2)));
//        pixelX_ = robot_.constants_.FISHEYE_CAMERA_FOCAL_LENGTH * Math.tan(Math.toRadians(Math.max(-horizontalFOV_/2., Math.min(horizontalFOV_/2., azimuth))));
    }

    public static class Setting {

        public String name_;
        public JLabel nameLabel_;
        public JLabel valueLabel_;
        public AtomicInteger value_;
        public JSlider slider_;

        public Setting(String name, int min, int max, int value) {
            name_ = name;
            value_ = new AtomicInteger(value);
            nameLabel_ = new JLabel(name);
            valueLabel_ = new JLabel("" + value_.get());
            slider_ = new JSlider(min, max, value);
            slider_.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    value_.set(slider_.getValue());
                    valueLabel_.setText("" + value_.get());
                }
            });
        }

        public int value() {
            return value_.get();
        }

        public static Map<String, Setting> initializeSettings(final String title, final Setting... settings) {
            Map<String, Setting> map = new HashMap<>();
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        JComponent panel = new JPanel(new GridLayout(0, 3));
                        for (Setting setting : settings) {
                            map.put(setting.name_, setting);
                            panel.add(setting.nameLabel_);
                            panel.add(setting.slider_);
                            panel.add(setting.valueLabel_);
                        }
                        JFrame controlFrame = new JFrame(title);
                        controlFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                        controlFrame.setAlwaysOnTop(true);
                        controlFrame.setContentPane(panel);
                        controlFrame.pack();
                        controlFrame.setVisible(true);
                    }
                });
            } catch (Exception ex) {
                // TODO: complain
            }
            return map;
        }
    }

    private NetworkTableEntry getNTE(String key) {
        NetworkTableEntry nte;
        if (NTMain.networkTable_ != null
                && ((nte = nte_.get(key)) != null
                || ((nte = NTMain.networkTable_.getEntry(key)) != null
                && nte_.put(key, nte) == null))) {
            return nte;
        }
        return null;
    }


    private void drawText(Mat mat, String text, double x, double y) {
        Imgproc.putText(mat, text, new Point(x, y), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 0, 0, 255), 2);
        Imgproc.putText(mat, text, new Point(x, y), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255, 255, 255, 255), 1);
    }

}
