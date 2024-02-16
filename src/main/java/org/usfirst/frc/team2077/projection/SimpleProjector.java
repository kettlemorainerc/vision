package org.usfirst.frc.team2077.projection;

public abstract class SimpleProjector implements Projector {
    private Double focalLength;

    @Override public double[] renderingProjection(RenderingProjection projection, double x, double y) {
        double[] rt = ProjectionUtils.transformCartesianToPolar(x, y);

        double radius = rt[0];
        double azimuth = rt[1];

        radius /= projection.focalLength;

        double polar = backProjection(projection.K, radius);

        if (polar < 0) return null; // out of range

        return new double[] {ProjectionUtils.mod(azimuth, 2*Math.PI), ProjectionUtils.mod(polar, Math.PI)};
    }

    protected abstract double backProjection(double K, double radius);

    public double[] sourceProjection(SourceProjection projection, double world_o_clock, double world_from_center) {
        double azimuth = world_o_clock;
        double radius = forwardProjection(projection.K, world_from_center);

        if (radius < 0) return null; // out of range

        if(focalLength == null) {
            focalLength = 1 / forwardProjection(projection.K, projection.fovAngleHorizontal / 2);
        }

        radius *= focalLength;

        return ProjectionUtils.transformPolarToCartesian(radius, azimuth);
    }

    protected abstract double forwardProjection(double K, double angle);
}
