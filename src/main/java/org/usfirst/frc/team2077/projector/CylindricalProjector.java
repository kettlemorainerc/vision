package org.usfirst.frc.team2077.projector;

import org.usfirst.frc.team2077.math.Matrix;
import org.usfirst.frc.team2077.projection.*;

import java.awt.*;

public class CylindricalProjector implements Projector {

    @Override public double[] renderingProjection(RenderingProjection projection, double x, double y) {
        double aspectRatio = projection.bounds.getWidth() / projection.bounds.getHeight();
        double horizontalFOV = projection.horizontalFovAngle;
        double verticalFOV = projection.verticalFovAngle > 0 ? projection.verticalFovAngle : horizontalFOV / aspectRatio;
        double horizontalScale = horizontalFOV / 2;
        double verticalScale = aspectRatio * verticalFOV / 2; // explicit vFOV is scaled to fit height

        double focalLength;
        if (verticalFOV > horizontalFOV) { // horizontal cylindrical
            focalLength = 1 / Math.tan(horizontalFOV/2);
            double hAngle = Math.atan2(x, focalLength);
            double vAngle = y * (verticalFOV/2);
            y = -focalLength * Math.tan(vAngle) * verticalScale;
        }
        else { // vertical cylindrical
            focalLength = 1 / Math.tan(verticalFOV/2);
            double vAngle = Math.atan2(y, focalLength);
            //double vAngle = y * (verticalFOV/2);
            double hAngle = x * (horizontalFOV/2);
            y = -y;
            x = focalLength * Math.tan(hAngle) * horizontalScale;
        }
        double radius = Math.sqrt(x*x + y*y);
        double polar = Math.atan2(radius, focalLength);
        double azimuth = Math.atan2(x, y);

        return new double[] {azimuth, polar};
    }

    @Override public double[] sourceProjection(SourceProjection projection, double azimuth, double polar) {
        if(true) {
            throw new UnsupportedOperationException("CylindricalProjector.sourceProjection");
        }
        // not valid, dont use.

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
