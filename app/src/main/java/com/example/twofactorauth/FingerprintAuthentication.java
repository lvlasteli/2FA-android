package com.example.twofactorauth;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;


public class FingerprintAuthentication extends AppCompatActivity {

    private final  String className = FingerprintAuthentication.class.getSimpleName();
    private FingerprintManager fingerprintManager;
    private KeyguardManager keyguardManager;

    private TextView txtHelp;

    private KeyStore keyStore;
    private Cipher cipher;
    private String KEY_NAME="AndroidFingerprintKey";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fingerprint_scan);
        txtHelp = (TextView) findViewById(R.id.txt_help);
        // Users device can use fingerprint scanner, android os must be bigger than Marshmallow
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            fingerprintManager = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);
            keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            if(!fingerprintManager.isHardwareDetected()) {
                txtHelp.setText("Fingerprint Scanner not detected in Device");
            } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                txtHelp.setText("Permission not granted to use Fingerprint Scanner");
                // The Fingerprint permission is not marked as dangerous and thus you don't have to ask for access to it. It is granted automatically if you declare the permission in your manifest.
            } else if (!keyguardManager.isKeyguardSecure()) {
                txtHelp.setText("Must have Security set up in the phone Settings.");
            } else if (! fingerprintManager.hasEnrolledFingerprints()) {
                txtHelp.setText("Add at least one fingerprints in the phone Settings!");
            } else {
                txtHelp.setText("Place your finger on scanner to proceed");
                generateKey();
                if (cipherInit()) {
                    FingerprintManager.CryptoObject cryptoObject = new FingerprintManager.CryptoObject(cipher);
                    runAuthentication(cryptoObject);

                }
            }
        } else {
            Intent intent = getIntent();
            setResult(RESULT_CANCELED, intent);
            finish();
        }
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(className, "resultCode");
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void generateKey() {
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

            keyStore.load(null);
            keyGenerator.init(new KeyGenParameterSpec.Builder(KEY_NAME, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            keyGenerator.generateKey();

        } catch (KeyStoreException | IOException | CertificateException
                | NoSuchAlgorithmException | InvalidAlgorithmParameterException
                | NoSuchProviderException e) {
            e.printStackTrace();
        }

    }

    @TargetApi(Build.VERSION_CODES.M)
    public boolean cipherInit() {
        try {
            cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get Cipher", e);
        }

        try {
            keyStore.load(null);
            SecretKey key = (SecretKey) keyStore.getKey(KEY_NAME,null);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return true;

        } catch (KeyPermanentlyInvalidatedException e) {
            return false;
        } catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void runAuthentication(FingerprintManager.CryptoObject cryptoObject) {
        CancellationSignal cancellationSignal = new CancellationSignal();
        fingerprintManager.authenticate(null, cancellationSignal, 0, new FingerprintManager.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Log.i(className, "Authentication error " + errorCode + " " + errString);
                response("Authentication error. " + errorCode, false);
            }

            @Override
            public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                super.onAuthenticationHelp(helpCode, helpString);
                Log.i(className, "Authentication help message thrown " + helpCode + " " + helpString);
                response("Fingerprint not properly scanned " + helpString, false);
            }

            @Override
            public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Log.i(className, "Authentication succeeded");
                response("Authentication succeeded", true);
                (new Handler()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = getIntent();
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                }, 1500);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Log.i(className, "Authentication failed");
                response("Authentication failed",  false);
            }
        }, null);

    }

    private void response(String message, boolean success) {
        TextView txtHelp = (TextView)  findViewById(R.id.txt_help);
        ImageView imageView = (ImageView) findViewById(R.id.image_fingerprint);

        txtHelp.setText(message);
        if(success) {
            txtHelp.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
            imageView.setImageResource(R.drawable.scan_done);
            imageView.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimary));
        } else {
            txtHelp.setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
            imageView.setImageResource(R.drawable.scan_failed);
            imageView.setColorFilter(ContextCompat.getColor(this, R.color.colorAccent));
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

}
