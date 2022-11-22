package org.usfirst.frc.team2077.processor;

import org.opencv.aruco.*;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.usfirst.frc.team2077.util.*;
//import com.squedgy.frc.team2077.april.tags.detection.Point;

import org.slf4j.*;

import java.util.*;

public class TestAprilTag extends Processor {
    private static final Logger logger = LoggerFactory.getLogger(TestAprilTag.class);

    private static final Scalar BRIGHT_GREEN = new Scalar(255, 0, 255, 0);
    private static final Scalar PURPLE_PINK = new Scalar(255, 255, 0, 255);

//    private final Detector detector = new Detector(TagFamily.TAG_36H11);

    private final Mat gray = new Mat();
    private final ArucoDetector detector = new ArucoDetector(Aruco.getPredefinedDictionary(Aruco.DICT_APRILTAG_36h11));
    private final Mat ids = new Mat();
    private final LinkedList<Mat> corners = new LinkedList<>();
    private final LinkedList<Mat> checked = new LinkedList<>();

    public TestAprilTag(SuperProperties properties) {
        super(properties);
    }

    private final float[] xy = new float[2];

    private static int sent;

    @Override public Mat process(Mat bgraFrame) {
        updateOverlayMat(bgraFrame);

        Imgproc.cvtColor(bgraFrame, gray, Imgproc.COLOR_BGRA2GRAY);
        corners.clear();
        detector.detectMarkers(gray, corners, ids, checked);

//        logger.info("Detected {} corners checked {}", corners.size(), checked.size());

        if((sent = (sent + 1) % 6) == 0) logger.info("Detected {} tags checked {}", corners.size(), checked.size());
        draw(checked, PURPLE_PINK);
        draw(corners, BRIGHT_GREEN);

        return overlay;
    }

    private void draw(List<Mat> cornerMats, Scalar color) {
        Iterator<Mat> mats = cornerMats.iterator();
        if(!mats.hasNext()) return;

        for(Mat m : cornerMats) {
            int cols = m.cols();
            Point first = null, previous = null, cur;
            for(int i = 0 ; i < cols; i++) {
                cur = toPoint(m, i);
                if(first == null) first = cur;
                if(previous != null) drawLine(overlay, previous, cur, color);
                previous = cur;
            }

            drawLine(overlay, previous, first, color);
        }
    }

    private Point toPoint(Mat on, int idx) {
        on.get(0, idx, xy);
        return new Point(xy[0], xy[1]);
    }

    private static void drawLine(Mat on, Point a, Point b) {
        Imgproc.line(on, a, b, BRIGHT_GREEN, 5);
    }

    private static void drawLine(Mat on, Point a, Point b, Scalar color) {
        Imgproc.line(on, a, b, color, 5);
    }

    private static void mark(Mat on, Point a) {
        Imgproc.drawMarker(on, a, BRIGHT_GREEN, Imgproc.MARKER_CROSS, 15);
    }
}
