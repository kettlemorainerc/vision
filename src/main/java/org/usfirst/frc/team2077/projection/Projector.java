package org.usfirst.frc.team2077.projection;

public interface Projector {
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
    double[] renderingProjection(RenderingProjection projection, double x, double y);

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
    double[] sourceProjection(SourceProjection projection, double world_o_clock, double world_from_center);

    double getDefaultK();
    double getDefaultFov();
}
