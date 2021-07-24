package org.usfirst.frc.team2077.vision.processors;

import java.util.Map;
import java.util.TreeMap;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.usfirst.frc.team2077.vision.Main.FrameProcessor;
import org.usfirst.frc.team2077.vision.NTMain;

import edu.wpi.first.networktables.NetworkTableEntry;

public class AimingOverlay implements FrameProcessor {

    private Map<String,NetworkTableEntry> nte_ = new TreeMap<>();

    @Override
    public void processFrame( Mat frameMat, Mat overlayMat ) {
        if(HSV_Module.FLAG_ISPIZZA) {
            Mat tempMap = frameMat.clone();//TODO: Remove this
            Core.rotate(tempMap, frameMat, Core.ROTATE_90_CLOCKWISE);//TODO: Autodetect type
//            tempMap = overlayMat.clone();
            Core.rotate(tempMap, overlayMat, Core.ROTATE_90_CLOCKWISE);//TODO: Autodetect type
        }
        Rect rectCrop = new Rect(0,frameMat.rows()/2,frameMat.cols(),frameMat.rows()/2);
        if(HSV_Module.FLAG_CROPPING_VISION_INPUT_DEBUGGING){
            System.out.println("rectCrop.x = "+rectCrop.x);
            System.out.println("rectCrop.y = "+rectCrop.y);
            System.out.println("rectCrop.width = "+rectCrop.width);
            System.out.println("rectCrop.height = "+rectCrop.height);
            System.out.println("framemat.col = "+frameMat.cols());
            System.out.println("framemat.row = "+frameMat.rows());
        }

        Mat image_output = frameMat.submat(rectCrop);
        frameMat = image_output;

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
        if ( (nte = getNTE("Position")) != null ) {
            double[] position = nte.getDoubleArray(new double[0]);
            if (position != null && position.length >= 3) {
                String location = "Location:" + (Math.round(position[0]*10.)/10.) + "N " + (Math.round(position[1]*10.)/10.) + "E";
                String heading = "Heading:" + (Math.round(position[2]*10.)/10.);
                drawText(overlayMat, location, 20, rows/2-80);
                drawText(overlayMat, heading, 20, rows/2-40);
            }
        }

        if ( (nte = getNTE("setPointShoot")) != null ) {
            double setPoint = nte.getDouble(0);
            if (setPoint >= 0) {
                String setPointS = "SetPoint:" + (Math.round(setPoint*10.)/10.);
                drawText(overlayMat, setPointS, 20, rows/2+80);
            }
        }

        if ( (nte = getNTE("RangeAV")) != null ) {
            double[] rangeAV = nte.getDoubleArray(new double[]{0, 0});

            String angle = "Angles:" + (Math.round(rangeAV[0] *100.)/100.);
            drawText(overlayMat, angle, 20, rows/2+35);

            String velocity = "Velocity:" + Math.round(rangeAV[1]);
            drawText(overlayMat, velocity, 20, rows/2 - 6);
        }
//        System.out.println(nte);

        if ( (nte = getNTE("ReadyShoot")) != null ) {
            boolean ready = nte.getBoolean(false);
            drawText(overlayMat, "Shoot?: ", 20, rows/2+160);
            Imgproc.circle(overlayMat, new Point(155, rows/2+150), 5, ready ? green : red, 9);
        }

//        AJ AJ_HSV_Module addition and testing code
//        TODO: Change to nte check
        if(true){
            Ball[] foundBallLocations = HSV_Module.findBallLocations(frameMat, overlayMat);
            System.out.print("findBallLocations = ");
            for(Ball ball: foundBallLocations){

            }
        }

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
