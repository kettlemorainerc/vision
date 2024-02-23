package org.usfirst.frc.team2077.projector;

import org.usfirst.frc.team2077.projection.SimpleProjector;

/**
 * Projections based on r = k * f * atan(\u03b8/k).
 * <p>
 * Not a standard lens projection, but may be useful for correcting camera barrel distortion.
 * Not usable as a RenderingProjection.
 * <p>
 * \u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public class InverseTangentProjector extends SimpleProjector {

    @Override protected double backProjection(
          double K,
          double radius
    ) {
        throw new UnsupportedOperationException("InverseTangentProjector does not support rendering projections");
    }

    @Override protected double forwardProjection(
          double K,
          double angle
    ) {
        if (angle < 0) return -1;
        return K * Math.atan2(angle, K);
    }

    @Override public double getDefaultK(boolean source) {
        return 1;
    }

    @Override public double getDefaultFov(boolean source) {
        return 55;
    }
}
