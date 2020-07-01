package com.example.twofactorauth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import org.bytedeco.javacv.FrameFilter;

import java.io.IOException;
import java.security.GeneralSecurityException;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private final  String className = MainActivity.class.getSimpleName();
    private static Button btnQRScann;
    private static Button btnResetSettings;
    private static Button btnConfigureSecurity;

    private static TextView selectedSecurity;


    private final String mPath = Environment.getExternalStorageDirectory()+"/facerecogOCV/";;
    UserSettings us = new UserSettings(mPath, this);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        selectedSecurity = findViewById(R.id.selected_security);
        btnConfigureSecurity = findViewById(R.id.btn_configure_security);
        btnQRScann = findViewById(R.id.btn_qr_code);
        btnResetSettings = findViewById(R.id.btn_delete_settings);
        us.createSharedPreferences();
        switch(us.getOption()) {
            case 1:
                Log.e(className, "case1");
                //if user is not using any type of security
                selectedSecurity.setVisibility(View.INVISIBLE);
                btnConfigureSecurity.setVisibility(View.VISIBLE);
                btnQRScann.setVisibility(View.VISIBLE);
                btnResetSettings.setVisibility(View.GONE);
                allowNavigation();
                break;
            case 2:
                selectedSecurity.setVisibility(View.VISIBLE);
                selectedSecurity.setText("Facial Recognition starting...");
                btnConfigureSecurity.setVisibility(View.INVISIBLE);
                btnQRScann.setVisibility(View.INVISIBLE);
                btnResetSettings.setVisibility(View.INVISIBLE);
                Intent intent = new Intent("android.intent.action.FaceIdentifier");
                intent.putExtra("mPath", mPath);
                (new Handler()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startActivityForResult(intent, 1);
                    }
                }, 2000);
                break;
            case 3:
                Log.e(className, "case3");
                selectedSecurity.setVisibility(View.VISIBLE);
                btnConfigureSecurity.setVisibility(View.INVISIBLE);
                btnQRScann.setVisibility(View.INVISIBLE);
                btnResetSettings.setVisibility(View.INVISIBLE);
                Intent intent2 = new Intent("android.intent.action.FingerprintAuthentication");
                intent2.putExtra("mPath", mPath);
                startActivityForResult(intent2, 2);
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void allowNavigation() {
        btnConfigureSecurity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                configureSecurity();
            }
        });

        btnQRScann.setOnClickListener(v -> {
            String secret = us.retrievePreferencesValue("Secret");
            if(secret.isEmpty())
                startQRScan();
            else
                generateCode();
        });

        btnResetSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                us.resetToDefaultSharedPreferences();
                us.removeSavedSettings();
            }
        });
    }

    private void configureSecurity() {
        Intent intent = new Intent("android.intent.action.ConfigureSecurity");
        intent.putExtra("mPath", mPath);
        startActivityForResult(intent, 4);
    }

    private void startQRScan() {
        Intent intent = new Intent("android.intent.action.QRScanner");
        startActivityForResult(intent, 3);
    }

    private void generateCode() {
        Intent generateCode = new Intent("android.intent.action.TotpCodeGenerator");
        startActivity(generateCode);
    }

    private void updateUserSettings(String option) {
            try {
                us.updateSharedPreferences(option, true);
                Log.e(className, "option: " + option);
            } catch (IOException | GeneralSecurityException e) {
                Log.e(className, e.getLocalizedMessage());
            }
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e(className, "" + requestCode);
        // if authentication succeeds
        if ((requestCode == 1 || requestCode == 2) && resultCode == RESULT_OK) {
            selectedSecurity.setVisibility(View.INVISIBLE);
            btnConfigureSecurity.setVisibility(View.VISIBLE);
            btnQRScann.setVisibility(View.VISIBLE);
            btnResetSettings.setVisibility(View.VISIBLE);
            allowNavigation();
        }
        else if (requestCode == 3 && resultCode == RESULT_OK) {
            generateCode();
        }
        else if(requestCode == 4 && resultCode == RESULT_OK) {
            String selected_security = data.getStringExtra("security");
            Toast.makeText(getApplicationContext(), "Security updated: " + selected_security, Toast.LENGTH_SHORT).show();
            updateUserSettings(selected_security);
            btnResetSettings.setVisibility(View.VISIBLE);
        }
    }
}


