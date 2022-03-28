package org.usfirst.frc.team2077.vision.processors.ball;

import org.opencv.core.*;
import org.usfirst.frc.team2077.vision.processors.*;

import java.util.*;

public interface BallDetector {
    Optional<Ball> detectNearestBall(Mat image);
}
