package org.rivertea.traffician;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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
import java.util.Map;

public class MainActivity extends CameraActivity {
    private static final double PERCENTAGE_25 = 25;
    private static final double PERCENTAGE_50 = 50;
    private static final double PERCENTAGE_75 = 75;
    private static final Integer CAMERA_REQUEST_CODE = 101;
    private static final String TAG = "MainActivity";
    private CameraBridgeViewBase cameraBridgeViewBase;

    // Optical Flow variables
    private LinearLayout firstLay, secLay;
    private OpticalFlow opticalFlow;
    private CombinedChart firstCombinedChart, secondCombinedChart;
    private TextView upLaneResult, downLaneResult;
    private Button exportUpLaneBtn, exportDownLanBtn;
    int countFrames = 0;
    List<BarEntry> firstDensity = new ArrayList<>(), secondDensity = new ArrayList<>();
    List<Entry> firstVelocity = new ArrayList<>(), secondVelocity = new ArrayList<>();

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
                opticalFlow.calDensityAndVelocity();
                Map<String, Double> densityAndVelocity = opticalFlow.calDensityAndVelocity();

                // Update UI every 10 frames
                runOnUiThread(() -> {
                    if (countFrames % 5 == 0) {
                        firstDensity.add(new BarEntry(countFrames,
                                Float.parseFloat(String.valueOf(densityAndVelocity.get("First Density")))));
                        firstVelocity.add(new BarEntry(countFrames,
                                Float.parseFloat(String.valueOf(densityAndVelocity.get("First Velocity")))));
                        secondDensity.add(new BarEntry(countFrames,
                                Float.parseFloat(String.valueOf(densityAndVelocity.get("Second Density")))));
                        secondVelocity.add(new BarEntry(countFrames,
                                Float.parseFloat(String.valueOf(densityAndVelocity.get("Second Velocity")))));

                        drawChart(firstDensity, firstVelocity, firstCombinedChart);
                        drawChart(secondDensity, secondVelocity, secondCombinedChart);
                        int firstResult = isDense(densityAndVelocity.get("First Density"), densityAndVelocity.get("First Velocity"));
                        int secondResult = isDense(densityAndVelocity.get("Second Density"), densityAndVelocity.get("Second Velocity"));
                        switch (firstResult) {
                            case 0:
                                upLaneResult.setText("Light Traffic");
                                upLaneResult.setTextColor(Color.GREEN);
                                upLaneResult.setBackgroundResource(R.drawable.green_bg_color);
                                break;
                            case 25:
                                upLaneResult.setText("Moderate Traffic");
                                upLaneResult.setTextColor(Color.parseColor("#E7B10A"));
                                upLaneResult.setBackgroundResource(R.drawable.yellow_bg_color);
                                break;
                            case 75:
                                upLaneResult.setText("Heavy Traffic");
                                upLaneResult.setTextColor(Color.parseColor("#F86F03"));
                                upLaneResult.setBackgroundResource(R.drawable.orange_bg_color);
                                break;
                            default:
                                upLaneResult.setText("Gridlock");
                                upLaneResult.setTextColor(Color.RED);
                                upLaneResult.setBackgroundResource(R.drawable.red_bg_color);
                        }
                        switch (secondResult) {
                            case 0:
                                downLaneResult.setText("Light Traffic");
                                downLaneResult.setTextColor(Color.GREEN);
                                downLaneResult.setBackgroundResource(R.drawable.green_bg_color);
                                break;
                            case 25:
                                downLaneResult.setText("Moderate Traffic");
                                upLaneResult.setTextColor(Color.parseColor("#E7B10A"));
                                upLaneResult.setBackgroundResource(R.drawable.yellow_bg_color);
                                break;
                            case 75:
                                downLaneResult.setText("Heavy Traffic");
                                downLaneResult.setTextColor(Color.parseColor("#F86F03"));
                                downLaneResult.setBackgroundResource(R.drawable.orange_bg_color);
                                break;
                            default:
                                downLaneResult.setText("Gridlock");
                                downLaneResult.setTextColor(Color.RED);
                                downLaneResult.setBackgroundResource(R.drawable.red_bg_color);
                        }
                    }
                });

                // Update the previous frame with the current frame
                opticalFlow.setNextFrame(opticalFlow.getPrevFrame());

                return rgba;
            }
        });

        exportUpLaneBtn.setOnClickListener(view -> {
//            exportData();
        });

        exportDownLanBtn.setOnClickListener(view -> {
//            exportData();
        });

        if (OpenCVLoader.initDebug()) {
            cameraBridgeViewBase.enableView();
        }
    }

    private int isDense(double density, double velocity) {
        double dense = (density + velocity) / 2;
        if (dense >= PERCENTAGE_75) {
            return 75;
        } else if (dense >= PERCENTAGE_50) {
            return 50;
        } else if (dense >= PERCENTAGE_25) {
            return 25;
        } else return 0;
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
        combinedChart.setVisibleXRangeMinimum(1);
        combinedChart.setVisibleXRangeMaximum(100);

        XAxis xAxis = combinedChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(5f);

        combinedChart.getAxisRight().setEnabled(false);
    }

    private void drawChart(List<BarEntry> densityData, List<Entry> velocityData, CombinedChart combinedChart) {
        BarDataSet barDataSet = new BarDataSet(densityData, "Density");
        barDataSet.setColor(Color.BLUE);

        LineDataSet lineDataSet = new LineDataSet(velocityData, "Velocity");
        lineDataSet.setColor(Color.RED);
        lineDataSet.setLineWidth(4f);
        lineDataSet.setCircleColor(Color.RED);
        lineDataSet.setCircleRadius(2f);

        BarData barData = new BarData(barDataSet);
        LineData lineData = new LineData(lineDataSet);

        CombinedData combinedData = new CombinedData();
        combinedData.setData(barData);
        combinedData.setData(lineData);

        combinedChart.setData(combinedData);
        combinedChart.invalidate();
    }

    public void exportData(StringBuilder data) {
        ExportData exportData = new ExportData();
        exportData.setExportData(data);
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
        firstCombinedChart = findViewById(R.id.upCombinedChart);
        secondCombinedChart = findViewById(R.id.downCombinedChart);
        setupCombinedChart(firstCombinedChart);
        setupCombinedChart(secondCombinedChart);
        upLaneResult = findViewById(R.id.upLaneResult);
        downLaneResult = findViewById(R.id.downLaneResult);
        exportUpLaneBtn = findViewById(R.id.upExport);
        exportDownLanBtn = findViewById(R.id.downExport);
        firstLay = findViewById(R.id.firstLayout);
        secLay = findViewById(R.id.secondLayout);
    }
}