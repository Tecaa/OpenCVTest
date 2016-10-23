package cl.bananaware.hwoc;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Marco on 21-04-2016.
 */
public class ImageViewer extends Activity {
    public final static boolean SHOW_PROCESS_DEBUG = false;
    public final static boolean GOOD_SIZE = false;
    public final static int I_LEVEL = 90;
    public final static boolean CHARS = false;

    private final String GROUP1 = "ABCDEFGHIJKLNPRSTUVXYZW";
    private final String GROUP2 = "BCDFGHJKLPRSTVXYZW0123456789";
    private final String GROUP3 = "0123456789";


    final int REQUEST_CODE_WRITE_EXTERNAL_PERMISSIONS= 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cameraview);
        getStorageAccessPermissions(); // Request storage read/write permissions from the user
    }
    public void onResume()
    {
        super.onResume();
        MainActivity.CameraCapturerFixer = true;

        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


    private void CodePostOpenCVLoaded() {

        DebugHWOC debugHWOC = new DebugHWOC(getResources());
        TimeProfiler.ResetCheckPoints();
        TimeProfiler.CheckPoint(0);


        PlateClient plateClient = new PlateClient(this.getBaseContext());

        Bitmap b;

        Intent intent = getIntent();
        String abs = intent.getExtras().getString("uri");
        Boolean captured = intent.getExtras().getBoolean("captured");

        try {
            if (captured) {
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();

                b = BitmapFactory.decodeFile(abs, bmOptions);
            }
            else {
                // SLOW OPERATION
                b = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), Uri.parse(abs));
            }
        } catch (Exception e) {
            e.printStackTrace();

            Toast.makeText(getBaseContext(),"Error cargando la imagen",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        //b = getGrayscale_custom_matrix(b);


        PlateRecognizer plateRecognizer = new PlateRecognizer();
        plateRecognizer.InitDebug(debugHWOC);

        Mat m_ = new Mat();
        Utils.bitmapToMat(b, m_);
        PlateRecognizer.PlateResult finalPlate = plateRecognizer.Recognize(m_);
        TimeProfiler.CheckPoint(36);
        String plate = plateRecognizer.CandidatesPlates;

        if (SHOW_PROCESS_DEBUG) {
            InitializeGallery(R.id.gallery1, plateRecognizer.firstProcessSteps);
            InitializeGallery(R.id.gallery2, plateRecognizer.secondProcessSteps);
            InitializeGallery(R.id.gallery3, plateRecognizer.finalCandidates);
        }
        //SetPlate(plate);
        SetFinalPlate(finalPlate);
        SetTime(TimeProfiler.GetTotalTime());
        Log.d("times", TimeProfiler.GetTimes(true));
        Log.d("times", TimeProfiler.GetTimes(false));
    }




    private void SetFinalPlate(PlateRecognizer.PlateResult finalPlate) {
        TextView p = (TextView) findViewById(R.id.finalPlateText);
        p.setText(finalPlate.Plate + " " + finalPlate.Confidence + "%");
    }


    private void SetTime(String s) {
        TextView p = (TextView) findViewById(R.id.totalTime);
        p.setText(s);
    }
/*
    private void SetPlate(String plate) {
        TextView p = (TextView) findViewById(R.id.plateText);
        p.setText(plate);
    }*/

    @TargetApi(23)
    private void getStorageAccessPermissions() {
        int hasWriteStoragePermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (hasWriteStoragePermission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_PERMISSIONS);
        }
    }

    private void InitializeGallery(int galleryId, final List<Mat> images) {
        //finalCandidates.addAll(processSteps); //agregamos pasos intermedios a los dibujos finales
        // Note that Gallery view is deprecated in Android 4.1---
        Gallery gallery = (Gallery) findViewById(galleryId);
        gallery.setAdapter(new ImageAdapter(this, images));
        gallery.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position,long id)
            {
                SetGalleryImage(position, images);
            }
        });
        SetGalleryImage(0, images);
    }

    private void SetGalleryImage(int position, List<Mat> images)
    {

        if (position >= images.size()) {
            Toast.makeText(getBaseContext(),"Position " + position + " not found.",
                    Toast.LENGTH_SHORT).show();
            // display the images selected
            return;
        }


        Bitmap bm = Bitmap.createBitmap(images.get(position).cols(),
                images.get(position).rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(images.get(position), bm);
        ImageView ii = (ImageView) findViewById(R.id.image1);
        ii.setImageBitmap(bm);
    }

    public class ImageAdapter extends BaseAdapter {
        private Context context;
        private int itemBackground;
        private List<Mat> images;
        public ImageAdapter(Context c, List<Mat> imgs)
        {
            context = c;
            // sets a grey background; wraps around the images
            TypedArray a =obtainStyledAttributes(R.styleable.MyGallery);
            itemBackground = a.getResourceId(R.styleable.MyGallery_android_galleryItemBackground, 0);
            images = imgs;
            a.recycle();
        }
        // returns the number of images
        public int getCount() {
            return images.size();
        }
        // returns the ID of an item
        public Object getItem(int position) {
            return position;
        }
        // returns the ID of an item
        public long getItemId(int position) {
            return position;
        }
        // returns an ImageView view
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView = new ImageView(context);

            Bitmap bm = Bitmap.createBitmap(images.get(position).cols(),
                    images.get(position).rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(images.get(position), bm);
            imageView.setImageBitmap(bm);
            imageView.setLayoutParams(new Gallery.LayoutParams(250, 250));
            imageView.setBackgroundResource(itemBackground);
            return imageView;
        }
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                    CodePostOpenCVLoaded();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


}
