package com.example.twofactorauth;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;

public class QRScanner extends AppCompatActivity {

    private final  String className = Security.class.getSimpleName();
    private SurfaceView backCamera;
    private TextView tvResult;
    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;
    private static final int REQUEST_CAMERA_PERMISSION = 201;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        backCamera = findViewById(R.id.svCamera);
        tvResult = findViewById(R.id.tvResult);
    }


    @Override
    protected void onStart() {
        super.onStart();
        barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();

        if (!barcodeDetector.isOperational()) {
            Toast.makeText(getApplicationContext(), "Barcode detector failed", Toast.LENGTH_SHORT).show();
            tvResult.setText("Barcode detector failed !");
            return;
        }
        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setAutoFocusEnabled(true)
                .build();

        tvResult.setText("Scanning...");
        startDetection();
    }

    private void startDetection() {
        backCamera.getHolder().addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                try {
                    if (ActivityCompat.checkSelfPermission(QRScanner.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraSource.start(backCamera.getHolder());
                    } else {
                        ActivityCompat.requestPermissions(QRScanner.this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                Log.i(className, "Camera is closed!");
                cameraSource.stop();
                backCamera.releasePointerCapture();
                backCamera = null;
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
                Toast.makeText(getApplicationContext(), "To prevent memory leaks barcode scanner has been stopped", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> qrCodes = detections.getDetectedItems();
                if (qrCodes.size() != 0){
                    tvResult.post(new Runnable(){
                        @Override
                        public void run() {
                            Vibrator vibrator = (Vibrator)getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)  {
                                vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
                            } else {
                                vibrator.vibrate(1000);
                            }
                            Log.i(className, " QR Code value: " + qrCodes.valueAt(0).displayValue);
                            tvResult.setText("Saving Result...");
                            // FUTURE FEATURE : need to store it somewhere safe
                            Intent intent = getIntent();
                            intent.putExtra("secret", qrCodes.valueAt(0).displayValue);
                            setResult(RESULT_OK, intent);
                            finish();
                        }
                    });
                }
            }
        });

    }

}