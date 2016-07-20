package cl.bananaware.hwoc;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
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
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG2;

import java.io.Console;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Marco on 21-04-2016.
 */
public class ImageViewer extends Activity {

    String TAG = "iw";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cameraview);


        int image = R.drawable.vehicle_ex2;
        ImageView imgFp = (ImageView) findViewById(R.id.imageView);

        Mat src = new Mat();
        Utils.bitmapToMat(BitmapFactory.decodeResource(getResources(), image) , src);





        Mat mat = new Mat();
        Mat dest = src.clone();
        mat = src.clone();
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY); //Convert to gray scale

        // Start dilation section
        Mat element = Imgproc.getStructuringElement( Imgproc.MORPH_RECT, new Size( 9, 3 ));
        Imgproc.dilate( mat, mat, element );
        // End dilation section


        // Start erotion section
        Mat element2 = Imgproc.getStructuringElement( Imgproc.MORPH_RECT, new Size( 9, 3 ));
        Imgproc.erode( mat, mat, element2 );
        // End erotion section

        // Start sustraction
        //Core.absdiff(src, mat, dest);

        for (int j= 0; j<src.cols(); ++j)
        {
            for (int i=0; i<src.rows(); ++i)
            {
                byte valor = (byte)Math.abs(src.get(i,j)[0] - mat.get(i,j)[0]);
                byte[] b = new byte[4];
                b[0] = valor;
                b[1] = valor;
                b[2] = valor;
                b[3] = (byte)(255 & 0xFF);
                dest.put(i,j, b);
            }
        }

        // End sustraction


        //START sobel
        //Imgproc.Sobel(dest, dest, CvType.CV_8U, 0, 1); almost work
        Mat grad_x = new Mat();
        Mat abs_grad_x = new Mat();
        Imgproc.Sobel(dest, grad_x, CvType.CV_8U, 1, 0, 3, 1, Core.BORDER_DEFAULT);
        //Imgproc.Sobel(dest, grad_y, CvType.CV_16S, 0, 1, 3, 1, Core.BORDER_DEFAULT);

        Core.convertScaleAbs(grad_x, abs_grad_x);
        //Core.convertScaleAbs(grad_y, abs_grad_y);
        Core.addWeighted(abs_grad_x, 1, abs_grad_x, 0, 0, dest); // or? Core.addWeighted(abs_grad_x, 0.5, abs_grad_x, 0, 0, dest);
        //END sobel

        //Start Gaussian Blur
        Imgproc.GaussianBlur(dest, dest, new Size(5,5), 2);
        //End Gaussian Blur


        // Start dilation section
        Mat element3 = Imgproc.getStructuringElement( Imgproc.MORPH_RECT, new Size( 9, 3 ));
        Imgproc.dilate( dest, dest, element3 );
        // End dilation section


        // Start erotion section
        Mat element4 = Imgproc.getStructuringElement( Imgproc.MORPH_RECT, new Size( 9, 3 ));
        Imgproc.erode( dest, dest, element4 );
        // End erotion section





        //start OTSU's threshold
        Imgproc.cvtColor(dest, dest, Imgproc.COLOR_RGB2GRAY); //Convert to gray scale
        Imgproc.threshold(dest, dest, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU); //aca se cae
        //end OTSU's threshold



        //STEP 1: start Finding outlines in the binary image
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        //cv::findContours(inputImg, contours, CV_RETR_EXTERNAL, CV_CHAIN_APPROX_NONE);
        Mat temp = dest.clone();
        Imgproc.findContours(temp, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE );
        // end Finding outlines in the binary image





        //STEP 2: start selecting outlines
        List<RotatedRect> rects = new ArrayList<RotatedRect>();
        List<MatOfPoint> debugContourns = new ArrayList<MatOfPoint>();
        for(int i=0; i<contours.size(); ++i)//while (itc != contours.end())
        {
            MatOfPoint mop = contours.get(i);
            MatOfPoint2f mop2f = mopToMop2f(mop);
            RotatedRect mr = Imgproc.minAreaRect(mop2f); // SE CAE EN ESTA LINEA

            double area = Math.abs(Imgproc.contourArea(contours.get(i)));
            double bbArea=mr.size.width * mr.size.height;
            float ratio = (float)(area/bbArea);

            if( (ratio < 0.45) || (bbArea < 400) ){
                ;// do nothing
            }else{
                rects.add(mr);
                debugContourns.add(contours.get(i));
            }
        }
        // end selecting outlines
        drawContornsToMatInBitmap(src, contours, debugContourns, imgFp);


/*



        //STEP 3: loop
        for (int i=0; i<rects.size(); ++i)
        {
            if (i!=0)
                continue;

            //STEP 4:
            double dx = 0.15126 * rects.get(i).boundingRect().size().width;
            double dy = 0.625* rects.get(i).boundingRect().size().height;;
            int newWidth = (int)(rects.get(i).boundingRect().size().width + dx);
            int newHeight = (int)(rects.get(i).boundingRect().size().height + dy);

            //STEP 5:
            Mat uncropped = src.clone();
            Rect roi = new Rect(rects.get(i).boundingRect().x, rects.get(i).boundingRect().y, newWidth, newHeight);
            Mat cropped = new Mat(uncropped, roi);




            Mat finalMat3 = cropped;
            Bitmap bm3 = Bitmap.createBitmap(finalMat3.cols(), finalMat3.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(finalMat3, bm3);
            imgFp.setImageBitmap(bm3);



            //STEP 6:
            Imgproc.cvtColor(cropped, cropped, Imgproc.COLOR_RGB2GRAY); //Convert to gray scale
            Imgproc.threshold(cropped, cropped, 0, 255, Imgproc.THRESH_OTSU | Imgproc.THRESH_BINARY);

            //STEP 7:
            List<MatOfPoint> contours2 = new ArrayList<MatOfPoint>();
            Mat hierarchy2 = new Mat();
            Imgproc.findContours(cropped.clone(), contours2, hierarchy2, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE );





            //STEP 8:
            double maxArea = 0;
            int maxAreaIndex = 0;
            for(int j=0; j<contours2.size(); ++j)//while (itc != contours.end())
            {
                MatOfPoint mop = contours2.get(j);
                MatOfPoint2f mop2f = mopToMop2f(mop);
                //MatOfPoint2f mop2f = new MatOfPoint2f(mop);
                RotatedRect mr = Imgproc.minAreaRect(mop2f); // SE CAE EN ESTA LINEA

                double currentArea=mr.size.width * mr.size.height;
                if (currentArea > maxArea) {
                    maxArea = currentArea;
                    maxAreaIndex = j;
                }
            }


            //STEP 9:
            MatOfPoint mop2 = contours2.get(maxAreaIndex);
            MatOfPoint2f mop2f2 = mopToMop2f(mop2);
            //MatOfPoint2f mop2f = new MatOfPoint2f(mop);
            RotatedRect mr2 = Imgproc.minAreaRect(mop2f2); // SE CAE EN ESTA LINEA

            //STEP 10:
            //checks
            // Autos chilenos 36cm x 13cm Concentrarse en estos
            // Motos chilenas nuevas 14,5cm x 12cm.
            // Motos chilenas antiguas 14,5cm x 8cm.



            //STEP 11:
            double angle = rects.get(i).angle;
            Size rect_size = rects.get(i).size;
            if (rects.get(i).angle < -45.) {
                angle += 90.0;
                //swaping height and width
                double widthTemp = rect_size.width;
                rect_size.width = rect_size.height;
                rect_size.height = widthTemp;
            }
            // get the rotation matrix
            Mat matrix = Imgproc.getRotationMatrix2D(rects.get(i).center, angle, 1.0);
            // perform the affine transformation
            Mat rotated = new Mat();
            Mat cropped2 = new Mat();
            Mat precrop = src.clone();
            Imgproc.cvtColor(precrop, precrop, Imgproc.COLOR_RGB2GRAY); //Convert to gray scale
            Imgproc.warpAffine(precrop, rotated, matrix, precrop.size(), Imgproc.INTER_CUBIC);
            // crop the resulting image
            Imgproc.getRectSubPix(rotated, rect_size, rects.get(i).center, cropped2);

            // Paso 11 sin rotación.
            //Mat sinCortar = src.clone();
            //Rect roi2 = new Rect(rects.get(i).boundingRect().x + mr2.boundingRect().x,
            //        rects.get(i).boundingRect().y + mr2.boundingRect().y, (int)mr2.boundingRect().width, (int)mr2.boundingRect().height);
            //Mat cropped2 = new Mat(sinCortar, roi2);


            if (i==0) {
                // mostrar en pantalla
                Mat finalMat1 = cropped2;
                Bitmap bm1 = Bitmap.createBitmap(finalMat1.cols(), finalMat1.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(finalMat1, bm1);
                imgFp.setImageBitmap(bm1);
            }

            // Apply Gray Scale, Skew Correction, Fixed Size.
            //Mat resizeimage = new Mat();
            //Size sz = new Size(100,100);
            //Imgproc.resize( croppedimage, resizeimage, sz );
            //
            //
        }

*/

/*

        Mat finalMat = dest;
        Bitmap bm = Bitmap.createBitmap(finalMat.cols(), finalMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(finalMat, bm);
        imgFp.setImageBitmap(bm);

*/



        //savedFuncion();
    }

    private void drawContornsToMatInBitmap(Mat m, List<MatOfPoint> cs, List<MatOfPoint> csRefine, ImageView imgFp) {
        Mat finalMat = m.clone();
        //Imgproc.cvtColor(finalMat, finalMat, Imgproc.COLOR_GRAY2RGB); //USAR SI APLICA, SI SE CAE LA APP, Convert to RGB
        for (int cId = 0; cId < cs.size(); cId++) {
            Imgproc.drawContours(finalMat, cs, cId, new Scalar(0, 255, 0), 1);
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

    private MatOfPoint2f mopToMop2f(MatOfPoint mop) {
        return new MatOfPoint2f( mop.toArray() );
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
    Bitmap bmFinal;
}
