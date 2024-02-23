package org.usfirst.frc.team2077.projector;

import org.usfirst.frc.team2077.math.Matrix;
import org.usfirst.frc.team2077.projection.*;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * EquirectangularProjector projection x = φ (azimuth), y = θ (polar).
 * <p>
 * In the global spherical coordinate space, center of the image is at
 * {φ,θ} = {0,π/2}. For a full spherical image,
 * φ = -π is left, φ = π is right,
 * θ = 0 is up, and θ = π is down.
 * <p>
 * \u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public class EquirectangularProjector implements Projector {
    @Override public double[] renderingProjection(
          RenderingProjection projection,
          double x,
          double y
    ) {
        Rectangle2D bounds = projection.bounds;
        double aspectRatio = bounds.getWidth() / bounds.getHeight();
        double horizontalFOV = projection.horizontalFovAngle;
        double verticalFOV = projection.verticalFovAngle > 0 ? projection.verticalFovAngle : horizontalFOV / aspectRatio;
        double horizontalScale = horizontalFOV / 2;
        double verticalScale = aspectRatio * verticalFOV / 2; // explicit vFOV is scaled to fit height

        double azimuth = x; // normal range -1 - 1
        double polar = y; // normal range -1/aspect - 1/aspect

        // scale to range -hFOV/2 - hFOV/2
        azimuth *= horizontalScale;
        polar *= verticalScale;

        azimuth = -azimuth; // world spherical convention reverses this sign

        // clip out of range values
        if (Math.abs(azimuth) > Math.min(horizontalFOV/2, Math.PI) || Math.abs(polar) > Math.min(verticalFOV/2, Math.PI/2)) {
            return null;
        }

        // normalize to spherical range
        azimuth += Math.PI; // 0 - 2PI
        polar += Math.PI/2; // 0 - PI

        double[] ap = ProjectionUtils.transform(azimuth, polar, Matrix.rotateY(90));
        azimuth = ap[0];
        polar = ap[1];

        return new double[] {azimuth, polar};
    }

    @Override public double[] sourceProjection(
          SourceProjection projection,
          double azimuth,
          double polar
    ) {
        double[] ap = ProjectionUtils.transform(azimuth, polar, Matrix.rotateY(-90));
        azimuth = ap[0];
        polar = ap[1];

        Dimension resolution = projection.resolution;
        double aspectRatio = resolution.getWidth() / resolution.getHeight();
        double horizontalFOV = projection.fovAngleHorizontal;
        double verticalFOV = horizontalFOV / aspectRatio;
        double scale = horizontalFOV/2;

        double x = azimuth; // normal range 0 - 2PI
        double y = polar; // normal range 0 - PI

        x -= Math.PI; // -PI - PI
        y -= Math.PI/2; // -PI/2 - PI/2

        x = -x; // projection cartesian convention reverses this sign

        // clip out of range values
        if (Math.abs(x) > Math.min(horizontalFOV/2, Math.PI) || Math.abs(y) > Math.min(verticalFOV/2, Math.PI/2)) {
            return null;
        }

        // normalize to horizontal FOV = range -1 to 1
        x /= scale;
        y /= scale;

        return new double[] {x, y};
    }

    @Override public double getDefaultK(boolean source) {
        return 0;
    }

    @Override public double getDefaultFov(boolean source) {
        return 360;
    }
}
