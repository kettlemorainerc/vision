package org.usfirst.frc.team2077.math;

public class Matrix {
    /**
     * Matrix multiplication.
     * @param a Matrix a.
     * @param b Matrix a.
     * @return a X b.
     */
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

    /**
     * Constructs a 3D rotation matrix.
     * @param degrees Degrees rotation about X axis.
     * @return 3x3 rotation matrix.
     */
    public static double[][] rotateX(double degrees) {
        double radians = Math.toRadians( degrees );
        double[][] matrix = {{1, 0, 0}, {0, Math.cos(radians), -Math.sin(radians)}, {0, Math.sin(radians), Math.cos(radians)}};
        return matrix;
    }

    /**
     * Constructs a 3D rotation matrix.
     * @param degrees Degrees rotation about Y axis.
     * @return 3x3 rotation matrix.
     */
    public static double[][] rotateY(double degrees) {
        double radians = Math.toRadians( degrees );
        double[][] matrix = {{Math.cos(radians), 0, Math.sin(radians)}, {0, 1, 0}, {-Math.sin(radians), 0, Math.cos(radians)}};
        return matrix;
    }

    /**
     * Constructs a 3D rotation matrix.
     * @param degrees Degrees rotation about Z axis.
     * @return 3x3 rotation matrix.
     */
    public static double[][] rotateZ(double degrees) {
        double radians = Math.toRadians( degrees );
        double[][] matrix = {{Math.cos(radians), -Math.sin(radians), 0}, {Math.sin(radians), Math.cos(radians), 0}, {0, 0, 1}};
        return matrix;
    }

    /**
     * Constructs a 3D rotation matrix.
     * @param degreesX Degrees rotation about X axis.
     * @param degreesY Degrees rotation about Y axis.
     * @param degreesZ Degrees rotation about Z axis.
     * @return <code>rotateX(degreesX) X rotateY(degreesY) X rotateZ(degreesZ)</code>
     */
    public static double[][] rotateXYZ(double degreesX, double degreesY, double degreesZ) {
        return multiply(rotateX(degreesX), multiply(rotateY(degreesY), rotateZ(degreesZ)));
    }

    /**
     * Constructs a 3D rotation matrix.
     * @param degreesZ Degrees rotation about Z axis.
     * @param degreesY Degrees rotation about Y axis.
     * @param degreesX Degrees rotation about X axis.
     * @return <code>rotateX(degreesX) X rotateY(degreesY) X rotateZ(degreesZ)</code>
     */
    public static double[][] rotateZYX(double degreesZ, double degreesY, double degreesX) {
        return multiply(rotateZ(degreesZ), multiply(rotateY(degreesY), rotateX(degreesX)));
    }
}
