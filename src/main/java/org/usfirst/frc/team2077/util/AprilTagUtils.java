package org.usfirst.frc.team2077.util;

import com.squedgy.frc.team2077.april.tags.ByteImage;
import com.squedgy.frc.team2077.april.tags.detection.Detector;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

public class AprilTagUtils {
    private AprilTagUtils() {}

    /**
     * Convert a mat into a {@link ByteImage} to be processed by a {@link Detector}
     *
     * @param convert a grayscale mat
     * @return an image referring to the same data as the provided mat
     */
    public static ByteImage imageFromMat(Mat convert) {
        return new ByteImage(
                convert.rows(),
                convert.cols(),
                convert.dataAddr()
        );
    }

    /**
     * Convert (or clone, if no transformation is needed) a mat to a grayscale image
     * that could be processed by the april-tags detector
     *
     * @param toPrepare a mat to convert/copy to scan for april-tags
     * @param source the color of the mat
     * @return a grayscale mat that can be converted to a {@link ByteImage} and processed by a {@link Detector}
     *
     * @see #imageFromMat(Mat) to convert the returned mat into a {@link ByteImage}
     */
    public static Mat prepareMatForScanning(Mat toPrepare, MatColor source) {
        Mat ret = new Mat();

        if(source == MatColor.GRAY) ret = toPrepare.clone();
        else Imgproc.cvtColor(toPrepare, ret, source.TO_GRAY);

        return ret;
    }
}
