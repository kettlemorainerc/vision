package org.usfirst.frc.team2077.projector;

import org.usfirst.frc.team2077.math.Matrix;
import org.usfirst.frc.team2077.projection.*;


/**
 * 3x2 cubemap format similar to one used by a major social media publisher.
 * <p>
 * \u00A9 2018
 * @author FRC Team 2077
 * @author R. A. Buchanan
 */
public class CubeMap3x2Projector implements Projector {
    private static final double[][][] faceToGlobal_ = new double[6][][];
    private static final double[][][] globalToFace_ = new double[6][][];
    static {
        double[] polar = {90, 90, 0, 180, 90, 90};
        double[] heading = {90, 270, 0, 0, 0, 180};
        for (int i = 0; i < 6; i++) {
            globalToFace_[i] = Matrix.multiply(Matrix.rotateZ(-heading[i]), Matrix.rotateY(-polar[i]));
            faceToGlobal_[i] = Matrix.rotateXYZ(0, polar[i], heading[i]);
        }
    }

    @Override public double[] renderingProjection(
          RenderingProjection projection,
          double x,
          double y
    ) {
        double[] columnX = {-1, -1/3., 1/3., 1};
        double[] rowY = {-2/3., 0, 2/3.};

        double focalLength = 1 / Math.tan(Math.PI/4);

        int column = -1;
        for ( int c = 0; c < 3; c++ ) {
            if ( x >= columnX[c] && x <= columnX[c + 1] ) {
                column = c;
            }
        }
        int row = -1;
        for ( int r = 0; r < 2; r++ ) {
            if ( y >= rowY[r] && y <= rowY[r + 1] ) {
                row = r;
            }
        }

        if (column == -1 || row == -1 || Math.abs(y) > projection.bounds.getHeight() / projection.bounds.getWidth()) {
            return null;
        }

        double faceX = (x - (columnX[column]+columnX[column+1])/2) * 3;
        double faceY = (y - (rowY[row]+rowY[row+1])/2) * 3;
        int face = column + row*3;

        double[] ra = ProjectionUtils.transformCartesianToPolar(faceX, faceY);
        double radius = ra[0];
        double azimuth = ra[1];
        radius /= focalLength;
        double polar = Math.atan2(radius, 1);
        double[] ap = ProjectionUtils.transform(azimuth, polar, globalToFace_[face]);
        azimuth = ap[0];
        polar = ap[1];

        return new double[] {azimuth, polar};
    }

    @Override public double[] sourceProjection(
          SourceProjection projection,
          double world_o_clock,
          double world_from_center
    ) {
        double[] originX = { -2/3., 0, 2/3., -2/3., 0, 2/3.};
        double[] originY = { -1/3., -1/3., -1/3., 1/3., 1/3., 1/3. };

        double focalLength = 1 / Math.tan(Math.PI/4);

        int nearestFace = 0;
        double nearestRadius = Integer.MAX_VALUE;
        double[] nearestXY = {0,0};

        for (int i = 0; i < 6; i++ ) {
            double[] ap = ProjectionUtils.transform(world_o_clock, world_from_center, faceToGlobal_[i]);
            double azimuth1 = ap[0];
            double polar1 = ap[1];

            if (polar1 >= Math.PI/2) {
                continue;
            }

            double radius = Math.tan(polar1);

            radius *= focalLength;

            if (radius < nearestRadius) {
                nearestFace = i;
                nearestRadius = radius;
                nearestXY = ProjectionUtils.transformPolarToCartesian(radius, azimuth1);
            }
        }

        double x = nearestXY[0];
        double y = nearestXY[1];
        x /= 3.;
        y /= 3.;
        x += originX[nearestFace];
        y += originY[nearestFace];

        return new double[] {x, y};
    }

    @Override public double getDefaultK(boolean source) {
        return 0;
    }

    @Override public double getDefaultFov(boolean source) {
        return 360;
    }
}
