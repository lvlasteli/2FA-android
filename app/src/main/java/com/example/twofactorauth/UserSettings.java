package com.example.twofactorauth;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;


public class UserSettings {
    private final  String className = UserSettings.class.getSimpleName();
    private String mPath;
    private Context context;
    private final String sharedPName = "UserSettings";
    private final ArrayList<String> userSettingsOptions = new ArrayList<String>(
            Arrays.asList("No Security",  "Facial Recognition", "Fingerprint Authentication")
    );

    public UserSettings(Context context) {
        this.context = context;
    }

    public UserSettings(String mPath) {
        this.mPath = mPath;
    }

    public UserSettings(String mPath, Context context) {
        this.mPath = mPath;
        this.context = context;
    }

    public File[] getFiles() {
        try {
            File pictureLocation = new File(mPath);
            FilenameFilter jpgFilter = (dir, name) -> name.toLowerCase().endsWith(".jpg") || name.endsWith(".pgm") || name.endsWith(".png");
            return pictureLocation.listFiles(jpgFilter);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean checkForSavedFaces() {
        if(getFiles().length >= 10)
            return true;
        else
            return  false;
    }

    public void removeSavedSettings() {
        if(getFiles() != null)
            for(File file : getFiles()) {
                Log.i(className, file.getPath() + " deleted.");
                file.delete();
            }
    }

    public void saveImages(String imageName, Mat m, int countImages) {
        Bitmap bmp = Bitmap.createBitmap(m.width(), m.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(m, bmp);
        int WIDTH= 128;
        int HEIGHT= 128;
        bmp= Bitmap.createScaledBitmap(bmp, WIDTH, HEIGHT, false);
        try {
            Log.w(className, "Saving Mat in a file: " + mPath + imageName + countImages + ".jpg");
            FileOutputStream f = new FileOutputStream(mPath + imageName + countImages+".jpg",true);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, f);
            f.close();
        } catch (Exception e) {
            Log.e(className,"Problem " + e.getMessage());
            e.printStackTrace();

        }
    }

    public void createSharedPreferences() {
        File f = new File(context.getApplicationContext().getApplicationInfo().dataDir + "/shared_prefs/"
                + sharedPName + ".xml");
        if (f.exists()) {
            Log.i(className, "SharedPreferences exist");
        }
        else {
            Log.e(className, "Shared Preferences doesn't exist. Setup default preferences");
            try {
                SharedPreferences firstSharedPref = getEncryptedSharedPreferences(true);
            } catch (GeneralSecurityException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void resetToDefaultSharedPreferences() {
        try {
            SharedPreferences firstSharedPref = getEncryptedSharedPreferences(true);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    public SharedPreferences getEncryptedSharedPreferences(boolean firstTime) throws GeneralSecurityException, IOException {
        String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
        EncryptedSharedPreferences sharedPreferences = (EncryptedSharedPreferences) EncryptedSharedPreferences
                .create(sharedPName,
                        masterKeyAlias,
                        context,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                );
        if(firstTime) {
            SharedPreferences.Editor sharedPrefsEditor = sharedPreferences.edit();
            for(String option : userSettingsOptions) {
                if(option.equals("No Security"))
                    sharedPrefsEditor.putString(option, String.valueOf(true));
                else
                    sharedPrefsEditor.putString(option, String.valueOf(false));
            }
            sharedPrefsEditor.putString("Secret", "");
            sharedPrefsEditor.apply();
        }
        return  sharedPreferences;

    }

    public void updateSharedPreferences(String optionTxt, boolean value) throws GeneralSecurityException, IOException {
        SharedPreferences.Editor sharedPrefsEditor = getEncryptedSharedPreferences(false).edit();
        for(String option : userSettingsOptions) {
            if(option.equals(optionTxt))
                sharedPrefsEditor.putString(option, String.valueOf(true));
            else
                sharedPrefsEditor.putString(option, String.valueOf(false));
        }
        sharedPrefsEditor.apply();
    }

    public String retrievePreferencesValue(String option) {
        try {
            return getEncryptedSharedPreferences(false).getString(option, "");
        } catch ( IOException | GeneralSecurityException ex) {
            Log.e(className, ex.getLocalizedMessage());
            return null;
        }
    }

    public int getOption() {
        int number = 1;
        for(String option : userSettingsOptions) {
            if(Boolean.parseBoolean(retrievePreferencesValue(option))) {
                return number;
            }
            number += 1;
        }
        return 0;
    }

    public void insertSecret(String secret, String value) throws GeneralSecurityException, IOException {
        getEncryptedSharedPreferences(false).edit()
                .putString(secret, value)
                .apply();
    }
}
