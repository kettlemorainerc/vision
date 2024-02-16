package org.usfirst.frc.team2077.startup;

import org.usfirst.frc.team2077.video.interfaces.*;

import java.util.List;

public abstract class Configuration {
    protected final VideoView view;
    protected final VideoSource source;

    protected Configuration(VideoSource source, VideoView view) {
        this.source = source;
        this.view = view;
    }

    public void start() {
        source.start();
        source.activateViews(List.of(view));
    }

    public void stop() {
        source.stop();
    }
}
