package org.usfirst.frc.team2077.startup;

import org.opencv.core.Scalar;
import org.opencv.osgi.OpenCVNativeLoader;
import org.usfirst.frc.team2077.configuration.SquedgyLocalConfig;

public class Startup {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(Startup.class);
    private static final Scalar GREEN = new Scalar(255, 0, 255, 0);

    public static void main(String[] args) {
        new OpenCVNativeLoader().init();

        try(Configuration config = SquedgyLocalConfig.create(true)) {
            config.processFrames();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
