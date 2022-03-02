package org.usfirst.frc.team2077.vision.processors;

import org.opencv.core.Point;

public class Ball implements Comparable<Ball>{

    private int x = -1, y = -1, r = -1;
    private double a = -1.0;

    private static double width = BallDetection.VISION_WIDTH;
    private static double horizPixels = Math.sqrt(width);
    private static double focalLength = 334.0;
    private static double anglePerPixel = horizPixels/focalLength;


    public Ball(){
        x = 0;
        y = 0;
        r = 0;
        a = 0.0;
    }

    public Ball(int x_, int y_){
        x = x_;
        y = y_;
        a = getAngle(x,y);
    }

    public Ball(int x_, int y_, int r_) {
        x = x_;
        y = y_;
        r = r_;
        a = getAngle(x,y);
    }
    public Ball(int x_, int y_, int r_, double a_) {
        x = x_;
        y = y_;
        r = r_;
        a = a_;
    }

    public final Point point(){//YES, I know that this should be getPoint
        return new Point(x,y);
    }
    public final double angle(){
        return a;
    }
    public final int radius(){ return r; }
    public final int x(){ return x; }
    public final int y(){ return y; }


    private static double getAngle(int x_, int y_){

//        int returnDeg = (int) Math.toDegrees(Math.atan(x_-500/(y_/2)));
//        return (returnDeg != -90.0)? returnDeg:0.0;
        return (width/2.0 - x_) * anglePerPixel;

    }





    @Override
    public int compareTo(Ball comparedTo_) {
        return Integer.compare(r, comparedTo_.r);
    }

}
