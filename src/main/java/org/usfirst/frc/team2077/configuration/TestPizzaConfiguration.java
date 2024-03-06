package org.usfirst.frc.team2077.configuration;

import org.usfirst.frc.team2077.projection.*;
import org.usfirst.frc.team2077.projector.*;
import org.usfirst.frc.team2077.source.*;
import org.usfirst.frc.team2077.startup.*;
import org.usfirst.frc.team2077.view.*;

import java.awt.*;

public class TestPizzaConfiguration extends Configuration {

    public static TestPizzaConfiguration create() {
        var sourceResolution = new Dimension(768, 540);
        var targetResolution = new Dimension(768, 540);
        var conn = new RaspberryPiCameraConnection(
              new RaspberryPiCameraConnection.PiVideoFeed()
                    .width(768)
                    .height(540)
                    .timeout(0)
                    .fps(20)
                    .regionOfInterest(.1, 0, .8, 1)
                    .bitrate(750000)
                    .device(1),
              "pi", "raspberry", "10.20.77.11"
        );

        conn.start();

        var sourceProj = new SourceProjection(
              new SourceProjection.Values(new SineProjector(), sourceResolution)
                    .fovAngleHorizontal(121.1)
                    .K(3)
        );

        var renderProj = new RenderingProjection(
              new RenderingProjection.Values(
                    new CylindricalProjector(),
                    targetResolution
              )
                    .horizontalFovAngle(121.1)
                    .verticalFovAngle(93)
        );

        var view = new OpenCvView(targetResolution, renderProj, sourceProj, (a, b) -> {});

        return new TestPizzaConfiguration(
              new GstreamerSource(
                    sourceResolution,
                    view,
                    String.join(
                          " ! ",
                          "udpsrc port=5801",
                          "application/x-rtp,media=video,encoding-name=H264",
                          "rtph264depay",
                          "avdec_h264",
                          "videoconvert",
                          "appsink"
                    )
              ),
              view
        );
    }

    protected TestPizzaConfiguration(
          VideoSource source,
          VideoView view
    ) {
        super(source, view);
    }
}
