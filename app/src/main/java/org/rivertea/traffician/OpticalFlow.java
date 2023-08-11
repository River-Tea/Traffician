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
            stepSize = 10,
            denseRegionThreshold = 10,
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

                if (magnitude > denseRegionThreshold) {
                    denseRegionCount++;
                }
            }
        }

        // Kiểm tra xem denseRegionCount có lớn hơn ngưỡng hay không để xác định xem đó có phải là vùng dày đặc hay không
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
                double magnitude = Math.sqrt(flowX * flowX + flowY * flowY);

                // Tính tốc độ trung bình của các xe dựa vào FRAMES_FOR_AVERAGE_SPEED frames gần nhất
                totalSpeed += magnitude;
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

    private void codeTest(Mat[] vector) {
        // Giả sử vectorFlow là mảng chứa vector flow của Farneback optical flow
        // Vector flow có cấu trúc: [dx1, dy1, dx2, dy2, ...]

        int totalVectors = vector.length / 2; // Số lượng vector (2 giá trị cho mỗi vector)
        int countUpward = 0; // Số lượng vector hướng lên
        int countDownward = 0; // Số lượng vector hướng xuống
        double sumMagnitudeUpward = 0.0; // Tổng độ lớn vector hướng lên
        double sumMagnitudeDownward = 0.0; // Tổng độ lớn vector hướng xuống

        for (int i = 0; i < vector.length; i += 2) {
            double dx = vector[i];
            double dy = vector[i + 1];
            double magnitude = Math.sqrt(dx * dx + dy * dy);

            if (dy < 0) { // Vector hướng lên
                countUpward++;
                sumMagnitudeUpward += magnitude;
            } else if (dy > 0) { // Vector hướng xuống
                countDownward++;
                sumMagnitudeDownward += magnitude;
            }
        }

        // Tính mật độ vector hướng lên và hướng xuống
        double densityUpward = (double) countUpward / totalVectors;
        double densityDownward = (double) countDownward / totalVectors;

        // Tính độ lớn trung bình của vector hướng lên và hướng xuống
        double avgMagnitudeUpward = sumMagnitudeUpward / countUpward;
        double avgMagnitudeDownward = sumMagnitudeDownward / countDownward;

        // In kết quả
        System.out.println("Mật độ vector hướng lên: " + densityUpward);
        System.out.println("Mật độ vector hướng xuống: " + densityDownward);
        System.out.println("Độ lớn trung bình vector hướng lên: " + avgMagnitudeUpward);
        System.out.println("Độ lớn trung bình vector hướng xuống: " + avgMagnitudeDownward);

    }
}
