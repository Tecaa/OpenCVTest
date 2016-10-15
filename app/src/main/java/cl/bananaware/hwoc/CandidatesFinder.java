package cl.bananaware.hwoc;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
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
    public static boolean PROBANDO = false;
    public Mat OriginalEqualizedImage, OriginalImage, OriginalImageRealSize;
    public Mat CurrentImage;
    public Mat PreMultiDilationImage;
    public List<MatOfPoint> BlueCandidates, LastBlueCandidates;
    //public List<RotatedRect> BlueCandidatesRR;
    public List<MatOfPoint> GreenCandidates, LastGreenCandidates;
    private double scale;

    public CandidatesFinder(Mat m_)
    {
        OriginalImage = m_;
        OriginalEqualizedImage = new Mat();
        PreMultiDilationImage = new Mat();
        //Utils.bitmapToMat(bm, OriginalImage);
        OriginalImageRealSize = new Mat();

        if (!PROBANDO)
            Imgproc.cvtColor(OriginalImage, OriginalImageRealSize, Imgproc.COLOR_RGB2GRAY); //Convert to gray scale

        final int MAX_PIXELS;
        if (ImageViewer.GOOD_SIZE)
            MAX_PIXELS = 1000;
        else
            MAX_PIXELS = 400;

        Resize(OriginalImage, MAX_PIXELS);
        TimeProfiler.CheckPoint(1.3);
        //OriginalImageRealSize = OriginalImage.clone(); //test
        scale = OriginalImageRealSize.size().width/OriginalImage.size().width;
        CurrentImage = OriginalImage.clone();

        BlueCandidates = new ArrayList<MatOfPoint>();
        //BlueCandidatesRR = new ArrayList<RotatedRect>();
        GreenCandidates = new ArrayList<MatOfPoint>();
        LastGreenCandidates = new ArrayList<MatOfPoint>();
        LastBlueCandidates = new ArrayList<MatOfPoint>();
        TimeProfiler.CheckPoint(1.6);
    }

    public static double Resize(Mat originalImage, int maxPixels) {

        Size s = originalImage.size();
        Size newSize = new Size();
        double ratio = s.width/s.height;
        if (s.width>s.height &&  s.width > maxPixels)
        {

            newSize.width = maxPixels;
            newSize.height = newSize.width / ratio;
            Imgproc.resize(originalImage, originalImage, newSize);
        }
        if (s.height>s.width && s.height > maxPixels)
        {
            newSize.height = maxPixels;
            newSize.width = newSize.height * ratio;
            Imgproc.resize(originalImage, originalImage, newSize);
        }
        return newSize.width/s.width;
    }

    public void ToGrayScale() {
        if (!PROBANDO)
            Imgproc.cvtColor(CurrentImage, CurrentImage, Imgproc.COLOR_RGB2GRAY);
    }

    public void EqualizeHistOriginalImage(boolean doit) {
        if (doit) {
            Imgproc.equalizeHist(CurrentImage, CurrentImage);/// Apply Histogram Equalization
        }

        OriginalEqualizedImage = CurrentImage.clone();
    }

    public void Dilate() {
        final float DILATATION_AMPLIFIER = 1.25f; //Estaba en 1.4
        Mat element = Imgproc.getStructuringElement( Imgproc.MORPH_RECT, new Size( 9*DILATATION_AMPLIFIER, 3*DILATATION_AMPLIFIER ));
        Imgproc.dilate( CurrentImage, CurrentImage, element);
    }

    public void Erode() {
        final float EROTION_AMPLIFIER= 1.9f; //estaba en 1. 1.5 funciona tb
        Mat element = Imgproc.getStructuringElement( Imgproc.MORPH_RECT, new Size( 9*EROTION_AMPLIFIER, 3*EROTION_AMPLIFIER ));
        Imgproc.erode( CurrentImage, CurrentImage, element);
    }

    public void Substraction() {
        //Mat temp = //OriginalImage.clone();
        if (!PROBANDO)
            Imgproc.cvtColor(OriginalImage, OriginalImage, Imgproc.COLOR_RGB2GRAY);
        Core.absdiff(OriginalImage, CurrentImage, CurrentImage); // This function should replace this section. But it doesn't work!
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
        Size size;
        int sigma;
        //if (ImageViewer.GOOD_SIZE) {
            size = new Size(5, 5);
            sigma = 2; //4?
        /*}
        else {
            size = new Size(3, 3);
            sigma = 1;
        }*/
        Imgproc.GaussianBlur(CurrentImage, PreMultiDilationImage, size, sigma);
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

        final float EROTATION_AMPLIFIER;
        if (ImageViewer.GOOD_SIZE)
            EROTATION_AMPLIFIER = 2.4F;
        else
            EROTATION_AMPLIFIER = 1.5F;
        Mat element = Imgproc.getStructuringElement( Imgproc.MORPH_RECT, new Size( 9*EROTATION_AMPLIFIER, 3 ));
        Imgproc.erode( CurrentImage, CurrentImage, element);
    }

    public void OtsusThreshold() {
        Imgproc.threshold(CurrentImage, CurrentImage, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
    }

    public void FindOutlines() {
        Mat hierarchy = new Mat();
        LastGreenCandidates = new ArrayList<MatOfPoint>();
        Imgproc.findContours(CurrentImage, LastGreenCandidates, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE );
        GreenCandidates.addAll(LastGreenCandidates);
    }

    final double MAX_AREA_PERC = 0.35;
    public List<RotatedRect> LastBlueCandidatesMAR;
    public void OutlinesSelection() {
        List<MatOfPoint> contours = LastGreenCandidates;
        LastBlueCandidates = new ArrayList<MatOfPoint>();
        LastBlueCandidatesMAR = new ArrayList<RotatedRect>();
        for(int i=0; i<contours.size(); ++i)
        {
            RotatedRect mr = Imgproc.minAreaRect(mopToMop2f(contours.get(i)));

            double area = Math.abs(Imgproc.contourArea(contours.get(i)));
            double rrArea=mr.size.width * mr.size.height;
            double bbArea = mr.boundingRect().area();
            float ratio = (float)(area/rrArea);
            double area_perc =rrArea /CurrentImage.size().area();

            //TODO: Poner un máximo de area?
            if( (ratio < 0.45) || (bbArea*scale/2 < 450)/*antes era 400*/ || area_perc > MAX_AREA_PERC ){
                ;// do nothing
            }else{
                //BlueCandidatesRR.add(mr);
                LastBlueCandidatesMAR.add(mr);
                LastBlueCandidates.add(contours.get(i));

            }
        }


    }
    private MatOfPoint2f mopToMop2f(MatOfPoint mop) {
        return new MatOfPoint2f( mop.toArray() );
    }

    public void OutlinesFilter() {

        quicksort(LastBlueCandidatesMAR, 0, LastBlueCandidatesMAR.size()-1, LastBlueCandidates);
        LastBlueCandidates = LastBlueCandidates.subList(0, Math.min(3, LastBlueCandidates.size()));
        LastBlueCandidatesMAR = LastBlueCandidatesMAR.subList(0, Math.min(3, LastBlueCandidatesMAR.size()));
    }
    public void quicksort(List<RotatedRect> A, int izq, int der, List<MatOfPoint> B) {

        if (der <= izq)
            return;
        double pivote= distanceToCenter(A.get(izq)); // tomamos primer elemento como pivote
        RotatedRect pivoteMat = A.get(izq);
        MatOfPoint pivoteMat2 = B.get(izq);
        int i=izq; // i realiza la búsqueda de izquierda a derecha
        int j=der; // j realiza la búsqueda de derecha a izquierda
        RotatedRect aux;
        MatOfPoint aux2;

        while(i<j){            // mientras no se crucen las búsquedas
            while(distanceToCenter(A.get(i))<=pivote && i<j) i++; // busca elemento mayor que pivote
            while(distanceToCenter(A.get(j))>pivote) j--;         // busca elemento menor que pivote
            if (i<j) {                      // si no se han cruzado
                aux= A.get(i);                  // los intercambia
                A.set(i, A.get(j));
                A.set(j, aux);

                aux2= B.get(i);                  // los intercambia
                B.set(i, B.get(j));
                B.set(j, aux2);
            }
        }
        A.set(izq, A.get(j)); // se coloca el pivote en su lugar de forma que tendremos
        A.set(j, pivoteMat); // los menores a su izquierda y los mayores a su derecha

        B.set(izq, B.get(j)); // se coloca el pivote en su lugar de forma que tendremos
        B.set(j, pivoteMat2); // los menores a su izquierda y los mayores a su derecha

        if(izq<j-1)
            quicksort(A,izq,j-1, B); // ordenamos subarray izquierdo
        if(j+1 <der)
            quicksort(A,j+1,der, B); // ordenamos subarray derecho
    }
    private double distanceToCenter (RotatedRect rr)
    {
        return Math.pow(rr.center.y-CurrentImage.height()/2,2) + Math.pow(rr.center.x-CurrentImage.width()/2,2);
    }

}
