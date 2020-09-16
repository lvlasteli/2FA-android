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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;
import java.security.GeneralSecurityException;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class QRScanner extends AppCompatActivity {

    private final  String className = QRScanner.class.getSimpleName();
    private SurfaceView backCamera;
    private TextView textResult;
    private ImageView imageViewIcon;
    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;
    private static final int REQUEST_CAMERA_PERMISSION = 201;
    private UserSettings us;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        backCamera = findViewById(R.id.svCamera);
        textResult = findViewById(R.id.tvResult);
        imageViewIcon = findViewById(R.id.iVIcon);
        context =  this;
        us = new UserSettings(this);
    }


    @Override
    protected void onStart() {
        super.onStart();
        barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();

        if (!barcodeDetector.isOperational()) {
            Toast.makeText(getApplicationContext(), "Barcode detector failed", Toast.LENGTH_SHORT).show();
            textResult.setText("Barcode detector failed !");
            return;
        }
        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setAutoFocusEnabled(true)
                .build();

        textResult.setText("Scanning");
        startDetection();
    }

    private void startDetection() {
        backCamera = findViewById(R.id.svCamera);
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
                    imageViewIcon.setImageResource(R.drawable.scan_done);
                    textResult.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
                    textResult.setText("Saving");
                    textResult.post(() -> {
                     Vibrator vibrator = (Vibrator)getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)  {
                            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                        } else {
                            vibrator.vibrate(500);
                        }
                        try {
                            Intent intent = getIntent();
                            us.insertSecret("Secret", qrCodes.valueAt(0).displayValue);
                            setResult(RESULT_OK, intent);
                            finish();
                        } catch (GeneralSecurityException | IOException e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
        });
    }
}