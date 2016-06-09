package cl.bananaware.hwoc;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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


        int image = R.mipmap.example;
        ImageView imgFp = (ImageView) findViewById(R.id.imageView);

        Mat src = new Mat();
        Utils.bitmapToMat(BitmapFactory.decodeResource(getResources(), image) , src);





        Mat mat = new Mat();
        Mat dest = src.clone();
        mat = src.clone();
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY); //Convert to gray scale

        // Start dilation section
        Mat element = Imgproc.getStructuringElement( Imgproc.MORPH_RECT, new Size( 9, 9 ));
        Imgproc.dilate( mat, mat, element );
        // End dilation section


        // Start erotion section
        Mat element2 = Imgproc.getStructuringElement( Imgproc.MORPH_RECT, new Size( 9, 9 ));
        Imgproc.erode( mat, mat, element2 );
        // End erotion section



        // Start sustraction
        //Core.absdiff(src, mat, dest);

/*
        for (int i= 0; i<src.cols(); ++i)
        {
            for (int j=0; j<src.rows(); ++j)
            {
                byte valor = (byte)Math.abs(src.get(i,j)[0] - mat.get(i,j)[0]);
                Log.e("q", String.valueOf(valor));
                byte[] b = new byte[4];
                b[0] = valor;
                b[1] = valor;
                b[2] = valor;
                b[3] = (byte)(255 & 0xFF);
                dest.put(i,j, b);
            }
        }*/

        Log.e("q", String.valueOf(src.cols()) +" " + String.valueOf(src.rows()) );
        // End sustraction




        Bitmap bm = Bitmap.createBitmap(dest.cols(), dest.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dest, bm);
        imgFp.setImageBitmap(bm);


        //savedFuncion();
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
