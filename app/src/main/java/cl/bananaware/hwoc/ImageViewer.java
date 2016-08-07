package cl.bananaware.hwoc;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.ShareCompat;
import android.support.v4.util.DebugUtils;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.ImageView;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG2;

import java.io.Console;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Marco on 21-04-2016.
 */
public class ImageViewer extends Activity {

    Map<Integer, Integer> patentIndexInImage = new HashMap<Integer, Integer>();
    String TAG = "iw";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cameraview);
        fillMap();


        int image = R.drawable.vehicle_ex;
        ImageView imgFp = (ImageView) findViewById(R.id.imageView);

        long time1 = System.currentTimeMillis();
        CandidatesFinder candidatesFinder = new CandidatesFinder(BitmapFactory.decodeResource(getResources(), image));
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
        candidatesFinder.Dilate2();
        long time9 = System.currentTimeMillis();
        candidatesFinder.Erode2();
        long time10 = System.currentTimeMillis();



        candidatesFinder.ToGrayScale();
        long time11 = System.currentTimeMillis();
        candidatesFinder.OtsusThreshold();
        long time12 = System.currentTimeMillis();
        if (false) {
            // DRAWING
            drawContornsToMatInBitmap(candidatesFinder.CurrentImage, null, null, imgFp);
            return;
        }


        //STEP 1: start Finding outlines in the binary image
        candidatesFinder.FindOutlines();
        long time13 = System.currentTimeMillis();

        //STEP 2: start selecting outlines
        candidatesFinder.OutlinesSelection();
        long time14 = System.currentTimeMillis();


        if (false) {
            // DRAWING GREEN AND BLUE LINES
            drawContornsToMatInBitmap(candidatesFinder.OriginalImage.clone(), candidatesFinder.GreenCandidates,
                    candidatesFinder.BlueCandidates, imgFp);
            return;
        }



        Log.d("Times", "Time 1-2: " + String.valueOf(time2-time1) + " Suma: " + String.valueOf(time2-time1));
        Log.d("Times", "Time 2-3: " + String.valueOf(time3-time2) + " Suma: " + String.valueOf(time3-time1));
        Log.d("Times", "Time 3-4: " + String.valueOf(time4-time3) + " Suma: " + String.valueOf(time4-time1));
        Log.d("Times", "Time 4-5: " + String.valueOf(time5-time4) + " Suma: " + String.valueOf(time5-time1));
        Log.d("Times", "Time 5-6: " + String.valueOf(time6-time5) + " Suma: " + String.valueOf(time6-time1));
        Log.d("Times", "Time 6-7: " + String.valueOf(time7-time6) + " Suma: " + String.valueOf(time7-time1));
        Log.d("Times", "Time 7-8: " + String.valueOf(time8-time7) + " Suma: " + String.valueOf(time8-time1));
        Log.d("Times", "Time 8-9: " + String.valueOf(time9-time8) + " Suma: " + String.valueOf(time9-time1));
        Log.d("Times", "Time 9-10: " + String.valueOf(time10-time9) + " Suma: " + String.valueOf(time10-time1));
        Log.d("Times", "Time 10-11: " + String.valueOf(time11-time10) + " Suma: " + String.valueOf(time11-time1));
        Log.d("Times", "Time 11-12: " + String.valueOf(time12-time11) + " Suma: " + String.valueOf(time12-time1));
        Log.d("Times", "Time 12-13: " + String.valueOf(time13-time12) + " Suma: " + String.valueOf(time13-time1));
        Log.d("Times", "Time 13-14: " + String.valueOf(time14-time13) + " Suma: " + String.valueOf(time14-time1));

        List<RotatedRect> outlines = candidatesFinder.BlueCandidatesRR;

        //STEP 3: loop
        for (int i=0; i<outlines.size(); ++i)
        {
            //STEP 4:
            time14 = System.currentTimeMillis();
            double dx = 0.15126 * outlines.get(i).boundingRect().size().width;
            double dy = 0.625* outlines.get(i).boundingRect().size().height;;
            int newWidth = (int)(outlines.get(i).boundingRect().size().width + dx);
            int newHeight = (int)(outlines.get(i).boundingRect().size().height + dy);
            long time15 = System.currentTimeMillis();

            //STEP 5:
            Mat uncropped = candidatesFinder.OriginalImage.clone();

            // si excedimos el ancho de la imagen, lo truncamos
            if (candidatesFinder.OriginalImage.width() < outlines.get(i).boundingRect().x + newWidth)
                newWidth = newWidth -  ((outlines.get(i).boundingRect().x + newWidth) - candidatesFinder.OriginalImage.width());
            // si excedimos el alto de la imagen, lo truncamos
            if (candidatesFinder.OriginalImage.height() < outlines.get(i).boundingRect().y + newHeight)
                newHeight = newHeight -  ((outlines.get(i).boundingRect().y + newHeight) - candidatesFinder.OriginalImage.height());

            Rect roi = new Rect(outlines.get(i).boundingRect().x, outlines.get(i).boundingRect().y, newWidth, newHeight);
            Mat cropped = new Mat(uncropped, roi);
            Mat croppedColor = cropped.clone();

            if (false && i==patentIndexInImage.get(image)) {
                // DRAW START DE INVENCION
                drawContornsToMatInBitmap(cropped, null, null, imgFp );
                break;
            }
            long time16 = System.currentTimeMillis();





            ////////////////////////////// START INVENCION MIA //////////////////////////////

            Mat grad_x2 = new Mat();
            Mat abs_grad_x2 = new Mat();
            Imgproc.Sobel(cropped, grad_x2, CvType.CV_8U, 1, 0, 3, 1, Core.BORDER_DEFAULT);


            Core.convertScaleAbs(grad_x2, abs_grad_x2);
            //Core.convertScaleAbs(grad_y, abs_grad_y);
            Core.addWeighted(abs_grad_x2, 1, abs_grad_x2, 0, 0, cropped); // or? Core.addWeighted(abs_grad_x, 0.5, abs_grad_x, 0, 0, dest);
            //END sobel




            //Start Gaussian Blur
            Imgproc.GaussianBlur(cropped, cropped, new Size(5,5), 2);
            //End Gaussian Blur


            // Start dilation section
            float dilationAmplifier3 = 1f;
            float horizontalDilatationAmplifier = calculateHorizontalAmplifier(cropped.size(), i==patentIndexInImage.get(image));
            Mat element5 = Imgproc.getStructuringElement( Imgproc.MORPH_RECT,
                    new Size( Math.round(9*dilationAmplifier3*horizontalDilatationAmplifier), 3*dilationAmplifier3 ));
            Imgproc.dilate( cropped, cropped, element5 );
            // End dilation section

            if (false && i==patentIndexInImage.get(image)) {
                // DRAW INVENCION DILATION
                drawContornsToMatInBitmap(cropped, null, null, imgFp );
                break;
            }


            // Start erotion section
            float erotionAmplifier3 = 1f;
            Mat element6 = Imgproc.getStructuringElement( Imgproc.MORPH_RECT, new Size( Math.round(9*erotionAmplifier3*horizontalDilatationAmplifier), 3*erotionAmplifier3 ));
            Imgproc.erode( cropped, cropped, element6 );
            // End erotion section
            long time17 = System.currentTimeMillis();

            ////////////////////////////// END INVENCION MIA //////////////////////////////

            if (false && i==patentIndexInImage.get(image)) {
                // DRAW FINAL DE INVENCION
                drawContornsToMatInBitmap(cropped, null, null, imgFp );
                break;
            }



            //STEP 6:
            Imgproc.cvtColor(cropped, cropped, Imgproc.COLOR_RGB2GRAY); //Convert to gray scale
            Imgproc.threshold(cropped, cropped, 0, 255, Imgproc.THRESH_OTSU | Imgproc.THRESH_BINARY);
            long time18 = System.currentTimeMillis();

            //STEP 7:
            List<MatOfPoint> contours2 = new ArrayList<MatOfPoint>();
            Mat hierarchy2 = new Mat();
            Imgproc.findContours(cropped.clone(), contours2, hierarchy2, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE );
            long time19 = System.currentTimeMillis();

            if (false && i==patentIndexInImage.get(image)) {
                // DRAW A SECTION WITH ITS CONTOURS
                Imgproc.cvtColor(cropped, cropped, Imgproc.COLOR_GRAY2RGB); //Convert to gray scale
                drawContornsToMatInBitmap(cropped, contours2, null, imgFp );
                break;
            }



            //STEP 8:
            double maxArea = 0;
            int maxAreaIndex = 0;
            for(int j=0; j<contours2.size(); ++j)
            {
                MatOfPoint mop = contours2.get(j);
                MatOfPoint2f mop2f = mopToMop2f(mop);
                RotatedRect mr = Imgproc.minAreaRect(mop2f);

                double currentArea=mr.size.width * mr.size.height;
                if (currentArea > maxArea) {
                    maxArea = currentArea;
                    maxAreaIndex = j;
                }
            }
            long time20 = System.currentTimeMillis();

            // DRAW PATENT IN GRAYSCALE
            //Imgproc.cvtColor(cropped, cropped, Imgproc.COLOR_GRAY2RGB); //Convert to gray scale
            //drawContornsToMatInBitmap(cropped, contours2.get(maxAreaIndex), imgFp );




            //STEP 9:
            MatOfPoint mop2 = contours2.get(maxAreaIndex);
            MatOfPoint2f mop2f2 = mopToMop2f(mop2);
            //MatOfPoint2f mop2f = new MatOfPoint2f(mop);
            RotatedRect mr2 = Imgproc.minAreaRect(mop2f2);
            long time21 = System.currentTimeMillis();

            if (false && i==patentIndexInImage.get(image)) {
                // DRAWING ROTATED RECTANGLE
                Imgproc.cvtColor(cropped, cropped, Imgproc.COLOR_GRAY2RGB);
                drawContornsToMatInBitmap(drawRotatedRectInMat(mr2, croppedColor), null, null, imgFp);
                break;
            }

            //STEP 10:
            //checks
            // Autos chilenos 36cm x 13cm Concentrarse en éstos
            // Motos chilenas nuevas 14,5cm x 12cm.
            // Motos chilenas antiguas 14,5cm x 8cm.
            double angle = mr2.angle;
            Size rect_size = mr2.size;
            if (mr2.angle < -45.) {
                angle += 90.0;
                //swaping height and width
                double widthTemp = rect_size.width;
                rect_size.width = rect_size.height;
                rect_size.height = widthTemp;
            }

            double imageRatio = rect_size.width / rect_size.height;//Math.max(mr2.size.width,mr2.size.height)/Math.min(mr2.size.width,mr2.size.height);
            final double OFFICIAL_RATIO = 36f/13f;
            final double MIN_RATIO = 2.2f;
            final double MAX_RATIO = 5.7f;
            boolean ratioCorrect = false;
            if (imageRatio >= MIN_RATIO && imageRatio <= MAX_RATIO)
                ratioCorrect = true;

            boolean areaCorrect = false;
            if (ratioCorrect)
            {
                // AREAS Y PORCENTAJE DE AREAS
                double area = rect_size.width*rect_size.height;
                double maxAreaImage = candidatesFinder.OriginalImage.size().width * candidatesFinder.OriginalImage.size().height;
                double percentajeImage = area/maxAreaImage;
                final double MIN_AREA = 950;
                final double MAX_PERCENTAJE_AREA = 0.15;
                if (area >= MIN_AREA && percentajeImage <= MAX_PERCENTAJE_AREA)
                    areaCorrect = true;
            }
            long time22 = System.currentTimeMillis();




            //STEP 11:
            if (!areaCorrect)
                continue;

            // get the rotation matrix
            Mat matrix = Imgproc.getRotationMatrix2D(mr2.center, angle, 1.0);
            // perform the affine transformation
            Mat rotated = new Mat();
            Mat cropped2 = new Mat();
            Mat precrop = croppedColor.clone();
            Imgproc.cvtColor(precrop, precrop, Imgproc.COLOR_RGB2GRAY); //Convert to gray scale
            Imgproc.warpAffine(precrop, rotated, matrix, precrop.size(), Imgproc.INTER_CUBIC);
            // crop the resulting image
            Imgproc.getRectSubPix(rotated, rect_size, mr2.center, cropped2);
            long time23 = System.currentTimeMillis();


            // Paso 11 sin rotación.
            //Mat sinCortar = candidatesFinder.OriginalImage.clone();
            //Rect roi2 = new Rect(rects.get(i).boundingRect().x + mr2.boundingRect().x,
            //        rects.get(i).boundingRect().y + mr2.boundingRect().y, (int)mr2.boundingRect().width, (int)mr2.boundingRect().height);
            //Mat cropped2 = new Mat(sinCortar, roi2);


            if (true && i==patentIndexInImage.get(image)) {
                //Mostrar en pantalla resultado de la iteración
                drawContornsToMatInBitmap(cropped2, null, imgFp);
                break; // IMPORTANTE, PROBLEMA DE THREAD PARECE, SI NO HAY BREAK LA LÍNEA ANTERIOR SE CAE.
            }

            // Apply Gray Scale, Skew Correction, Fixed Size.
            //Mat resizeimage = new Mat();
            //Size sz = new Size(100,100);
            //Imgproc.resize( croppedimage, resizeimage, sz );
            //
            //

            Log.d("Times", "i=" + i + "----------------------------");
            Log.d("Times", "Time 14-15: " + String.valueOf(time15-time14) + " Suma: " + String.valueOf(time15-time1));
            Log.d("Times", "Time 15-16: " + String.valueOf(time16-time15) + " Suma: " + String.valueOf(time16-time1));
            Log.d("Times", "Time 16-17: " + String.valueOf(time17-time16) + " Suma: " + String.valueOf(time17-time1));
            Log.d("Times", "Time 17-18: " + String.valueOf(time18-time17) + " Suma: " + String.valueOf(time18-time1));
            Log.d("Times", "Time 18-19: " + String.valueOf(time19-time18) + " Suma: " + String.valueOf(time19-time1));
            Log.d("Times", "Time 19-20: " + String.valueOf(time20-time19) + " Suma: " + String.valueOf(time20-time1));
            Log.d("Times", "Time 20-21: " + String.valueOf(time21-time20) + " Suma: " + String.valueOf(time21-time1));
            Log.d("Times", "Time 21-22: " + String.valueOf(time22-time21) + " Suma: " + String.valueOf(time22-time1));
            Log.d("Times", "Time 22-23: " + String.valueOf(time23-time22) + " Suma: " + String.valueOf(time23-time1));
            Log.d("Times", "----------------------------");
        }



/*

        Mat finalMat = dest;
        Bitmap bm = Bitmap.createBitmap(finalMat.cols(), finalMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(finalMat, bm);
        imgFp.setImageBitmap(bm);

*/


    }

    private void fillMap() {
        patentIndexInImage.put(R.drawable.vehicle_ex, 2);
        patentIndexInImage.put(R.drawable.vehicle_ex2, 1);
        patentIndexInImage.put(R.drawable.vehicle_ex3, 0);
        patentIndexInImage.put(R.drawable.vehicle_ex4, 0);
        //patentIndexInImage.put(R.drawable.vehicle_ex5, ?);
        patentIndexInImage.put(R.drawable.vehicle_ex6, 0);
        patentIndexInImage.put(R.drawable.vehicle_ex7, 0);
        patentIndexInImage.put(R.drawable.vehicle_ex8, 4);
        patentIndexInImage.put(R.drawable.vehicle_ex9, 4);
        patentIndexInImage.put(R.drawable.vehicle_ex10, 10);//?
        patentIndexInImage.put(R.drawable.vehicle_ex12, 3);
    }


    private float calculateHorizontalAmplifier(Size size, boolean debugPrint) {
        // CODIGO EN PRUEBA, QUIZAS NO FUNCIONA Y NO TIENE REAL RELACION CON EL TAMAÑO DE LA IMAGEN
        final float MIN_VALUE = 0.3f;
        final float MAX_VALUE = 10f;
        float val = 1;
        if (size.width > 150)
            val = (float)(size.width* 13.0/744.0-1663.0/744.0);
        if (debugPrint)
            Log.d("test", "WIDTH: " + String.valueOf(size.width));
        return val;//Math.max(val, MIN_VALUE);
    }

    private void drawContornsToMatInBitmap(Mat m, List<MatOfPoint> cs, List<MatOfPoint> csRefine, ImageView imgFp) {
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
                Imgproc.drawContours(finalMat, csRefine, cId, new Scalar(0, 0, 255), 1);
            }
        }
        Bitmap bm = Bitmap.createBitmap(finalMat.cols(), finalMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(finalMat, bm);
        imgFp.setImageBitmap(bm);
    }

    private void drawContornsToMatInBitmap(Mat m, MatOfPoint cs, ImageView imgFp) {
        List<MatOfPoint> l = new ArrayList<MatOfPoint>();
        if (cs != null)
            l.add(cs);
        drawContornsToMatInBitmap(m, l, null, imgFp);
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


    private void sub(Mat src1, Mat src2, Mat src3)
    {
        Core.subtract(src1, src2, src3);
    }
    private void savedFuncion()
    {

        int image = R.mipmap.bin_im;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ImageView imgFp = (ImageView) findViewById(R.id.imageView);
        // imgFp.setImageResource(image);


        Mat src = new Mat();
        Mat eyeMat = new Mat();
        Mat eyeMat2 = new Mat();
        Utils.bitmapToMat(BitmapFactory.decodeResource(getResources(), image) , src);

        Imgproc.GaussianBlur(src, eyeMat, new Size(3, 3), 2, 2);

        //   eyeMat.convertTo(eyeMat, CvType.CV_8UC1);
        Imgproc.cvtColor(eyeMat, eyeMat, Imgproc.COLOR_RGB2GRAY);

        Imgproc.threshold(eyeMat, eyeMat, 10, 255, Imgproc.THRESH_BINARY);



        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.Canny(src, eyeMat, 80, 100);

        Imgproc.findContours(eyeMat, contours, hierarchy, Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);
        Imgproc.drawContours(src, contours, -1, new Scalar(0, 255, 0), 3);
        Bitmap bm = Bitmap.createBitmap(src.cols(), src.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(src, bm);
        imgFp.setImageBitmap(bm);


        //drawing
        //Mat contourImg = new Mat(eyeMat.size(), eyeMat.type());
        //Imgproc.drawContours(src, contours, -1, new Scalar(0, 255, 0), 3);
/*
        for (int i = 0; i < contours.size(); i++) {
            Imgproc.drawContours(src, contours, i, new Scalar(200, 160, 140), -1);
        }*/
        //Imgproc.cvtColor(src, src, Imgproc.COLOR_GRAY2RGB);


        /*
        bmFinal = Bitmap.createBitmap(src.cols(), src.rows(),Bitmap.Config.RGB_565);
        Utils.matToBitmap(src, bmFinal);


        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                ImageView imgFp = (ImageView) findViewById(R.id.imageView);
                imgFp.setImageBitmap(bmFinal);
            }
        }, 2000);*/
    }
    private MatOfPoint2f mopToMop2f(MatOfPoint mop) {
        return new MatOfPoint2f( mop.toArray() );
    }
    Bitmap bmFinal;
}
