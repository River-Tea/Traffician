package org.rivertea.traffician;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends CameraActivity {
    private static final int REQUEST_CODE_PICK_VIDEO = 100;
    private static final Integer CAMERA_REQUEST_CODE = 101;
    private static final String TAG = "MainActivity";
    private CameraBridgeViewBase cameraBridgeViewBase;

    // Optical Flow variables
    private OpticalFlow opticalFlow;
    private Button startButton;
    private LineChart lineChart;
    double upDensity = 0.0, downDensity = 0.0;
    int countFrames = 0;
    List<Entry> velocity = new ArrayList<>();
    List<Entry> density = new ArrayList<>();

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
//                runOnUiThread(() -> {
//                    isDense.setText("Is dense region: " + opticalFlow.isDense());
//                });

                // Compute density of upward and downward flow vectors
                Map<String, Double> vectorDirectDensity = opticalFlow.calVectorDensity();
                long averageSpeed = opticalFlow.calAverageSpeed();

                runOnUiThread(() -> {
                    int frameMark = 0;
                    if (countFrames <= 30) {
                        countFrames++;
                    } else {
                        frameMark += countFrames;
                        upDensity = vectorDirectDensity.get("UpDense");
                        downDensity = vectorDirectDensity.get("DownDense");
                        velocity.add(new Entry(frameMark, averageSpeed));
                        density.add(new Entry(frameMark, (float) downDensity));
                        countFrames = 0;
//                        upDensityRate.setText("Up Density Rate: " + upDensity + "%");
//                        downDensityRate.setText("Down Density Rate: " + downDensity + "%");
                        drawChart(velocity, density);
                    }
                });


//                // Tính tốc độ trung bình của các xe dựa vào vector flowVector
//                long averageSpeed = opticalFlow.calAverageSpeed();
//                // Hiển thị tốc độ trung bình của các xe lên TextView
//                runOnUiThread(() -> speedAverage.setText("Average Speed: " + averageSpeed));
//
//                // Đánh giá tình trạng kẹt xe
//                runOnUiThread(() -> {
//                    if (opticalFlow.inTrafficJam()) {
//                        isTrafficJams.setVisibility(View.VISIBLE);
//                    } else isTrafficJams.setVisibility(View.GONE);
//                });

                // Update the previous frame with the current frame
                opticalFlow.setNextFrame(opticalFlow.getPrevFrame());

                return rgba;
            }
        });

        if (OpenCVLoader.initDebug()) {
            cameraBridgeViewBase.enableView();
        }
    }

    private void drawChart(List<Entry> dataSet1, List<Entry> dataSet2) {
        LineDataSet lineDataSet1 = new LineDataSet(dataSet1, "Density");
        LineDataSet lineDataSet2 = new LineDataSet(dataSet2, "Velocity");

        lineDataSet1.setColor(getResources().getColor(R.color.colorAccent)); // Set line color
        lineDataSet2.setColor(getResources().getColor(R.color.colorPrimary)); // Set line color

        LineData lineData = new LineData(lineDataSet1, lineDataSet2);

        // Customize X-axis labels if needed
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new MyXAxisFormatter());

        lineChart.setData(lineData);
        lineChart.invalidate();
    }

    private static class MyXAxisFormatter extends ValueFormatter {
        @Override
        public String getAxisLabel(float value, AxisBase axis) {
            return String.valueOf((int) value); // Customize X-axis labels here
        }
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
        startButton = findViewById(R.id.startButton);
        lineChart = findViewById(R.id.lineChart);
    }
}