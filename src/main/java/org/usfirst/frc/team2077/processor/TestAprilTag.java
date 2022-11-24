package org.usfirst.frc.team2077.processor;

import org.bytedeco.javacpp.indexer.*;
import org.bytedeco.opencv.global.*;
import org.bytedeco.opencv.opencv_aruco.*;
import org.bytedeco.opencv.opencv_aruco.Dictionary;
import org.bytedeco.opencv.opencv_core.*;
import org.usfirst.frc.team2077.util.*;

import org.slf4j.*;

public class TestAprilTag extends Processor {
    private static final Logger logger = LoggerFactory.getLogger(TestAprilTag.class);

    private static final Scalar BRIGHT_GREEN = new Scalar(255, 0, 255, 0);
    private static final Scalar PURPLE_PINK = new Scalar(255, 255, 0, 255);

    private static final Dictionary TAG_36H11 = Dictionary.get(opencv_aruco.DICT_APRILTAG_36h11);
    private static final DetectorParameters PARAMS = new DetectorParameters();
    private final MatVector corners = new MatVector();
    private final MatVector checked = new MatVector();
    private final Mat ids = new Mat();

    private final GpuMatVector gpuCorners = new GpuMatVector();
    private final GpuMatVector gpuChecked = new GpuMatVector();
    private final GpuMat gpuIds = new GpuMat();

    private final Mat gray = new Mat();
    private final GpuMat gpuGray = new GpuMat();

    public TestAprilTag(SuperProperties properties) {
        super(properties);
    }

    private static int sent;

    @Override public Mat process(Mat bgraFrame) {
        updateOverlayMat(bgraFrame);

        opencv_imgproc.cvtColor(bgraFrame, gray, opencv_imgproc.COLOR_BGRA2GRAY);

        corners.clear();
        opencv_aruco.detectMarkers(gray, TAG_36H11, corners, ids, PARAMS, checked);

        draw(checked, PURPLE_PINK);
        draw(corners, BRIGHT_GREEN);

        return overlay;
    }

    @Override public GpuMat process(GpuMat frame) {
        updateOverlayMat(frame);

        opencv_imgproc.cvtColor(frame, gpuGray, opencv_imgproc.COLOR_BGRA2GRAY);

        corners.clear();
        opencv_aruco.detectMarkers(gpuGray, TAG_36H11, gpuCorners, gpuIds, PARAMS, gpuChecked);

        draw(gpuChecked, PURPLE_PINK);
        draw(gpuChecked, BRIGHT_GREEN);

        return gpuGray;
    }

    private void draw(MatVector cornerMats, Scalar color) {
        MatVector.Iterator mats = cornerMats.begin();

        long count = cornerMats.size();
        for(long mat = 0; mat < count; mat++) {
            Mat m = mats.get();
            FloatRawIndexer indexer = m.createIndexer();

            int cols = m.cols();
            Point first = null, previous = null, cur;

            for(int corner = 0 ; corner < cols; corner++) {
                cur = toPoint(indexer, corner);
                if(first == null) first = cur;
                if(previous != null) drawLine(overlay, previous, cur, color);
                previous = cur;
            }

            drawLine(overlay, previous, first, color);
            mats.increment();
        }
    }

    private void draw(GpuMatVector cornerMats, Scalar color) {
        Mat local = new Mat();
        GpuMatVector.Iterator mats = cornerMats.begin();

        long count = cornerMats.size();
        for(long mat = 0; mat < count; mat++) {
            GpuMat m = mats.get();
            m.download(local);
            FloatRawIndexer indexer = local.createIndexer();

            int cols = m.cols();
            Point first = null, previous = null, cur;

            for(int corner = 0 ; corner < cols; corner++) {
                cur = toPoint(indexer, corner);
                if(first == null) first = cur;
                if(previous != null) drawLine(overlay, previous, cur, color);
                previous = cur;
            }

            drawLine(overlay, previous, first, color);
            mats.increment();
        }
    }

    private Point toPoint(FloatRawIndexer on, int corner) {
        return new Point((int) on.get(0, corner, 0), (int) on.get(0, corner, 1));
    }

    private static void drawLine(Mat on, Point a, Point b) {
        opencv_imgproc.line(on, a, b, BRIGHT_GREEN, 5, opencv_imgproc.FILLED, 0);
    }

    private static void drawLine(Mat on, Point a, Point b, Scalar color) {
        opencv_imgproc.line(on, a, b, color, 5, opencv_imgproc.FILLED, 0);
    }

    private static void mark(Mat on, Point a) {
        opencv_imgproc.drawMarker(on, a, BRIGHT_GREEN, opencv_imgproc.MARKER_CROSS, 15, 1, opencv_imgproc.FILLED);
    }
}
