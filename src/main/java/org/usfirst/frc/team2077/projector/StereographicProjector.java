package org.usfirst.frc.team2077.projector;

/**
 * Stereographic fisheye projection r = 2 * f * tan(θ/2).
 *
 * <p>© 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public class StereographicProjector extends TangentProjector {
    @Override public double getDefaultFov(boolean source) {
        return 180;
    }

    @Override public double getDefaultK(boolean source) {
        return 2;
    }
}
