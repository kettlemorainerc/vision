package org.usfirst.frc.team2077.startup;

import org.usfirst.frc.team2077.source.VideoSource;
import org.usfirst.frc.team2077.view.VideoView;

public abstract class Configuration implements AutoCloseable {
    protected final VideoView view;
    protected final VideoSource source;

    protected Configuration(VideoSource source, VideoView view) {
        this.source = source;
        this.view = view;

        view.forSource(source);
    }

    @Override public void close() throws Exception {
        source.close();
    }
}
