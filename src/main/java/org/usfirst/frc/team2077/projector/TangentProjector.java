package org.usfirst.frc.team2077.projector;

import org.usfirst.frc.team2077.projection.SimpleProjector;

public class TangentProjector extends SimpleProjector {
    @Override public double getDefaultK() {
        return 1;
    }

    @Override public double getDefaultFov() {
        return 55;
    }

    @Override protected double backProjection(double K, double radius) {
        if (radius < 0) return -1;
        return K * Math.atan2(radius, K);
    }

    @Override protected double forwardProjection(double K, double angle) {
        if (angle < 0 || angle/K >= Math.PI/2) return -1;
        return K * Math.tan(angle / K);
    }
}