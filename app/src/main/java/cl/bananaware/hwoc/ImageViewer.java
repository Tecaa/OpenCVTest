package cl.bananaware.hwoc;

import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.PipedOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Marco on 21-04-2016.
 */
public class ImageViewer extends Activity {
    int image = R.drawable.vehicle_ex11;
    Map<Integer, Integer> patentIndexInImage = new HashMap<Integer, Integer>();
    List<Mat> finalCandidates = new ArrayList<Mat>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cameraview);
        fillMap();
        MainActivity.CameraCapturerFixer = true;
    }
    public void onResume()
    {
        super.onResume();
        //new DoAll().execute();
        CodePostOpenCVLoaded();

        /*
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }*/
    }
/*
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
*/
    private void CodePostOpenCVLoaded() {
        //ImageView imgFp = (ImageView) findViewById(R.id.imageView);

        long time1 = System.currentTimeMillis();
        CandidatesFinder candidatesFinder;

        Bitmap b;
        if (MainActivity.TAKE_PICTURE)
        {
            Intent intent = getIntent();
            b = (Bitmap) intent.getParcelableExtra("photo");

        }
        else {
            b = BitmapFactory.decodeResource(getResources(), image);
        }
        candidatesFinder = new CandidatesFinder(b);
        long time2 = System.currentTimeMillis();
        candidatesFinder.ToGrayScale();
        long time3 = System.currentTimeMillis();
        candidatesFinder.Dilate();
        long time4 = System.currentTimeMillis();
        candidatesFinder.Erode();
        long time5 = System.currentTimeMillis();
        candidatesFinder.Substraction();
        long time6 = System.currentTimeMillis();



        candidatesFinder.Sobel();
        long time7 = System.currentTimeMillis();
        candidatesFinder.GaussianBlur();
        long time8 = System.currentTimeMillis();
        List<RotatedRect> outlines = new ArrayList<RotatedRect>();
        for (ImageSize is : ImageSize.values()) {
            candidatesFinder.Dilate2(is);
            long time9 = System.currentTimeMillis();
            candidatesFinder.Erode2();
            long time10 = System.currentTimeMillis();



            //candidatesFinder.ToGrayScale();
            long time11 = System.currentTimeMillis();
            candidatesFinder.OtsusThreshold();
            long time12 = System.currentTimeMillis();
            if (false && is == ImageSize.GRANDE) {
                // DRAWING
                drawContornsToMatInBitmap(candidatesFinder.CurrentImage, null, null);
                return;
            }


            //STEP 1: start Finding outlines in the binary image
            candidatesFinder.FindOutlines();
            long time13 = System.currentTimeMillis();


            //STEP 2: start selecting outlines
            candidatesFinder.OutlinesSelection();
            long time14 = System.currentTimeMillis();

            if (false && is == ImageSize.GRANDE) {
                // DRAWING GREEN AND BLUE LINES IN COLOR IMAGE
                drawContornsToMatInBitmap(candidatesFinder.OriginalImage.clone(), null,
                        candidatesFinder.LastBlueCandidates);
                return;
            }

            if (false && is == ImageSize.GRANDE) {
                // DRAWING GREEN AND BLUE LINES IN GRAY SCALE IMAGE
                Mat temp = candidatesFinder.CurrentImage.clone();
                Imgproc.cvtColor(temp, temp, Imgproc.COLOR_GRAY2RGB);
                drawContornsToMatInBitmap(temp, null,
                        candidatesFinder.LastBlueCandidates);
                return;
            }


/*
            Log.d("Times", "Size=" + is.name() + " Time 1-2: " + String.valueOf(time2-time1) + " Suma: " + String.valueOf(time2-time1));
            Log.d("Times", "Size=" + is.name() + " Time 2-3: " + String.valueOf(time3-time2) + " Suma: " + String.valueOf(time3-time1));
            Log.d("Times", "Size=" + is.name() + " Time 3-4: " + String.valueOf(time4-time3) + " Suma: " + String.valueOf(time4-time1));
            Log.d("Times", "Size=" + is.name() + " Time 4-5: " + String.valueOf(time5-time4) + " Suma: " + String.valueOf(time5-time1));
            Log.d("Times", "Size=" + is.name() + " Time 5-6: " + String.valueOf(time6-time5) + " Suma: " + String.valueOf(time6-time1));
            Log.d("Times", "Size=" + is.name() + " Time 6-7: " + String.valueOf(time7-time6) + " Suma: " + String.valueOf(time7-time1));
            Log.d("Times", "Size=" + is.name() + " Time 7-8: " + String.valueOf(time8-time7) + " Suma: " + String.valueOf(time8-time1));
            Log.d("Times", "Size=" + is.name() + " Time 8-9: " + String.valueOf(time9-time8) + " Suma: " + String.valueOf(time9-time1));
            Log.d("Times", "Size=" + is.name() + " Time 9-10: " + String.valueOf(time10-time9) + " Suma: " + String.valueOf(time10-time1));
            Log.d("Times", "Size=" + is.name() + " Time 10-11: " + String.valueOf(time11-time10) + " Suma: " + String.valueOf(time11-time1));
            Log.d("Times", "Size=" + is.name() + " Time 11-12: " + String.valueOf(time12-time11) + " Suma: " + String.valueOf(time12-time1));
            Log.d("Times", "Size=" + is.name() + " Time 12-13: " + String.valueOf(time13-time12) + " Suma: " + String.valueOf(time13-time1));
            Log.d("Times", "Size=" + is.name() + " Time 13-14: " + String.valueOf(time14-time13) + " Suma: " + String.valueOf(time14-time1));
*/

        }
        outlines.addAll(candidatesFinder.BlueCandidatesRR);

        //STEP 3: loop
        for (int i=0; i<outlines.size(); ++i)
        {
            Log.d("TEST", "i="+i);
            CandidateSelector candidateSelector =
                    new CandidateSelector(candidatesFinder.OriginalImage, candidatesFinder.OriginalImageRealSize, outlines.get(i));
            //STEP 4:
            long time14_5 = System.currentTimeMillis();
            candidateSelector.CalculateBounds();
            long time15 = System.currentTimeMillis();
/*
            if (!candidateSelector.PercentajeAreaCandidateCheck(0.3)) {
                Log.d("test", "pass " + i+ "=i");
                continue;
            }*/


            if (false && i==patentIndexInImage.get(image)) {
                // DRAW START DE INVENCION
                Mat temp = candidateSelector.OriginalImage.clone();
                Imgproc.cvtColor(temp, temp, Imgproc.COLOR_RGB2GRAY);
                Imgproc.cvtColor(temp, temp, Imgproc.COLOR_GRAY2RGB);
                drawContornsToMatInBitmap(drawRotatedRectInMat(candidateSelector.CandidateRect,
                        temp), null, null);
                break;
            }

            //STEP 5:
            candidateSelector.TruncateBounds();
            candidateSelector.CropExtraBoundingBox();

            if (false && i==patentIndexInImage.get(image)) {
                // DRAW START DE INVENCION
                drawContornsToMatInBitmap(candidateSelector.CurrentImage, null, null);
                break;
            }
            long time16 = System.currentTimeMillis();



            ////////////////////////////// START INVENCION MIA //////////////////////////////

            candidateSelector.Sobel();
            candidateSelector.GaussianBlur();
            candidateSelector.Dilate();


            if (false && i==patentIndexInImage.get(image)) {
                // DRAW INVENCION DILATION
                drawContornsToMatInBitmap(candidateSelector.CurrentImage, null, null);
                break;
            }

            candidateSelector.Erode();
            long time17 = System.currentTimeMillis();

            ////////////////////////////// END INVENCION MIA //////////////////////////////

            if (false && i==patentIndexInImage.get(image)) {
                // DRAW FINAL DE INVENCION
                drawContornsToMatInBitmap(candidateSelector.CurrentImage, null, null);
                break;
            }



            //STEP 6:
            candidateSelector.OtsusThreshold();
            long time18 = System.currentTimeMillis();


            //STEP 7:
            candidateSelector.FindOutlines();

            long time19 = System.currentTimeMillis();

            if (false && i==patentIndexInImage.get(image)) {
                // DRAW A SECTION WITH ITS CONTOURS
                Mat colorCurrentImage = candidateSelector.CurrentImage.clone();
                Imgproc.cvtColor(colorCurrentImage, colorCurrentImage, Imgproc.COLOR_GRAY2RGB);
                drawContornsToMatInBitmap(colorCurrentImage, candidateSelector.GreenCandidatesPro, null);
                break;
            }



            //STEP 8:
            candidateSelector.FindMaxAreaCandidatePro();
            long time20 = System.currentTimeMillis();


            //STEP 9:
            candidateSelector.FindMinAreaRectInMaxArea();
            long time21 = System.currentTimeMillis();

            if (false && i==patentIndexInImage.get(image)) {
                // DRAWING ROTATED RECTANGLE
                Mat tempCurrentImage = candidateSelector.CurrentImage.clone();
                Imgproc.cvtColor(tempCurrentImage, tempCurrentImage, Imgproc.COLOR_GRAY2RGB);
                drawContornsToMatInBitmap(drawRotatedRectInMat(candidateSelector.MinAreaRect,
                        tempCurrentImage), null, null);
                break;
            }



            //STEP 10 and 11
            long time22 = System.currentTimeMillis();
            if (!candidateSelector.DoChecks()) {
                time22 = System.currentTimeMillis();
                continue;
            }


            candidateSelector.CropMinRotatedRect();
            long time23 = System.currentTimeMillis();


            // Paso 11 sin rotación.
            //Mat sinCortar = candidatesFinder.OriginalImage.clone();
            //Rect roi2 = new Rect(rects.get(i).boundingRect().x + mr2.boundingRect().x,
            //        rects.get(i).boundingRect().y + mr2.boundingRect().y, (int)mr2.boundingRect().width, (int)mr2.boundingRect().height);
            //Mat cropped2 = new Mat(sinCortar, roi2);




            // Apply Gray Scale, Skew Correction, Fixed Size.
            //Mat resizeimage = new Mat();
            //Size sz = new Size(100,100);
            //Imgproc.resize( croppedimage, resizeimage, sz );
            //
            //

            Log.d("Times", "i=" + i + "----------------------------");
  /*          Log.d("Times", "Time 14_5-15: " + String.valueOf(time15-time14_5) + " Suma: " + String.valueOf(time15-time1));
            Log.d("Times", "Time 15-16: " + String.valueOf(time16-time15) + " Suma: " + String.valueOf(time16-time1));
            Log.d("Times", "Time 16-17: " + String.valueOf(time17-time16) + " Suma: " + String.valueOf(time17-time1));
            Log.d("Times", "Time 17-18: " + String.valueOf(time18-time17) + " Suma: " + String.valueOf(time18-time1));
            Log.d("Times", "Time 18-19: " + String.valueOf(time19-time18) + " Suma: " + String.valueOf(time19-time1));
            Log.d("Times", "Time 19-20: " + String.valueOf(time20-time19) + " Suma: " + String.valueOf(time20-time1));
            Log.d("Times", "Time 20-21: " + String.valueOf(time21-time20) + " Suma: " + String.valueOf(time21-time1));
            Log.d("Times", "Time 21-22: " + String.valueOf(time22-time21) + " Suma: " + String.valueOf(time22-time1));
            Log.d("Times", "Time 22-23: " + String.valueOf(time23-time22) + " Suma: " + String.valueOf(time23-time1));
            Log.d("Times", "----------------------------");*/

            finalCandidates.add(candidateSelector.CurrentImage);

            if (false && i==patentIndexInImage.get(image)) {
                //Mostrar en pantalla resultado de la iteración
                drawContornsToMatInBitmap(candidateSelector.CurrentImage, null);
                break; // IMPORTANTE, PROBLEMA DE THREAD PARECE, SI NO HAY BREAK LA LÍNEA ANTERIOR SE CAE.
            }
        }

        InitializeGallery();
        //DrawImages();
    }

    private void InitializeGallery() {
        // Note that Gallery view is deprecated in Android 4.1---
        Gallery gallery = (Gallery) findViewById(R.id.gallery1);
        gallery.setAdapter(new ImageAdapter(this));
        gallery.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position,long id)
            {
                SetGalleryImage(position);
            }
        });
        SetGalleryImage(0);
    }

    private void SetGalleryImage(int position)
    {
        if (position >= finalCandidates.size()) {
            Toast.makeText(getBaseContext(),"Position " + position + " not found.",
                  Toast.LENGTH_SHORT).show();
            // display the images selected
            return;
        }


        Bitmap bm = Bitmap.createBitmap(finalCandidates.get(position).cols(),
                finalCandidates.get(position).rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(finalCandidates.get(position), bm);
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

    private void fillMap() {
        patentIndexInImage.put(R.drawable.vehicle_ex, 2);
        patentIndexInImage.put(R.drawable.vehicle_ex2, 1);
        patentIndexInImage.put(R.drawable.vehicle_ex3, 0);
        patentIndexInImage.put(R.drawable.vehicle_ex4, 0);
        patentIndexInImage.put(R.drawable.vehicle_ex5, 6);
        patentIndexInImage.put(R.drawable.vehicle_ex6, 0);
        patentIndexInImage.put(R.drawable.vehicle_ex7, 0);
        patentIndexInImage.put(R.drawable.vehicle_ex8, 4);
        patentIndexInImage.put(R.drawable.vehicle_ex9, 4);
        patentIndexInImage.put(R.drawable.vehicle_ex10, 10);//?
        patentIndexInImage.put(R.drawable.vehicle_ex11, 2); // ?
        patentIndexInImage.put(R.drawable.vehicle_ex12, 2);
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
        // Rotating 90 clock degrees
        finalMat = finalMat.t();
        Core.flip(finalMat, finalMat, 2);

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

    private Mat drawRotatedRectInMat(RotatedRect rRect, Mat mat)
    {
        Point[] vertices = new Point[4];
        rRect.points(vertices);
        for (int j = 0; j < 4; j++){
            Imgproc.line(mat, vertices[j], vertices[(j+1)%4], new Scalar(255,0,0));
        }
        return mat;
    }


    public class ImageAdapter extends BaseAdapter {
        private Context context;
        private int itemBackground;
        public ImageAdapter(Context c)
        {
            context = c;
            // sets a grey background; wraps around the images
            TypedArray a =obtainStyledAttributes(R.styleable.MyGallery);
            itemBackground = a.getResourceId(R.styleable.MyGallery_android_galleryItemBackground, 0);
            a.recycle();
        }
        // returns the number of images
        public int getCount() {
            return finalCandidates.size();
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

            Bitmap bm = Bitmap.createBitmap(finalCandidates.get(position).cols(),
                    finalCandidates.get(position).rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(finalCandidates.get(position), bm);
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
}
