package cl.bananaware.hwoc.ImageProcessing;

import android.support.annotation.BoolRes;
import android.support.v4.app.NotificationCompatSideChannelService;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import cl.bananaware.hwoc.ImageViewer;
import cl.bananaware.hwoc.R;

/**
 * Created by fergu on 07-08-2016.
 */
public class CandidateSelector {
    private static final boolean ROTATED_RECT = false;
    private double OriginalAngle;
    public RotatedRect CandidateRect, MinAreaRotatedRect;
    public Rect MinAreaRect;
    public Mat OriginalEqualizedImage, CurrentImage, OriginalImageRealSize;
    public Mat CroppedExtraBoundingBox;
    private double MinAreaRectAngle;
    private Size MinAreaRectSize;
    public int newWidth;
    public int newHeight;
    private int maxAreaCandidateProIndex;
    private float horizontalDilatationAmplifier;
    final double EXTRA = 1.1;
    final double EXTRA2 = 1.0;
    public List<MatOfPoint> GreenCandidatesPro;
    private double scale;
    public CandidateSelector(Mat OriginalEqualizedImage, Mat OriginalImageRealSize, RotatedRect candidate)
    {
        this.OriginalEqualizedImage = OriginalEqualizedImage;
        this.OriginalImageRealSize = OriginalImageRealSize;
        scale = OriginalImageRealSize.size().width/OriginalEqualizedImage.size().width;
        this.CandidateRect = candidate;
        GreenCandidatesPro = new ArrayList<MatOfPoint>();
        double angle  = candidate.angle;
        if (angle < -45.)
            angle += 90.0;
        this.OriginalAngle = angle;
    }
    int dx;
    int dy;
    public void CalculateBounds() {
        //FUNCIONA MEJOR CON DX =0 DY=0 :o?
        dx = Math.round(0.01f * (float)CandidateRect.boundingRect().size().width);//Math.round(0.02f * (float)CandidateRect.boundingRect().size().width);
        dy = Math.round(0.05f * (float)CandidateRect.boundingRect().size().height);;//Math.round(0.06f * (float)CandidateRect.boundingRect().size().height);;

        newWidth = (int)(CandidateRect.boundingRect().size().width + 2*dx);
        newHeight = (int)(CandidateRect.boundingRect().size().height + 2*dy);
    }

    public void TruncateBounds() {
        newWidth = Math.min(newWidth, OriginalEqualizedImage.width() - CandidateRect.boundingRect().x-dx);
        newHeight = Math.min(newHeight, OriginalEqualizedImage.height() - CandidateRect.boundingRect().y-dy);
/*
        // si excedimos el ancho de la imagen, lo truncamos
        if (OriginalEqualizedImage.width() < CandidateRect.boundingRect().x + newWidth)
            newWidth = newWidth -  ((CandidateRect.boundingRect().x + newWidth) - OriginalEqualizedImage.width());
        // si excedimos el alto de la imagen, lo truncamos
        if (OriginalEqualizedImage.height() < CandidateRect.boundingRect().y + newHeight)
            newHeight = newHeight -  ((CandidateRect.boundingRect().y + newHeight) - OriginalEqualizedImage.height());
            */
    }

    public double CropExtraRotatedRect(boolean realSize) {
        double factor = 0;
        if (realSize) {

            RotatedRect rr = CandidateRect.clone();
            rr.size.width = ((double)newWidth) * EXTRA*scale;
            rr.size.height = ((double)newHeight) * EXTRA*scale;
            rr.center.x = (rr.center.x - dx) *scale;
            rr.center.y = (rr.center.y - dy) *scale;

            if (rr.angle < -45.) {
                rr.angle += 90.0;
            }
            OriginalAngle = rr.angle;

            // perform the affine transformation
            Mat rotated = new Mat();
            CurrentImage = new Mat();
            Mat precrop = OriginalImageRealSize.clone();

            Mat matrix = Imgproc.getRotationMatrix2D(rr.center, /*MinAreaRectAngle + */rr.angle, 1.0);
            Imgproc.warpAffine(precrop, rotated, matrix, precrop.size(), Imgproc.INTER_CUBIC); // SLOW OPERATION
            // crop the resulting image
            Imgproc.getRectSubPix(rotated, rr.size, rr.center, CurrentImage);


            factor = ResizeImageMax(CurrentImage, 250);

            if (ImageViewer.SHOW_PROCESS_DEBUG)
                CroppedExtraBoundingBox = CurrentImage.clone();

        }
        else {


            RotatedRect rr = CandidateRect.clone();
            /*rr.size.width *= EXTRA;
            rr.size.height *= EXTRA;
            */
            rr.size.width = ((double)newWidth) * EXTRA;
            rr.size.height = ((double)newHeight) * EXTRA;

            //rr.center.x -= dx;
            //rr.center.y -= dy;

            if (rr.angle < -45.) {
                rr.angle += 90.0;

                /*double widthTemp = rr.size.width;
                rr.size.width = rr.size.height;
                rr.size.height = widthTemp;*/
            }
            OriginalAngle = rr.angle;
            Mat matrix = Imgproc.getRotationMatrix2D(rr.center, /*MinAreaRectAngle + */rr.angle, 1.0);
            // perform the affine transformation
            Mat rotated = new Mat();
            CurrentImage = new Mat();
            Mat precrop = OriginalEqualizedImage.clone();
            Imgproc.warpAffine(precrop, rotated, matrix, precrop.size(), Imgproc.INTER_CUBIC);
            // crop the resulting image
            Imgproc.getRectSubPix(rotated, rr.size, rr.center, CurrentImage);
            if (ImageViewer.SHOW_PROCESS_DEBUG)
                CroppedExtraBoundingBox = CurrentImage.clone();

        }

        /*
        CurrentImage = new Mat(OriginalEqualizedImage, roi).clone();
        Log.d("caida", "check 2.2.7");
        CroppedExtraBoundingBox = CurrentImage.clone();
        Log.d("caida", "check 2.2.8");*/
        return factor;
    }

    private double ResizeImageMax(Mat currentImage, int max) {
        double factor = GetResizeFactor(currentImage.size(), max);
        Log.d("factor", String.valueOf(factor));

        Imgproc.resize(currentImage, currentImage,
                new Size(currentImage.width()*factor,
                        currentImage.height()*factor));
        return factor;
    }

    private double ResizeImages(RotatedRect rr, Mat originalImageRealSize) {
        double factor = GetResizeFactor(rr.size, 250);
        Log.d("factor", String.valueOf(factor));
        rr.size.width *= factor;
        rr.size.height *= factor;
        rr.center.x *= factor;
        rr.center.y *= factor;

        Imgproc.resize(originalImageRealSize, originalImageRealSize,
                new Size(originalImageRealSize.width()*factor,
                        originalImageRealSize.height()*factor));
        return factor;
    }

    private double GetResizeFactor(Size s, int max) {
        if (s.width>s.height &&  s.width > max)
        {
            return max/s.width;
        }
        if (s.height>s.width && s.height > max)
        {
            return max/s.height;
        }
        return 1;
    }


    public void CropExtraBoundingBox(boolean realSizeCrop) {
        Rect roi = new Rect(Math.max(CandidateRect.boundingRect().x -dx, 0),
                Math.max(CandidateRect.boundingRect().y-dx,0), newWidth, newHeight);
        if (realSizeCrop) {
            Rect roiScaled = new Rect((int) (roi.x * scale),
                    (int) (roi.y * scale), (int) (roi.width * scale), (int) (roi.height * scale));
            if (roiScaled.x + roiScaled.width > OriginalImageRealSize.width()) {
                roiScaled.width = OriginalImageRealSize.width() - roiScaled.x;
            }
            if (roiScaled.y + roiScaled.height > OriginalImageRealSize.height())
                roiScaled.height = OriginalImageRealSize.height() - roiScaled.y;
            Log.d("caida", "check 2.2.6");
            Log.d("caida", roiScaled.x + " " + roiScaled.y + " " + roiScaled.width + " " + roiScaled.height + " | "
                    + (roiScaled.x + roiScaled.width) + "x" + (roiScaled.y + roiScaled.height) + " ||"
                    + OriginalImageRealSize.width() + " " + OriginalImageRealSize.height());
            CurrentImage = new Mat(OriginalImageRealSize, roiScaled);
        }
        else {
            if (roi.x + roi.width > OriginalEqualizedImage.width()) {
                roi.width = OriginalEqualizedImage.width() - roi.x;
            }
            if (roi.y + roi.height > OriginalEqualizedImage.height())
                roi.height = OriginalEqualizedImage.height() - roi.y;
            CurrentImage = new Mat(OriginalEqualizedImage, roi).clone();
        }
        Log.d("caida", "check 2.2.7");
        if (ImageViewer.SHOW_PROCESS_DEBUG)
            CroppedExtraBoundingBox = CurrentImage.clone();
        Log.d("caida", "check 2.2.8");
    }

    public void Sobel() {
        Mat grad_x2 = new Mat();
        Mat abs_grad_x2 = new Mat();
        //Imgproc.Sobel(CurrentImage, grad_x2, CvType.CV_8U, 1, 0, 3, 1, Core.BORDER_DEFAULT); //Antiguo Sobel
        if (ImageViewer.GOOD_SIZE)
            Imgproc.Sobel(CurrentImage, grad_x2, CvType.CV_8U, 1, 0, 3, 1, Core.BORDER_DEFAULT);
        else
            Imgproc.Sobel(CurrentImage, grad_x2, CvType.CV_8U, 1, 0, 1, 1, Core.BORDER_DEFAULT);

        Core.convertScaleAbs(grad_x2, abs_grad_x2);
        //Core.convertScaleAbs(grad_y, abs_grad_y);
        Core.addWeighted(abs_grad_x2, 1, abs_grad_x2, 0, 0, CurrentImage); // or? Core.addWeighted(abs_grad_x, 0.5, abs_grad_x, 0, 0, dest);
    }

    public void GaussianBlur() {

        int factor = GetGaussianBlurFactor(CurrentImage.size());
        Imgproc.GaussianBlur(CurrentImage, CurrentImage, new Size(factor,factor), 2);
    }

    private int GetGaussianBlurFactor(Size size) {

        return 1;
    }

    public void Dilate() {
        float DILATION_AMPLIFIER;//1.3f //1f; // EVALUAR HACERLO Dinámico
        horizontalDilatationAmplifier = calculateHorizontalAmplifier(CurrentImage.size());
        Mat element;
        if (ImageViewer.GOOD_SIZE) {
            DILATION_AMPLIFIER = 1.3f;
            element = Imgproc.getStructuringElement( Imgproc.MORPH_RECT,
                    new Size( Math.round(12*DILATION_AMPLIFIER*horizontalDilatationAmplifier), 3*DILATION_AMPLIFIER ));
        }
        else {
            DILATION_AMPLIFIER = 1f; //1.2 estaba, evaluar hacerlo dinámico
            element = Imgproc.getStructuringElement( Imgproc.MORPH_RECT,
                    new Size( Math.round(12*DILATION_AMPLIFIER*horizontalDilatationAmplifier), 2 /* estaba en 1 */ ));
        }


        Imgproc.dilate( CurrentImage, CurrentImage, element );
    }

    public void Erode() {
        float EROTION_AMPLIFIER;
        if (ImageViewer.GOOD_SIZE)
            EROTION_AMPLIFIER = 1;
        else
            EROTION_AMPLIFIER = 0.6f;
        Mat element = Imgproc.getStructuringElement( Imgproc.MORPH_RECT,
                new Size( Math.round(9*EROTION_AMPLIFIER*horizontalDilatationAmplifier), 3*EROTION_AMPLIFIER ));
        Imgproc.erode( CurrentImage, CurrentImage, element);
    }

    private float calculateHorizontalAmplifier(Size size) {
        // CODIGO EN PRUEBA, QUIZAS NO FUNCIONA Y NO TIENE REAL RELACION CON EL TAMAÑO DE LA IMAGEN
        final float MIN_VALUE = 0.3f;
        final float MAX_VALUE = 10f;
        float val = 1;
        if (size.width > 150)
            val = (float)(size.width* 13.0/744.0-1663.0/744.0);

        Log.d("test", "WIDTH: " + String.valueOf(size.width));
        return val;//Math.max(val, MIN_VALUE);
    }


    public void OtsusThreshold() {
//        Imgproc.cvtColor(CurrentImage, CurrentImage, Imgproc.COLOR_RGB2GRAY); //Convert to gray scale
        Imgproc.threshold(CurrentImage, CurrentImage, 0, 255, Imgproc.THRESH_OTSU | Imgproc.THRESH_BINARY);
    }

    public void FindOutlines() {
        Mat hierarchy = new Mat();
        Imgproc.findContours(CurrentImage.clone(), GreenCandidatesPro, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE );
    }

    public void FindMaxAreaCandidatePro() {
        double maxArea = 0;
        maxAreaCandidateProIndex = 0;
        for(int j=0; j<GreenCandidatesPro.size(); ++j)
        {
            MatOfPoint mop = GreenCandidatesPro.get(j);
            MatOfPoint2f mop2f = mopToMop2f(mop);
            RotatedRect mr = Imgproc.minAreaRect(mop2f);

            double currentArea=mr.size.width * mr.size.height;
            if (currentArea > maxArea) {
                maxArea = currentArea;
                maxAreaCandidateProIndex = j;
            }
        }
    }
    public static MatOfPoint2f mopToMop2f(MatOfPoint mop) {
        return new MatOfPoint2f( mop.toArray() );
    }

    public boolean FindMinAreaRectInMaxArea() {
        if (GreenCandidatesPro.size() == 0)
            return false;

        if (ROTATED_RECT) {
            MatOfPoint mop2 = GreenCandidatesPro.get(maxAreaCandidateProIndex);
            MatOfPoint2f mop2f2 = mopToMop2f(mop2);
            MinAreaRotatedRect = Imgproc.minAreaRect(mop2f2);
        }
        else
        {
            MatOfPoint mop2 = GreenCandidatesPro.get(maxAreaCandidateProIndex);
            MinAreaRect = Imgproc.boundingRect(mop2);
        }
        return true;
    }

    final double MIN_AREA = 420; //950 is a good value
    final double MAX_PERCENTAJE_AREA = 0.30;
    public CheckError DoChecks() {
        // Autos chilenos 36cm x 13cm Concentrarse en éstos
        // Motos chilenas nuevas 14,5cm x 12cm.
        // Motos chilenas antiguas 14,5cm x 8cm.
        Size rectSize;
        if (ROTATED_RECT) {
            MinAreaRectAngle = MinAreaRotatedRect.angle;
            MinAreaRectSize = MinAreaRotatedRect.size;
            if (MinAreaRotatedRect.angle < -45.) {
                MinAreaRectAngle += 90.0;
                //swaping height and width
                double widthTemp = MinAreaRectSize.width;
                MinAreaRectSize.width = MinAreaRectSize.height;
                MinAreaRectSize.height = widthTemp;
            }
            rectSize.width = MinAreaRectSize.width;
            rectSize.height = MinAreaRectSize.height;
        }
        else
        {
            rectSize= MinAreaRect.size();
        }

        double imageRatio = rectSize.width / rectSize.height;//Math.max(mr2.size.width,mr2.size.height)/Math.min(mr2.size.width,mr2.size.height);
        final double OFFICIAL_RATIO = 36f/13f;
        final double MIN_RATIO = 2.1f;// funciona 2.2f;
        final double MAX_RATIO = 5.7f;
        CheckError checkError = null;

        if (imageRatio < MIN_RATIO)
            checkError = CheckError.MinRatio;
        if (imageRatio > MAX_RATIO)
            checkError = CheckError.MaxRatio;

        if (checkError == null)
        {
            // AREAS Y PORCENTAJE DE AREAS
            double area = rectSize.width*rectSize.height;
            double maxAreaImage = OriginalEqualizedImage.size().width * OriginalEqualizedImage.size().height;
            double percentajeImage = area/maxAreaImage;
            if (area*scale < MIN_AREA)
                checkError = CheckError.MinArea;
            if (percentajeImage > MAX_PERCENTAJE_AREA)
                checkError = CheckError.MaxPercentajeArea;
        }
        return checkError;
    }
    public void CropMinRotatedRect(boolean realSizeCrop, double factor) {
        // get the rotation matrix
        if (realSizeCrop) {

            if (factor != 0) {
                MinAreaRotatedRect.center.x *= scale * EXTRA * factor ;
                MinAreaRotatedRect.center.y *= scale * EXTRA * factor ;
                MinAreaRotatedRect.size.height *= scale * factor;
                MinAreaRotatedRect.size.width *= scale * factor;
            }
        }
        Mat matrix = Imgproc.getRotationMatrix2D(MinAreaRotatedRect.center, MinAreaRectAngle  /*this.OriginalAngle */, 1.0);
        // perform the affine transformation
        Mat rotated = new Mat();
        Mat precrop = CurrentImage;
        //Mat precrop = CroppedExtraBoundingBox.clone();
        Imgproc.warpAffine(precrop, rotated, matrix, precrop.size(), Imgproc.INTER_CUBIC);
        // crop the resulting image
        Imgproc.getRectSubPix(rotated, MinAreaRectSize, MinAreaRotatedRect.center, CurrentImage);
    }

    private void CropRect(boolean realSizeCrop, double factor) {

        if (realSizeCrop) {

            if (factor != 0) {
                MinAreaRect.x *= (scale * EXTRA * factor) ;/// 2.0;
                MinAreaRect.y *= (scale * EXTRA * factor) ;/// 2.0;
                MinAreaRect.height *= scale * factor;
                MinAreaRect.width *= scale * factor;

/*
                if (MinAreaRect.width / MinAreaRect.height > 0)
                    MinAreaRect.width *= EXTRA2;
                else
                    MinAreaRect.height *= EXTRA2;*/

                MinAreaRect.x = Math.max(0, MinAreaRect.x);
                MinAreaRect.width = Math.min(CurrentImage.width() - MinAreaRect.x, MinAreaRect.width);
                MinAreaRect.y = Math.max(0, MinAreaRect.y);
                MinAreaRect.height = Math.min(CurrentImage.height() - MinAreaRect.y, MinAreaRect.height);



        }}
        CurrentImage = CurrentImage.submat(MinAreaRect);
    }


    public boolean PercentajeAreaCandidateCheck() {
        double area1 = OriginalEqualizedImage.size().area();
        double area2 = newHeight * newWidth;
        double perc = area2/area1;
        return area2*scale >= MIN_AREA && perc<=MAX_PERCENTAJE_AREA;
    }

    public Mat GetFinalImage(boolean realSizeCrop) {
        if (realSizeCrop)
        {
            double factor = CropExtraRotatedRect(true);
            if (ROTATED_RECT)
                CropMinRotatedRect(true, factor);
            else
                CropRect(true, factor);
        }
        return CurrentImage;//o CurrentImage.clone()?
    }




    public void Equalize() {
        Imgproc.equalizeHist( CurrentImage, CurrentImage);/// Apply Histogram Equalization
    }

    public enum CheckError {
        MinRatio(R.drawable.do_checks_min_rate), MinArea(R.drawable.do_checks_min_area),
        MaxPercentajeArea(R.drawable.do_checks_max_percentaje_area), MaxRatio(R.drawable.do_checks_max_rate);

        private final int value;
        private CheckError(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
