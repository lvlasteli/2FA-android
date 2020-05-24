package com.example.twofactorauth;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;

import java.io.FileOutputStream;
import java.io.IOException;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


public class FaceDetector extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private final  String className = FaceDetector.class.getSimpleName();
    private CameraBridgeViewBase cameraBridgeViewBase; //our front facing camera
    private BaseLoaderCallback baseLoaderCallback;
    private Toolbar tlbAlhorithamName;
    private String detectorName;
    static int countImages;
    static final int MAX_IMAGES = 10;
    //network model
    Net faceDetector;

    static  {
        OpenCVLoader.initDebug();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        detectorName = intent.getStringExtra("name");

        setContentView(R.layout.activity_face_detection);
        String protoPath;
        String caffeWeights;

        if (ActivityCompat.checkSelfPermission(FaceDetector.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            // external memory is actually phones hard drive memory
            // we must have deep neural networks downloaded and pasted in dnns folder below
             protoPath = Environment.getExternalStorageDirectory() + "/dnns/deploy.prototxt";
            caffeWeights = Environment.getExternalStorageDirectory() + "/dnns/res10_300x300_ssd_iter_140000.caffemodel";
            Log.i(className, "External Storage finished. proto path: " + protoPath + "caffeModel path: "  + caffeWeights +  " .Creating our Network");
            faceDetector = Dnn.readNetFromCaffe(protoPath, caffeWeights);

        } else {
            ActivityCompat.requestPermissions(FaceDetector.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        // make our Java camera support OpenCV functions
        tlbAlhorithamName = (Toolbar) findViewById(R.id.toolbar_algorithm);
        tlbAlhorithamName.setTitle(detectorName);

        cameraBridgeViewBase = (JavaCameraView) findViewById(R.id.java_camera_view);
        cameraBridgeViewBase.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);

        baseLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) throws IOException {
                super.onManagerConnected(status);
                switch (status) {
                    case BaseLoaderCallback.SUCCESS:
                        Log.i(className, "OpenCv loaded successfully");
                        cameraBridgeViewBase.enableView();
                        break;
                    default:
                        super.onManagerConnected(status);
                        break;
                }
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
         countImages = 0;

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat frame = inputFrame.rgba();
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);
//        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGB2BGR);
//        Mat imageBlob = Dnn.blobFromImage(frame, 1.0, new Size(300, 300),
//                new Scalar(104.0, 117.0, 123.0), true, false, CvType.CV_32F);
        Mat imageBlob = Dnn.blobFromImage(frame, 1.0, new Size(300, 300),
                new Scalar(0, 0, 0),false,false);
        faceDetector.setInput(imageBlob);
        Mat detections = faceDetector.forward(); //feed forward the input to the network

        // borders of image x,y dimensions
        int cols = frame.cols();
        int rows = frame.rows();
        // our confidence thresholds
        final double THRESHOLD = 0.5;

        //important
        detections = detections.reshape(1, (int) detections.total() / 7);
//        Log.i(className, "Detection rows: " + detections.rows());

        //iterate over the rows of our output matrix
        for (int i = 0; i < detections.rows(); ++i) {
            // get confidence value from second position (first one is class value)
            double confidence  = detections.get(i, 2)[0];
           // Log.i(className, "Confidence: "+ confidence + " Threshold: " + THRESHOLD);
            if(confidence >= THRESHOLD) {
                // the net outputs left,top,right,bottom as percentage % values which we use to multiply with corresponding
                // x (cols), y (rows) dimensions of the image to get the exact integer pixel values
                int left = (int) (detections.get(i, 3)[0] * cols);
                int top = (int) (detections.get(i, 4)[0] * rows);
                int right = (int) (detections.get(i, 5)[0] * cols);
                int bottom = (int) (detections.get(i, 6)[0] * rows);

                // we must ensure not to draw rectangle outside of borders
                if(left < 0)
                    left = 0;
                if(top < 0)
                    top = 0;
                if(right < 0)
                    right = 0;
                if(bottom < 0)
                    bottom = 0;

                int xMaxLimit = frame.size(1);
                int yMaxLimit = frame.size(0);

                if(left >= xMaxLimit)
                    left = xMaxLimit - 2;
                if(right >= xMaxLimit)
                    right = xMaxLimit - 2;
                if(top >= yMaxLimit)
                    top = yMaxLimit - 2;
                if(bottom >= yMaxLimit)
                    bottom = yMaxLimit - 2;

                Rect rect = new Rect(new Point(left, top),  new Point(right, bottom));
                Mat m = frame.submat(rect);
                if (countImages < MAX_IMAGES) {
                    add(m, countImages);
                    countImages++;
                } else {
                    Intent intent = getIntent();
                    setResult(RESULT_OK, intent);
                    finish();
                }
                // create yellow rectangle with these points
                Imgproc.rectangle(frame, new Point(left, top), new Point(right, bottom), new Scalar(255, 255, 0), 2);
            }
        }
        return frame;
    }

    public void add(Mat m, int countImages) {
        Bitmap bmp = Bitmap.createBitmap(m.width(), m.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(m, bmp);
        String mPath = Environment.getExternalStorageDirectory()+"/facerecogOCV/";
        int WIDTH= 128;
        int HEIGHT= 128;
        bmp= Bitmap.createScaledBitmap(bmp, WIDTH, HEIGHT, false);
        try {
            Log.w(className, "Saving Mat in a file: " + mPath+"UserDNN-"+countImages+".jpg");
            FileOutputStream f = new FileOutputStream(mPath+"UserDNN-"+countImages+".jpg",true);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, f);
            f.close();
        } catch (Exception e) {
            Log.e(className,e.getCause()+" "+e.getMessage());
            e.printStackTrace();

        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }
    @Override
    public void onCameraViewStopped() {

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!OpenCVLoader.initDebug()) {
            Toast.makeText(getApplicationContext(), "There's a problem", Toast.LENGTH_SHORT).show();
        } else {
            try {
                baseLoaderCallback.onManagerConnected(baseLoaderCallback.SUCCESS);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(cameraBridgeViewBase != null) {
            cameraBridgeViewBase.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(cameraBridgeViewBase != null) {
            cameraBridgeViewBase.disableView();
            Log.i(className, "Destroying...");
        }
    }
}
