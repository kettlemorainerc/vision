package org.usfirst.frc.team2077.projection;

import org.usfirst.frc.team2077.math.Matrix;

public class ProjectionUtils {

    /**
     * Normalizes angles to range such as 0 - 2\u03c0.
     * @param angle Angle.
     * @param mod Divisor.
     * @return Modulus in the range 0 - mod.
     */
    public static double mod(double angle, double mod) {
        return ((angle % mod) + mod) % mod;
    }

    public static double[] transform(double world_o_clock, double world_from_center, double[][] transform) {

        if (transform == null) {
            return new double[] {world_o_clock, world_from_center};
        }

        double[] xyz = transformSphericalToCartesian(1, world_o_clock, world_from_center);
        double[][] zyxM = Matrix.multiply(transform, new double[][] {{xyz[0]}, {xyz[1]}, {xyz[2]}});
        double[] rap = transformCartesianToSpherical(zyxM[0][0], zyxM[1][0], zyxM[2][0]);
        return new double[] {mod(rap[1], 2*Math.PI), mod(rap[2], Math.PI)};
    }

    /**
     * Cartesian to polar coordinate conversion. Uses projection conventions.
     * @param x -left, +right
     * @param y -up, +down
     * @return {
     * <br> radius.
     * <br> azimuth angle (0 up, +cw).
     * <br>}
     */
    public static double[] transformCartesianToPolar(double x, double y) {
        double radius = Math.sqrt(x*x + y*y);
        double azimuth = Math.atan2(x, -y);
        return new double[] {radius, mod(azimuth, 2*Math.PI)};
    }

    /**
     * Polar to cartesian coordinate conversion. Uses projection conventions.
     * @param radius Radius.
     * @param azimuth Azimuth angle (0 up, +cw).
     * @return {
     * <br> x, (-left, +right).
     * <br> y (-up, +down).
     * <br>}
     */
    public static double[] transformPolarToCartesian(double radius, double azimuth) {
        double x = radius * Math.sin(azimuth);
        double y = -radius * Math.cos(azimuth);
        return new double[] {x, y};
    }

    /**
     * Cartesian to spherical coordinate conversion. Uses world conventions.
     * @param x (-back, +front).
     * @param y (-left, +right).
     * @param z (-down, +up).
     * @return {
     * <br> Radius.
     * <br> Azimuth angle (0 forward, +clockwise from above).
     * <br> Polar angle (0 upward, +downward).
     * <br>}
     * @return {radius, azimuth angle, polar angle}
     */
    public static double[] transformCartesianToSpherical(double x, double y, double z) {
        double radius = Math.sqrt(x*x + y*y + z*z);
        double azimuth = Math.atan2(y, x);
        double polar = Math.acos(z/radius);
        return new double[] {radius, mod(azimuth, 2*Math.PI), mod(polar, Math.PI)};
    }

    /**
     * Spherical to cartesian coordinate conversion. Uses world conventions.
     * @param radius Radius
     * @param azimuth Azimuth angle (0 forward, +clockwise from above).
     * @param polar Polar angle (0 upward, +downward).
     * @return {
     * <br> x, (-back, +front).
     * <br> y, (-left, +right).
     * <br> z (-down, +up).
     * <br>}
     */
    public static double[] transformSphericalToCartesian(double radius, double azimuth, double polar) {
        double x = radius * Math.sin(polar) * Math.cos(azimuth);
        double y = radius * Math.sin(polar) * Math.sin(azimuth);
        double z = radius * Math.cos(polar);
        return new double[] {x, y, z};
    }
}
