package org.usfirst.frc.team2077.projector;

/**
 * Orthographic fisheye projection r = f * sin(\u03b8).
 *
 * <p>\u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public class OrthographicProjector extends SineProjector {
    @Override public double getDefaultK(boolean source) {
        return 1;
    }
}
