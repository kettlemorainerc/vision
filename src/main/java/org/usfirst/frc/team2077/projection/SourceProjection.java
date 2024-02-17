package org.usfirst.frc.team2077.projection;

import org.usfirst.frc.team2077.math.Matrix;
import org.usfirst.frc.team2077.source.VideoSource;

import java.awt.*;

/**
 * Back projection and coordinate transform math for video input. The purpose of the source projection is to normalize
 * input to common world space spherical coordinates regardless of camera lens geometry. This permits merging of video
 * data from multiple cameras (even with differing lens types) and independent specification of viewing projections.
 * <p>
 * While the interface defines several methods, its core is {@link #sourceProjection(double, double)}, which describes
 * the pure lens function by mapping from spherical world coordinates to normalized cartesian projection space. Other
 * functions may typically be handled in a common base class.
 * <p>
 * Source projections are created by {@link VideoSource}s and are required to have a constructor taking parameters
 * (String name, VideoSource videoSource), where <code>name</code> is its configuration property prefix and
 * <code>videoSource</code> is the calling VideoSource.
 * <p>
 * \u00A9 2018
 * 
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public final class SourceProjection {

    private final Projector projector;
    public final double fovAngleHorizontal;
    public final double cameraFovDiameter;
    public final double cameraFovCenterX;
    public final double cameraFovCenterY;
    public final double K;
    
    public final double[] cameraOriginXYZ;
    public final double[][] globalBackTransform;
    public final double[][] nominalBackTransform;
    public final Dimension resolution;

    public SourceProjection(Values values) {
        projector = values.projector;
        fovAngleHorizontal = values.fovAngleHorizontal;
        cameraFovDiameter = values.cameraFovDiameter;
        cameraFovCenterX = values.cameraFovCenterX;
        cameraFovCenterY = values.cameraFovCenterY;
        K = values.K;
        cameraOriginXYZ = values.cameraOriginXYZ;
        globalBackTransform = values.globalBackTransform;
        nominalBackTransform = values.nominalBackTransform;
        resolution = values.resolution;
    }

    /**
     * Maps global spherical view vector to source-relative spherical coordinates.
     *
     * @param azimuth World-relative spherical azimuth angle in radians clockwise from upward.
     * @param polar World-relative spherical polar angle in radians outward from view axis.
     * @return { <br>
     *         azimuth (Image-relative spherical azimuth angle in radians clockwise from upward), <br>
     *         polar (Image-relative spherical normal angle in radians outward from view axis) <br>
     *         }.
     */
    public double[] transformGlobalWorldToSource(double azimuth, double polar) {
        return ProjectionUtils.transform(azimuth, polar, globalBackTransform);
    }

    /**
     * Maps nominal source-relative spherical view vector to source-relative spherical coordinates.
     *
     * @param azimuth Nominal source-relative spherical azimuth angle in radians clockwise from upward.
     * @param polar Nominal source spherical polar angle in radians outward from view axis.
     * @return { <br>
     *         azimuth (Image-relative spherical azimuth angle in radians clockwise from upward), <br>
     *         polar (Image-relative spherical normal angle in radians outward from view axis) <br>
     *         }.
     */
    public double[] transformNominalWorldToSource(double azimuth, double polar) {
        return ProjectionUtils.transform(azimuth, polar, nominalBackTransform);
    }

    /**
     * Maps source-relative spherical view vector to normalized source projection coordinates.
     *
     * @param world_o_clock Viewer-relative spherical azimuth angle in radians clockwise from upward.
     * @param world_from_center Viewer-relative spherical polar angle in radians outward from view axis.
     * @return { <br>
     *         x (Horizontal projection space coordinate in range -1 to 1), <br>
     *         y (Vertical projection space coordinate on same scale). <br>
     *         }.
     */
    public double[] sourceProjection(double world_o_clock, double world_from_center) { //TODO: warning about overriding this method and forwardProjection
        return projector.sourceProjection(this, world_o_clock, world_from_center);
    }

    /**
     * Maps a point in normalized source projection coordinates to source image space.
     *
     * @param x Horizontal projection space coordinate in range -1 to 1.
     * @param y Vertical projection space coordinate on same scale.
     * @return { <br>
     *         pixelX, (Horizontal image coordinate in range 0 to width from left). <br>
     *         pixelY (Vertical image coordinate in range 0 to width from top). <br>
     *         }
     */
    public double[] transformCartesianToPixel( double x, double y ) {
        double renderingX = x;
        double renderingY = y;

        double r = (cameraFovDiameter * Math.max(resolution.width, resolution.height)) / 2.;
        renderingX *= r;
        renderingY *= r;

        renderingX += cameraFovCenterX * resolution.width; // [0 to w]
        renderingY += cameraFovCenterY * resolution.height; // [h to 0]

        return new double[] {renderingX, renderingY};
    }

    public static class Values {
        private final Projector projector;
        private double fovAngleHorizontal;
        private double cameraFovDiameter;
        private double cameraFovCenterX;
        private double cameraFovCenterY;
        private double K;

        private double[] cameraOriginXYZ;
        private double[][] globalBackTransform;
        private double[][] nominalBackTransform;
        private final Dimension resolution;

        public Values(Projector projector, Dimension source) {
            this.projector = projector;
            resolution = source;
            fovAngleHorizontal = Math.toRadians(projector.getDefaultFov());
            cameraFovDiameter = 1;
            cameraFovCenterX = 0.5;
            cameraFovCenterY = 0.5;
            K = projector.getDefaultK();

            cameraOriginXYZ = new double[] {0, 0, 24};

            backTransforms(0d);
        }

        public Values K(double K) {
            this.K = K;
            return this;
        }

        public Values fovAngleHorizontal(double val) {
            this.fovAngleHorizontal = val;
            return this;
        }

        public Values backTransforms(Double heading) {
            if(heading == null || heading == 0) {
                return backTransforms(0, 0, 0, 0, 0);
            }

            return backTransforms(heading, 90, 0, 0, 0);
        }

        public Values backTransforms(double heading, double polar, double roll, double pitch, double yaw) {
            double[][] nominalToGlobal = Matrix.multiply(
                    Matrix.rotateY(polar),
                    Matrix.rotateZ(heading)
            );

            double[][] actualToNominal = Matrix.rotateZYX(-roll, -pitch, -yaw);

            globalBackTransform = Matrix.multiply(actualToNominal, nominalToGlobal);
            nominalBackTransform = actualToNominal;

            return this;
        }
    }
}
