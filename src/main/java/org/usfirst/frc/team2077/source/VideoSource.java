package org.usfirst.frc.team2077.source;

import org.usfirst.frc.team2077.view.VideoView;

import java.awt.*;
import java.nio.IntBuffer;
import java.util.function.Consumer;

public abstract class VideoSource implements AutoCloseable {
    private final VideoView view;

    public VideoSource(VideoView view) {
        if(view == null) throw new IllegalArgumentException("A source must have a valid view!");
        this.view = view;
    }

    /** More of a "could" have more frames. */
    public abstract boolean hasMoreFrames();
    /** Attempts to grab the next frame. Can block if necessary. */
    public abstract IntBuffer getNextFrame() throws InterruptedException;

    public abstract Dimension getResolution();

}
