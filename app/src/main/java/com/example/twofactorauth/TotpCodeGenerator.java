package com.example.twofactorauth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import androidx.appcompat.app.AppCompatActivity;

public class TotpCodeGenerator extends AppCompatActivity {

    private final String className = TotpCodeGenerator.class.getSimpleName();
    public static final int DEFAULT_TIME_STEP_SECONDS = 30;
    private static Button generateCodeBtn;
    private static ImageButton resetSecret;
    private String secret;
    private TextView resultText;
    UserSettings us;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_totp_code_generator);
        resetSecret = findViewById(R.id.btn_resetSecret);
        resultText = findViewById(R.id.qrResult);
    }

    @Override
    protected void onStart() {
        super.onStart();
        us = new UserSettings(this);
        secret = us.retrievePreferencesValue("Secret");
        final ExecutorService es = Executors.newCachedThreadPool();
        ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
        ses.scheduleAtFixedRate(() -> es.submit(() -> runTOTPAlgorithm()), 0, 1, TimeUnit.SECONDS);
        resetSecret.setOnClickListener(view -> resetSecret());
    }

    private void resetSecret() {
        try {
            us.insertSecret("Secret", "");
            finish();
        } catch ( IOException | GeneralSecurityException e) {

        }
    }
    private void runTOTPAlgorithm()
    {
        try {
            String result = Long.toString(generateCurrentNumber(secret));
            // result that has 0 as first digit doesn't show that digit 0 so we will write it in the string
            if(result.length() < 6) {
                for (int i = 0; i < 6 - result.length(); i++) {
                    result = "0" + result;
                }
            }
            resultText.setText(result);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }

    }

    public long generateCurrentNumber(String base32Secret) throws GeneralSecurityException {
        long unixTimestamp = Instant.now().getEpochSecond();
        return generateNumber(base32Secret, unixTimestamp, DEFAULT_TIME_STEP_SECONDS);
    }


    public static long generateNumber(String base32Secret, long timeSeconds, int timeStepSeconds)
            throws GeneralSecurityException {

        byte[] key = base32Secret.getBytes();
        byte[] data = new byte[8];
        long value = timeSeconds / timeStepSeconds;
        for (int i = 7; value > 0; i--) {
            data[i] = (byte) (value & 0xFF);
            value >>= 8;
        }

        // encrypt the data with the key and return the SHA1 of it in hex
        SecretKeySpec signKey = new SecretKeySpec(key, "HmacSHA1");
        // if this is expensive, could put in a thread-local
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(signKey);
        byte[] hash = mac.doFinal(data);

        // take the 4 least significant bits from the encrypted string as an offset
        int offset = hash[hash.length - 1] & 0xF;

        // We're using a long because Java hasn't got unsigned int.
        long truncatedHash = 0;
        for (int i = offset; i < offset + 4; ++i) {
            truncatedHash <<= 8;
            // get the 4 bytes at the offset
            truncatedHash |= (hash[i] & 0xFF);
        }
        // cut off the top bit
        truncatedHash &= 0x7FFFFFFF;

        // the token is then the last 6 digits in the number
        truncatedHash %= 1000000;

        return truncatedHash;
    }
}

