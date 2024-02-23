package org.usfirst.frc.team2077.projector;

import org.usfirst.frc.team2077.projection.*;

/**
 * Projections based on r = k * f * sin(\u03b8/k).
 * <p>
 * These include two classical projections and approach a third:
 * <ul>
 * <li>Orthographic Fisheye (k = 1)</li>
 * <li>EquisolidProjector Fisheye (k = 2)</li>
 * <li>Equidistant Fisheye (k = \u221E)</li>
 * </ul>
 * <p>
 * \u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public class SineProjector extends SimpleProjector {

    @Override public double backProjection(double K, double radius) {
        if (radius < 0 || (radius / K) > 1) return -1;
        return K * Math.asin(radius / K);
    }

    @Override public double forwardProjection(double K, double angle) {
        if (angle < 0 || angle/K > Math.PI/2 || angle > Math.PI*2) return -1;
        return K * Math.sin(angle / K);
    }

    @Override public double getDefaultK(boolean source) {
        return 1;
    }

    @Override public double getDefaultFov(boolean source) {
        return 180;
    }
}
