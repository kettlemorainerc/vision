package org.usfirst.frc.team2077.projection;

import org.usfirst.frc.team2077.source.FrameSource;
import org.usfirst.frc.team2077.util.SuperProperties;

public class SineSource extends SourceProjection {
    private Double k;

    public SineSource(SuperProperties runProps, FrameSource source, RenderProjection rendering) {
        super(runProps, source, rendering);
    }

    private double getK() {
        if(k == null) k = props.getDouble("k", 1D);
        return k;
    }

    @Override protected double forwardProjection(double angle) {
        double k = getK();
        if (angle < 0 || angle / k > Math.PI / 2 || angle > Math.PI * 2) return -1;
        return k * Math.sin(angle / k);
    }
}
