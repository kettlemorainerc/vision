package org.usfirst.frc.team2077.startup;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.osgi.OpenCVNativeLoader;
import org.usfirst.frc.team2077.FrameCounter;
import org.usfirst.frc.team2077.projection.*;
import org.usfirst.frc.team2077.projector.*;
import org.usfirst.frc.team2077.source.*;
import org.usfirst.frc.team2077.view.*;
import org.usfirst.frc.team2077.view.processors.FrameProcessor;

import java.awt.*;
import java.nio.IntBuffer;

public class Startup {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(Startup.class);
    private static final Scalar GREEN = new Scalar(255, 0, 255, 0);

    public static void main(String[] args) {
        LOG.info(System.getProperty("java.library.path"));
        new OpenCVNativeLoader().init();
        Dimension viewResolution = new Dimension(1920, 1080);
        Dimension sourceResolution = new Dimension(1920, 1080);

        FrameCounter counter = new FrameCounter();

        VideoView view = new OpenCvView(
                viewResolution,
                new RenderingProjection(
                      new RenderingProjection.Values(new EquirectangularProjector(), viewResolution)
                              .horizontalFovAngle(90)
                ),
                new SourceProjection(
                      new SourceProjection.Values(new EquirectangularProjector(), sourceResolution)
                                .fovAngleHorizontal(90)
                ),
                (FrameProcessor) (frame, overlay) -> {
                    counter.recordFrame();

                    Imgproc.rectangle(
                            overlay,
                            new Rect(0, 0, 100, 100),
                            GREEN,
                            5,
                            Imgproc.LINE_4
                    );
                }
        );

//        String pipeline = String.join(
//              " ! ",
//              "mfvideosrc device-path=\"\\\\\\\\\\?\\\\display\\#int3480\\#4\\&8bc03bf\\&0\\&uid144512\\#\\{e5323777-f976-4f5b-9b55-b94699c46e44\\}\\\\\\{bf89b5a5-61f7-4127-a279-e187013d7caf\\}\"",
//              "video/x-raw,format=NV12,width=1920,height=1080,framerate=30/1",
//              "videoconvert",
//              "appsink"
//        );

         String pipeline = String.join(
                 " ! ",
                 "v4l2src device=/dev/video0",
                 "image/jpeg,width=1920,height=1080,framerate=30/1",
                 "jpegdec",
                 "videoconvert",
                 "appsink"
         );

        try(VideoSource source = new GstreamerSource(sourceResolution, view, pipeline)) {
            view.forSource(source);

            while (source.hasMoreFrames()) {
                IntBuffer frame = source.getNextFrame();
                view.processFrame(frame);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
