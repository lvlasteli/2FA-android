package com.example.twofactorauth;

import androidx.appcompat.app.AppCompatActivity;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

public class MainActivity extends AppCompatActivity  {

    private static Button btnConfirm;
    private static Switch swtFaceRecognition;
    private static TextView txtResult;
    private static boolean savedSecurity = false;
    private static final int startQR = 1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Default code
        btnConfirm = findViewById(R.id.btnConfirm);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(!savedSecurity) {
            btnConfirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    configureSecurity();
                }
            });
        }
        else {
            //we have owners face saved so we need to open camera and screen users face
        }
    }


    private void configureSecurity() {
        swtFaceRecognition = findViewById(R.id.swtFaceRecognition);
        txtResult = findViewById(R.id.txtResult);
        if (swtFaceRecognition.isChecked()) {
            txtResult.setText("facial recognition");
            Intent intent = new Intent("android.intent.action.Security");
            startActivityForResult(intent,2);
            //(new Handler()).postDelayed(this::startActivityForResult(intent, 1)), 5000);
        }
        else {
            txtResult.setText("QR Scan");
            Intent intent = new Intent("android.intent.action.QRScanner");
            startActivityForResult(intent, startQR);

        }
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data){
        super.onActivityResult (requestCode, resultCode, data);
        if(requestCode == 1)
            if(resultCode == RESULT_OK){
//                txtResult = data.getStringExtra("QRCode");
//                txtResult.setText("" + result);
            }
    }
}


