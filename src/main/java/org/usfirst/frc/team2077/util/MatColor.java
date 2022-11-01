package org.usfirst.frc.team2077.util;

import org.opencv.imgproc.Imgproc;

public enum MatColor {
    BGR(Imgproc.COLOR_BGR2GRAY),
    RGB(Imgproc.COLOR_RGB2GRAY),
    BGRA(Imgproc.COLOR_BGRA2GRAY),
    RGBA(Imgproc.COLOR_RGBA2GRAY),
    BGR565(Imgproc.COLOR_BGR5652GRAY),
    BGR555(Imgproc.COLOR_BGR5552GRAY),
    GRAY(-1),
    ;
    
    public final int TO_GRAY;
    
    MatColor(int toGray) {
        this.TO_GRAY = toGray;
    }
}
