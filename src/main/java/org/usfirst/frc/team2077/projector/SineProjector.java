package org.usfirst.frc.team2077.projector;

import org.usfirst.frc.team2077.projection.*;

public class SineProjector extends SimpleProjector {

    @Override public double backProjection(double K, double radius) {
        if (radius < 0 || (radius / K) > 1) return -1;
        return K * Math.asin(radius / K);
    }

    @Override public double forwardProjection(double K, double angle) {
        if (angle < 0 || angle/K > Math.PI/2 || angle > Math.PI*2) return -1;
        return K * Math.sin(angle / K);
    }

    @Override public double getDefaultK() {
        return 1;
    }

    @Override public double getDefaultFov() {
        return 180;
    }
}
