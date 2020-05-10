package com.example.twofactorauth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.security.GeneralSecurityException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import androidx.appcompat.app.AppCompatActivity;

public class TotpCodeGenerator extends AppCompatActivity {

    public static final int DEFAULT_TIME_STEP_SECONDS = 30;
    private static Button generateCodeBtn;
    private String secret;
    private TextView resultText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_totp_code_generator);
        generateCodeBtn = findViewById(R.id.generateTOTP);
        resultText = findViewById(R.id.qrResult);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = getIntent();
        secret = intent.getStringExtra("secret");
        resultText.setText("" + secret);

        try {
            long result = generateCurrentNumber(secret);
            resultText.setText("" + result);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        setResult(RESULT_OK,intent);

        generateCodeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onTOTPClicked();
            }
        });
    }

    private void onTOTPClicked()
    {
        try {
            long result = generateCurrentNumber(secret);
            resultText.setText("" + result);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }

    }

    public static long generateCurrentNumber(String base32Secret) throws GeneralSecurityException {
        return generateNumber(base32Secret, System.currentTimeMillis(), DEFAULT_TIME_STEP_SECONDS);
    }


    public static long generateNumber(String base32Secret, long timeMillis, int timeStepSeconds)
            throws GeneralSecurityException {

        byte[] key = base32Secret.getBytes();

        byte[] data = new byte[8];
        long value = timeMillis / 1000 / timeStepSeconds;
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

