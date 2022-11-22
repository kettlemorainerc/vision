package org.usfirst.frc.team2077.projection;

import org.usfirst.frc.team2077.util.SuperProperties;
import org.usfirst.frc.team2077.view.View;

public class SineRender extends RenderProjection {
    private Double k;

    public SineRender(SuperProperties runProps, View parent) {
        super(runProps, parent);
    }

    private double getK() {
        if(k == null) k = props.getDouble("k", 1D);
        return k;
    }

    @Override protected Double defaultFov() {return 180D;}

    @Override protected double forwardProjection(double angle) {
        double k = getK();
        if (angle < 0 || angle / k > Math.PI / 2 || angle > Math.PI * 2) return -1;
        return k * Math.sin(angle / k);
    }

    @Override protected double backProjection(double radius) {
        if (radius < 0 || (radius / k) > 1) return -1;
        return k * Math.asin(radius / k);
    }
}
