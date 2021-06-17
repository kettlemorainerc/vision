package org.usfirst.frc.team2077.video.sources;

import java.time.*;
import java.util.concurrent.*;

public class TimeOut {
    private long from = System.currentTimeMillis();
    private final Duration after;
    private long lastDiff;

    public TimeOut(Duration after) {
        this.after = after;
    }

    public void reset() {
        from = System.currentTimeMillis();
    }

    public boolean hasTimedOut() {
        lastDiff = from - System.currentTimeMillis();
        return Duration.ofMillis(lastDiff).compareTo(after) < 0;
    }

    public long lastDiff() {
        return lastDiff;
    }

    public long lastDiff(TimeUnit in) {
        return in.convert(lastDiff, TimeUnit.MILLISECONDS);
    }
}
