package org.usfirst.frc.team2077.projector;

/**
 * EquisolidProjector fisheye projection r = 2 * f * sin(\u03b8/2).
 *
 * <p>\u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public class EquisolidProjector extends SineProjector {
    @Override public double getDefaultK(boolean source) {
        return 2;
    }
}
