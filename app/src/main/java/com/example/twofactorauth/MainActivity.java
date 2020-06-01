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
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity  implements AdapterView.OnItemSelectedListener {

    private final  String className = MainActivity.class.getSimpleName();
    private static Button btnQRScann;
    private static Button btnFaceRecognition;
    private static Spinner spnFacialAlgorithm;

    private static  Button btnRecognize;


    private final String mPath = Environment.getExternalStorageDirectory()+"/facerecogOCV/";;
    static String selectedItem;


    //private static Button getBtnFaceRecognition2;
    private static boolean savedSecurity = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Default code
        btnQRScann = findViewById(R.id.btn_qr_code);
        btnFaceRecognition = findViewById(R.id.btn_facial_recognition);
        spnFacialAlgorithm = findViewById(R.id.facial_detection_options);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.detection_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnFacialAlgorithm.setAdapter(adapter);
        spnFacialAlgorithm.setOnItemSelectedListener(this);

        //temporary
        btnRecognize = findViewById(R.id.btn_recognize);

        //check if we have saved set of faces
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(!savedSecurity) {
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

            btnRecognize.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent("android.intent.action.FaceIdentifier");
                    intent.putExtra("mPath", mPath);
                    startActivityForResult(intent, 3);
                }
            });
        }
        else {
            //we have owners face saved so we need to open camera and screen users face

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

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data){
        super.onActivityResult (requestCode, resultCode, data);
        if(requestCode == 1 || requestCode == 3) {
            if(resultCode == RESULT_OK) {
                startQRScan();
            } else  {
                Log.e(className, "CANT PROCEED");
            }
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


