package org.usfirst.frc.team2077.projection;

import org.opencv.core.*;
import org.opencv.core.Point;
import org.usfirst.frc.team2077.util.SuperProperties;
import org.usfirst.frc.team2077.view.View;

import java.awt.geom.*;

public abstract class Projection {
//    public Point viewToSource(Point viewPoint) {
//        Point renderingPixel = transformRawToDisplay(viewPoint);
//        if (renderingPixel == null) { // TODO: decide which methods may return null
//            return null;
//        }
//
//        Point renderingXY = transformPixelToNormalized(renderingPixel);
//        SphericalPoint renderingSpherical = renderingProjection(renderingXY);
//        if (renderingSpherical == null) {
//            return null;
//        }
//
//        SphericalPoint worldSpherical = renderingToWorld(renderingSpherical);
//        SphericalPoint sourceSpherical = worldToSource(worldSpherical);
//        Point sourceXY = sourceProjection(sourceSpherical);
//        if (sourceXY == null) {
//            return null;
//        }
//
//        Point sourcePixel = normalizedToPixel(sourceXY);
//        if (sourcePixel == null || !bounds.contains(sourcePixel.x, sourcePixel.y)) {
//            return null;
//        }
//
//        return sourcePixel;
//    }

    private static String couple(Object key, Object value) {
        return "[" + key + "=" + value + "]";
    }

    protected static void print(Object... pairs) {
        StringBuilder output = new StringBuilder("new ");
        for(int i = 0 ; i < pairs.length; i+= 2) {
            output.append(couple(pairs[i], pairs[i + 1]));
        }

        System.out.println(output);
    }

    protected Double getFocalLength(SuperProperties props, String fovKey) {
        Double immediate = props.getDouble("focal-length");
        if(immediate != null) return immediate;

        double fov = Math.toRadians(props.getDouble(fovKey, defaultFov()));
        double v = forwardProjection(fov / 2);
        return 1 / v;
    }

    protected Double defaultFov() {return null;}


    protected abstract double forwardProjection(double polar);

    // "SourceProjection"
    /** World-Relative to source-relative */
//    SphericalPoint globalToSource(SphericalPoint point);
    /** Nominal Source-Relative to source-relative */
//    SphericalPoint nominalToSource(SphericalPoint point);
    public static Point3 transformSphericalToCartesian(double radius, SphericalPoint point) {
        double polarSin = Math.sin(point.polar);

        double x = radius * polarSin * Math.cos(point.azimuth);
        double y = radius * polarSin * Math.sin(point.azimuth);
        double z = radius * Math.cos(point.polar);

        return new Point3(x, y, z);
    }

    public static double[][] multiply(double[][] a, double[][] b) {
        int n = b.length;
        int rows = a.length;
        int cols = b[0].length;
        double[][] p = new double[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                for (int i = 0; i < n; i++) {
                    p[r][c] += a[r][i] * b[i][c];
                }
            }
        }
        return p;
    }

    public static double[] transformCartesianToSpherical(double x, double y, double z) {
        double radius = Math.sqrt(x*x + y*y + z*z);
        double azimuth = Math.atan2(y, x);
        double polar = Math.acos(z/radius);
        return new double[] {radius, mod(azimuth, 2*Math.PI), mod(polar, Math.PI)};
    }

    protected static void transform(SphericalPoint point, double[][] transform) {
        if (transform == null) {
            return ;
        }

        Point3 xyz = transformSphericalToCartesian(1, point);
        double[][] zyxM = multiply(transform, new double[][] {{xyz.x}, {xyz.y}, {xyz.z}});

        double[] rap = transformCartesianToSpherical(zyxM[0][0], zyxM[1][0], zyxM[2][0]);

        point.azimuth = mod(rap[1], 2*Math.PI);
        point.polar = mod(rap[2], Math.PI);
    }

    protected static double mod(double angle, double mod) {
        return ((angle % mod) + mod) % mod;
    }
    
    public static double[] transformCartesianToPolar(Point p) {
        double radius = Math.sqrt(p.x * p.x + p.y * p.y);
        double azimuth = Math.atan2(p.x, -p.y);

        return new double[] {radius, mod(azimuth, 2 * Math.PI)};
    }

    public static double[][] rotateX(double degrees) {
        double radians = Math.toRadians( degrees );
        return new double[][]{{1, 0, 0}, {0, Math.cos(radians), -Math.sin(radians)}, {0, Math.sin(radians), Math.cos(radians)}};
    }

    public static double[][] rotateY(double degrees) {
        double radians = Math.toRadians( degrees );
        return new double[][]{{Math.cos(radians), 0, Math.sin(radians)}, {0, 1, 0}, {-Math.sin(radians), 0, Math.cos(radians)}};
    }

    public static double[][] rotateZ(double degrees) {
        double radians = Math.toRadians( degrees );
        return new double[][]{{Math.cos(radians), -Math.sin(radians), 0}, {Math.sin(radians), Math.cos(radians), 0}, {0, 0, 1}};
    }

    public static double[][] rotateZYX(double degreesZ, double degreesY, double degreesX) {
        return multiply(rotateZ(degreesZ), multiply(rotateY(degreesY), rotateX(degreesX)));
    }

    public static Point transformPolarToCartesian(double radius, double azimuth) {
        double x = radius * Math.sin(azimuth);
        double y = -radius * Math.cos(azimuth);
        return new Point(x, y);
    }
}
