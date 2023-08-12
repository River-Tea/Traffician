package org.rivertea.traffician;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import java.util.ArrayList;
import java.util.List;

public class OpticalFlow {
    private static final double
            PYR_SCALE = 0.5,
            POLY_SIGMA = 1.2;
    private static final int
            LEVELS = 3,
            WIN_SIZE = 15,
            ITERATIONS = 5,
            POLYGON_EDGE = 5,
            FLAGS = 0,
            STEP_SIZE = 10;
    Mat prevFrame, nextFrame, vectorFlow;

    public OpticalFlow() {
    }

    public OpticalFlow(Mat prevFrame, Mat nextFrame, Mat vectorFlow) {
        this.prevFrame = prevFrame;
        this.nextFrame = nextFrame;
        this.vectorFlow = vectorFlow;
    }

    public Mat getPrevFrame() {
        return prevFrame;
    }

    public void setPrevFrame(Mat prevFrame) {
        this.prevFrame = prevFrame;
    }

    public Mat getNextFrame() {
        return nextFrame;
    }

    public void setNextFrame(Mat nextFrame) {
        this.nextFrame = nextFrame;
    }

    public Mat getVectorFlow() {
        return vectorFlow;
    }

    public void setVectorFlow(Mat vectorFlow) {
        this.vectorFlow = vectorFlow;
    }

    public Mat calOpticalFlowVector() {
        // Calculate optical flow using Farneback method
        Video.calcOpticalFlowFarneback(prevFrame, nextFrame, vectorFlow, PYR_SCALE, LEVELS, WIN_SIZE,
                ITERATIONS, POLYGON_EDGE, POLY_SIGMA, FLAGS);
        return vectorFlow;
    }

    public void drawVectorFlow(Mat outputFrame) {
        for (int y = 0; y < vectorFlow.rows(); y += STEP_SIZE) {
            for (int x = 0; x < vectorFlow.cols(); x += STEP_SIZE) {
                double[] flowVector = vectorFlow.get(y, x);
                double flowX = flowVector[0];
                double flowY = flowVector[1];

                Point startPoint = new Point(x, y);
                Point endPoint = new Point(x + flowX, y + flowY);
                Imgproc.arrowedLine(outputFrame, startPoint, endPoint, new Scalar(0, 255, 0), 1);
            }
        }
    }

    public List<Double> calDensityAndVelocity(int frameCount) {
        // Calculate vector flow statistics
        List<Double> densityAndVelocity = new ArrayList<>();
        double upwardDensity = 0;
        double downwardDensity = 0;
        double avgMagnitudeUpward = 0;
        double avgMagnitudeDownward = 0;

        for (int y = 0; y < vectorFlow.rows(); y++) {
            for (int x = 0; x < vectorFlow.cols(); x++) {
                double[] flowVector = vectorFlow.get(y, x);

                double angle = Math.atan2(flowVector[0], flowVector[1]);
                if (angle < 0) {
                    angle += 2 * Math.PI;
                }

                // Calculate vector value
                double magnitude = Math.sqrt(flowVector[0] * flowVector[0] + flowVector[1] * flowVector[1]);

                // Classify vector direction and average magnitude
                if (angle >= 0.25 * Math.PI && angle < 0.75 * Math.PI) {
                    upwardDensity++;
                    if (magnitude >= 10) {
                        avgMagnitudeUpward = magnitude / frameCount;
                    }
                } else if (angle >= 1.25 * Math.PI && angle < 1.75 * Math.PI) {
                    downwardDensity++;
                    if (magnitude >= 10) {
                        avgMagnitudeDownward = magnitude / frameCount;
                    }
                }
            }
        }

        double totalDensity = upwardDensity + downwardDensity;
        upwardDensity /= totalDensity;
        downwardDensity /= totalDensity;

        // Lưu kết quả
        densityAndVelocity.add(upwardDensity);
        densityAndVelocity.add(downwardDensity);
        densityAndVelocity.add(avgMagnitudeUpward);
        densityAndVelocity.add(avgMagnitudeDownward);

        return densityAndVelocity;
    }
}
