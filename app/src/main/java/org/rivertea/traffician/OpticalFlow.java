package org.rivertea.traffician;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import java.util.HashMap;
import java.util.Map;

public class OpticalFlow {
    private static final double
            pyr_scale = 0.5,
            poly_sigma = 1.2;
    private static final int
            levels = 3,
            winSize = 15,
            iterations = 5,
            poly_n = 5,
            flags = 0,
            stepSize = 8,
            denseRegionThreshold = 20,
            FRAMES_FOR_AVERAGE_SPEED = 10;
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
        Video.calcOpticalFlowFarneback(prevFrame, nextFrame, vectorFlow, pyr_scale, levels, winSize,
                iterations, poly_n, poly_sigma, flags);
        return vectorFlow;
    }

    public boolean isDense() {
        int denseRegionCount = 0;
        int threshold = 30;

        for (int y = 0; y < vectorFlow.rows(); y += stepSize) {
            for (int x = 0; x < vectorFlow.cols(); x += stepSize) {
                double[] flowVector = vectorFlow.get(y, x);
                double flowX = flowVector[0];
                double flowY = flowVector[1];
                // Tính độ lớn của mỗi vector flow (tốc độ)
                double magnitude = Math.sqrt(Math.pow(flowX, 2) + Math.pow(flowY, 2));

                // Kiểm tra xem denseRegionCount có lớn hơn ngưỡng hay không để xác định xem đó có phải là vùng dày đặc hay không
                if (magnitude > denseRegionThreshold) {
                    denseRegionCount++;
                }
            }
        }

        return denseRegionCount > threshold;
    }

    public void drawVectorFlow(Mat outputFrame) {
        for (int y = 0; y < vectorFlow.rows(); y += stepSize) {
            for (int x = 0; x < vectorFlow.cols(); x += stepSize) {
                double[] flowVector = vectorFlow.get(y, x);
                double flowX = flowVector[0];
                double flowY = flowVector[1];

                Point startPoint = new Point(x, y);
                Point endPoint = new Point(x + flowX, y + flowY);
                Imgproc.arrowedLine(outputFrame, startPoint, endPoint, new Scalar(0, 255, 0), 1);
            }
        }
    }

    public long calAverageSpeed() {
        // Tính tốc độ trung bình của các xe dựa vào vector flow
        long averageSpeed = 0;
        int countFrames = 0;
        int totalSpeed = 0;

        for (int y = 0; y < vectorFlow.rows(); y += stepSize) {
            for (int x = 0; x < vectorFlow.cols(); x += stepSize) {
                double[] flowVector = vectorFlow.get(y, x);
                double flowX = flowVector[0];
                double flowY = flowVector[1];

                // Tính độ lớn của mỗi vector flow (tốc độ)
                double speed = Math.sqrt(flowX * flowX + flowY * flowY);

                // Tính tốc độ trung bình của các xe dựa vào FRAMES_FOR_AVERAGE_SPEED frames gần nhất
                totalSpeed += speed;
                countFrames++;

                if (countFrames >= FRAMES_FOR_AVERAGE_SPEED) {
                    averageSpeed = totalSpeed / countFrames;
                    countFrames = 0;
                    totalSpeed = 0;
                }
            }
        }

        return averageSpeed;
    }

    public boolean inTrafficJam() {
        return isDense() && calAverageSpeed() <= 5;
    }

    public Map<String, Double> calVectorDensity() {
        Map<String, Double> directDensity = new HashMap<>();

        double upwardDensity = 0;
        double downwardDensity = 0;

        for (int y = 0; y < vectorFlow.rows(); y++) {
            for (int x = 0; x < vectorFlow.cols(); x++) {
                double[] flowVector = vectorFlow.get(y, x);
                double flowX = flowVector[0];
                double flowY = flowVector[1];

                // Tính độ lớn của mỗi vector flow (tốc độ)
                double vectorValue = Math.sqrt(flowX * flowX + flowY * flowY);

                if (vectorValue >= 10) {
                    if (flowY > 0) {
                        downwardDensity++;
                    } else if (flowY < 0) {
                        upwardDensity++;
                    }
                }
            }
        }

        // Normalize densities by dividing by the total number of pixels
        int totalPixels = vectorFlow.rows() * vectorFlow.cols();
        upwardDensity /= totalPixels;
        downwardDensity /= totalPixels;
        double totalDense = upwardDensity + downwardDensity;
        double upDense = upwardDensity / totalDense;
        double downDense = downwardDensity / totalDense;

        directDensity.put("UpDensity", upwardDensity * 100);
        directDensity.put("DownDensity", downwardDensity * 100);

        directDensity.put("UpDense", upDense * 100);
        directDensity.put("DownDense", downDense * 100);

        return directDensity;
    }
}
