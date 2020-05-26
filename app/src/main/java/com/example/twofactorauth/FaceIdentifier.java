package com.example.twofactorauth;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FaceIdentifier extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{

    private final  String className = FaceDetector2.class.getSimpleName();
    private TextView txtbResult;
    private JavaCameraView cameraView;
    private String result = "Scanning...";
    private boolean savedFaces = false;
    private String mPath;
    private boolean facesH;
    private boolean facesDNN;
    private Mat mRgba;
    private Mat mGray;
    File cascadeFile;
    CascadeClassifier faceDetector;
    //PersonRecognizer personRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_identifier);

        Intent intent = getIntent();
        mPath = intent.getStringExtra("mPath");

        txtbResult = findViewById(R.id.txtResult);
        txtbResult.setText(result);
        cameraView = (JavaCameraView)findViewById(R.id.java_camera_view);
        cameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);

        if(!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this,baseCallback);
        } else {
            try {
                baseCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        cameraView.setCvCameraViewListener(this);


    }

    public void checkForSavedFaces() {
        int counterH = 0;
        int counterDNN = 0;
        for(int i=0; i < 10; i++) {
            String fileNameH = mPath+"UserH-"+i+".jpg";
            String fileNameDNN = mPath+"UserDNN-"+i+".jpg";
            File fileH = new File(fileNameH);
            File fileDNN = new File(fileNameDNN);
            if(fileH.exists())
                counterH += 1;
            if(fileDNN.exists())
                counterDNN += 1;
        }
        if(counterDNN == 10) {
            facesH = true;
            Toast.makeText(getApplicationContext(), "Faces from DNN located", Toast.LENGTH_LONG).show();
        }
        if(counterH == 10) {
            facesDNN = true;
            Toast.makeText(getApplicationContext(), "Faces from H located", Toast.LENGTH_LONG).show();
        }
        savedFaces = true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkForSavedFaces();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();
        return mRgba;
    }

    private BaseLoaderCallback baseCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) throws IOException {
            Log.i(className, "OpenCV loaded successfully");
            //personRecognizer = new PersonRecognizer(mPath);
            //personRecognizer.loadFaceImages();

            switch (status) {
                case BaseLoaderCallback.SUCCESS: {
                    try {
                        InputStream inputStream = getResources().openRawResource(R.raw.haarcascade_frontalface_alt2);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        cascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt2.xml");
                        FileOutputStream fileOutputStream = new FileOutputStream(cascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;

                        while((bytesRead = inputStream.read(buffer)) != -1) {
                            fileOutputStream.write(buffer, 0, bytesRead);
                        }
                        inputStream.close();
                        fileOutputStream.close();

                        faceDetector = new CascadeClassifier(cascadeFile.getAbsolutePath());

                        if(faceDetector.empty())
                            faceDetector = null;
                        else
                            cascadeDir.delete();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(className, "Failed to load cascade. Exception thrown: " + e);
                    }

                    cameraView.enableView();
                    cameraView.setMaxFrameSize(1280, 720);
                }
                break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat();
        mGray = new Mat();
    }
    @Override
    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraView != null)
            cameraView.disableView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraView != null)
            cameraView.disableView();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = getIntent();
        setResult(RESULT_CANCELED, intent);
        finish();
    }
}