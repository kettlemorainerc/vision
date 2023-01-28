package org.usfirst.frc.team2077.vision.processors;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.usfirst.frc.team2077.vision.Main;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class frameSaver implements Main.FrameProcessor {
    JFrame frame;
    Checkbox checkbox;
    public frameSaver(){
        frame = new JFrame();
        frame.setMinimumSize(new Dimension(200,50));
        checkbox = new Checkbox();
        checkbox.setMinimumSize(new Dimension(200,50));
        checkbox.setLabel("Save Image");
        frame.add(checkbox);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
        File f = new File("pictures");
        if(!f.exists()) f.mkdir();
    }


    @Override
    public void processFrame(Mat frameMat, Mat overlayMat) {
        if(checkbox.getState()){
            Path p = Paths.get("pictures", System.currentTimeMillis() + ".png").toAbsolutePath();
            Imgcodecs.imwrite(p.toString(), frameMat);
            checkbox.setState(false);
        }
    }

}
