package org.usfirst.frc.team2077.projector;

/**
 * Rectilinear perspective "pinhole camera" projection r = f * tan(\u03b8).
 *
 * <p>\u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public class PerspectiveProjector extends TangentProjector {
    @Override public double getDefaultFov(boolean source) {
        return 55;
    }

    @Override public double getDefaultK(boolean source) {
        return 1;
    }
}
