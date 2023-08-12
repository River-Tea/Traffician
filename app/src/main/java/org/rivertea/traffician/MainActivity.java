package org.rivertea.traffician;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.SurfaceView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends CameraActivity {
    private static final double DENSITY_THRESHOLD = 100;
    private static final double VELOCITY_THRESHOLD = 100;
    private static final Integer CAMERA_REQUEST_CODE = 101;
    private static final String TAG = "MainActivity";
    private CameraBridgeViewBase cameraBridgeViewBase;

    // Optical Flow variables
    private OpticalFlow opticalFlow;
    private CombinedChart upCombinedChart, downCombinedChart;
    private TextView upLaneResult, downLaneResult;
    int countFrames = 0;
    List<BarEntry> upDensity = new ArrayList<>(), downDensity = new ArrayList<>();
    List<Entry> upVelocity = new ArrayList<>(), downVelocity = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setControl();
        getPermission();
        setEvent();
    }

    private void setEvent() {
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
            @Override
            public void onCameraViewStarted(int width, int height) {
                opticalFlow = new OpticalFlow();
                opticalFlow.setPrevFrame(new Mat(height, width, CvType.CV_8UC1));
                opticalFlow.setNextFrame(new Mat(height, width, CvType.CV_8UC1));
                opticalFlow.setVectorFlow(new Mat(height, width, CvType.CV_32FC2));
            }

            @Override
            public void onCameraViewStopped() {
                opticalFlow.getPrevFrame().release();
                opticalFlow.getNextFrame().release();
                opticalFlow.getVectorFlow().release();
            }

            @SuppressLint("SetTextI18n")
            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                Mat gray = inputFrame.gray();
                Mat rgba = inputFrame.rgba();
                opticalFlow.setPrevFrame(gray);
                if (opticalFlow.getPrevFrame().empty()) {
                    opticalFlow.setNextFrame(opticalFlow.getPrevFrame());
                }

                // Calculate optical flowVector using Farneback method
                Mat flowVector = opticalFlow.calOpticalFlowVector();
                opticalFlow.setVectorFlow(flowVector);

                // Draw optical flowVector vectors on the frame
                opticalFlow.drawVectorFlow(rgba);

                countFrames++;
                List<Double> densityAndVelocity = opticalFlow.calDensityAndVelocity(countFrames);

                // Update UI every 20 frames
                runOnUiThread(() -> {
                    if (countFrames % 20 == 0) {
                        upDensity.add(new BarEntry(countFrames,
                                Float.parseFloat(String.valueOf(densityAndVelocity.get(0)))));
                        downDensity.add(new BarEntry(countFrames,
                                Float.parseFloat(String.valueOf(densityAndVelocity.get(1)))));
                        upVelocity.add(new Entry(countFrames,
                                Float.parseFloat(String.valueOf(densityAndVelocity.get(2)))));
                        downVelocity.add(new Entry(countFrames,
                                Float.parseFloat(String.valueOf(densityAndVelocity.get(3)))));
                        drawChart(upDensity, upVelocity, upCombinedChart);
                        drawChart(downDensity, downVelocity, downCombinedChart);
                        if (isDense(densityAndVelocity.get(0), densityAndVelocity.get(2))) {
                            upLaneResult.setText("Up lane is dense");
                        } else {
                            upLaneResult.setText("Up lane is not dense");
                        }

                        if (isDense(densityAndVelocity.get(1), densityAndVelocity.get(3))) {
                            downLaneResult.setText("Down lane is dense");
                        } else {
                            downLaneResult.setText("Down lane is not dense");
                        }
                    }
                });

                // Update the previous frame with the current frame
                opticalFlow.setNextFrame(opticalFlow.getPrevFrame());

                return rgba;
            }
        });

        if (OpenCVLoader.initDebug()) {
            cameraBridgeViewBase.enableView();
        }
    }

    private boolean isDense(double density, double velocity) {
        if (density >= 0.5) {
            return !(velocity >= 0.5);
        } else {
            return velocity >= 0.5;
        }
    }

    private void setupCombinedChart(CombinedChart combinedChart) {
        combinedChart.getDescription().setEnabled(false);
        combinedChart.setBackgroundColor(Color.WHITE);
        combinedChart.setDrawGridBackground(false);
        combinedChart.setDrawBarShadow(false);
        combinedChart.setHighlightFullBarEnabled(false);

        // Enable horizontal scrolling
        combinedChart.setScaleEnabled(true);
        combinedChart.setDragEnabled(true);

        // Set the visible range of data
        combinedChart.setVisibleXRangeMinimum(100);
        combinedChart.setVisibleXRangeMaximum(400);

        XAxis xAxis = combinedChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(20f);

        combinedChart.getAxisRight().setEnabled(false);
    }

    private void drawChart(List<BarEntry> densityData, List<Entry> velocityData, CombinedChart combinedChart) {
        BarDataSet barDataSet = new BarDataSet(densityData, "Density");
        barDataSet.setColor(Color.BLUE);

        LineDataSet lineDataSet = new LineDataSet(velocityData, "Velocity");
        lineDataSet.setColor(Color.RED);
        lineDataSet.setLineWidth(10f);
        lineDataSet.setCircleColor(Color.RED);
        lineDataSet.setCircleRadius(4f);

        BarData barData = new BarData(barDataSet);
        LineData lineData = new LineData(lineDataSet);

        CombinedData combinedData = new CombinedData();
        combinedData.setData(barData);
        combinedData.setData(lineData);

        combinedChart.setData(combinedData);
        combinedChart.invalidate();
    }

    void getPermission() {
        // Check for camera permission at runtime (for Android 6.0 and higher)
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_REQUEST_CODE);
        } else {
            // Permission already granted, start camera
            cameraBridgeViewBase.enableView();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission granted, start camera
                cameraBridgeViewBase.enableView();
            } else {
                // Camera permission denied, handle this case (e.g., show an explanation to the user)
                getPermission();
            }
        }
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(cameraBridgeViewBase);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraBridgeViewBase.enableView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraBridgeViewBase.disableView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraBridgeViewBase.disableView();
    }

    private void setControl() {
        cameraBridgeViewBase = findViewById(R.id.cameraView);
        upCombinedChart = findViewById(R.id.upCombinedChart);
        downCombinedChart = findViewById(R.id.downCombinedChart);
        setupCombinedChart(upCombinedChart);
        setupCombinedChart(downCombinedChart);
        upLaneResult = findViewById(R.id.upLaneResult);
        downLaneResult = findViewById(R.id.downLaneResult);
    }
}