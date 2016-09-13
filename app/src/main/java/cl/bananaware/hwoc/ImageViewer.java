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
    public final static boolean SHOW_PROCESS_DEBUG = true;
    public final static boolean GOOD_SIZE = false;
    public final static int I_LEVEL = 90;
    public final static boolean EXPERIMENTAL_EQUALITATION = false;
    public final static boolean CHARS = false;
    List<Mat> finalCandidates;
    List<Mat> firstProcessSteps;
    List<Mat> secondProcessSteps;


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
        CleanAll();

        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private void CleanAll() {
        finalCandidates = new ArrayList<Mat>();
        firstProcessSteps = new ArrayList<Mat>();
        secondProcessSteps = new ArrayList<Mat>();
    }


    private void CodePostOpenCVLoaded() {

        DebugHWOC debugHWOC = new DebugHWOC(getResources());
        TimeProfiler.ResetCheckPoints();
        TimeProfiler.CheckPoint(0);



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

        PlateRecognizer plateRecognizer = new PlateRecognizer();
        plateRecognizer.InitDebug(debugHWOC);

        Mat m_ = new Mat();
        Utils.bitmapToMat(b, m_);
        String finalPlate = plateRecognizer.Recognize(m_);
        String plate = plateRecognizer.CandidatesPlates;

        TimeProfiler.CheckPoint(6);
        if (SHOW_PROCESS_DEBUG) {
            InitializeGallery(R.id.gallery1, firstProcessSteps);
            InitializeGallery(R.id.gallery2, secondProcessSteps);
            InitializeGallery(R.id.gallery3, finalCandidates);
        }
        SetPlate(plate);
        SetFinalPlate(finalPlate);
        //Log.d("times", TimeProfiler.GetTotalTime());
        SetTime(TimeProfiler.GetTotalTime());
        Log.d("times", TimeProfiler.GetTimes(true, 10));
        Log.d("times", TimeProfiler.GetTimes(false));
    }

    private boolean correctPlate(String lastPlate) {
        if (lastPlate.length() == 6
                && GROUP1.contains(String.valueOf(lastPlate.charAt(0)))
                && GROUP1.contains(String.valueOf(lastPlate.charAt(1)))
                && GROUP2.contains(String.valueOf(lastPlate.charAt(2)))
                && GROUP2.contains(String.valueOf(lastPlate.charAt(3)))
                && GROUP3.contains(String.valueOf(lastPlate.charAt(4)))
                && GROUP3.contains(String.valueOf(lastPlate.charAt(5)))
                )
            return true;
        else
            return false;
    }

    private void SetFinalPlate(String finalPlate) {
        TextView p = (TextView) findViewById(R.id.finalPlateText);
        p.setText(finalPlate);
    }

    private String process(String recognizedText, int n) {
        if (hasSixChars(recognizedText)) {
            return recognizedText.replace(" ", "").substring(n*2, n*2+2);
        }
        else if (hasThreeGroups(recognizedText))
        {
            int index;
            switch (n)
            {
                case 0:
                    return recognizedText.substring(0,2);
                case 1:
                    index = recognizedText.indexOf(" ")+1;
                    return recognizedText.substring(index,Math.min(index+2, recognizedText.length()));
                case 2:
                default:
                    index = recognizedText.length();
                    return recognizedText.substring(Math.max(index-2,0),index);
            }

        }
        else if (hasTwoGroups(recognizedText))
        {


            int index;
            switch (n)
            {
                case 0:
                    return recognizedText.substring(0,Math.min(2, recognizedText.length()));
                case 1:
                    String[] groups = recognizedText.replace("  ", " ").replace("   ", " ").split(" ");
                    if (groups[0].length() > groups[1].length()) {
                        index = recognizedText.indexOf(" ");
                        return recognizedText.substring(Math.max(index-2,0),index);
                    }
                    else{
                        index = recognizedText.indexOf(" ")+1;
                        return recognizedText.substring(index,Math.min(index+2, recognizedText.length()));
                    }

                case 2:
                default:
                    index = recognizedText.length();
                    return recognizedText.substring(Math.max(index-2,0),index);
            }

        }
        else
        {
            int index;
            switch (n)
            {
                case 0:
                    return recognizedText.substring(0,Math.min(2, recognizedText.length()));
                case 1:

                    index = recognizedText.length() /2-1;
                    return recognizedText.substring(Math.max(index, 0),Math.min(index+2, recognizedText.length()));

                case 2:
                default:
                    index = recognizedText.length();
                    return recognizedText.substring(Math.max(index-2,0),index);
            }
        }

    }

    private boolean hasTwoGroups(String recognizedText) {
        return recognizedText.replace("   ", " ").replace("  ", " ").split(" ").length == 2;
    }
    private boolean hasThreeGroups(String recognizedText) {
        return recognizedText.replace("   ", " ").replace("  ", " ").split(" ").length == 3;
    }

    private boolean hasSixChars(String recognizedText) {
        return recognizedText.replace(" ", "").length() == 6;
    }

    private void SetTime(String s) {
        TextView p = (TextView) findViewById(R.id.totalTime);
        p.setText(s);
    }

    private void SetPlate(String plate) {
        TextView p = (TextView) findViewById(R.id.plateText);
        p.setText(plate);
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

    protected void DrawImages(){
        LinearLayout ll = (LinearLayout)findViewById(R.id.linearLayout);
        for(int i=0;i<finalCandidates.size();++i)
        {
            Bitmap bm = Bitmap.createBitmap(finalCandidates.get(i).cols(), finalCandidates.get(i).rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(finalCandidates.get(i), bm);
            ImageView ii= new ImageView(this);
            //ii.setBackgroundResource(R.drawable.ic_action_search);
            ii.setImageBitmap(bm);
            ll.addView(ii);
        }
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

    private class DoAll extends AsyncTask<Void, Void, Void> {
        // Do the long-running work in here
        protected Void doInBackground(Void ... params) {
            CodePostOpenCVLoaded();
            return null;
        }

        // This is called each time you call publishProgress()
        protected void onProgressUpdate() {

        }

        // This is called when doInBackground() is finished
        protected void onPostExecute() {
            Log.d("test","finished!!!");
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
