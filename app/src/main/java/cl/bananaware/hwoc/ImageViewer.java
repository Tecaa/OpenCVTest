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
    public final static boolean TRAMPA = false;
    public final static boolean EXPERIMENTAL_EQUALITATION = false;
    List<Mat> finalCandidates;
    List<Mat> firstProcessSteps;
    List<Mat> secondProcessSteps;


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
//        CodePostOpenCVLoaded();
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
        //debugHWOC.CleanAll();
    }


    private void CodePostOpenCVLoaded() {

        DebugHWOC debugHWOC = new DebugHWOC(getResources());
        TimeProfiler.ResetCheckPoints();
        TimeProfiler.CheckPoint(0);
        CandidatesFinder candidatesFinder;


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


        TimeProfiler.CheckPoint(1);
        candidatesFinder = new CandidatesFinder(b);


        candidatesFinder.ToGrayScale();

        boolean fueGaussianBlureada = false;
        candidatesFinder.EqualizeHistOriginalImage(false);

        debugHWOC.AddStep(firstProcessSteps, candidatesFinder.CurrentImage);


        if (EXPERIMENTAL_EQUALITATION) {
            Imgproc.GaussianBlur(candidatesFinder.OriginalEqualizedImage, candidatesFinder.CurrentImage, new Size(25, 25), 25);
            fueGaussianBlureada = true;

        }


        candidatesFinder.Dilate();

        debugHWOC.AddStep(firstProcessSteps, candidatesFinder.CurrentImage);

        candidatesFinder.Erode();

        debugHWOC.AddStep(firstProcessSteps, candidatesFinder.CurrentImage);

        TimeProfiler.CheckPoint(2);


        candidatesFinder.Substraction();

        debugHWOC.AddStep(firstProcessSteps, candidatesFinder.CurrentImage);




        candidatesFinder.Sobel();
        debugHWOC.AddStep(firstProcessSteps, candidatesFinder.CurrentImage);




        candidatesFinder.GaussianBlur();
        debugHWOC.AddStep(firstProcessSteps, candidatesFinder.CurrentImage);

        List<RotatedRect> outlines = new ArrayList<RotatedRect>();
        TimeProfiler.CheckPoint(3);
        for (ImageSize is : ImageSize.values())
        {

            candidatesFinder.Dilate2(is);
            candidatesFinder.Erode2();

            candidatesFinder.OtsusThreshold();


            //STEP 1: start Finding outlines in the binary image
            candidatesFinder.FindOutlines();


            //STEP 2: start selecting outlines
            candidatesFinder.OutlinesSelection();


            debugHWOC.AddStepWithContourns(firstProcessSteps, candidatesFinder.CurrentImage,
                   candidatesFinder.LastGreenCandidates, candidatesFinder.LastBlueCandidates);


        }
        outlines.addAll(candidatesFinder.BlueCandidatesRR);

        TimeProfiler.CheckPoint(4);
        //STEP 3: loop
        for (int i=0; i<outlines.size(); ++i)
        {
            CandidateSelector candidateSelector =
                    new CandidateSelector(candidatesFinder.OriginalEqualizedImage, candidatesFinder.OriginalImageRealSize, outlines.get(i));
            //STEP 4:

            candidateSelector.CalculateBounds();
            candidateSelector.TruncateBounds();

            debugHWOC.AddImage(firstProcessSteps, R.drawable.ipp);
            debugHWOC.AddCountournedImage(firstProcessSteps, candidateSelector.OriginalEqualizedImage.clone(), candidateSelector.CandidateRect.clone());


            if (!candidateSelector.PercentajeAreaCandidateCheck()) {
                debugHWOC.AddImage(firstProcessSteps, R.drawable.percentaje_area_candidate_check);
                Log.d("filter","i=" + i + "!PercentajeAreaCandidateCheck");
                continue;
            }

            candidateSelector.CropExtraRotatedRect(false);   //STEP 5:
//            candidateSelector.CropExtraBoundget(i).angle);



            ////////////////////////////// START INVENCION MIA //////////////////////////////

            debugHWOC.AddStep(firstProcessSteps, candidateSelector.CurrentImage);
            candidateSelector.Sobel();
            debugHWOC.AddStep(firstProcessSteps, candidateSelector.CurrentImage);
            candidateSelector.GaussianBlur();
            debugHWOC.AddStep(firstProcessSteps, candidateSelector.CurrentImage);
            candidateSelector.Dilate();
            debugHWOC.AddStep(firstProcessSteps, candidateSelector.CurrentImage);


            candidateSelector.Erode();
            debugHWOC.AddStep(firstProcessSteps, candidateSelector.CurrentImage);
            ////////////////////////////// END INVENCION MIA //////////////////////////////


            //STEP 6:
            candidateSelector.OtsusThreshold();

            debugHWOC.AddStep(firstProcessSteps, candidateSelector.CurrentImage);

            //STEP 7:
            candidateSelector.FindOutlines();



            //STEP 8:
            candidateSelector.FindMaxAreaCandidatePro();


            //STEP 9:
            if(!candidateSelector.FindMinAreaRectInMaxArea()) {
                debugHWOC.AddImage(firstProcessSteps, R.drawable.find_min_area_rect_in_max_area);
                Log.d("filter","i=" + i + " !FindMinAreaRectInMaxArea");
                continue;
            }



            debugHWOC.AddCountournedImage(firstProcessSteps, candidateSelector.CurrentImage.clone(), candidateSelector.MinAreaRect.clone());


            //STEP 10 and 11

            CandidateSelector.CheckError checkError = candidateSelector.DoChecks();
            if (checkError != null) {

                debugHWOC.AddImage(firstProcessSteps, checkError.getValue());
                Log.d("filter","i=" + i + "!DoChecks");
                continue;
            }



            // Paso 11 sin rotación.
            //Mat sinCortar = candidatesFinder.OriginalImage.clone();
            //Rect roi2 = new Rect(rects.get(i).boundingRect().x + mr2.boundingRect().x,
            //        rects.get(i).boundingRect().y + mr2.boundingRect().y, (int)mr2.boundingRect().width, (int)mr2.boundingRect().height);
            //Mat cropped2 = new Mat(sinCortar, roi2);




            // Apply Gray Scale, Skew Correction, Fixed Size.
            // hacer reequalizacion????
            //Mat resizeimage = new Mat();
            //Size sz = new Size(100,100);
            //Imgproc.resize( croppedimage, resizeimage, sz );
            //
            //

            //debugHWOC.AddStep(firstProcessSteps, candidateSelector.CurrentImage.clone());
    //        debugHWOC.AddStep(firstProcessSteps, candidateSelector.OriginalEqualizedImage.clone());
            //debugHWOC.AddStep(firstProcessSteps, candidateSelector.GetFinalImage(true).clone());

            //InitializeGallery(R.id.gallery1, firstProcessSteps);
            //if(i==i)
//                return;


            finalCandidates.add(candidateSelector.GetFinalImage(true));


        }
/*
        if (1==1) {
            InitializeGallery(R.id.gallery1, firstProcessSteps);
            return;
        }
*/
        TimeProfiler.CheckPoint(5);
        //DrawImages();

        String plate = "";
        final boolean CHARS = true;
        for (int q=0; q< finalCandidates.size(); ++q) {
            //NOTA: ACA RECIEN OBTENER IMAGEN TAMAÑO REAL.
            debugHWOC.AddImage(secondProcessSteps, R.drawable.qpp);

            CharacterSeparator characterSeparator = new CharacterSeparator(finalCandidates.get(q).clone());
            debugHWOC.AddStep(secondProcessSteps, characterSeparator.CurrentImage);

            characterSeparator.AdaptiveThreshold();

            characterSeparator.FindCountourns();



            if(!characterSeparator.FilterCountourns()) {

                debugHWOC.AddStep(secondProcessSteps, characterSeparator.ImageWithContourns);
                debugHWOC.AddImage(secondProcessSteps, R.drawable.filter_countourns);

                Log.d("filter","q=" + q + " !FilterCountourns");
                continue;
            }



            debugHWOC.AddStep(secondProcessSteps, characterSeparator.ImageWithContourns);
            debugHWOC.AddStep(secondProcessSteps, characterSeparator.CleanedImage);

            characterSeparator.CalculatePlateLength();

            characterSeparator.CalculateCharsPositions();

            //characterSeparator.CalculateHistrograms();
            if (CHARS)
                characterSeparator.CropChars();
            else
                characterSeparator.CropAll();



            String whiteList = "";
            for (int n=0; n<characterSeparator.CroppedChars.size(); ++n) {

                debugHWOC.AddImage(secondProcessSteps, R.drawable.npp);
                debugHWOC.AddStep(secondProcessSteps, characterSeparator.CroppedChars.get(n));
                switch (n)
                {
                    case 0://1
                        whiteList = "ABCDEFGHIJKLNPRSTUVXYZW";
                        break;
                    case 2://3
                        plate +="-";
                        whiteList = "BCDFGHJKLPRSTVXYZW0123456789";
                        break;
                    case 4://5
                        plate +="-";
                        whiteList = "0123456789";
                        break;
                }

                MainActivity.baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST,whiteList);


                Mat m = characterSeparator.CroppedChars.get(n);
                Imgproc.cvtColor(m, m, Imgproc.COLOR_GRAY2RGB);
                //Imgproc.threshold(m, m, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
                Bitmap bmp = Bitmap.createBitmap(m.cols(), m.rows(), Bitmap.Config.ARGB_8888);

                Utils.matToBitmap(m, bmp);
                MainActivity.baseApi.setImage(bmp);
                String recognizedText = MainActivity.baseApi.getUTF8Text();
                Log.d("output", "n=" +n+" text:" +recognizedText);
                plate += recognizedText;
            }
            plate += " || ";
        }
//        MainActivity.baseApi.end();
        plate = plate.replace("\n", "").replace("\r", "");

        TimeProfiler.CheckPoint(6);
        if (SHOW_PROCESS_DEBUG) {
            InitializeGallery(R.id.gallery1, firstProcessSteps);
            InitializeGallery(R.id.gallery2, secondProcessSteps);
            InitializeGallery(R.id.gallery3, finalCandidates);
        }
        SetPlate(plate);
        Log.d("output", "plate="+plate);
        Log.d("times", TimeProfiler.GetTotalTime());
        Log.d("times", TimeProfiler.GetTimes(true, 10));
        Log.d("times", TimeProfiler.GetTimes(false));
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

    private void drawContornsToMatInBitmap(Mat m, List<MatOfPoint> cs, List<MatOfPoint> csRefine) {
        Mat finalMat = m.clone();
        //Imgproc.cvtColor(finalMat, finalMat, Imgproc.COLOR_GRAY2RGB); //USAR SI APLICA, SI SE CAE LA APP, Convert to RGB
        if (cs != null)
        {
            for (int cId = 0; cId < cs.size(); cId++) {
                Imgproc.drawContours(finalMat, cs, cId, new Scalar(0, 255, 0), 1);
            }
        }
        if (csRefine != null)
        {
            for (int cId = 0; cId < csRefine.size(); cId++) {
                Imgproc.drawContours(finalMat, csRefine, cId, new Scalar(0, 0, 255), 6);
            }
        }

        if (true) {
            // Rotating 90 clock degrees
            finalMat = finalMat.t();
            Core.flip(finalMat, finalMat, 2);
        }
        Bitmap bm = Bitmap.createBitmap(finalMat.cols(), finalMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(finalMat, bm);

        ImageView ll = (ImageView)findViewById(R.id.image1);
        ll.setImageBitmap(bm);
    }

    private void drawContornsToMatInBitmap(Mat m, MatOfPoint cs) {
        List<MatOfPoint> l = new ArrayList<MatOfPoint>();
        if (cs != null)
            l.add(cs);
        drawContornsToMatInBitmap(m, l, null);
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
