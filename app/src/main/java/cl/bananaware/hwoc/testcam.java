package cl.bananaware.hwoc;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.*;
//import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.w3c.dom.Text;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.ListIterator;

public class testcam extends Activity implements CustomCameraBridgeViewBase.CustomCvCameraViewListener2, OnTouchListener {

    private static final String TAG = "OCVSample::Activity";
    private Mat mRgba, mRgbaF, mRgbaT;
    private Mat mGray;
    private CustomCameraBridgeViewBase mOpenCvCameraView;


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(testcam.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public testcam() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }
    PlateRecognizer plateRecognizer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.test);



        mOpenCvCameraView = (CustomCameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);
        //mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_native_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        //CustomJavaCameraView cameraView = (CustomJavaCameraView) findViewById(R.id.tutorial1_activity_java_surface_view);

        mOpenCvCameraView.setPreviewSize(1920, 1080);
        mOpenCvCameraView.setPictureSize(4160, 3120);
        //mOpenCvCameraView.setSize(1920, 1080);
        mOpenCvCameraView.setCvCameraViewListener(this);





        //mOpenCvCameraView.setMaxFrameSize(50000,50000); //Si funciona
        /*Parameters params = mOpenCvCameraView.s .getParameters();
        params.setPictureSize(resolution.width,resolution.height);
        mCamera.setParameters(params);
*/

        plateRecognizer = new PlateRecognizer();
        plateRecognizer.InitDebug(new DebugHWOC(getResources()));
        context = getApplicationContext();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    Context context;

    @Override
    public void onResume()
    {
        super.onResume();

        /*if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");*/
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        /*} else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }*/
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }


    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mRgbaF = new Mat(height, width, CvType.CV_8UC4);
        mRgbaT = new Mat(width, width, CvType.CV_8UC4);

  //      setResolution();

    }
/*
    private void setResolution() {
        CameraView cameraView = (CameraView) findViewById(R.id.tutorial1_activity_java_surface_view);findViewById(R.id.tutorial1_activity_java_surface_view);

        List<android.hardware.Camera.Size> ress = cameraView.getResolutionList();
        cameraView.setResolution(ress.get(0).width  ,ress.get(0).height);

    }*/

    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(CustomCameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        mRgba = inputFrame.rgba();

        // Rotate mRgba 90 degrees
        Core.transpose(mRgba, mRgbaT);
        Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0,0, 0);
        Core.flip(mRgbaF, mRgba, 1 );


        Log.d("frame", mRgba.width() + " " + mRgba.height());

        if (!IsBusy /*touched*/) {

            testcam.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Bitmap bm = Bitmap.createBitmap(mRgba.cols(),
                            mRgba.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(mRgba, bm);
                    ImageView ii = (ImageView) findViewById(R.id.imagenx);
                    ii.setImageBitmap(bm);
                }
            });


            if (!IsBusy) {
                IsBusy = true;
                TimeProfiler.ResetCheckPoints();
                TimeProfiler.CheckPoint(0);
                new LongOperation().execute("");
            }
            touched = false;
        }
        return mRgba;
    }

    boolean touched = false;
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        touched = true;
        return touched;
    }
    boolean IsBusy = false;
    private int counter = 0;
    private class LongOperation extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            final PlateRecognizer.PlateResult plate = plateRecognizer.Recognize(mRgba.clone());

            Log.d("plate", TimeProfiler.GetTimes(true, 10));
            Log.d("plate", ++counter + " Plate:" + plate.Plate + " " + plate.Confidence + "% " + TimeProfiler.GetTimes(false, 1) + " TOTAL: " + TimeProfiler.GetTotalTime());
            testcam.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (plate.Confidence >= 60) {
                        TextView textView = (TextView) findViewById(R.id.textView);
                        textView.setText(++counter + " Plate:" + plate.Plate + " " + plate.Confidence + "%\n" + textView.getText());
                    }
                }});
            IsBusy = false;
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {

            // might want to change "executed" for the returned string passed
            // into onPostExecute() but that is upto you
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }
}

