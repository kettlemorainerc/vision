package org.usfirst.frc.team2077.vision.processors;

import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.cfg.ContextAttributes;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.networktables.NetworkTableInstance;

import org.ejml.data.BMatrixRMaj;
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
    public static final boolean FLAG_ISPIZZA = false;
    public static final boolean FLAG_SMARTDASHBOARD = false;
    public static final boolean FLAG_BALL_TEXT_LABELS = false;
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
    private final static String VISION_DATA_KEY = "all_vision_data";
    private byte lastNTECall = (byte) 0b10000000;
//    private final static String BALL_DISTANCE_KEY = "ball_distance";

    @Override
    public void processFrame( Mat frameMat, Mat overlayMat ) {

        Rect rectCrop = new Rect(0,frameMat.rows()/2,frameMat.cols(),frameMat.rows()/2);
        if( FLAG_CROPPING_VISION_INPUT_DEBUGGING){
            System.out.println("rectCrop.x = "+rectCrop.x);
            System.out.println("rectCrop.y = "+rectCrop.y);
            System.out.println("rectCrop.width = "+rectCrop.width);
            System.out.println("rectCrop.height = "+rectCrop.height);
            System.out.println("framemat.col = "+frameMat.cols());
            System.out.println("framemat.row = "+frameMat.rows());
        }

        Mat ball1submat = frameMat.submat(rectCrop);
//        ball1submat.setTo(new Scalar(255,0,255,255));
        Imgproc.rectangle(overlayMat, new Point(rectCrop.x, rectCrop.y), new Point(rectCrop.x + ball1submat.width(), rectCrop.y+ball1submat.height()), GREEN, 5);
        Mat area = new Mat(overlayMat.rows(), overlayMat.cols(), overlayMat.type());
        Imgproc.rectangle(area, new Point(0, 750), new Point(260,1000), new Scalar(255,255,255,255), -1);
        Imgproc.rectangle(area, new Point(1000, 750), new Point(1000-260,1000), new Scalar(255,255,255,255), -1);

        Imgproc.rectangle(area, new Point(300, 600), new Point(1000-300,750), new Scalar(255,255,255,255), -1);


        overlayMat.setTo(new Scalar(255,255,255,50), area);
        frameMat.setTo(new Scalar(0,0,0,255), area);
//        overlayMat.setTo(new Scalar(0,0,0,255), area);
//        area.copyTo(overlayMat);
//        ball1submat.copyTo(overlayMat);
//        frameMat = ball1submat;

        int rows = overlayMat.rows();
        int cols = overlayMat.cols();

        Imgproc.line(overlayMat, new Point(cols/2, 0), new Point(cols/2, rows), GREEN, 1);

        NetworkTableEntry nte;
        if ( (nte = getNTE("launcher_RPM")) != null ) {
            double setPoint = NetworkTableInstance.getDefault().getEntry("launcher_RPM").getDouble(4_000);
            String setPointS = "Target RPM:" + (Math.round(setPoint*10.)/10.);
            drawText(overlayMat, setPointS.substring(0, setPointS.indexOf('.')), 20, rows/2-200, TEXT_BACKGROUND_COLOR);
        }else{
            NetworkTableInstance.getDefault().getEntry("launcher_RPM").setDouble(4_000);
        }

        if ( (nte = getNTE("ReadyShoot")) != null ) {
            boolean ready = nte.getBoolean(false);
            drawText(overlayMat, "Shoot?: ", 20, rows/2-160, TEXT_BACKGROUND_COLOR);
            Imgproc.circle(overlayMat, new Point(155, rows/2-170), 5, ready ? GREEN : RED, 9);
        }


        if(FIND_BALLS){// && BallDetection.settings_.get("Detection").value() == 0){
//            Rect ballRect = new Rect(0,(int) (frameMat.rows() * 0.5), frameMat.cols(), (int) (frameMat.rows() * 0.5));
            Ball[] foundBallLocations = BallDetection.findBallLocations(frameMat, overlayMat);//.submat(ballRect));

            double angle = 0;
            double distance = 0;
            byte mask = 0;
            boolean seesBall = foundBallLocations.length > 0;
            if(seesBall) {
                angle = foundBallLocations[0].angleHoriz();
                distance = foundBallLocations[0].distance();
            }

            boolean direction = angle > 0;
            // 0000000X
            mask |= direction? 1 : 0;

            int rotateSpeedOrdinal = Speed.forAngle(seesBall, angle).ordinal();
            // 00000XX0
            mask |= rotateSpeedOrdinal << 1;

            Speed moveSpeed = Speed.forDistance(distance);
            // 000XX000

            mask |= (rotateSpeedOrdinal == 0? moveSpeed.ordinal() : 0) << 3;

            // 00X00000
            boolean runObtainer = distance <= 40;
            mask |= (runObtainer? 1 : 0) << 5;


            if(lastNTECall != mask && seesBall) {
                lastNTECall = mask;
//                System.out.println(distance);

                SmartDashboard.getEntry(VISION_DATA_KEY).setNumber(mask);
                System.out.println(String.format("%8s", Integer.toBinaryString(mask & 0xFF)).replace(' ', '0'));
            }else if(lastNTECall == (byte) 0b10000000){
                lastNTECall = (byte) 0b11000000;
                SmartDashboard.getEntry(VISION_DATA_KEY).setNumber((byte) 0b00000110);
//                System.out.println(String.format("%8s", Integer.toBinaryString(thisIsOnlyHereSoICanPrintAValueLolSorryAJOopsSorryLOLShouldBeCapitalisedLMAO & 0xFF)).replace(' ', '0'));
            }


            if(FLAG_DEBUG_ALL_BALLS_INFO){
                for(Ball ball: foundBallLocations){
                    System.out.print("findBallLocations = ");
                }
            }
        }else if(FIND_BALLS && BallDetection.settings_.get("Detection").value() == 1){
            Rect hoopRect = new Rect(0,0, frameMat.cols(), (int) (frameMat.rows() * 0.30));
            Ball[] foundBallLocations = BallDetection.findBallLocations(frameMat.submat(hoopRect), overlayMat);//.submat(ballRect));

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
        Imgproc.rectangle(mat, new Point(x-3,y+6), new Point(x+text.length()*17+10,y-26), boxColor,-1);
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

    public enum Speed{
        NONE, LOW, MID, HIGH;

        public static Speed forAngle(boolean seesBall, double angle){
            angle = Math.abs(angle);
            if(angle > 30 || !seesBall) {
                return HIGH;
            }else if(angle > 15){
                return MID;
            }else if(angle > 5){
                return LOW;
            }else {
                return NONE;
            }
        }
        public static Speed forDistance(double distance){
            if(distance > 300){
                return HIGH;
            }else if(distance > 175){
                return MID;
            }else if(distance > 100){
                return LOW;
            }else{
                return NONE;
            }
        }


    }

}
