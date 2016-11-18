package cl.bananaware.hwoc;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    public static TessBaseAPI baseApi;
    public static PlateProcessSystem plateProcessSystem;
    public static LocationController locationController;
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

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Button button = (Button) findViewById(R.id.detectorNoAutoFocusButton);
        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                detectorNoAutoFocusButtonClick();
            }
        });
        button = (Button) findViewById(R.id.detectorViewButton);
        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                detectorAutoFocusButtonClick();
            }
        });
        locationController = new LocationController(this.getApplicationContext());
        InitOCT();
        InitPPS();

    }

    private void InitPPS() {
        plateProcessSystem = new PlateProcessSystem(this.getBaseContext());
        plateProcessSystem.InitDebug(new DebugHWOC(getResources()));
    }

    private void InitOCT() {
        Log.d("oct", "init OCT");
        String baseDir = getExternalFilesDir(Environment.MEDIA_MOUNTED).toString();
        String tessdataDir = baseDir + File.separator + "tessdata";
        CopyAssets(tessdataDir);
        baseApi = new TessBaseAPI();
        baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_CHAR);
        final String lang = "eng";
        final String DATA_PATH = baseDir + File.separator;

        baseApi.init(DATA_PATH, lang);

    }

    private void CopyAssets(String storageDirectory) {
        AssetManager assetManager = getAssets();
        String[] files = null;
        try {
            files = assetManager.list("tessdata");
        } catch (IOException e) {
            Log.e("tag", e.getMessage());
        }

        boolean success = false;
        File folder = new File(storageDirectory);
        if (!folder.exists()) {
            success = folder.mkdir();
        }
        for(String filename : files) {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = assetManager.open("tessdata/"+filename);   // if files resides inside the "Files" directory itself
                out = new FileOutputStream(storageDirectory +File.separator + "eng.traineddata");
                copyFile(in, out);
                in.close();
                in = null;
                out.flush();
                out.close();
                out = null;
            } catch(Exception e) {
                Log.e("tag", e.getMessage());
            }
        }
    }


    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
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

    private Camera.PreviewCallback  previewCallback= new Camera.PreviewCallback()
    {
        @Override
        public void onPreviewFrame(byte[] data,Camera cam)
        {
            Camera.Size previewSize = cam.getParameters().getPreviewSize();
            YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21,previewSize.width,previewSize.height, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0,0,previewSize.width,previewSize.height),80,baos);
            byte[] jdata = baos.toByteArray();
            Bitmap bitmap = BitmapFactory.decodeByteArray(jdata,0,jdata.length);
        }
    };
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
    public void camera2Click(View v) {
        Intent i = new Intent(this, CameraContinously.class);
        startActivity(i);
    }
    public void detectorAutoFocusButtonClick() {
        DetectorView.autoFocus = true;
        Intent i = new Intent(this, DetectorView.class);
        startActivity(i);
    }

    public void detectorNoAutoFocusButtonClick() {
        DetectorView.autoFocus = false;
        Intent i = new Intent(this, DetectorView.class);
        startActivity(i);
    }


    public void testcamClick(View v) {

        Intent i = new Intent(this, testcam.class);
        startActivity(i);
    }

    public void batchClick(View v) {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/DCIM/Imágenes con Patentes2";
        FolderBatch folderBatch = new FolderBatch(this.getBaseContext());
        folderBatch.Process(path);
    }

    private void directoryPicker() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.parse(Environment.getExternalStorageDirectory().getPath());
        intent.setDataAndType(uri, "resource/folder");
        if (intent.resolveActivityInfo(getPackageManager(), 0) != null)
        {
            startActivity(intent);
        }
        else
        {
            // if you reach this place, it means there is no any file
            // explorer app installed on your device
            ;
        }
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