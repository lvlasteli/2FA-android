package com.example.twofactorauth;

import android.util.Log;

import org.bytedeco.javacpp.Pointer;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_face.FaceRecognizer;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;

import org.bytedeco.opencv.opencv_core.*;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.IntBuffer;



public class PersonRecognizer {

    private final  String className = PersonRecognizer.class.getSimpleName();
    FaceRecognizer faceRecognizer;
    String picturesPath;

    PersonRecognizer(String picturesPath) {
        this.picturesPath = picturesPath;
    }

    public void loadFaces() {
        File pictureLocation = new File(picturesPath);
        FilenameFilter jpgFilter = (dir, name) -> name.toLowerCase().endsWith(".jpg") || name.endsWith(".pgm") || name.endsWith(".png");
        File[] savedFaceImages = pictureLocation.listFiles(jpgFilter);

        MatVector matVectorImages = new MatVector(savedFaceImages.length);
        Mat labels = new Mat(savedFaceImages.length, 1, CV_32SC1);
        IntBuffer labelsBuf = labels.createBuffer();

        int counter = 0;
        for (File savedFace : savedFaceImages) {
            String picturesAbsPath = savedFace.getAbsolutePath();
            Log.w(className, "File : " + picturesAbsPath);
            String strLabel = savedFace.getName().split("-")[0];
            int intLabel = 69; // we have only one user
            Log.i(className, "For " + strLabel + " we are setting index of " + intLabel);

            //Convert to grayscale which is better for identification
            Mat grayImage = imread(picturesAbsPath, IMREAD_GRAYSCALE);
            matVectorImages.put(counter, grayImage);

            labelsBuf.put(counter, intLabel);

            counter ++;
        }
        Log.i(className, "Finished loading Faces from Memory. Starting Recognizer.");
        // lets train our recognizer
        faceRecognizer = LBPHFaceRecognizer.create(2, 8, 8, 8, 200);
        faceRecognizer.train(matVectorImages, labels);
    }

    public boolean predictUser(org.opencv.core.Mat mat) {
        int[] label = new int[1];
        double[] confidence = new double[1];
        //conversion from org.opencv.core.Mat to org.bytedeco.opencv.opencv_core.Mat
       Mat mat2 = new Mat((Pointer) null) {
           { address = mat.getNativeObjAddr(); }
       };
        faceRecognizer.predict(mat2, label, confidence);
        Log.i(className, "Probability for number: " +label[0] + " is " + confidence[0]);
        return confidence[0] < 85;
    }


}
