package com.example.twofactorauth;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
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


public class FaceIdentifier extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private CameraBridgeViewBase mOpenCvCameraView;
    private final  String className = FaceIdentifier.class.getSimpleName();
    private ImageButton switchCamera;
    private boolean frontFacingCamera;
    File cascadeFile;
    CascadeClassifier faceDetector;
    private TextView txtResult;
    private String mPath;
    private boolean userFound;
    private Mat mRgba, mGray;

    //class for Facial Identification
    PersonRecognizer personRecognizer;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        mPath = intent.getStringExtra("mPath");

        setContentView(R.layout.activity_face_identifier);
        txtResult = findViewById(R.id.txtResult);
        switchCamera = findViewById(R.id.btn_camera);
        txtResult.setText("Scanning");
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.java_camera_view);
        mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
        frontFacingCamera = true;
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        switchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchCameras();
            }
        });
    }

    public void switchCameras()
    {
        mOpenCvCameraView.disableView();
        if(frontFacingCamera) {
            mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
            frontFacingCamera = false;
        } else {
            mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
            frontFacingCamera = true;
        }
        mOpenCvCameraView.enableView();
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

        //Compare saved faces
        if(facesArray.length > 0) {
            Mat mat = new Mat();
            mat = mGray.submat(facesArray[0]);
            Bitmap mBitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mat, mBitmap);
            userFound = personRecognizer.predictUser(mat);
            if(userFound) {
                Log.e(className, "User found");
                Intent intent = getIntent();
                setResult(RESULT_OK, intent);
                finish();
            }

        }

        for(Rect rect: facesArray) {
            Imgproc.rectangle(mRgba, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255,255, 0), 3);
        }

        return mRgba;
    }

    // needed when we extend or class with CameraActivity
    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        personRecognizer = new PersonRecognizer(mPath);
        try {
            personRecognizer.loadFaces();
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
            Log.e(className, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.i(className, "OpenCV library found inside package. Using it!");
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

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
