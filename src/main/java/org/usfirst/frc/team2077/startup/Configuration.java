package org.usfirst.frc.team2077.startup;

import org.usfirst.frc.team2077.source.VideoSource;
import org.usfirst.frc.team2077.view.VideoView;

import java.nio.IntBuffer;

public abstract class Configuration implements AutoCloseable {
    protected final VideoView view;
    protected final VideoSource source;

    protected Configuration(VideoSource source, VideoView view) {
        this.source = source;
        this.view = view;

        view.forSource(source);
    }

    public void processFrames() throws InterruptedException {
        while(source.hasMoreFrames()) {
            IntBuffer frame = source.getNextFrame();
            view.processFrame(frame);
        }
    }

    @Override public void close() throws Exception {
        source.close();
    }
}
