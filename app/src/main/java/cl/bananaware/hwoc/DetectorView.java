package cl.bananaware.hwoc;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.LruCache;
import android.util.Size;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import cl.bananaware.hwoc.ImageProcessing.PlateResult;

/**
 * Created by agall on 15-11-2016.
 */

public class DetectorView extends Activity {
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAIT_LOCK = 1;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    private static final int COLUMN1_WIDTH= 220;
    private static final int COLUMN2_WIDTH= 150;
    private static final int COLUMN3_WIDTH= 250;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);

    }

   /* private static final SparseIntArray ORIENTATIONS_PREVIEW = new SparseIntArray();
    static {
        ORIENTATIONS_PREVIEW.append(Surface.ROTATION_0, 0);
        ORIENTATIONS_PREVIEW.append(Surface.ROTATION_90, 90);
        ORIENTATIONS_PREVIEW.append(Surface.ROTATION_180, 270);
        ORIENTATIONS_PREVIEW.append(Surface.ROTATION_270, 180);
    }*/

    private TableLayout table;
    private int mState;
    private static final String TAG = "AndroidCameraApi";
    private TextureView mTextureView;
    private Size mPreviewSize;
    private String mCameraId;
    private CameraDevice mCameraDevice;
    private CameraDevice.StateCallback mCameraDeviceStateCallBack = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            cameraDevice.close();
            mCameraId = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            cameraDevice.close();
            mCameraId = null;
        }
    };
    private CaptureRequest mPreviewCaptureRequest;
    private CaptureRequest.Builder mPreviewCaptureRequestBuilder;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;
    public static DetectorView instance;
    private ImageView viewDisc;

    private void createCameraPreviewSession() {
        try {
            SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            mPreviewCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewCaptureRequestBuilder.addTarget(previewSurface);
            //int rotation = getWindowManager().getDefaultDisplay().getRotation();
            //mPreviewCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    if (mCameraDevice == null) {
                        return;
                    }
                    try {
                        mPreviewCaptureRequest = mPreviewCaptureRequestBuilder.build();
                        mCaptureSession = cameraCaptureSession;
                        mCaptureSession.setRepeatingRequest(mPreviewCaptureRequest, mSessionCaptureCallback, mBackgroundHandler);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(getApplicationContext(), "Create camera session failed!", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private CameraCaptureSession mCaptureSession;
    private CameraCaptureSession.CaptureCallback mSessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        private void process(CaptureResult result) {
            switch (mState) {
                case STATE_PREVIEW:
                    //DO NOTHING
                    break;
                case STATE_WAIT_LOCK:
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (autoFocus && afState == CaptureRequest.CONTROL_AF_STATE_FOCUSED_LOCKED) {
                        /*unLockFocus();*/
                        //Toast.makeText(getApplicationContext(), "FOCUSED", Toast.LENGTH_SHORT).show();
                        Log.d("SCANNER", "FOCUSED");

                        captureStillImage();
                    } else {
                        captureStillImage();
                    }
                    break;
            }

        }

        @Override
        public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            process(result);
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
        }
    };
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            setupCamera(width, height);
            openCamera();
            configureTransform(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }
    };

    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = this;
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    private void openBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera2 background thread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void closeBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HandlerThread mBackgroundThreadScanner;
    private Handler mBackgroundHandlerScanner;

    private void openBackgroundThreadScanner() {
        mBackgroundThreadScanner = new HandlerThread("Camera2 background thread");
        mBackgroundThreadScanner.start();
        mBackgroundHandlerScanner = new Handler(mBackgroundThreadScanner.getLooper());
    }

    private void closeBackgroundThreadScanner() {
        mBackgroundThreadScanner.quitSafely();
        try {
            mBackgroundThreadScanner.join();
            mBackgroundThreadScanner = null;
            mBackgroundHandlerScanner = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Handler UIHandler = new Handler(Looper.getMainLooper());

    private static class PlateScanner implements Runnable {
        private final Image mImage;

        private PlateScanner(Image image) {
            mImage = image;
        }

        private Bitmap ReaderToBitmap(Image image) {
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            //buffer.rewind();
            byte[] data = new byte[buffer.capacity()];
            buffer.get(data);
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            return bitmap;
        }

        @Override
        public void run() {
            Log.d("SCANNER", "Scaneando");
            TimeProfiler.ResetCheckPoints();
            TimeProfiler.CheckPoint(0);
            final Bitmap img = ReaderToBitmap(mImage);

            final long millis = System.currentTimeMillis();
            final PlateResult result = MainActivity.plateProcessSystem.ProcessCapture(img);
            Log.d("SCANNER", "Scan Result" + result.Plate + " " + result.Confidence + "%"
                    + " " + String.valueOf(System.currentTimeMillis() - millis) + " [ms]");
            UIHandler.post(new Runnable() {
                @Override
                public void run() {
                    DetectorView.instance.viewDisc.setImageBitmap(img);
                    TableRow row = new TableRow(DetectorView.instance);
                    TextView tv = new TextView(DetectorView.instance);
                    tv.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
                    tv.setWidth(COLUMN1_WIDTH);
                    result.Plate = (result.Plate == "") ? "------" : result.Plate;
                    tv.setText(result.Plate);
                    row.addView(tv);

                    TextView tv2 = new TextView(DetectorView.instance);
                    tv2.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
                    tv2.setWidth(COLUMN2_WIDTH);
                    tv2.setText(result.Confidence + "%");
                    row.addView(tv2);

                    TextView tv3 = new TextView(DetectorView.instance);
                    tv3.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
                    tv3.setWidth(COLUMN3_WIDTH);
                    tv3.setText(String.valueOf(System.currentTimeMillis() - millis) + " [ms]");
                    row.addView(tv3);
                    DetectorView.instance.table.addView(row, 1);
                    if (DetectorView.instance.table.getChildCount() > 20)
                        DetectorView.instance.table.removeViewAt(20);
                }
            });


            mImage.close();
            DetectorView.instance.capturePhoto();

        }
    }

    private ImageReader mImageReader;
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader imageReader) {
            mBackgroundHandlerScanner.post(new PlateScanner(imageReader.acquireNextImage()));
            //imageReader.close();

        }
    };

    private void openCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            cameraManager.openCamera(mCameraId, mCameraDeviceStateCallBack, mBackgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    public static Boolean autoFocus = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ImageViewer.SHOW_PROCESS_DEBUG = false;
        instance = this;
        setContentView(R.layout.detector_view);
        mTextureView = (TextureView) findViewById(R.id.viewCont);
        viewDisc = (ImageView) findViewById(R.id.viewDisco);

        table = (TableLayout) findViewById(R.id.scannerTable);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
        Button button = (Button) findViewById(R.id.startCaptureButton);
        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                capturePhoto();
            }
        });
        createTableHeader();
        //mTextureView.setRotation(90.0f);
        //mTextureView.setLayoutParams(new RelativeLayout.LayoutParams(mTextureView.getHeight(), mTextureView.getWidth()));
    }


    private void createTableHeader() {
        TableRow row = new TableRow(DetectorView.instance);
        TextView tv = new TextView(DetectorView.instance);
        tv.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
        tv.setWidth(COLUMN1_WIDTH);
        tv.setText("Patente");
        //tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        row.addView(tv);

        TextView tv2 = new TextView(DetectorView.instance);
        tv2.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
        tv2.setWidth(COLUMN2_WIDTH);
        tv2.setText("Conf");
        //tv2.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        row.addView(tv2);

        TextView tv3 = new TextView(DetectorView.instance);
        tv3.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
        tv3.setWidth(COLUMN3_WIDTH);
        tv3.setText("T");
        //tv3.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        row.addView(tv3);


        table.addView(row, 0);
    }

    public void capturePhoto(){
        lockFocus();
    }
    @Override
    protected void onResume() {
        super.onResume();
        openBackgroundThread();
        openBackgroundThreadScanner();
        if (mTextureView.isAvailable()) {
            setupCamera(mTextureView.getWidth(), mTextureView.getHeight());
            openCamera();
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    protected void onPause() {
        closeCamera();
        closeBackgroundThread();
        closeBackgroundThreadScanner();
        super.onPause();
    }

    private void closeCamera() {
        if(mCaptureSession != null){
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if(mCameraDevice != null){
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if(mImageReader != null){
            mImageReader.close();
            mImageReader = null;
        }
    }

    private void setupCamera(int width, int height) {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                Size largestImageSize = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new Comparator<Size>() {
                    @Override
                    public int compare(Size lhs, Size rhs) {
                        return Long.signum(lhs.getWidth()*lhs.getHeight() - rhs.getWidth()*rhs.getHeight());
                    }
                });

                //mImageReader = ImageReader.newInstance(largestImageSize.getWidth(), largestImageSize.getHeight(), ImageFormat.JPEG, 1);
                mImageReader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, 1);
                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);

                mPreviewSize = getPreferredPreviewSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Size getPreferredPreviewSize(Size[] mapSizes, int width, int height) {
        List<Size> collectorSizes = new ArrayList<>();
        for (Size option : mapSizes) {
            if (width > height) {
                if (option.getWidth() > width && option.getHeight() > height) {
                    collectorSizes.add(option);
                } else {
                    if (option.getWidth() > width && option.getHeight() > height) {
                        collectorSizes.add(option);
                    }
                }
            }
        }
        if (collectorSizes.size() > 0) {
            return Collections.min(collectorSizes, new Comparator<Size>() {
                @Override
                public int compare(Size size, Size t1) {
                    return Long.signum(size.getWidth() * size.getHeight() - t1.getWidth() * t1.getHeight());
                }
            });
        }
        return mapSizes[0];
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("DetectorView Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    private void captureStillImage(){
        mState = STATE_PREVIEW;
        try {
            CaptureRequest.Builder captureStillBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureStillBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            captureStillBuilder.addTarget(mImageReader.getSurface());


            CameraCaptureSession.CaptureCallback captureCallBack = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    //Toast.makeText(getApplicationContext(), "IMAGEN CAPTURADA", Toast.LENGTH_SHORT).show();
                    Log.d("SCANNER", "IMAGEN CAPTURADA");

                    unLockFocus();
                }
            };
            mCaptureSession.capture(captureStillBuilder.build(), captureCallBack, null);

        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void lockFocus() {
        try{
            mState = STATE_WAIT_LOCK;
            int state;
            if(autoFocus)
                state = CaptureRequest.CONTROL_AF_TRIGGER_START;
            else{
                state = CaptureRequest.CONTROL_AF_TRIGGER_CANCEL;
            }
            mPreviewCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, state);
            mCaptureSession.capture(mPreviewCaptureRequestBuilder.build(), mSessionCaptureCallback, mBackgroundHandler);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void unLockFocus() {
        try{
            mState = STATE_PREVIEW;
            mPreviewCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL);
            mCaptureSession.capture(mPreviewCaptureRequestBuilder.build(), mSessionCaptureCallback, mBackgroundHandler);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
