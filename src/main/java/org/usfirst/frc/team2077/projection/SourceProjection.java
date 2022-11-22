package org.usfirst.frc.team2077.projection;

import org.opencv.core.Point;
import org.usfirst.frc.team2077.source.FrameSource;
import org.usfirst.frc.team2077.util.SuperProperties;

import java.awt.*;
import java.util.Arrays;

public abstract class SourceProjection extends Projection {
    public final double[][] backward;
    private final double focalLength;
    private final double fovDiameter;
    private final Point fovCenter;
    private final Dimension dimension;
    protected final SuperProperties props;

    public SourceProjection(SuperProperties runProps, FrameSource source, RenderProjection rendering) {
        props = new SuperProperties(runProps, source.name);
        this.dimension = source.getResolution();

        backward = getBackwardTransform(props, rendering.global);

        focalLength = getFocalLength(props, "camera-fov-angle");

        fovDiameter = props.getDouble("fov-diameter", 1D);
        fovCenter = new Point(
                props.getDouble("fov-center-x", 0.5),
                props.getDouble("fov-center-y", 0.5)
        );
    }

    @Override protected Double defaultFov() {return 180D;}

    public SphericalPoint worldToSource(SphericalPoint world) {
        transform(world, backward);
        return world;
    }

    public Point sourceProjection(SphericalPoint source) {
        double radius = forwardProjection(source.polar);

        if (radius < 0) return null; // out of range

        radius *= focalLength;

        return transformPolarToCartesian(radius, source.azimuth);
    }

    public Point normalizedToPixel(Point normalized) {
        double width = dimension.getWidth();
        double height = dimension.getHeight();

        double renderingX = normalized.x;
        double renderingY = normalized.y;

        //renderingX *= cameraFOVDiameter_ * resolution.width / 2.;
        //renderingY *= cameraFOVDiameter_ * resolution.width / 2.;
        double r = (fovDiameter * Math.max(width, height)) / 2.;
        renderingX *= r;
        renderingY *= r;

        //renderingX += .5 * resolution.width;
        //renderingY += .5 * resolution.height;
        renderingX += fovCenter.x * width; // [0 to w]
        renderingY += fovCenter.y * height; // [h to 0]

        return new Point(Math.round(renderingX), Math.round(renderingY));
    }

    private static double[][] getBackwardTransform(SuperProperties props, boolean global) {
        double rollCorrection = props.getDouble("camera-roll-correction", 0D);
        double pitchCorrection = props.getDouble("camera-pitch-correction", 0D);
        double yawCorrection = props.getDouble("camera-yaw-correction", 0D);

        double[][] actualToNominal = rotateZYX(-rollCorrection, -pitchCorrection, -yawCorrection);

        if(global) {
            Double heading = props.getDouble("heading");
            double polar = props.getDouble("polar-angle", heading != null ? 90D : 0);
            heading = heading == null ? 0 : heading;
            double[][] nominalToGlobal = multiply(rotateY(polar), rotateZ(heading));

            return multiply(actualToNominal, nominalToGlobal);
        }

        return actualToNominal;
    }
}
