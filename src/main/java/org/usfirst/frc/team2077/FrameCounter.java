package org.usfirst.frc.team2077;

import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FrameCounter {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(FrameCounter.class);

    private final AtomicInteger frameCount = new AtomicInteger(0);
    private final Timer timer = new Timer();
    private final List<Integer> frameCounts = new ArrayList<>(10);

    public FrameCounter() {
        timer.scheduleAtFixedRate(new java.util.TimerTask() {
            public void run() {
                int count = frameCount.getAndSet(0);

                if(frameCounts.size() == 10) {
                    frameCounts.remove(0);
                }

                frameCounts.add(count);

                double avg = frameCounts.stream().mapToInt(Integer::intValue).average().orElse(0);
                LOGGER.info("Frame rate: [count={}][{}FrameAvg={}]", count, frameCounts.size(), avg);
            }
        }, 1000, 1000);
    }

    public void recordFrame() {
        frameCount.incrementAndGet();
    }
}
