package cl.bananaware.hwoc;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

import java.io.File;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements CvCameraViewListener2
{
    public static final boolean UNFORCE_IMAGE = true ;
    private static final String TAG = "OCVSample::Activity";

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }
    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully :)");
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
        setContentView(R.layout.show_camera);


    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        return inputFrame.rgba();
    }

    @Override
    public void onPause()
    {
        super.onPause();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        verifyStoragePermissions(this);
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }


    }

    public static boolean CameraCapturerFixer = true;
    @Override
    public void onStart()
    {
        super.onStart();

    }
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int SELECT_FILE = 2;
    Uri outputFileUri ;
    String imgPath;
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

// Store image in dcim
        File file = new File(Environment.getExternalStorageDirectory() + "/DCIM/", "image" + new Date().getTime() + ".png");
        outputFileUri = Uri.fromFile(file);
        this.imgPath = file.getAbsolutePath();



        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE );
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Intent intent = new Intent(this, ImageViewer.class);
            boolean captured;
            switch (requestCode) {
                case REQUEST_IMAGE_CAPTURE:
                    captured = true;
                    break;
                case SELECT_FILE:
                default:
                    captured = false;
                    if (data != null) {
                        imgPath = data.getData().toString();
                    }
                    break;
            }
            intent.putExtra("uri", imgPath);
            intent.putExtra("captured", captured);
            startActivity(intent);
        }
    }


    public void cameraClick(View v) {
        if (CameraCapturerFixer) {
            if (UNFORCE_IMAGE) {
                dispatchTakePictureIntent();
            } else {
                Intent i = new Intent(this, ImageViewer.class);
                startActivity(i);
            }
        }
        CameraCapturerFixer = false;
    }

    public void galleryClick(View v) {

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);//
        startActivityForResult(Intent.createChooser(intent, "Select File"),SELECT_FILE);
    }

    public void onDestroy() {
        super.onDestroy();
    }
}