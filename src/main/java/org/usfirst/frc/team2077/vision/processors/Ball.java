package org.usfirst.frc.team2077.vision.processors;

import org.opencv.core.Point;

public class Ball implements Comparable<Ball>{

    private int x, y, r = -1;
    private double ah = 0.0, av = 0.0;

    private static double width = 0; // BallDetection.VISION_WIDTH;
    private static double horizPixels = Math.sqrt(width * 2);
    private static double focalLength = 500;//334.0;
    private static double anglePerPixel = horizPixels/focalLength;

    private static double heightFromBall = 56.515; //TODO, set to robot camera height - ball radius/2


    public Ball(){
        r = 0;
    }

    public Ball(int x_, int y_){
        x = x_;
        y = y_;
        ah = getAngle(x);
        av = getAngle(y);
    }

    public Ball(int x_, int y_, int r_) {
        x = x_;
        y = y_;
        r = r_;
        ah = getAngle(x);
        av = getAngle(y);
    }
    public Ball(int x_, int y_, int r_, double a_) {
        x = x_;
        y = y_;
        r = r_;
        ah = a_;
        av = getAngle(y);
    }

    public final Point point(){//YES, I know that this should be getPoint
        return new Point(x,y);
    }
    public final double angleVert(){
        return av;
    }
    public final double angleHoriz(){ return ah; }
    public final int radius(){ return r; }
    public final int x(){ return x; }
    public final int y(){ return y; }
    public final double distance(){ return getDistance(av);}


    public static double getAngle(int coord_){
        return (width * 0.5 - coord_) * anglePerPixel;
    }

    public static double getDistance(double av_){
        return heightFromBall * Math.tan(Math.toRadians(90 + av_));
    }


    @Override
    public int compareTo(Ball comparedTo_) {
        return Integer.compare(r, comparedTo_.r);
    }

}
