package org.rivertea.traffician;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MainActivity extends CameraActivity {
    private static final Integer CAMERA_REQUEST_CODE = 101;
    private static final String TAG = "MainActivity";
    private CameraBridgeViewBase cameraBridgeViewBase;

    // Optical Flow variables
    private OpticalFlow opticalFlow;
    TextView isDense, upDensityRate, downDensityRate, speedAverage, isTrafficJams;
    double upDensity = 0.0, downDensity = 0.0;
    int countFrames = 0;

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
                runOnUiThread(() -> {
                    isDense.setText("Is dense region: " + opticalFlow.isDense());
                });

                // Compute density of upward and downward flow vectors
                Map<String, Double> vectorDirectDensity = opticalFlow.calVectorDensity();

                runOnUiThread(() -> {
                    if (countFrames <= 30) {
                        countFrames++;
                    } else {
                        upDensity = vectorDirectDensity.get("UpDensity");
                        downDensity = vectorDirectDensity.get("DownDensity");
                        countFrames = 0;
                        upDensityRate.setText("Up Density Rate: " + upDensity + "%");
                        downDensityRate.setText("Down Density Rate: " + downDensity + "%");
                    }
                });

                // Tính tốc độ trung bình của các xe dựa vào vector flowVector
                long averageSpeed = opticalFlow.calAverageSpeed();
                // Hiển thị tốc độ trung bình của các xe lên TextView
                runOnUiThread(() -> speedAverage.setText("Average Speed: " + averageSpeed));

                // Đánh giá tình trạng kẹt xe
                runOnUiThread(() -> {
                    if (opticalFlow.inTrafficJam()) {
                        isTrafficJams.setVisibility(View.VISIBLE);
                    } else isTrafficJams.setVisibility(View.GONE);
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
        isDense = findViewById(R.id.txtIsDense);
        upDensityRate = findViewById(R.id.txtUpDensity);
        downDensityRate = findViewById(R.id.txtDownDensity);
        speedAverage = findViewById(R.id.txtSpeed);
        isTrafficJams = findViewById(R.id.txtInJams);
    }
}