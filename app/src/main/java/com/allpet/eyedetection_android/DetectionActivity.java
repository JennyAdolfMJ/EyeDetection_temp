package com.allpet.eyedetection_android;

import android.Manifest;
import android.content.Context;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.opencv.core.Core.mean;
import static org.opencv.core.CvType.CV_16U;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class DetectionActivity extends AppCompatActivity  implements CvCameraViewListener2 {

    private CameraBridgeViewBase mOpenCvCameraView;
    private File mCascadeFile;
    private CascadeClassifier mEyeCascade;
    private Mat mRgba;
    private Mat mGray;
    private int absoluteEyeSize = 200;
    private int mThreshold;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.haarcascade_eye);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "haarcascade_eye.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mEyeCascade = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mEyeCascade.empty())
                        {
                            mEyeCascade = null;
                        }

                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection);

        int hasWriteStoragePermission = ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.CAMERA);
        if (hasWriteStoragePermission == getPackageManager().PERMISSION_GRANTED) {
            mOpenCvCameraView = findViewById(R.id.cv_camera);
            mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
            mOpenCvCameraView.setCvCameraViewListener(this);

            mThreshold = getIntent().getIntExtra("threshold", 200);

        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba(); //RGBA
        mGray = inputFrame.gray(); //单通道灰度图

        if (absoluteEyeSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * 0.2f) > 0) {
                absoluteEyeSize = Math.round(height * 0.2f);
            }
        }

        //检测并显示
        MatOfRect eyes = new MatOfRect();
        if (mEyeCascade != null) {
            mEyeCascade.detectMultiScale(mGray, eyes, 1.1, 10, 2, new Size(absoluteEyeSize, absoluteEyeSize), new Size());
        }
        Rect[] facesArray = eyes.toArray();
        if (facesArray.length > 0){
            for (int i = 0; i < facesArray.length; i++) {    //用框标记
                Mat imageSobel = new Mat();
                Imgproc.Laplacian(mGray.submat(facesArray[i]), imageSobel, CV_16U);

                double meanValue = mean(imageSobel).val[0];

                if (meanValue > mThreshold)
                    Imgproc.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 0, 255, 255), 3);
                else
                    Imgproc.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0, 255), 3);
            }
        }
        return mRgba;
    }
}
