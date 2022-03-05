package org.usfirst.frc.team2077.vision.processors;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import org.opencv.core.Point;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.usfirst.frc.team2077.vision.Main.FrameProcessor;
import org.usfirst.frc.team2077.vision.NTMain;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * This class is the replacement of the HSVFilter_MERGED which now is being deprecated. The goal is to
 * switch the filter to a more external process which can be called static-aly.
 */
public class BallDetection implements FrameProcessor {

    /* START CONSTENTS */
    public static final int VISION_WIDTH = 1_000;//TODO: implement get rows or colloms
    public static final int VISION_DEGREES = 90;


    private final static Map<String, Setting> settings_ = Setting.initializeSettings( "HSVFilter Settings",

            new BallDetection.Setting( "H min", 0, 255, 120 ), // BLUE 0, RED: 100
            new BallDetection.Setting( "H max", 0, 255, 150 ), // 50, 150
            new BallDetection.Setting( "S min", 0, 255, 126 ), // 104, 136
            new BallDetection.Setting( "S max", 0, 255, 255 ), // 255, 255
            new BallDetection.Setting( "V min", 0, 255, 56 ),  // 23, 119
            new BallDetection.Setting( "V max", 0, 255, 255 ), //255, 255
            new BallDetection.Setting( "R min", 0, 255, 30 ),
            new BallDetection.Setting( "R max", 0, 255, 130 ),
            new BallDetection.Setting( "Threshold", 0, 255, 65 ),
            new BallDetection.Setting("Alliance",0,1,1, "Red", "Blue"));

    public static class Setting {

        public String name_;
        public JLabel nameLabel_;
        public JLabel valueLabel_;
        public AtomicInteger value_;
        public JSlider slider_;

        public JCheckBox checkBox_;//TODO: Make JComponent so it works with other buttons as well?
        public String onName_;
        public String offName_;

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
//        public Setting( String name, String onName, String offName, boolean state ) {
        public Setting( String name, int min, int max, int value, String onName, String offName ) {
            name_ = name;
            onName_ = onName;
            offName_ = offName;
            value_ = new AtomicInteger( value );
            nameLabel_ = new JLabel( name );
            valueLabel_ = new JLabel( "" + (value_.get()==1? onName:offName) );
//            checkBox_ = new JCheckBox( "" + (value_.get()==1? onName:offName));
            slider_ = new JSlider( min, max, value );
            slider_/*checkBox_*/.addChangeListener( new ChangeListener() {
                @Override
                public void stateChanged( ChangeEvent e ) {
                    value_.set( slider_.getValue()/*(value_.equals(0)?1:0)*/ );
                    valueLabel_.setText( "" + (value_.get()==1? onName:offName) );
                }
            } );
        }
//        public Setting( String name, String offName, String onName, boolean state ) {
//            name_ = state? onName:offName;
//            onName_ = onName;
//            offName_ = offName;
//            value_ = new AtomicInteger( (int) ((state)?0:1) );
//            nameLabel_ = new JLabel( name );
//            valueLabel_ = new JLabel( "" + (value_.get()==1? onName_:offName_) );
////            valueLabel_ = new JLabel( "" + value_.get() );
////            checkBox_ = new JCheckBox( );
//            slider_ = new JSlider( 0, 1, value_.get());
//            slider_.addChangeListener( new ChangeListener() {
//                @Override
//                public void stateChanged( ChangeEvent e ) {
//                    value_.set( slider_.getValue() );//store boolean as atomic integer to keep variables similar
//                    valueLabel_.setText( "" + value_.get());//(value_.get()==1? onName_:offName_ ));
//                }
//            } );
//        }

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
                        controlFrame.setAlwaysOnTop( false );
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

    public static Ball[] findBallLocations(Mat frameMat, Mat overlayMat) {

        Map<String, NetworkTableEntry> nte_ = new TreeMap<>();

        Mat rgb = new Mat();
        Mat hsv = new Mat();
        Mat gray = new Mat();

        List<Ball> ballsList = new LinkedList<Ball>();//TODO: Care about under center
//        List<Ball> ballsList = new LinkedList<Ball>();

        long t0 = System.currentTimeMillis();

        Imgproc.cvtColor(frameMat, hsv, Imgproc.COLOR_RGB2HSV);


        Imgproc.GaussianBlur(hsv, hsv, new Size(5, 5), 1);
        // Core.inRange(hsv, new Scalar(27, 75, 120), new Scalar(31, 255, 255), hsv);
        Core.inRange(hsv, new Scalar(
                (settings_.get("Alliance").value() == 0? 0  : 100), // HMin
                (settings_.get("Alliance").value() == 0? 92: 136), // SMin
                (settings_.get("Alliance").value() == 0? 64 : 119)), // HVMin

                new Scalar(
                        (settings_.get("Alliance").value() == 0? 50 : 150),
                        255,
                        255), gray);


        Imgproc.GaussianBlur(gray, gray, new Size(25, 25), 0, 0);

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

//        Imgproc.findContours
//        Imgproc.findContours(gray, circles, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        Imgproc.HoughCircles(gray, circles, Imgproc.CV_HOUGH_GRADIENT, accumulatorScale, circleProximity, cannyThreshhold, circleThreshold, circleSizeRange[0], circleSizeRange[1]);

        // Imgproc.Canny(gray, gray, Math.max(1, cannyThreshhold/2), cannyThreshhold); // approximates internal
        // HoughCircles processing

        Imgproc.cvtColor(gray, frameMat, Imgproc.COLOR_GRAY2BGRA);

//        System.out.println("#rows " + circles.rows() + " #cols " + circles.cols());


        for (int i = 0; i < Math.min(5, circles.cols()); i++) {
            double[] data = circles.get(0, i);

            int x = (int) data[0];
            int y = (int) data[1];
            int r = (int) data[2];


            if(DisplayOverlay.FLAG_DEBUGLINE)Imgproc.line( overlayMat, new Point(0,VISION_WIDTH), new Point(VISION_WIDTH, VISION_WIDTH), DisplayOverlay.COLOR_DEBUG_MIDLINE, 3);
            if(y >= VISION_WIDTH/2){
                Point center = new Point(x, y);
                System.out.println("CIRCLE " + i + " " + center + " " + r);

                NetworkTableInstance.getDefault().getEntry("ball").setDoubleArray(new double[]{x, y, (double) r});

                // all circle outline
                Imgproc.circle(frameMat, center, (int)r, DisplayOverlay.ALL_OVERLAYMAT_BALL_OUTLINE, 3);
                Imgproc.circle(overlayMat, center, (int)r, DisplayOverlay.ALL_OVERLAYMAT_BALL_OUTLINE, 3);
            }

            ballsList.add(new Ball(x,y+frameMat.rows(),r));
        }

        ballsList.sort(Comparator.reverseOrder());
        Ball[] balls = new Ball[ballsList.size()];
        if(DisplayOverlay.FLAG_DEBUG_ALL_BALLS_INFO){System.out.println("========================");}
        for(int i = 0; i<ballsList.size(); i++){
            balls[i] = ballsList.get(i);
            if(DisplayOverlay.FLAG_DEBUG_ALL_BALLS_INFO){
                Ball tempBall = balls[i];

                System.out.println("> Ball #["+i+"]");
                System.out.println("    |-(r)-> "+tempBall.radius());
                System.out.println("    |-(a)-> "+ tempBall.angleHoriz());
                System.out.println("    |-(x)-> "+tempBall.x());
                System.out.println("    \\-(y)-> "+tempBall.y());
//                System.out.println();
            }
        }
        if(DisplayOverlay.FLAG_DEBUG_ALL_BALLS_INFO){System.out.println("========================");}


        for (int i = 0; i < Math.min(5, circles.cols()); i++) {

            double[] presentCiricleData = circles.get(0, i);

            double x = presentCiricleData[0];
            double y = presentCiricleData[1];
            int r = (int) presentCiricleData[2];
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

        _1A = (balls.length > 0 && balls[0] != null)? balls[0].angleHoriz() : -1;
        _2A = (balls.length > 1 && balls[1] != null)? balls[1].angleHoriz() : -1;
        _3A = (balls.length > 2 && balls[2] != null)? balls[2].angleHoriz() : -1;


//        FILL the MAIN ball
        if(balls.length > 0){
            Imgproc.circle(frameMat, balls[0].point(), 0, DisplayOverlay.MAIN_BALL_FILL_COLOR, 2*(int)(balls[0].radius()*.75));
            Imgproc.circle(overlayMat, balls[0].point(), 0, DisplayOverlay.MAIN_BALL_FILL_COLOR, 2*(int)(balls[0].radius()*.75));
        }

        for(int i = 1; i<3 && i<balls.length; i++){

            Point temp = balls[i].point();
//            if(!temp.equals(new Point(_1X,_1Y))){
            // Circle center

            // Circle outline
            Imgproc.circle(frameMat, temp, balls[i].radius(), DisplayOverlay.FRAMEMAT_BALL_OUTLINE, 5);
            Imgproc.circle(overlayMat, temp, balls[i].radius(), DisplayOverlay.OVERLAYMAT_BALL_OUTLINE, 5);

        }
//        HEAD CIRCLE
        if(balls.length > 0){
            Imgproc.circle(frameMat, balls[0].point(), (int) balls[0].radius(), DisplayOverlay.MAIN_BALL_OUTLINE_COLOR, 5);
            Imgproc.circle(overlayMat, balls[0].point(), (int) balls[0].radius(), DisplayOverlay.MAIN_BALL_OUTLINE_COLOR, 5);
        }


//DisplayOverlay.drawText
        if(DisplayOverlay.FLAG_DEBUG_ANGLE_IN_CENTER)
            DisplayOverlay.drawText(overlayMat, "("+_1A+""+")", VISION_WIDTH/2-40, VISION_WIDTH/2);

        try{//TODO: Should I keep this or just make everything it's .length? Also kinda on a time crunch
//            if(balls[0].radius() > 0 && FLAG_BALLTEXTLABLES){
//                DisplayOverlay.drawText(overlayMat, ">[1]<", balls[0].x()-(balls[0].radius()/2), balls[0].y());
//                if(balls[1].radius() > 0) {
//                    DisplayOverlay.drawText(overlayMat, "[2]", balls[1].x()-(balls[1].radius()/2), balls[1].y());
//                    if(balls[2].radius() > 0)
//                        DisplayOverlay.drawText(overlayMat, "[3]", balls[2].x()-(balls[2].radius()/2), balls[2].y());
//                }
//            }
            if(balls[0].radius() > 0 && DisplayOverlay.FLAG_BALL_TEXT_LABELS){
                DisplayOverlay.drawText(overlayMat, balls[0].angleHoriz()+"", balls[0].x()-(balls[0].radius()/2), balls[0].y());
                if(balls[1].radius() > 0) {
                    DisplayOverlay.drawText(overlayMat, "[2]", balls[1].x()-(balls[1].radius()/2), balls[1].y());
                    if(balls[2].radius() > 0)
                        DisplayOverlay.drawText(overlayMat, "[3]", balls[2].x()-(balls[2].radius()/2), balls[2].y());
                }
            }
        }catch(ArrayIndexOutOfBoundsException e){
//            Do nothing, this is going to be happening more times then not, it will run up to where it needs to
        }

//OUTPUT TO SMARTDASHBOARD
        if(DisplayOverlay.FLAG_SMARTDASHBOARD){//TODO: Do something else here now
        }

        Imgproc.line( frameMat, new Point(VISION_WIDTH,0), new Point(VISION_WIDTH,VISION_WIDTH), new Scalar(0,0,255,150), 3);
        Imgproc.line( frameMat, new Point(0,0), new Point(VISION_WIDTH,VISION_WIDTH), new Scalar(0,0,255,150), 3);
//        Imgproc.line( frameMat, new Point(VISION_WIDTH,VISION_WIDTH), new Point(VISION_WIDTH,VISION_WIDTH), new Scalar(0,0,0,150), 3);
//        Imgproc.line( frameMat, new Point(VISION_WIDTH,0), new Point(VISION_WIDTH,VISION_WIDTH), new Scalar(0,0,0,150), 3);

//        System.out.println( " processFrame time " + (System.currentTimeMillis() - t0) + "ms" );


//        frameMat.copyTo(overlayMat);

//        frameMat.copyTo(secondMat);


        return balls;
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

    @Override
    public void processFrame(Mat frameMat, Mat overlayMat) {

    }
}
