package org.usfirst.frc.team2077.vision.processors;

import com.squedgy.frc.team2077.april.tags.AprilTag;
import com.squedgy.frc.team2077.april.tags.ByteImage;
import com.squedgy.frc.team2077.april.tags.TagFamily;
import com.squedgy.frc.team2077.april.tags.detection.Detection;
import com.squedgy.frc.team2077.april.tags.detection.DetectionResult;
import com.squedgy.frc.team2077.april.tags.detection.Detector;
import com.squedgy.frc.team2077.april.tags.detection.Point;
import org.opencv.aruco.Aruco;
import org.opencv.aruco.ArucoDetector;
import org.opencv.aruco.Dictionary;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.osgi.OpenCVNativeLoader;
import org.usfirst.frc.team2077.util.AprilTagUtils;
import org.usfirst.frc.team2077.util.MatColor;
import org.usfirst.frc.team2077.video.NTMain;
import org.usfirst.frc.team2077.vision.Main;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class AprilTagDetection implements Main.FrameProcessor {
    Mat camera;
    Mat distance;
    Mat newCameraMatrix;

    public AprilTagDetection(){
        Properties properties = NTMain.getProperties();
        camera = new Mat(3,3, CvType.CV_64FC1);
        distance = new Mat(1,5,CvType.CV_64FC1);

        buildMat(properties,camera,"camera");
        buildMat(properties,distance,"distance");
        System.out.println("\n\n\n\n\n\n\n\nconstructed\n\n\n\n\n\n\n\n");
    }

    static void buildMat(Properties properties, Mat mat, String matId){
        for(int row = 0; row < mat.rows(); row++){
            for(int col = 0; col < mat.cols(); col++){
                String[] stringvalues = properties.get("AprilTag." + matId + "." + row + "." + col).toString().split(", *");
//                double[] values = new double[3];
//                values[0] = Double.parseDouble(stringvalues[0]);
                //values[1] = Double.parseDouble(stringvalues[1]);
                //values[2] = Double.parseDouble(stringvalues[2]);
                double values = Double.parseDouble(stringvalues[0]);
                mat.put(row,col,values);


            }
        }

    }




    static{

        try{
            System.out.println(new File(".").getAbsolutePath());

            AprilTag.initialize();
        }catch(Exception e){
            System.out.println("does dll exist: " + new File("./april_tags_2077.dll").exists());
            System.out.println("does lib exist: " + new File("./apriltagd.lib").exists());
            throw new RuntimeException(e);
        }

    }

    private final ArucoDetector detector = new ArucoDetector(Dictionary.get(20));

    private static org.opencv.core.Point transform(Point p){
        return new org.opencv.core.Point(p.getX(), p.getY());
    }

    private static void print(Object s) {
        System.out.println("\n" + s + "\n");
    }

    @Override
    public void processFrame(Mat input2, Mat overlayMat) {
        Mat input = new Mat();
        Imgproc.cvtColor(input2, input, Imgproc.COLOR_BGRA2BGR);


        Mat frameMat = new Mat(input.rows(), input.cols(), input.type());

        Calib3d.undistort(input, frameMat, camera, distance);

        List<Mat> corners = new LinkedList<Mat>();
        Mat ids = new Mat();

        Mat grayscale = new Mat();

        Imgproc.cvtColor(frameMat, grayscale, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(frameMat, overlayMat, Imgproc.COLOR_BGR2RGB);
        detector.detectMarkers(grayscale, corners, ids);
        Aruco.drawDetectedMarkers(overlayMat, corners, ids);

        Imgproc.cvtColor(overlayMat, overlayMat, Imgproc.COLOR_RGB2BGRA);



//        print(corners.size());
//        Aruco.estimatePoseSingleMarkers

    }
}
