package org.usfirst.frc.team2077.vision.processors;

import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.cfg.ContextAttributes;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.usfirst.frc.team2077.video.Main;
import org.usfirst.frc.team2077.vision.Main.FrameProcessor;
import org.usfirst.frc.team2077.vision.NTMain;

import edu.wpi.first.networktables.NetworkTableEntry;

public class DisplayOverlay implements FrameProcessor {
//    OVERLAYS

    /*           FLAGS               */
    public static final boolean FLAG_DEBUGLINE = false;
    public static final boolean FLAG_ISPIZZA = true;
    public static final boolean FLAG_SMARTDASHBOARD = false;
    public static final boolean FLAG_BALL_TEXT_LABELS = true;
    public static final boolean FLAG_DEBUG_ANGLE_IN_CENTER = false;//TODO: Make changeable
    public static final boolean FLAG_CROPPING_VISION_INPUT_DEBUGGING = false;
    public static final boolean FLAG_DEBUG_ALL_BALLS_INFO = false;
    public static final boolean FIND_BALLS = true;

    //COLORS
    public static final Scalar RED = new Scalar(0,0,255,255);
    public static final Scalar GREEN = new Scalar(0,255,0,255);
    public static final Scalar WHITE = new Scalar(255,255,255,255);
    public static final Scalar GREY = new Scalar(180,180,180,255);
    public static final Scalar BLACK = new Scalar(0,0,0,255);
    public static final Scalar PURPLELY = new Scalar(255, 0, 255, 255);
    //    public static final Scalar BLOOD_ORANGE = new Scalar(255, 93, 64, 255);
    public static final Scalar BLOOD_ORANGE = new Scalar(93, 64, 255, 120);

    //USED COLORS
    public static final Scalar COLOR_DEBUG_MIDLINE = GREY;
    public static final Scalar MAIN_BALL_FILL_COLOR = PURPLELY;
    public static final Scalar MAIN_BALL_OUTLINE_COLOR = GREEN;
    public static final Scalar BALL_FILL_COLOR = BLOOD_ORANGE;
    public static final Scalar TEXT_BACKGROUND_COLOR = GREY;
    public static final Scalar FRAMEMAT_BALL_OUTLINE = GREY;//new Scalar(0, 0, 255, 64);
    public static final Scalar OVERLAYMAT_BALL_OUTLINE = BLOOD_ORANGE;// new Scalar(0, 0, 255, 128);
    public static final Scalar ALL_OVERLAYMAT_BALL_OUTLINE = BLACK;
    /* END CONSTENTS */


    public static final int MAT_DISPLAY = 0; // 0 default camera stream (ready for comp), 1 is hsv overlay

    private Map<String,NetworkTableEntry> nte_ = new TreeMap<>();

    private final static String BALL_ANGLE_KEY = "ball_angle";
    private final static String BALL_DISTANCE_KEY = "ball_distance";

    @Override
    public void processFrame( Mat frameMat, Mat overlayMat ) {


        Rect rectCrop = new Rect(0,frameMat.rows()/2,frameMat.cols(),frameMat.rows()/2);
        if(FLAG_CROPPING_VISION_INPUT_DEBUGGING){
            System.out.println("rectCrop.x = "+rectCrop.x);
            System.out.println("rectCrop.y = "+rectCrop.y);
            System.out.println("rectCrop.width = "+rectCrop.width);
            System.out.println("rectCrop.height = "+rectCrop.height);
            System.out.println("framemat.col = "+frameMat.cols());
            System.out.println("framemat.row = "+frameMat.rows());
        }

        Mat image_output = frameMat.submat(rectCrop);
        frameMat = image_output;

        int rows = overlayMat.rows();
        int cols = overlayMat.cols();

        Imgproc.line(overlayMat, new Point(cols/2, 0), new Point(cols/2, rows), GREEN, 1);

        NetworkTableEntry nte;

        if ( (nte = getNTE("setPointShoot")) != null ) {
            double setPoint = nte.getDouble(0);
            if (setPoint >= 0) {
                String setPointS = "Target RPM:" + (Math.round(setPoint*10.)/10.);
                drawText(overlayMat, setPointS, 20, rows/2+80, TEXT_BACKGROUND_COLOR);
            }
        }

        if ( (nte = getNTE("ReadyShoot")) != null ) {
            boolean ready = nte.getBoolean(false);
            drawText(overlayMat, "Shoot?: ", 20, rows/2-160, TEXT_BACKGROUND_COLOR);
            Imgproc.circle(overlayMat, new Point(155, rows/2-150), 5, ready ? GREEN : RED, 9);
        }



        if(FIND_BALLS){
            Ball[] foundBallLocations = BallDetection.findBallLocations(frameMat, overlayMat);

            if(foundBallLocations.length > 0) {
                SmartDashboard.getEntry(BALL_ANGLE_KEY).setDouble(foundBallLocations[0].angleHoriz());
//                SmartDashboard.getEntry(BALL_DISTANCE_KEY).setDouble(foundBallLocations[0].distance());
            }


            if(FLAG_DEBUG_ALL_BALLS_INFO){
                for(Ball ball: foundBallLocations){
                    System.out.print("findBallLocations = ");
                }
            }
        }

        switch(MAT_DISPLAY){
            case(0):
                break;
            case(1):
                frameMat.copyTo(overlayMat);
                break;
        }

    }

    private static void drawText(Mat mat, String text, double x, double y, Scalar boxColor){
        Imgproc.rectangle(mat, new Point(x-3,y+6), new Point(x+text.length()*14.55+6,y-26), boxColor,-1);
        drawText(mat, text, x, y);
    }

    public static void drawText(Mat mat, String text, double x, double y) {
        Imgproc.putText( mat, text, new Point(x, y), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0,0,0,255), 2 );
//        Imgproc.putText( mat, text, new Point(x, y), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255,255,255,255), 2 );
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
