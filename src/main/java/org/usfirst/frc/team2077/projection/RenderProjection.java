package org.usfirst.frc.team2077.projection;

import org.opencv.core.Point;
import org.usfirst.frc.team2077.util.SuperProperties;
import org.usfirst.frc.team2077.video.interfaces.RenderingProjection;
import org.usfirst.frc.team2077.view.View;

import java.awt.geom.*;

public abstract class RenderProjection extends Projection {
    public final Rectangle2D bounds;
    private final AffineTransform viewToRendering;
    public final boolean global;
    public final double[][] forward;
    protected final double focalLength;
    protected final SuperProperties props;


    public RenderProjection(SuperProperties runProps, View parent) {
        props = new SuperProperties(runProps, parent.name);
        bounds = new Rectangle2D.Double(0, 0, parent.width, parent.height);
        global = props.getBoolean("global", false);

        viewToRendering = new AffineTransform();
        viewToRendering.translate(bounds.getCenterX(), bounds.getCenterY());

        String flip = props.get("rotate");
        if(flip != null && !flip.isEmpty()) {
            flip = flip.toUpperCase();

            int xScale = 1, yScale = 1;
            if(flip.contains("H")) xScale *= -1;
            if(flip.contains("V")) yScale *= -1;

            viewToRendering.scale(xScale, yScale);
        }

        viewToRendering.translate(-bounds.getCenterX(), -bounds.getCenterY());
        viewToRendering.translate(-bounds.getX(), - bounds.getY());

        if(global) {
            String headingStr = props.get("heading");
            double polar = props.getDouble("polar-angle", headingStr != null ? 90d : 0);
            double heading = headingStr == null ? 0 : Double.parseDouble(headingStr);

            forward = multiply(rotateZ(-heading), rotateY(-polar));
        } else {
            // straight ahead
            //double woc = 0;
            //double wfc = 0;

            // 45 degrees up
            //double woc =  0 * Math.PI/180.;
            //double wfc = 45 * Math.PI/180.;

            // 60 degrees right
            //double woc = 90 * Math.PI/180.;
            //double wfc = 60 * Math.PI/180.;
            double woc = props.getDouble("pan-direction", 0d);
            double wfc = props.getDouble("pan-amount", 0d);
            double rot = props.getDouble("pan-rotation", 0d);

            if (woc != 0 && wfc != 0 && rot != 0) {
                double[][] panAzimuth = {{Math.cos(woc), -Math.sin(woc), 0}, {Math.sin(woc), Math.cos(woc), 0}, {0, 0, 1}};
                double[][] panAngle = {{Math.cos(wfc), 0, Math.sin(wfc)}, {0, 1, 0}, {-Math.sin(wfc), 0, Math.cos(wfc)}};
                double[][] panReverseAzimuth = {{Math.cos(-woc), -Math.sin(-woc), 0}, {Math.sin(-woc), Math.cos(-woc), 0}, {0, 0, 1}};
                double[][] panRotation = {{Math.cos(rot), -Math.sin(rot), 0}, {Math.sin(rot), Math.cos(rot), 0}, {0, 0, 1}};
                forward = multiply(multiply(panAzimuth, panAngle), multiply(panReverseAzimuth, panRotation));
            } else {
                forward = null;
            }
        }

        this.focalLength = getFocalLength(props, "horizontal-fov");
    }

    public Point transformRawToDisplay(Point raw) {
        if (!bounds.contains(raw.x, raw.y)) {
            return null;
        }

        Point2D transformed = new Point2D.Double(raw.x, raw.y);
        viewToRendering.transform(transformed, transformed);

        return new Point(transformed.getX(), transformed.getY());
    }

    public Point transformPixelToNormalized(Point display) {
        double x = display.x;
        double y = display.y;

        x -= bounds.getWidth()/2.;
        y -= bounds.getHeight()/2.;

        x /= bounds.getWidth()/2.;
        y /= bounds.getWidth()/2.;

        return new Point(x, y);
    }

    protected abstract double backProjection(double radius);

    public SphericalPoint renderingProjection(Point normalized) {
        double[] rt = transformCartesianToPolar(normalized);

        double radius = rt[0];
        double azimuth = rt[1];

        radius /= focalLength;

        double polar = backProjection(radius);

        if (polar < 0) return null; // out of range

        return new SphericalPoint(mod(azimuth, 2*Math.PI), mod(polar, Math.PI));
    }

    public SphericalPoint renderingToWorld(SphericalPoint rendering) {
        transform(rendering, forward);
        return rendering;
    }
}
