package cl.bananaware.hwoc;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.util.List;

import cl.bananaware.hwoc.ApiRestClasses.Plate;
import cl.bananaware.hwoc.ImageProcessing.PlateRecognizer;
import cl.bananaware.hwoc.ImageProcessing.PlateResult;

/**
 * Created by Marco on 21-04-2016.
 */
public class ImageViewer extends Activity {
    public static boolean SHOW_PROCESS_DEBUG = true;
    public static boolean USE_API_DEBUG = false;
    public final static boolean GOOD_SIZE = false;
    public final static int I_LEVEL = 90;
    public final static boolean CHARS = false;

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
        TimeProfiler.ResetCheckPoints();
        TimeProfiler.CheckPoint(0);

        Bitmap b = getImage();
        if (b == null)
            return;

        MainActivity.plateProcessSystem.ProcessCapture(b, false);
        TimeProfiler.CheckPoint(36);

        if (SHOW_PROCESS_DEBUG) {
            InitializeGallery(R.id.gallery1, MainActivity.plateProcessSystem.plateRecognizer.firstProcessSteps);
            InitializeGallery(R.id.gallery2, MainActivity.plateProcessSystem.plateRecognizer.secondProcessSteps);
            InitializeGallery(R.id.gallery3, MainActivity.plateProcessSystem.plateRecognizer.finalCandidates);
        }

        SetFinalPlate(MainActivity.plateProcessSystem.LastPlateReaded);
        SetTime(TimeProfiler.GetTotalTime());
        Log.d("times", TimeProfiler.GetTimes(true));
        Log.d("times", TimeProfiler.GetTimes(false));
    }




    private void SetFinalPlate(PlateResult finalPlate) {
        TextView p = (TextView) findViewById(R.id.finalPlateText);
        p.setText(finalPlate.Plate + " " + finalPlate.Confidence + "%");
    }


    private void SetTime(long s) {
        TextView p = (TextView) findViewById(R.id.totalTime);
        p.setText(String.valueOf(s) + " [ms]");
    }

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

    public Bitmap getImage() {
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
            return null;
        }
        return b;
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