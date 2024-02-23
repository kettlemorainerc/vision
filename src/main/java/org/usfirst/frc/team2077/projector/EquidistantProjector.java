package org.usfirst.frc.team2077.projector;

import org.usfirst.frc.team2077.projection.SimpleProjector;

/**
 * Equidistant fisheye projection r = f * \u03b8.
 *
 * <p>\u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public class EquidistantProjector extends SimpleProjector {
    @Override protected double backProjection(
          double K,
          double radius
    ) {
        if (radius < 0 || radius > Math.PI) return -1;
        return radius;
    }

    @Override protected double forwardProjection(
          double K,
          double angle
    ) {
        if (angle < 0 || angle > Math.PI) return -1;
        return angle;
    }

    @Override public double getDefaultK(boolean source) {
        if(source) return 0;
        return 1;
    }

    @Override public double getDefaultFov(boolean source) {
        if(source) return 0;
        return 180;
    }
}
