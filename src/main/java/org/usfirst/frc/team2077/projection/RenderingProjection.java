package org.usfirst.frc.team2077.projection;

import org.usfirst.frc.team2077.math.Matrix;
import org.usfirst.frc.team2077.video.interfaces.Rendering;
import org.usfirst.frc.team2077.view.VideoView;

import java.awt.*;
import java.awt.geom.*;

/**
 * Forward projection and coordinate transform math for video rendering.
 * The purpose of the rendering projection is to present the video data in the desired form
 * for presentation to either the human viewers or downstream processing. Wide angle, fisheye,
 * and/or multiple cameras may provide large field of view coverage, but significantly
 * distort all or part of it. In combination with correct source projections configured to
 * provide normalized spherical image data, well chosen rendering projections can present
 * the most important parts of the overall field in the most usable form for the intended
 * purpose. Sources that are normalized to a common global coordinate space may also be
 * rendered into a single view to "stitch" multiple feeds into a single panorama, birdseye,
 * or other projection, even if the cameras that produce them are not perfectly matched.
 * <p>
 * While the interface defines several methods, its core is {@link #renderingProjection(double, double)},
 * which describes the pure projection function by mapping from normalized cartesian projection space
 * to the spherical world coordinates produced by the SourceProjection of the video source.
 * Other functions may typically be handled in a common base class.
 * <p>
 * Rendering projections are created by {@link Rendering}s and are required to have
 * a constructor taking parameters (String name, RenderedView view),
 * where <code>name</code> is its configuration property prefix and <code>view</code>
 * is the containing RenderedView.
 * <p>
 * \u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public final class RenderingProjection {

    private final Projector projector;
    public final Rectangle2D bounds;

    private final boolean global;

    private final AffineTransform transform;
    public final double[][] forwardTransform;
    public final double[][] globalForwardTransform;

    public final double horizontalFovAngle;
    public final double verticalFovAngle;

    public final double K;
    public final double focalLength;

    public RenderingProjection(Values values) {
        projector = values.projector;
        bounds = values.bounds;
        global = values.global;
        transform = values.transform;
        forwardTransform = values.forwardTransform;
        globalForwardTransform = values.globalForwardTransform;
        horizontalFovAngle = values.horizontalFovAngle;
        verticalFovAngle = values.verticalFovAngle;
        K = values.K;
        focalLength = values.focalLength;
    }
    
    // RenderingProjection(String name, RenderedView view);

    /**
     * Maps a point in view image coordinates to rendering image space.
     * @param viewPixelX Horizontal image coordinate in range 0 to width from left.
     * @param viewPixelY Vertical image coordinate in range 0 to width from top.
     * @return {
     * <br> renderingPixelX (Horizontal image coordinate in range 0 to width from left),
     * <br> renderingPixelY (Vertical image coordinate in range 0 to width from top)
     * <br>}.
     */
    public double[] transformViewToRendering(double viewPixelX, double viewPixelY) {

        if (viewPixelX < bounds.getX() || viewPixelX >= (bounds.getX() + bounds.getWidth())
                || viewPixelY < bounds.getY() || viewPixelY >= (bounds.getY() + bounds.getHeight())) {
            return null;
        }

        double[] renderingPixel = {viewPixelX, viewPixelY};

        transform.transform(renderingPixel, 0, renderingPixel, 0, 1);

        return new double[] { renderingPixel[0], renderingPixel[1] };
    }
    
    /**
     * Maps a point in rendering image coordinates to normalized rendering projection space.
     * @param renderingX Horizontal image coordinate in range 0 to width from left.
     * @param renderingY Vertical image coordinate in range 0 to width from top.
     * @return {
     * <br> x (Horizontal projection space coordinate in range -1 to 1),
     * <br> y (Vertical projection space coordinate on same scale)
     * <br>}.
     */
    public double[] transformPixelToCartesian(double renderingX, double renderingY) {
        renderingX -= bounds.getWidth()/2.;
        renderingY -= bounds.getHeight()/2.;

        renderingX /= bounds.getWidth()/2.;
        renderingY /= bounds.getWidth()/2.;

        double x = renderingX;
        double y = renderingY;

        return new double[] {x, y};
    }
    
    /**
     * Maps normalized source projection coordinates to source image coordinates.
     * Maps a point in normalized projection coordinates to view-relative spherical vector.
     * @param projectionX Horizontal projection space coordinate in range -1 to 1.
     * @param projectionY Vertical projection space coordinate on same scale.
     * @return {
     * <br> azimuth (Viewer-relative spherical azimuth angle in radians clockwise from upward),
     * <br> polar (Viewer-relative spherical normal angle in radians outward from view axis)
     * <br>}.
     */
    public double[] renderingProjection(double projectionX, double projectionY) {
        return projector.renderingProjection(this, projectionX, projectionY);
    }

    /**
     * Maps source-relative spherical view vector to world-relative spherical coordinates
     * as supplied by source.
     * World coordinate space is defined by the video source, and is relative to either
     * the view axis of the source itself or to a global upward-looking axis.
     * @see SourceProjection#transformGlobalWorldToSource
     * @see SourceProjection#transformNominalWorldToSource
     * @param azimuth Viewer-relative spherical azimuth angle in radians clockwise from upward.
     * @param polar Viewer-relative spherical normal angle in radians outward from view axis.
     * @return {
     * <br> azimuth (Viewer-relative spherical azimuth angle in radians clockwise from upward),
     * <br> polar (Viewer-relative spherical normal angle in radians outward from view axis)
     * <br>}.
     */
    public double[] transformRenderingToWorld(double azimuth, double polar) {
        return ProjectionUtils.transform(azimuth, polar, isGlobal() ? globalForwardTransform : forwardTransform);
    }
    
    /**
     * Global source coordinate flag.
     * @return True if the rendering projection expects the source to use global (vs source-relative) coordinates.
     */
    public boolean isGlobal() {
        return global;
    }

    public static class Values {
        private final Projector projector;
        protected double[][] forwardTransform;
        protected double[][] globalForwardTransform;
        protected double horizontalFovAngle;
        protected double verticalFovAngle;
        protected double K;
        protected double focalLength;
        protected boolean global;
        protected Rectangle2D bounds;
        protected AffineTransform transform;

        public Values(Projector projector, Dimension view) {
            this.projector = projector;
            Dimension resolution = view;
            transform(new AffineTransform());
            bounds(new Rectangle2D.Double(0, 0, resolution.getWidth(), resolution.getHeight()));
            global(false);
            globalForwardTransform(0, 0);
            horizontalFovAngle = Math.toRadians(projector.getDefaultFov());
            verticalFovAngle = 0;
            this.K = projector.getDefaultK();
            focalLength = 0;
        }

        public Values bounds(Rectangle2D bounds) {
            this.bounds = bounds;
            return this;
        }

        public Values global(boolean global) {
            this.global = global;
            return this;
        }

        public Values transform(AffineTransform transform) {
            this.transform = transform;
            return this;
        }

        public Values focalLength(double focalLength) {
            this.focalLength = focalLength;
            return this;
        }

        /**
         * In degrees.<br>
         * <br>
         * // straight ahead<br>
         * //double panDir = 0;<br>
         * //double panAmount = 0;<br>
         *<br>
         * // 45 degrees up<br>
         * //double panDir =  0 * Math.PI/180.;<br>
         * //double panAmount = 45 * Math.PI/180.;<br>
         *<br>
         * // 60 degrees right<br>
         * //double panDir = 90 * Math.PI/180.;<br>
         * //double panAmount = 60 * Math.PI/180.;<br>
         */
        public Values forwardTransform(double panDir, double panAmount, double panRotation) {
            panDir = Math.toRadians(panDir);
            panAmount = Math.toRadians(panAmount);
            panRotation = Math.toRadians(panRotation);

            if (100*panDir != 0 || 100*panAmount != 0 || 100*panRotation != 0) {
                double[][] panAzimuth = {{Math.cos(panDir), -Math.sin(panDir), 0}, {Math.sin(panDir), Math.cos(panDir), 0}, {0, 0, 1}};
                double[][] panAngle = {{Math.cos(panAmount), 0, Math.sin(panAmount)}, {0, 1, 0}, {-Math.sin(panAmount), 0, Math.cos(panAmount)}};
                double[][] panReverseAzimuth = {{Math.cos(-panDir), -Math.sin(-panDir), 0}, {Math.sin(-panDir), Math.cos(-panDir), 0}, {0, 0, 1}};
                double[][] rotation = {{Math.cos(panRotation), -Math.sin(panRotation), 0}, {Math.sin(panRotation), Math.cos(panRotation), 0}, {0, 0, 1}};
                forwardTransform = Matrix.multiply(Matrix.multiply(panAzimuth, panAngle), Matrix.multiply(panReverseAzimuth, rotation));
            } else {
                forwardTransform = null;
            }
            return this;
        }

        public Values globalForwardTransform(Double heading) {
            if(heading != null && heading != 0) {
                return globalForwardTransform(heading, 90);
            }
            return globalForwardTransform(0, 0);
        }

        public Values globalForwardTransform(double heading, double polar) {
            globalForwardTransform = Matrix.multiply(
                    Matrix.rotateZ(-heading),
                    Matrix.rotateY(-polar)
            );
            return this;
        }
    }
}
