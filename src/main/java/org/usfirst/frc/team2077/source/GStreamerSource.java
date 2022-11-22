package org.usfirst.frc.team2077.source;

import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.Buffer;
import org.freedesktop.gstreamer.elements.AppSink;
import org.freedesktop.gstreamer.lowlevel.GstAPI;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.slf4j.*;
import org.usfirst.frc.team2077.util.SuperProperties;
import org.usfirst.frc.team2077.view.View;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.*;
import java.time.LocalDateTime;
import java.time.format.*;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.Timer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link FrameSource} that generates and processes frames utilizing a gstreamer pipeline.
 * <p></p>
 * Extra configuration properties include
 * <dl>
 *     <dt>pipeline</dt>
 *     <dd>The actual gstreamer (gst-start-1.0) pipeline to utilize for retrieving frames. This is required</dd>
 *     <dt>capture.file-name</dt>
 *     <dd>The target file name to utilize if the given pipeline contains the string "$FILE". Defaults to the name of the source</dd>
 * </dl>
 */
public class GStreamerSource extends FrameSource {
    private static final Logger logger = LoggerFactory.getLogger(GStreamerSource.class);

    private final String pipelineStr;
    private Dimension dimension;
    private AppSink sink;
    private Pipeline pipeline;
    private Mat output;
    private final Timer fpsLogging = new Timer();

    public GStreamerSource(SuperProperties runProps, String name) {
        super(runProps, name);

        String pipelineStr = props.get("pipeline");
        if(pipelineStr == null) throw new IllegalStateException("Cannot have a gstreamer source without a 'pipeline' property");

        this.pipelineStr = checkForCaptureFile(props, pipelineStr, name);
    }

    @Override public Dimension getResolution() {return dimension;}

    @Override public void start() {
        if(closed()) return;
        super.start();

        sink = null;
        Bin bin = Gst.parseBinFromDescription(pipelineStr, true);

        for(Element e : bin.getElementsSorted()) {
            if(e instanceof AppSink) {
                sink = (AppSink) e;
                break;
            }
        }

        if(sink == null) throw new IllegalStateException("No 'appsink' element in gstreamer pipeline!");
        pipeline = new Pipeline();
        pipeline.add(bin);
//        sink.setCaps(new Caps("video/x-raw,format=RGBA"));
        sink.set("drop", true);
        sink.set("max-buffers", 1);
        sink.set("emit-signals", true);
        AppSink.NEW_SAMPLE listener = this::sampleReady;
        sink.connect(listener);
        pipeline.play();
        logger.info("Playing pipeline");
    }

    @Override protected void processFrame() {
        if(dimension == null || output == null) return;

        Mat out = output;
        for(View v : myViews) v.process(out);
        frames.getAndIncrement();
        output = null;
        System.gc();
    }

    private FlowReturn sampleReady(AppSink sink) {
        Sample sample = sink.pullSample();
        if(sample != null) processSample(sample);

        return FlowReturn.OK;
    }

    private final AtomicInteger frames = new AtomicInteger();
    private void processSample(Sample sample) {
        if(dimension == null) {
            Caps caps = sample.getCaps();
            logger.info("Caps: {}", caps);
            Structure struc = caps.getStructure(0);
            dimension = new Dimension(struc.getInteger("width"), struc.getInteger("height"));

            long seconds = 5;
            long interval = TimeUnit.SECONDS.toMillis(seconds);
            fpsLogging.scheduleAtFixedRate(new TimerTask() {@Override public void run() {
                logger.info("FPS: {}", frames.getAndSet(0) / seconds);
            }}, interval, interval);

            for(View v : myViews) {
                v.frame.revalidate();
                v.frame.pack();
                v.frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                v.frame.setVisible(true);

                v.buildSourceMapping(props.unprefixed(), this);
            }
        }

        Buffer buffer = sample.getBuffer();
        ByteBuffer bb = buffer.map(false);
        bb.rewind();

        Mat next = new Mat(dimension.height, dimension.width, CvType.CV_8UC4, bb);
        output = next;

        buffer.unmap();
        sample.dispose();
    }

    @Override protected void cleanup() {
        sink = null;
        pipeline = null;
        dimension = null;
    }

    private static String checkForCaptureFile(SuperProperties props, String currentPipeline, String source) {
        if(currentPipeline.contains("$FILE")) {
            String name = props.get(".capture.file-name", source);
            String dt = LocalDateTime.now().format(getFileTimestampFormat());
            String fileName = name + '-' + dt;

            File parent = new File(fileName).getParentFile();
            if(parent != null) parent.mkdirs();

            return currentPipeline.replace("$FILE", fileName);
        }

        return currentPipeline;
    }

    private static DateTimeFormatter getFileTimestampFormat() {
        return new DateTimeFormatterBuilder()
                .append(DateTimeFormatter.ISO_LOCAL_DATE)
                .appendLiteral('-')
                .appendValue(ChronoField.HOUR_OF_DAY, 2)
                .appendLiteral('-')
                .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                .appendLiteral('-')
                .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                .optionalStart()
                .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                .optionalEnd()
                .appendOffsetId()
                .toFormatter();
    }
}
