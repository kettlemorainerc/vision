package org.usfirst.frc.team2077.vision.processors;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.osgi.OpenCVNativeLoader;
import org.usfirst.frc.team2077.vision.Main;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class CameraCalibration{




    private static List<Mat> objectPoints = new LinkedList<>();
    private static List<Mat> imagePoints = new LinkedList<>();

    private static int sampleSize = 0;

    private static Size imageSize = new Size(9, 6);

    private static Mat obj;
    public static void printMat(Mat mat){
        System.out.println("[");

        double[][] arr = new double[mat.cols()][mat.channels()];
        for(int i = 0; i < mat.rows(); i++){

            for(int n = 0; n < mat.cols(); n++){
                mat.convertTo(mat,6);
                mat.get(i,n,arr[n]);
            }
            System.out.printf("%s;%n",Arrays.deepToString(arr));

        }
        System.out.println("]");
    }
    public static void main(String[] args) {
        new OpenCVNativeLoader().init();
        File imageDirectory = new File("./pictures");
        if (!imageDirectory.exists()) {
            throw new RuntimeException("image directory does not exist");
        }


        //I now know what this does but why is it like this?
        Mat obj = new Mat(9 * 6, 1, CvType.CV_32FC3);



        byte col = 0;
        byte row = 0;
        for (int matRow = 0; matRow < obj.rows(); matRow++) {
            obj.put(matRow, 0, new float[]{col++, row, 0});
            if (col == 6) {
                row++;
                col = 0;
            }
        }





        TermCriteria criteria = new TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER, 30, 0.001);


        File[] imageArray = imageDirectory.listFiles();

        for (File image : imageArray) {

            Mat imageMat = Imgcodecs.imread(image.getAbsolutePath());

            if (imageMat.empty()) continue;

            Mat grayscaleMat = new Mat();
            Imgproc.cvtColor(imageMat, grayscaleMat, Imgproc.COLOR_BGR2GRAY);

            MatOfPoint2f corners = new MatOfPoint2f();

            boolean foundPattern = Calib3d.findChessboardCorners(grayscaleMat, imageSize, corners);

            if (!foundPattern) {
                HighGui.destroyAllWindows();
                return;
            }


            objectPoints.add(obj);
            Imgproc.cornerSubPix(grayscaleMat, corners, new Size(11, 11), new Size(-1, -1), criteria);
            imagePoints.add(corners);


            Calib3d.drawChessboardCorners(imageMat, imageSize, corners, true);

            HighGui.imshow("img", imageMat);
            HighGui.waitKey(500);


        }
        HighGui.destroyAllWindows();
        //System.out.println("Running camera calibration");

        List<Mat> rvecs = new LinkedList<>();
        List<Mat> tvecs = new LinkedList<>();
        Mat imageMatrix = new Mat(3,3,CvType.CV_32F);

        Mat distance = new Mat();
        Calib3d.calibrateCamera(objectPoints, imagePoints, imageSize, imageMatrix, distance, rvecs, tvecs);

        System.out.println("objectPoints: ");
        for(int i = 0; i < objectPoints.size(); i++){
            printMat(objectPoints.get(i));
        }
        System.out.println("imagePoints: ");
        for(int i = 0; i < imagePoints.size(); i++){
            printMat(imagePoints.get(i));
        }
        System.out.println("Camera Matrix: " );
        printMat(imageMatrix);
        System.out.println("Distance: ");
        printMat(distance);
        System.out.println("rvecs: ");
        for(int i = 0; i < rvecs.size(); i++){
            printMat(rvecs.get(i));
        }
        System.out.print("tvecs: ");
        for(int i = 0; i < tvecs.size(); i++){
            printMat(tvecs.get(i));
        }

//    @Override
//    public void processFrame(Mat frameMat, Mat overlayMat) {
//        calibrateCamera(frameMat, overlayMat);
//    }
}}