package org.usfirst.frc.team2077.startup;

import org.opencv.core.Mat;
import org.usfirst.frc.team2077.projection.*;
import org.usfirst.frc.team2077.projector.*;
import org.usfirst.frc.team2077.source.*;
import org.usfirst.frc.team2077.view.*;
import org.usfirst.frc.team2077.view.processors.FrameProcessor;

import java.awt.*;
import java.nio.IntBuffer;

public class Startup {
    public static void main(String[] args) {
        Dimension viewResolution = new Dimension(1000, 1000);
        Dimension sourceResolution = new Dimension(768, 540);

        VideoView view = new OpenCvView(
                viewResolution,
                new RenderingProjection(new RenderingProjection.Values(new CylindricalProjector(), viewResolution)),
                new SourceProjection(new SourceProjection.Values(new SineProjector(), sourceResolution)),
                (FrameProcessor) (frame, overlay) -> {

                }
        );

        String pipeline = "gst-launch-1.0 v4l2src device=/dev/video0 ! image/jpeg,width=1920,height=1080,framerate=30/1 ! jpegdec ! videoconvert ! appsink";

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
