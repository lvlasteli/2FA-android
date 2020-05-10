package com.example.twofactorauth;

import androidx.appcompat.app.AppCompatActivity;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

public class MainActivity extends AppCompatActivity  {

    private final  String className = Security.class.getSimpleName();
    private static Button btnConfirm;
    private static Switch swtFaceRecognition;
    private static boolean savedSecurity = false;

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
        if (swtFaceRecognition.isChecked()) {
            btnConfirm.setText("Facial scan");
            Intent intent = new Intent("android.intent.action.Security");
            startActivityForResult(intent,1);
            //(new Handler()).postDelayed(this::startActivityForResult(intent, 1)), 5000);
        }
        else {
            btnConfirm.setText("Continue without security");
            Intent intent = new Intent("android.intent.action.QRScanner");
            startActivityForResult(intent, 2);

        }
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data){
        super.onActivityResult (requestCode, resultCode, data);
        if(requestCode == 2)
            if(resultCode == RESULT_OK) {
                String secret = data.getStringExtra("secret");
                Intent generateCode = new Intent("android.intent.action.TotpCodeGenerator");
                Log.i(className, "" + secret);
                generateCode.putExtra("secret", secret);
                startActivity(generateCode);
            }
    }
}


