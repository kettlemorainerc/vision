package org.usfirst.frc.team2077.video.sources;

import java.util.concurrent.*;

public class TimeOut {
    private long from = System.currentTimeMillis();
    private final long after;
    private long lastDiff;

    public TimeOut(long after) {
        this.after = after;
    }

    public void reset() {
        from = System.currentTimeMillis();
    }

    public boolean hasTimedOut() {
        lastDiff = from - System.currentTimeMillis();
        return lastDiff > after;
    }

    public long lastDiff() {
        return lastDiff;
    }

    public long lastDiff(TimeUnit in) {
        return in.convert(lastDiff, TimeUnit.MILLISECONDS);
    }
}
