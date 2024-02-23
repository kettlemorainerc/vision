package org.usfirst.frc.team2077.projector;

import org.usfirst.frc.team2077.projection.SimpleProjector;

/**
 * Projections based on r = k * f * tan(θ/k).
 * <p>
 * These include two classical projections and approach a third:
 * <ul>
 * <li>Rectilinear Perspective (k = 1)</li>
 * <li>Stereographic Fisheye (k = 2)</li>
 * <li>Equidistant Fisheye (k = ∞)</li>
 * </ul>
 * <p>
 * © 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public class TangentProjector extends SimpleProjector {
    @Override public double getDefaultK(boolean source) {
        return 1;
    }

    @Override public double getDefaultFov(boolean source) {
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