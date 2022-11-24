package org.usfirst.frc.team2077.processor;

import org.bytedeco.javacpp.indexer.*;
import org.bytedeco.opencv.global.*;
import org.bytedeco.opencv.opencv_aruco.*;
import org.bytedeco.opencv.opencv_aruco.Dictionary;
import org.bytedeco.opencv.opencv_core.*;
import org.usfirst.frc.team2077.util.*;
//import com.squedgy.frc.team2077.april.tags.detection.Point;

import org.slf4j.*;

public class TestAprilTag extends Processor {
    private static final Logger logger = LoggerFactory.getLogger(TestAprilTag.class);

    private static final Scalar BRIGHT_GREEN = new Scalar(255, 0, 255, 0);
    private static final Scalar PURPLE_PINK = new Scalar(255, 255, 0, 255);

    private static final Dictionary TAG_36H11 = Dictionary.get(opencv_aruco.DICT_APRILTAG_36h11);
    private static final DetectorParameters PARAMS = new DetectorParameters();
    private final MatVector corners = new MatVector();
    private final Mat ids = new Mat();
    private final MatVector checked = new MatVector();

//    private final Detector detector = new Detector(TagFamily.TAG_36H11);

    private final Mat gray = new Mat();
    // private final MarkerDetector detector = new MarkerDetector();
    // private final Mat ids = new Mat();
    // private final LinkedList<Mat> corners = new LinkedList<>();
    // private final LinkedList<Mat> checked = new LinkedList<>();

    public TestAprilTag(SuperProperties properties) {
        super(properties);
    }

    private static int sent;

    @Override public Mat process(Mat bgraFrame) {
        updateOverlayMat(bgraFrame);

        opencv_imgproc.cvtColor(bgraFrame, gray, opencv_imgproc.COLOR_BGRA2GRAY);

        corners.clear();
        opencv_aruco.detectMarkers(gray, TAG_36H11, corners, ids, PARAMS, checked);

        if((sent = (sent + 1) % 6) == 0) logger.info("Detected {} tags checked {}", corners.size(), checked.size());
        draw(checked, PURPLE_PINK);
        draw(corners, BRIGHT_GREEN);

        return overlay;
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
