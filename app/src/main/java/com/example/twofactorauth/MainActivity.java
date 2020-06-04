package com.example.twofactorauth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity  implements AdapterView.OnItemSelectedListener {

    private final  String className = MainActivity.class.getSimpleName();
    private static Button btnQRScann;
    private static Button btnFaceRecognition;
    private static Spinner spnFacialAlgorithm;
    private static Button btnGenerateCode;
    private static Button btnDeleteSecret;
    private static Button btnConfigureSecurity;

    private static  Button btnFingerPrintScan;


    private final String mPath = Environment.getExternalStorageDirectory()+"/facerecogOCV/";;
    static String selectedItem;


    //private static Button getBtnFaceRecognition2;
    private static boolean savedSecurity = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkForSavedFaces();
        if(!savedSecurity) {
            setContentView(R.layout.activity_set_security);
            btnQRScann = findViewById(R.id.btn_qr_code);
            btnFaceRecognition = findViewById(R.id.btn_facial_recognition);
            spnFacialAlgorithm = findViewById(R.id.facial_detection_options);
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.detection_options, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spnFacialAlgorithm.setAdapter(adapter);
            spnFacialAlgorithm.setOnItemSelectedListener(this);
        } else {
            Intent intent = new Intent("android.intent.action.FaceIdentifier");
            intent.putExtra("mPath", mPath);
            Toast.makeText(getApplicationContext(), "Facial recognition is set up!", Toast.LENGTH_SHORT).show();
            startActivityForResult(intent, 3);
        }
    }

    private void checkForSavedFaces() {
        File pictureLocation = new File(mPath);
        FilenameFilter jpgFilter = (dir, name) -> name.toLowerCase().endsWith(".jpg") || name.endsWith(".pgm") || name.endsWith(".png");
        File[] savedFaceImages = pictureLocation.listFiles(jpgFilter);
        if(savedFaceImages.length >= 10)
            savedSecurity = true;
    }

    @Override
    protected void onStart() {
        Toast.makeText(getApplicationContext(), "Start", Toast.LENGTH_SHORT).show();
        super.onStart();
        if (!savedSecurity) {
            btnFaceRecognition.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setUpFacialRecognition();
                }
            });

            btnQRScann.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startQRScan();
                }
            });

            btnFingerPrintScan.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
        } else {
            btnGenerateCode.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            });

            btnDeleteSecret.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                }
            });

            btnConfigureSecurity.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
        }
    }


    private void startQRScan() {
        Intent intent = new Intent("android.intent.action.QRScanner");
        startActivityForResult(intent, 2);
    }

    private void setUpFacialRecognition() {
        Log.e(className, "Selected method: " + selectedItem);
        if(selectedItem.equals("Neural Network (Fast)")) {
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

    public void createDefaultActivity() {
        setContentView(R.layout.activity_after_authentication);
        btnGenerateCode = findViewById(R.id.btn_totp_algorithm);
        btnDeleteSecret = findViewById(R.id.btn_delete_secret);
        btnConfigureSecurity = findViewById(R.id.btn_configure_security);
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data){
        super.onActivityResult (requestCode, resultCode, data);
        if(requestCode == 1 || requestCode == 3) {
            createDefaultActivity();
        }
        if(requestCode == 2)
            if(resultCode == RESULT_OK) {
                String secret = data.getStringExtra("secret");
                Intent generateCode = new Intent("android.intent.action.TotpCodeGenerator");
                Log.i(className, "" + secret);
                generateCode.putExtra("secret", secret);
                startActivity(generateCode);
            }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        selectedItem = parent.getItemAtPosition(position).toString();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}


