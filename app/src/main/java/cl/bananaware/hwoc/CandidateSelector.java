package cl.bananaware.hwoc;

import android.util.Log;
import android.webkit.WebStorage;

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

/**
 * Created by fergu on 07-08-2016.
 */
public class CandidateSelector {
    public RotatedRect CandidateRect, MinAreaRect;
    public Mat OriginalImage, CurrentImage;
    public Mat CroppedExtraBoundingBox;
    private double MinAreaRectAngle;
    private Size MinAreaRectSize;
    private int newWidth;
    private int newHeight;
    private int maxAreaCandidateProIndex;
    private float horizontalDilatationAmplifier;
    public List<MatOfPoint> GreenCandidatesPro;

    public CandidateSelector(Mat OriginalImage, RotatedRect candidate)
    {
        this.OriginalImage = OriginalImage;
        this.CandidateRect = candidate;
        GreenCandidatesPro = new ArrayList<MatOfPoint>();
    }

    public void CalculateBounds() {

        double dx = 0.15126 * CandidateRect.boundingRect().size().width;
        double dy = 0.625* CandidateRect.boundingRect().size().height;;
        newWidth = (int)(CandidateRect.boundingRect().size().width + dx);
        newHeight = (int)(CandidateRect.boundingRect().size().height + dy);
    }

    public void TruncateBounds() {
        // si excedimos el ancho de la imagen, lo truncamos
        if (OriginalImage.width() < CandidateRect.boundingRect().x + newWidth)
            newWidth = newWidth -  ((CandidateRect.boundingRect().x + newWidth) - OriginalImage.width());
        // si excedimos el alto de la imagen, lo truncamos
        if (OriginalImage.height() < CandidateRect.boundingRect().y + newHeight)
            newHeight = newHeight -  ((CandidateRect.boundingRect().y + newHeight) - OriginalImage.height());
    }

    public void CropExtraBoundingBox() {
        Rect roi = new Rect(Math.max(CandidateRect.boundingRect().x, 0),
                Math.max(CandidateRect.boundingRect().y,0), newWidth, newHeight);
        CurrentImage = new Mat(OriginalImage.clone(), roi);
        CroppedExtraBoundingBox = CurrentImage.clone();
    }

    public void Sobel() {
        Mat grad_x2 = new Mat();
        Mat abs_grad_x2 = new Mat();
        Imgproc.Sobel(CurrentImage, grad_x2, CvType.CV_8U, 1, 0, 3, 1, Core.BORDER_DEFAULT);

        Core.convertScaleAbs(grad_x2, abs_grad_x2);
        //Core.convertScaleAbs(grad_y, abs_grad_y);
        Core.addWeighted(abs_grad_x2, 1, abs_grad_x2, 0, 0, CurrentImage); // or? Core.addWeighted(abs_grad_x, 0.5, abs_grad_x, 0, 0, dest);
    }

    public void GaussianBlur() {
        Imgproc.GaussianBlur(CurrentImage, CurrentImage, new Size(5,5), 2);
    }

    public void Dilate(boolean printWidth) {
        float DILATION_AMPLIFIER = 1f;
        horizontalDilatationAmplifier = calculateHorizontalAmplifier(CurrentImage.size(), printWidth);
        Mat element = Imgproc.getStructuringElement( Imgproc.MORPH_RECT,
                new Size( Math.round(9*DILATION_AMPLIFIER*horizontalDilatationAmplifier), 3*DILATION_AMPLIFIER ));
        Imgproc.dilate( CurrentImage, CurrentImage, element );
    }

    public void Erode() {
        float EROTION_AMPLIFIER= 1f;
        Mat element = Imgproc.getStructuringElement( Imgproc.MORPH_RECT,
                new Size( Math.round(9*EROTION_AMPLIFIER*horizontalDilatationAmplifier), 3*EROTION_AMPLIFIER ));
        Imgproc.erode( CurrentImage, CurrentImage, element);
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


    public void OtsusThreshold() {
        Imgproc.cvtColor(CurrentImage, CurrentImage, Imgproc.COLOR_RGB2GRAY); //Convert to gray scale
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
    private MatOfPoint2f mopToMop2f(MatOfPoint mop) {
        return new MatOfPoint2f( mop.toArray() );
    }

    public void FindMinAreaRectInMaxArea() {
        MatOfPoint mop2 = GreenCandidatesPro.get(maxAreaCandidateProIndex);
        MatOfPoint2f mop2f2 = mopToMop2f(mop2);
        MinAreaRect = Imgproc.minAreaRect(mop2f2);
    }

    public boolean DoChecks() {
        // Autos chilenos 36cm x 13cm Concentrarse en éstos
        // Motos chilenas nuevas 14,5cm x 12cm.
        // Motos chilenas antiguas 14,5cm x 8cm.
        MinAreaRectAngle = MinAreaRect.angle;
        MinAreaRectSize = MinAreaRect.size;
        if (MinAreaRect.angle < -45.) {
            MinAreaRectAngle += 90.0;
            //swaping height and width
            double widthTemp = MinAreaRectSize.width;
            MinAreaRectSize.width = MinAreaRectSize.height;
            MinAreaRectSize.height = widthTemp;
        }

        double imageRatio = MinAreaRectSize.width / MinAreaRectSize.height;//Math.max(mr2.size.width,mr2.size.height)/Math.min(mr2.size.width,mr2.size.height);
        final double OFFICIAL_RATIO = 36f/13f;
        final double MIN_RATIO = 2.2f;
        final double MAX_RATIO = 5.7f;
        boolean ratioCorrect = false;
        if (imageRatio >= MIN_RATIO && imageRatio <= MAX_RATIO)
            ratioCorrect = true;

        boolean passChecks = false;
        if (ratioCorrect)
        {
            // AREAS Y PORCENTAJE DE AREAS
            double area = MinAreaRectSize.width*MinAreaRectSize.height;
            double maxAreaImage = OriginalImage.size().width * OriginalImage.size().height;
            double percentajeImage = area/maxAreaImage;
            final double MIN_AREA = 950;
            final double MAX_PERCENTAJE_AREA = 0.15;
            if (area >= MIN_AREA && percentajeImage <= MAX_PERCENTAJE_AREA)
                passChecks = true;
        }
        return passChecks;
    }

    public void CropMinRotatedRect() {
        // get the rotation matrix
        Mat matrix = Imgproc.getRotationMatrix2D(MinAreaRect.center, MinAreaRectAngle, 1.0);
        // perform the affine transformation
        Mat rotated = new Mat();
        Mat cropped2 = new Mat();
        Mat precrop = CroppedExtraBoundingBox.clone();
        Imgproc.cvtColor(precrop, precrop, Imgproc.COLOR_RGB2GRAY); //Convert to gray scale
        Imgproc.warpAffine(precrop, rotated, matrix, precrop.size(), Imgproc.INTER_CUBIC);
        // crop the resulting image
        Imgproc.getRectSubPix(rotated, MinAreaRectSize, MinAreaRect.center, CurrentImage);
    }
}
