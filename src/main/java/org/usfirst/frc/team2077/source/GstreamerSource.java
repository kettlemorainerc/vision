package org.usfirst.frc.team2077.source;

import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.Buffer;
import org.freedesktop.gstreamer.elements.AppSink;
import org.usfirst.frc.team2077.view.VideoView;

import java.awt.*;
import java.nio.*;

public class GstreamerSource extends VideoSource {
    static {Gst.init();}

    private final Pipeline pipeline;
    private final AppSink sink;
    private final Bin bin;
    private final Dimension resolution;

    private Sample lastSample = null;

    public GstreamerSource(Dimension resolution, VideoView view, String pipelineString) {
        super(view);
        this.resolution = resolution;

        bin = Gst.parseBinFromDescription(pipelineString, true);

        AppSink sink = null;
        for (Element e : bin.getElementsSorted()) {
            if(e instanceof AppSink) {
                sink = (AppSink)e;
                break;
            }
        }

        if(sink == null) {
            throw new RuntimeException("No AppSink found in pipeline.");
        }
        this.sink = sink;
        this.sink.set("drop", true);
        this.sink.set("max-buffers", 1);
        this.sink.setCaps(new Caps("video/x-raw," + (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? "format=BGRx" : "format=xRGB")));

        pipeline = new Pipeline();
        pipeline.add(bin);

        pipeline.play();
    }

    @Override public boolean hasMoreFrames() {return pipeline.isPlaying();}

    @Override public IntBuffer getNextFrame() throws InterruptedException {
        if(lastSample != null) {
            lastSample.getBuffer().unmap();
            lastSample.dispose();
        }

        lastSample = sink.pullSample();

        if(lastSample == null) {
            return null;
        }

        Buffer buffer = lastSample.getBuffer();
        ByteBuffer byteBuffer = buffer.map(false);

        return byteBuffer.asIntBuffer();
    }

    @Override public Dimension getResolution() {
        return resolution;
    }

    @Override public void close() throws Exception {
        pipeline.stop();
        pipeline.dispose();

        bin.dispose();
        bin.close();
    }
}
