package org.usfirst.frc.team2077.vision.processors;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import org.usfirst.frc.team2077.vision.Main.FrameProcessor;

import org.usfirst.frc.team2077.vision.Main;
import org.usfirst.frc.team2077.vision.NTMain;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * This class is the replacement of the HSVFilter_MERGED which now is being deprecated. The goal is to
 * switch the filter to a more external process which can be called static-aly.
 */
public class HSV_Module implements Main.FrameProcessor {

/*           FLAGS               */
    public static final boolean FLAG_DEBUGLINE = false;
    public static final boolean FLAG_ISPIZZA = false;
    public static final boolean FLAG_SMARTDASHBOARD = false;
    public static final boolean FLAG_BALLTEXTLABLES = false;
    public static final boolean FLAG_DEBUGANGLE_IN_CENTER = false;//TODO: Make changeable
    public static final boolean FLAG_CROPPING_VISION_INPUT_DEBUGGING = false;
    public static final boolean FLAG_DEBUG_ALL_BALLS_INFO = false;


            /* START CONSTENTS */
    public static final int VISION_WIDTH = 1_000;//TODO: implement get rows or colloms
    public static final int VISION_DEGREES = 90;


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
    public static final Scalar FRAMEMAT_BALL_OUTLINE = GREY;//new Scalar(0, 0, 255, 64);
    public static final Scalar OVERLAYMAT_BALL_OUTLINE = BLOOD_ORANGE;// new Scalar(0, 0, 255, 128);
    public static final Scalar ALL_OVERLAYMAT_BALL_OUTLINE = BLACK;
    /* END CONSTENTS */


    private final static Map<String, Setting> settings_ = Setting.initializeSettings( "HSVFilter Settings",

            new HSV_Module.Setting( "H min", 0, 255, 15 ),
            new HSV_Module.Setting( "H max", 0, 255, 49 ),
            new HSV_Module.Setting( "S min", 0, 255, 148 ),
            new HSV_Module.Setting( "S max", 0, 255, 255 ),
            new HSV_Module.Setting( "V min", 0, 255, 106 ),
            new HSV_Module.Setting( "V max", 0, 255, 255 ),
            new HSV_Module.Setting( "R min", 0, 255, 12 ),
            new HSV_Module.Setting( "R max", 0, 255, 95 ),
            new HSV_Module.Setting( "Threshold", 0, 255, 63 ) );

    public static class Setting {

        public String name_;
        public JLabel nameLabel_;
        public JLabel valueLabel_;
        public AtomicInteger value_;
        public JSlider slider_;

        public Setting( String name, int min, int max, int value ) {
            name_ = name;
            value_ = new AtomicInteger( value );
            nameLabel_ = new JLabel( name );
            valueLabel_ = new JLabel( "" + value_.get() );
            slider_ = new JSlider( min, max, value );
            slider_.addChangeListener( new ChangeListener() {
                @Override
                public void stateChanged( ChangeEvent e ) {
                    value_.set( slider_.getValue() );
                    valueLabel_.setText( "" + value_.get() );
                }
            } );
        }

        public int value() {
            return value_.get();
        }

        public static Map<String, Setting> initializeSettings( final String title, final Setting... settings ) {
            Map<String, Setting> map = new HashMap<>();
            try {
                SwingUtilities.invokeAndWait( new Runnable() {
                    @Override
                    public void run() {
                        JComponent panel = new JPanel( new GridLayout( 0, 3 ) );
                        for ( Setting setting : settings ) {
                            map.put( setting.name_, setting );
                            panel.add( setting.nameLabel_ );
                            panel.add( setting.slider_ );
                            panel.add( setting.valueLabel_ );
                        }
                        JFrame controlFrame = new JFrame( title );
                        controlFrame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
                        controlFrame.setAlwaysOnTop( true );
                        controlFrame.setContentPane( panel );
                        controlFrame.pack();
                        controlFrame.setVisible( true );
                    }
                } );
            } catch ( Exception ex ) {
                // TODO: complain
            }
            return map;
        }
    }

    public static double[] findBallLocations(Mat frameMat, Mat overlayMat) {

        Map<String, NetworkTableEntry> nte_ = new TreeMap<>();

        Mat rgb = new Mat();
        Mat hsv = new Mat();
        Mat gray = new Mat();

        List<Ball> ballsList = new LinkedList<Ball>();//TODO: Care about under center
//        List<Ball> ballsList = new LinkedList<Ball>();

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

        // gray 8-bit, single-channel, grayscale input image.
        // circles Output vector of found circles. Each vector is encoded as a 3-element floating-point vector
        // (x,y,radius) .
        // method Detection method, see cv::HoughModes. Currently, the only implemented method is HOUGH_GRADIENT
        // accumulatorScale Inverse ratio of the accumulator resolution to the image resolution. For example, if dp=1 ,
        // the accumulator has the same resolution as the input image. If dp=2 , the accumulator has half as big width
        // and height.
        // circleProximity Minimum distance between the centers of the detected circles. If the parameter is too small,
        // multiple neighbor circles may be falsely detected in addition to a true one. If it is too large, some circles
        // may be missed.
        // cannyThreshhold First method-specific parameter. In case of CV_HOUGH_GRADIENT , it is the higher threshold of
        // the two passed to the Canny edge detector (the lower one is twice smaller).
        // circleThreshold Second method-specific parameter. In case of CV_HOUGH_GRADIENT , it is the accumulator
        // threshold for the circle centers at the detection stage. The smaller it is, the more false circles may be
        // detected. Circles, corresponding to the larger accumulator values, will be returned first.
        // circleSizeRange[0] Minimum circle radius.
        // circleSizeRange[1] Maximum circle radius.
        Imgproc.HoughCircles(gray, circles, Imgproc.CV_HOUGH_GRADIENT, accumulatorScale, circleProximity, cannyThreshhold, circleThreshold, circleSizeRange[0], circleSizeRange[1]);

        // Imgproc.Canny(gray, gray, Math.max(1, cannyThreshhold/2), cannyThreshhold); // approximates internal
        // HoughCircles processing
        Imgproc.cvtColor(gray, frameMat, Imgproc.COLOR_GRAY2BGRA);

        System.out.println("#rows " + circles.rows() + " #cols " + circles.cols());


        for (int i = 0; i < Math.min(5, circles.cols()); i++) {
            double[] data = circles.get(0, i);

            int x = (int) data[0];
            int y = (int) data[1];
            int r = (int) data[2];


            if(FLAG_DEBUGLINE)Imgproc.line( overlayMat, new Point(0,VISION_WIDTH), new Point(VISION_WIDTH, VISION_WIDTH), COLOR_DEBUG_MIDLINE, 3);
            if(y >= VISION_WIDTH/2){
                Point center = new Point(x, y);
                System.out.println("CIRCLE " + i + " " + center + " " + r);

                NetworkTableInstance.getDefault().getEntry("ball").setDoubleArray(new double[]{x, y, (double) r});


                // all circle outline
                Imgproc.circle(frameMat, center, (int)r, ALL_OVERLAYMAT_BALL_OUTLINE, 3);
                Imgproc.circle(overlayMat, center, (int)r, ALL_OVERLAYMAT_BALL_OUTLINE, 3);
            }

            ballsList.add(new Ball(x,y+frameMat.rows(),r));
        }

        ballsList.sort(Comparator.reverseOrder());
        Ball[] balls = new Ball[ballsList.size()];
        if(FLAG_DEBUG_ALL_BALLS_INFO){System.out.println("========================");}
        for(int i = 0; i<ballsList.size(); i++){
            balls[i] = ballsList.get(i);
            if(FLAG_DEBUG_ALL_BALLS_INFO){
                Ball tempBall = balls[i];
                System.out.println("> Ball #["+i+"]");
                System.out.println("    |-(r)-> "+tempBall.radius());
                System.out.println("    |-(a)-> "+tempBall.angle());
                System.out.println("    |-(x)-> "+tempBall.x());
                System.out.println("    \\-(y)-> "+tempBall.y());
                System.out.println();
            }
        }
        if(FLAG_DEBUG_ALL_BALLS_INFO){System.out.println("========================");}


        for (int i = 0; i < Math.min(5, circles.cols()); i++) {

            double[] presentCiricleData = circles.get(0, i);

            double x = presentCiricleData[0];
            double y = presentCiricleData[1];
            int r = (int) presentCiricleData[2];

//            SUPER QUICK CODE - WILL BE CHANGED LATER
//            if(presentCiricleData[3] != 0.0){//Ignore if not existing
//            if(presentCiricleData[1] < (VISION_WIDTH/2)){//Ignore if above center
            if(presentCiricleData[1] >= (VISION_WIDTH/2)){//Ignore if above center

            }
        }
//SEND THE THREE CLOSET BALLS
/*          Description = networktablename
            Ball Closest = ball1
            Ball Second Closest = ball2
            Ball Third Closest = ball3
*/


        double _1A = 0.0;//TODO: Cleanup this thingamagigger
        double _2A = 0.0;
        double _3A = 0.0;
//Will compute angle from North center

        _1A = (balls.length > 0 && balls[0] != null)? balls[0].angle() : -1;
        _2A = (balls.length > 1 && balls[1] != null)? balls[1].angle() : -1;
        _3A = (balls.length > 2 && balls[2] != null)? balls[2].angle() : -1;


//        FILL the MAIN ball
        if(balls.length > 0){
            Imgproc.circle(frameMat, balls[0].point(), 0, MAIN_BALL_FILL_COLOR, 2*(int)(balls[0].radius()*.75));
            Imgproc.circle(overlayMat, balls[0].point(), 0, MAIN_BALL_FILL_COLOR, 2*(int)(balls[0].radius()*.75));
        }

        for(int i = 1; i<3 && i<balls.length; i++){

            Point temp = balls[i].point();
//            if(!temp.equals(new Point(_1X,_1Y))){
            // Circle center

            // Circle outline
            Imgproc.circle(frameMat, temp, balls[i].radius(), FRAMEMAT_BALL_OUTLINE, 5);
            Imgproc.circle(overlayMat, temp, balls[i].radius(), OVERLAYMAT_BALL_OUTLINE, 5);

        }
//        HEAD CIRCLE
        if(balls.length > 0){
            Imgproc.circle(frameMat, balls[0].point(), (int) balls[0].radius(), MAIN_BALL_OUTLINE_COLOR, 5);
            Imgproc.circle(overlayMat, balls[0].point(), (int) balls[0].radius(), MAIN_BALL_OUTLINE_COLOR, 5);
        }


//DRAWTEXT
        if(FLAG_DEBUGANGLE_IN_CENTER)
            drawText(overlayMat, "("+_1A+""+")", VISION_WIDTH/2-40, VISION_WIDTH/2);

        try{//TODO: Should I keep this or just make everything it's .length? Also kinda on a time crunch
            if(balls[0].radius() > 0 && FLAG_BALLTEXTLABLES){
                drawText(overlayMat, ">[1]<", balls[0].x()-(balls[0].radius()/2), balls[0].y());
                if(balls[1].radius() > 0) {
                    drawText(overlayMat, "[2]", balls[1].x()-(balls[1].radius()/2), balls[1].y());
                    if(balls[2].radius() > 0)
                        drawText(overlayMat, "[3]", balls[2].x()-(balls[2].radius()/2), balls[2].y());
                }
            }
        }catch(ArrayIndexOutOfBoundsException e){
//            Do nothing, this is going to be happening more times then not, it will run up to where it needs to
        }

//OUTPUT TO SMARTDASHBOARD
        if(FLAG_SMARTDASHBOARD){//TODO: Do something else here now
        }

        Imgproc.line( frameMat, new Point(VISION_WIDTH,0), new Point(VISION_WIDTH,VISION_WIDTH), new Scalar(0,0,255,150), 3);
        Imgproc.line( frameMat, new Point(0,0), new Point(VISION_WIDTH,VISION_WIDTH), new Scalar(0,0,255,150), 3);
//        Imgproc.line( frameMat, new Point(VISION_WIDTH,VISION_WIDTH), new Point(VISION_WIDTH,VISION_WIDTH), new Scalar(0,0,0,150), 3);
//        Imgproc.line( frameMat, new Point(VISION_WIDTH,0), new Point(VISION_WIDTH,VISION_WIDTH), new Scalar(0,0,0,150), 3);

        System.out.println( " processFrame time " + (System.currentTimeMillis() - t0) + "ms" );

        return new double[]{0.0};
    }



    private static NetworkTableEntry getNTE(String key, Map<String,NetworkTableEntry> nte_) {
        NetworkTableEntry nte;
        if ( NTMain.networkTable_ != null
                && ( (nte = nte_.get(key)) != null
                || ( (nte = NTMain.networkTable_.getEntry(key)) != null
                && nte_.put(key, nte) == null ) ) ) {
            return nte;
        }
        return null;
    }

    private static void drawText(Mat mat, String text, double x, double y) {
        Imgproc.putText( mat, text, new Point(x, y), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0,0,0,255), 2 );
        Imgproc.putText( mat, text, new Point(x, y), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255,255,255,255), 1 );
    }

    @Override
    public void processFrame(Mat frameMat, Mat overlayMat) {

    }
}