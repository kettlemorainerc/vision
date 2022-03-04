package org.usfirst.frc.team2077.vision.processors;

import java.util.Map;
import java.util.TreeMap;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.usfirst.frc.team2077.vision.Main.FrameProcessor;
import org.usfirst.frc.team2077.vision.NTMain;

import edu.wpi.first.networktables.NetworkTableEntry;

public class AimingOverlay implements FrameProcessor {

    private Map<String,NetworkTableEntry> nte_ = new TreeMap<>();
    private final static String BALL_ANGLE_KEY = "ball_angle";
    private final static String BALL_DISTANCE_KEY = "ball_distance";

    @Override
    public void processFrame( Mat frameMat, Mat overlayMat ) {
        if(DisplayOverlay.FLAG_ISPIZZA) {
            Mat tempMap = frameMat.clone();//TODO: Remove this
            Core.rotate(tempMap, frameMat, Core.ROTATE_90_CLOCKWISE);//TODO: Autodetect type
            tempMap = overlayMat.clone();
            Core.rotate(tempMap, overlayMat, Core.ROTATE_90_CLOCKWISE);//TODO: Autodetect type
        }
//        Rect rectCrop = new Rect(0,frameMat.rows()/2,frameMat.cols(),frameMat.rows()/2);
        Rect rectCrop = new Rect(0,frameMat.rows(),frameMat.cols(),frameMat.rows());
        if(DisplayOverlay.FLAG_CROPPING_VISION_INPUT_DEBUGGING){
            System.out.println("rectCrop.x = "+rectCrop.x);
            System.out.println("rectCrop.y = "+rectCrop.y);
            System.out.println("rectCrop.width = "+rectCrop.width);
            System.out.println("rectCrop.height = "+rectCrop.height);
            System.out.println("framemat.col = "+frameMat.cols());
            System.out.println("framemat.row = "+frameMat.rows());
        }

//        Mat image_output = frameMat.submat(rectCrop);
//        frameMat = image_output;

        Scalar red = new Scalar(0,0,255,255);
        Scalar green = new Scalar(0,255,0,255);
        Scalar white = new Scalar(255,255,255,255);
        Scalar black = new Scalar(0,0,0,255);

        int rows = overlayMat.rows();
        int cols = overlayMat.cols();

        Imgproc.line(overlayMat, new Point(cols/2, 0), new Point(cols/2, rows), green, 1);

        NetworkTableEntry nte;
        if ( (nte = getNTE("Crosshairs")) != null ) {
            double[] crosshairs = nte.getDoubleArray(new double[0]);
            if (crosshairs != null && crosshairs.length >= 4) {
                // draw crosshairs
                double w = crosshairs[2];
                double h = crosshairs[3];
                double x = Math.round(crosshairs[0] + w/2);
                double y = rows - Math.round(crosshairs[1] + h/2);
                Imgproc.line( overlayMat, new Point((int)x, 0), new Point((int)x, rows), white, 1);
                Imgproc.line( overlayMat, new Point(0, (int)y), new Point(cols, (int)y), white, 1);
            }
        }


//        if ( (nte = getNTE("setPointShoot")) != null ) {
//            double setPoint = nte.getDouble(0);
//            if (setPoint >= 0) {
//                String setPointS = "SetPoint:" + (Math.round(setPoint*10.)/10.);
//                drawText(overlayMat, setPointS, 20, rows/2+80);
//            }
//        }

//        AJ AJ_HSV_Module addition and testing code
//        TODO: Change to nte check
        if(true){
            int frameRow = frameMat.rows(), frameCol = frameMat.cols();
            Ball[] foundBallLocations = BallDetection.findBallLocations(frameMat/*.submat(new Rect(0, (int) (frameCol * 0.6), frameRow, (int) (frameCol * 0.4) ))*/, overlayMat);

//            SmartDashboard.getEntry(BALL_ANGLE_KEY).setDouble(foundBallLocations[0].angleHoriz());
//            SmartDashboard.getEntry(BALL_DISTANCE_KEY).setDouble(foundBallLocations[0].distance());

//            System.out.print("findBallLocations = ");
//            for(Ball ball: foundBallLocations){
//            }
//            HoopVision.findReflectorLocations(frameMat.submat(new Rect(0, 0, frameRow, (int) (frameCol * 1))), overlayMat);
        }
//        frameMat.copyTo(overlayMat);
        overlayMat.copyTo(frameMat);
    }

    private void drawText(Mat mat, String text, double x, double y) {
        Imgproc.putText( mat, text, new Point(x, y), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0,0,0,255), 2 );
        Imgproc.putText( mat, text, new Point(x, y), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255,255,255,255), 1 );
    }

    private NetworkTableEntry getNTE( String key ) {
        NetworkTableEntry nte;
        if ( NTMain.networkTable_ != null
                && ( (nte = nte_.get(key)) != null
                || ( (nte = NTMain.networkTable_.getEntry(key)) != null
                && nte_.put(key, nte) == null ) ) ) {
            return nte;
        }
        return null;
    }


}
