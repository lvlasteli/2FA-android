package com.example.twofactorauth;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;


public class FaceDetector2 extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private CameraBridgeViewBase mOpenCvCameraView;
    private final  String className = FaceDetector2.class.getSimpleName();
    File cascadeFile;
    CascadeClassifier faceDetector;
    private TextView txtView;
    private String detectorName;
    private String mPath;
    static final long MAX_IMAGES = 10;
    int countImages;
    private Mat mRgba, mGray;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        detectorName = intent.getStringExtra("name");
        mPath = intent.getStringExtra("mPath");

        setContentView(R.layout.activity_face_detection);
        txtView = findViewById(R.id.txt_detectorName);
        txtView.setText(detectorName);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.java_camera_view);
        mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        countImages = 0;
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        MatOfRect faces = new MatOfRect();

        //scaleFactor  - Parameter specifying how much the image size is reduced at each image scale.
        //minNeighbors – Parameter specifying how many neighbors each candidate rectangle should have to retain it.
        // This parameter will affect the quality of the detected faces. Higher value results in less detections but with higher quality. 3~6 is a good value for it.
        faceDetector.detectMultiScale(mGray, faces, 1.05, 6, 2,
                new Size(200,200), new Size());

        Rect[] facesArray = faces.toArray();

        for(Rect rect: facesArray) {
            Imgproc.rectangle(mRgba, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255,255, 0), 3);
        }

        if(facesArray.length == 1) {
            Mat m;
            Rect rect = facesArray[0];
            m = mRgba.submat(rect);
            if (countImages < MAX_IMAGES) {
                add(m, countImages);
                countImages++;
            } else {
                Intent intent = getIntent();
                setResult(RESULT_OK, intent);
                finish();
            }
        }

        return mRgba;
    }

    public void add(Mat m, int countImages) {
        Bitmap bmp = Bitmap.createBitmap(m.width(), m.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(m, bmp);
        int WIDTH= 128;
        int HEIGHT= 128;
        bmp= Bitmap.createScaledBitmap(bmp, WIDTH, HEIGHT, false);
        try {
            Log.w(className, "Saving Mat in a file: " + mPath+"UserH-"+countImages+".jpg");
            FileOutputStream f = new FileOutputStream(mPath+"UserH-"+countImages+".jpg",true);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, f);
            f.close();

        } catch (Exception e) {
            Log.e(className,e.getCause()+" "+e.getMessage());
            e.printStackTrace();

        }
    }

    // needed when we extend or class with CameraActivity
    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
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

        }

    }
    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(className, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d(className, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(className, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };
}
