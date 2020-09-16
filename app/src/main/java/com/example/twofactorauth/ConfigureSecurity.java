package com.example.twofactorauth;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

public class ConfigureSecurity<pivate> extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private final  String className = ConfigureSecurity.class.getSimpleName();
    static String selectedItem;
    private static Spinner spnFacialAlgorithm;
    private static Button btnFaceDetection;
    private static  Button btnFingerPrintScan;
    private static Button btnResetSettings;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private String mPath;
    UserSettings us;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        mPath = intent.getStringExtra("mPath");

        setContentView(R.layout.activity_configure_security);
        btnFaceDetection = findViewById(R.id.btn_facial_detection);
        spnFacialAlgorithm = findViewById(R.id.facial_detection_options);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.detection_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnFacialAlgorithm.setAdapter(adapter);
        spnFacialAlgorithm.setOnItemSelectedListener(this);
        btnFingerPrintScan = findViewById(R.id.btn_fingerprint_scan);
        btnResetSettings = findViewById(R.id.btn_reset_setting);
        Log.e(className,"permiss"+ checkPermission() );
        if (!checkPermission()) {
            ActivityCompat.requestPermissions(ConfigureSecurity.this, new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE }, PERMISSION_REQUEST_CODE);
            ActivityCompat.requestPermissions(ConfigureSecurity.this, new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE }, PERMISSION_REQUEST_CODE);
        } else {
            us = new UserSettings(mPath, this);
            allowButtonClicks();
        }
    }

    private void allowButtonClicks() {
        //allow clicks if we can use storage
        btnFaceDetection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setUpFacialRecognition();
            }
        });

        btnFingerPrintScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setFingerprintAuthentication();
            }
        });
        btnResetSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                us.resetToDefaultSharedPreferences();
                us.removeSavedSettings();
            }
        });
    }

    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    public void setFingerprintAuthentication() {
        Intent intent = new Intent("android.intent.action.FingerprintAuthentication");
        intent.putExtra("mPath", mPath);
        startActivityForResult(intent, 2);
    }

    private void setUpFacialRecognition() {
        Log.i(className, "Selected method: " + selectedItem);
        if(selectedItem.equals("Neural Network (Slow)")) {
            Intent intent = new Intent("android.intent.action.FaceDetector");
            intent.putExtra("name", selectedItem);
            intent.putExtra("mPath", mPath);
            (new Handler()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivityForResult(intent, 1);
                }
            }, 1000);
        } else {
            Intent intent = new Intent("android.intent.action.FaceDetector2");
            intent.putExtra("name", selectedItem);
            intent.putExtra("mPath", mPath);
            (new Handler()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivityForResult(intent, 1);
                }
            }, 1000);
        }
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        super.onActivityResult (requestCode, resultCode, data);
        if(requestCode == 1 && resultCode == RESULT_OK) {
            Intent intent = getIntent();
            setResult(RESULT_OK, intent);
            intent.putExtra("security", "Facial Recognition");
            finish();
        }
        if(requestCode == 2 && resultCode == RESULT_OK) {
            Intent intent = getIntent();
            setResult(RESULT_OK, intent);
            intent.putExtra("security", "Fingerprint Authentication");
            finish();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        selectedItem = parent.getItemAtPosition(position).toString();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e(className, "Permission Granted, Now you can use local drive .");
                    allowButtonClicks();
                } else {
                    Log.e(className, "Permission Denied, You cannot use local drive .");
                }
                break;
        }
    }
}
