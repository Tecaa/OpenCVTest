package cl.bananaware.hwoc;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fergu on 06-08-2016.
 */
public class CandidatesFinder {
    public Mat OriginalEqualizedImage, OriginalImage, OriginalImageRealSize;
    public Mat CurrentImage;
    public Mat PreMultiDilationImage;
    public List<MatOfPoint> BlueCandidates, LastBlueCandidates;
    public List<RotatedRect> BlueCandidatesRR;
    public List<MatOfPoint> GreenCandidates, LastGreenCandidates;
    private double scale;

    public CandidatesFinder(Bitmap bm)
    {
        OriginalImage = new Mat();
        OriginalEqualizedImage = new Mat();
        PreMultiDilationImage = new Mat();
        Utils.bitmapToMat(bm, OriginalImage);
        OriginalImageRealSize = OriginalImage.clone();

        OriginalImage = Resize(OriginalImage);
        scale = OriginalImageRealSize.size().width/OriginalImage.size().width;
        CurrentImage = OriginalImage.clone();
        BlueCandidates = new ArrayList<MatOfPoint>();
        BlueCandidatesRR = new ArrayList<RotatedRect>();
        GreenCandidates = new ArrayList<MatOfPoint>();
        LastGreenCandidates = new ArrayList<MatOfPoint>();
        LastBlueCandidates = new ArrayList<MatOfPoint>();
    }

    private Mat Resize(Mat originalImage) {
        final int MAX_PIXELS = 1000;
        Size s = originalImage.size();
        Size newSize = new Size();
        double ratio = s.width/s.height;
        if (s.width>s.height &&  s.width > MAX_PIXELS)
        {

            newSize.width = MAX_PIXELS;
            newSize.height = newSize.width / ratio;
            Imgproc.resize(originalImage, originalImage, newSize);
        }
        if (s.height>s.width && s.height > MAX_PIXELS)
        {
            newSize.height = MAX_PIXELS;
            newSize.width = newSize.height * ratio;
            Imgproc.resize(originalImage, originalImage, newSize);
        }
        return originalImage;
    }

    public void ToGrayScale() {
        Imgproc.cvtColor(CurrentImage, CurrentImage, Imgproc.COLOR_RGB2GRAY);
    }

    public void EqualizeHistOriginalImage(boolean doit) {
        if (doit)
            Imgproc.equalizeHist( CurrentImage, CurrentImage);/// Apply Histogram Equalization
        OriginalEqualizedImage = CurrentImage.clone();
    }

    public void Dilate() {
        final float DILATATION_AMPLIFIER = 1.4f;
        Mat element = Imgproc.getStructuringElement( Imgproc.MORPH_RECT, new Size( 9*DILATATION_AMPLIFIER, 3*DILATATION_AMPLIFIER ));
        Imgproc.dilate( CurrentImage, CurrentImage, element);
    }

    public void Erode() {
        final float EROTION_AMPLIFIER= 1;
        Mat element = Imgproc.getStructuringElement( Imgproc.MORPH_RECT, new Size( 9*EROTION_AMPLIFIER, 3*EROTION_AMPLIFIER ));
        Imgproc.erode( CurrentImage, CurrentImage, element);
    }

    public void Substraction() {
        Mat temp = OriginalImage.clone();
        Imgproc.cvtColor(temp, temp, Imgproc.COLOR_RGB2GRAY);
        Core.absdiff(temp, CurrentImage, CurrentImage); // This function should replace this section. But it doesn't work!
        // NOW IT WORKS!!!
        /*
        Mat dest = OriginalImage.clone();
        byte[] b = new byte[4];
        for (int j= 0; j<OriginalImage.cols(); ++j)
        {
            for (int i=0; i<OriginalImage.rows(); ++i)
            {
                b[0] = (byte)Math.abs(OriginalImage.get(i,j)[0] - CurrentImage.get(i,j)[0]);
                //b[1] = valor;
                //b[2] = valor;
                //b[3] = (byte)(255 & 0xFF);
                dest.put(i,j, b);
            }
        }
        CurrentImage = dest;*/
    }

    public void Sobel() {
        //Imgproc.Sobel(dest, dest, CvType.CV_8U, 0, 1); almost work
        Mat grad_x = new Mat();
        Mat abs_grad_x = new Mat();
        Imgproc.Sobel(CurrentImage, grad_x, CvType.CV_8U, 1, 0, 3, 1, Core.BORDER_DEFAULT);
        //Imgproc.Sobel(dest, grad_y, CvType.CV_16S, 0, 1, 3, 1, Core.BORDER_DEFAULT);


        Core.convertScaleAbs(grad_x, abs_grad_x);
        //Core.convertScaleAbs(grad_y, abs_grad_y);
        Core.addWeighted(abs_grad_x, 1, abs_grad_x, 0, 0, CurrentImage); // or? Core.addWeighted(abs_grad_x, 0.5, abs_grad_x, 0, 0, dest);
    }

    public void GaussianBlur() {
        Imgproc.GaussianBlur(CurrentImage, PreMultiDilationImage, new Size(5,5), 2);
    }

    public void Dilate2(ImageSize imSize) {
        //evaluar hacer una interpolacion lineal o algo similar con el amplifier ya que falla en ex10 y ex2 (unno bien o el otro mal)
        //Poniendolo en 2 se corrige
        //float DILATATION_AMPLIFIER = 2.9f;
        Mat element = Imgproc.getStructuringElement( Imgproc.MORPH_RECT,
                new Size( 9*imSize.DilationAmplifier, 3*imSize.DilationAmplifier));
        Imgproc.dilate( PreMultiDilationImage, CurrentImage, element);
    }

    public void Erode2() {
        final float EROTATION_AMPLIFIER = 2.4F;
        Mat element = Imgproc.getStructuringElement( Imgproc.MORPH_RECT, new Size( 9*EROTATION_AMPLIFIER, 3 ));
        Imgproc.erode( CurrentImage, CurrentImage, element);
    }

    public void OtsusThreshold() {
        Imgproc.threshold(CurrentImage, CurrentImage, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
    }

    public void FindOutlines() {
        Mat hierarchy = new Mat();
        Mat temp = CurrentImage.clone();
        LastGreenCandidates = new ArrayList<MatOfPoint>();
        Imgproc.findContours(temp, LastGreenCandidates, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE );
        GreenCandidates.addAll(LastGreenCandidates);
    }

    public void OutlinesSelection() {
        List<MatOfPoint> contours = LastGreenCandidates;
        LastBlueCandidates = new ArrayList<MatOfPoint>();
        for(int i=0; i<contours.size(); ++i)
        {
            MatOfPoint mop = contours.get(i);
            MatOfPoint2f mop2f = mopToMop2f(mop);
            RotatedRect mr = Imgproc.minAreaRect(mop2f);

            double area = Math.abs(Imgproc.contourArea(contours.get(i)));
            double bbArea=mr.size.width * mr.size.height;
            float ratio = (float)(area/bbArea);

            if( (ratio < 0.45) || (bbArea*scale/2 < 400) ){
                ;// do nothing
            }else{
                BlueCandidatesRR.add(mr);
                LastBlueCandidates.add(contours.get(i));
            }
        }

        BlueCandidates.addAll(LastBlueCandidates);
    }
    private MatOfPoint2f mopToMop2f(MatOfPoint mop) {
        return new MatOfPoint2f( mop.toArray() );
    }

}
