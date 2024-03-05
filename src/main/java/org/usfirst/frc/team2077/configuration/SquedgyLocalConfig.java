package org.usfirst.frc.team2077.configuration;

import org.usfirst.frc.team2077.projection.*;
import org.usfirst.frc.team2077.projector.EquirectangularProjector;
import org.usfirst.frc.team2077.source.*;
import org.usfirst.frc.team2077.startup.Configuration;
import org.usfirst.frc.team2077.view.*;

import java.awt.*;

public class SquedgyLocalConfig extends Configuration {
    public static SquedgyLocalConfig create(boolean linux) {
        Dimension viewResolution = new Dimension(1920, 1080);
        Dimension sourceResolution = new Dimension(1920, 1080);

        var renderProj = new RenderingProjection(
              new RenderingProjection.Values(new EquirectangularProjector(), viewResolution)
                    .horizontalFovAngle(90)
        );

        var sourceProj = new SourceProjection(
              new SourceProjection.Values(new EquirectangularProjector(), sourceResolution)
                    .fovAngleHorizontal(90)
        );

        String command;
        if(linux) {
             command = String.join(
                   " ! ",
                   "v4l2src device=/dev/video0",
                   "image/jpeg,width=1920,height=1080,framerate=30/1",
                   "jpegdec",
                   "videoconvert",
                   "appsink"
             );
        } else {
            command = String.join(
                    " ! ",
                    "mfvideosrc device-path=\"\\\\\\\\\\?\\\\display\\#int3480\\#4\\&8bc03bf\\&0\\&uid144512\\#\\{e5323777-f976-4f5b-9b55-b94699c46e44\\}\\\\\\{213a57b6-aacf-4210-9800-cd4c54dae676\\}\"",
                    // "mfvideosrc device-path=\"\\\\\\\\\\?\\\\display\\#int3480\\#4\\&8bc03bf\\&0\\&uid144512\\#\\{e5323777-f976-4f5b-9b55-b94699c46e44\\}\\\\\\{bf89b5a5-61f7-4127-a279-e187013d7caf\\}\"",
                    "video/x-raw,format=NV12,width=1920,height=1080,framerate=30/1",
                    "videoconvert",
                    "appsink"
            );;
        }

        VideoView view = new OpenCvView(viewResolution, renderProj, sourceProj, (frame, overlay) -> {});
        VideoSource source = new GstreamerSource(sourceResolution, view, command);

        return new SquedgyLocalConfig(source, view);
    }

    protected SquedgyLocalConfig(
          VideoSource source,
          VideoView view
    ) {
        super(source, view);
    }
}
