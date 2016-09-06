package cl.bananaware.hwoc;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fergu on 12-08-2016.
 */
public class CharacterSeparator {
    public Mat CurrentImage;
    public Mat CleanedImage;
    public Mat ImageWithContourns;
    public Mat OriginalImage;
    //private Mat VerticalHistogram;
    private List<MatOfPoint> contourns = new ArrayList<MatOfPoint>();
    private List<MatOfPoint> finalsContourns = new ArrayList<MatOfPoint>();
    private List<Double> correctLeftPositions = new ArrayList<Double>();
    private List<Double> correctRightPositions = new ArrayList<Double>();
    public CharacterSeparator(Mat mat) {
        CurrentImage = mat;
        OriginalImage = mat.clone();
        ImageWithContourns = new Mat();
        CleanedImage = new Mat();
      //  VerticalHistogram = new Mat(new Size(1, CurrentImage.width()), CvType.CV_32SC1);
        correctLeftPositions.add(0.00);
        correctLeftPositions.add(0.14);
        correctLeftPositions.add(0.36);
        correctLeftPositions.add(0.50);
        correctLeftPositions.add(0.74);
        correctLeftPositions.add(0.87);
        correctRightPositions.add(0.12);
        correctRightPositions.add(0.27);
        correctRightPositions.add(0.47);
        correctRightPositions.add(0.65);
        correctRightPositions.add(0.84);
        correctRightPositions.add(1.00);
    }

    public void AdaptiveThreshold() {
        Imgproc.adaptiveThreshold(CurrentImage, CurrentImage, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C,
                Imgproc.THRESH_BINARY, 13, 5);
    }

    public void FindCountourns()
    {
        Core.bitwise_not(CurrentImage, CurrentImage);

        Mat hierarchy = new Mat();
        Imgproc.findContours(CurrentImage.clone(),contourns,hierarchy,Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);


        if (ImageViewer.SHOW_PROCESS_DEBUG) {
            Imgproc.cvtColor(CurrentImage, ImageWithContourns, Imgproc.COLOR_GRAY2RGB);
            for (int cId = 0; cId < contourns.size(); cId++) {
                Imgproc.drawContours(ImageWithContourns, contourns, cId, new Scalar(0, 255, 0), 1);
            }
        }
    }

    public boolean FilterCountourns() {

        int min_x = CurrentImage.width();
        int max_x = 0;
        int min_y = CurrentImage.height();
        int max_y = 0;

        final boolean REMOVE_OUTSIDE = true;
        Mat maskInsideContour = Mat.zeros(CurrentImage.size(), CvType.CV_8UC1);
        for (int i=0; i<contourns.size(); ++i)
        {
            Rect boundingRect = Imgproc.boundingRect(contourns.get(i));
            if (areaCheck(boundingRect) && aspectRatioCheck(boundingRect))
            {
                finalsContourns.add(contourns.get(i));
                if (min_x > boundingRect.x)
                    min_x = boundingRect.x;
                if (max_x < boundingRect.x + boundingRect.width)
                    max_x = boundingRect.x + boundingRect.width;
                if (min_y > boundingRect.y)
                    min_y = boundingRect.y;
                if (max_y < boundingRect.y + boundingRect.height)
                    max_y = boundingRect.y + boundingRect.height;

            }

            if (REMOVE_OUTSIDE) {
                if (boundingRect.area() >= 0.03 * CurrentImage.size().area()) {
                    Imgproc.drawContours(maskInsideContour, contourns, i,
                            new Scalar(255), -1); // This is a OpenCV function
                }
            }

        }
        // REMOVER LO QUE ESTÁ SOBRE LAS ROJAS Y BAJO ELLAS
        // ADEMAS REMOVER LO QUE ES MUY  PEQUEÑO (CIRCULOS INTERIORES PARA EVITAR ERRORES)
        if (REMOVE_OUTSIDE) {


            // 'contours' is the vector of contours returned from findContours
            // 'image' is the image you are masking

            // Create mask for region within contour
            //Mat maskInsideContour = Mat.zeros(CurrentImage.size(), CvType.CV_8UC1);

            //for (int idxOfContour = 0; idxOfContour < contourns.size(); idxOfContour++) {
            // Change to the index of the contour you wish to draw


            //}

            //Ajustamos debido a que es un borde interior, y ahora necesitamos el borde exterior del contorno.
            final int LIMIT = 1;
            min_y -= LIMIT;
            min_y = Math.max(min_y,0);
            min_x -= LIMIT;
            min_x = Math.max(min_x,0);
            max_y += LIMIT;
            max_y = Math.min(max_y,CurrentImage.height());
            max_x += LIMIT;
            max_x = Math.min(max_x,CurrentImage.width());
            if (0 != min_y)
                Imgproc.rectangle(CurrentImage, new Point(0,0), new Point(CurrentImage.width(), min_y), new Scalar(0,0,0), -1);
            if (0 != min_x)
                Imgproc.rectangle(CurrentImage, new Point(0,0), new Point(min_x, CurrentImage.height()), new Scalar(0,0,0), -1);
            if (CurrentImage.height() != max_y)
                Imgproc.rectangle(CurrentImage, new Point(0,max_y), new Point(CurrentImage.width(), CurrentImage.height()), new Scalar(0,0,0), -1);
            if (CurrentImage.width() != max_x)
                Imgproc.rectangle(CurrentImage, new Point(max_x,0), new Point(CurrentImage.width(), CurrentImage.height()), new Scalar(0,0,0), -1);

            // At this point, maskInsideContour has value of 255 for pixels
            // within the contour and value of 0 for those not in contour.

            Mat maskedImage = new Mat(CurrentImage.size(), CvType.CV_8UC1);  // Assuming you have 3 channel image

            // Do one of the two following lines:
            maskedImage.setTo(new Scalar(0, 0, 0));  // Set all pixels to (180, 180, 180)
            CurrentImage.copyTo(maskedImage, maskInsideContour);  // Copy pixels within contour to maskedImage.
            CurrentImage = maskedImage;
            // Now regions outside the contour in maskedImage is set to (180, 180, 180) and region
            // within it is set to the value of the pixels in the contour.
            CleanedImage = maskedImage;
        }
        if (ImageViewer.SHOW_PROCESS_DEBUG) {
            for (int cId = 0; cId < finalsContourns.size(); cId++) {
                Imgproc.drawContours(ImageWithContourns, finalsContourns, cId, new Scalar(255, 0, 0), 1);
            }
        }
        return finalsContourns.size() != 0;

    }
    int charsPlateLength;
    int InitialPixelX;
    public void CalculatePlateLength()
    {
        int finalPixelX = 0;
        InitialPixelX = Integer.MAX_VALUE;
        final double ERROR = 0.04;
        List<HeightCandidate> hcs = new ArrayList<HeightCandidate>();
        for (int i=0; i<finalsContourns.size(); ++i)
        {
            Rect r = Imgproc.boundingRect(finalsContourns.get(i));
            HeightCandidate hc = new HeightCandidate(r.y, r.y +r.height);

            boolean added = false;
            for (int k=0; k<hcs.size(); ++k) {
                if (Math.abs(hcs.get(k).Start - r.y)/r.height <= ERROR && Math.abs(hcs.get(k).End - r.y + r.width)/r.height <= ERROR) {
                    ++hcs.get(k).Counts;
                    added = true;
                }

            }
            if (!added)
                hcs.add(hc);

            if (InitialPixelX > r.x)
                InitialPixelX = r.x;
            if (finalPixelX < r.x + r.width)
                finalPixelX = r.x+r.width;
        }
        charsPlateLength = finalPixelX- InitialPixelX;
        int moda=0;
        int modaIndex=0;
        for (int k=0; k<hcs.size(); ++k) {
            if (hcs.get(k).Counts > moda) {
                moda =  hcs.get(k).Counts;
                modaIndex = k;
            }
        }
        InitialPixelY = hcs.get(modaIndex).Start;
        HeightChars = hcs.get(modaIndex).End - InitialPixelY;
    }
    int InitialPixelY;
    int HeightChars;
    List<CharHorizontalPosition> positions = new ArrayList<CharHorizontalPosition>();
    public void CalculateCharsPositions()
    {
        Log.d("posi", "largo=" + charsPlateLength);
        for (int i=0; i<6; ++i)
        {
            CharHorizontalPosition chp = new CharHorizontalPosition();
            int left = ExistCorrectPositionLeft(i);
            int right = ExistCorrectPositionRight(i);
            if (left != -1)
                chp.Start = left;
            else
                chp.Start = (int)(correctLeftPositions.get(i)*charsPlateLength + InitialPixelX);
            if (right != -1)
                chp.End = right;
            else
                chp.End = (int)(correctRightPositions.get(i)*charsPlateLength + InitialPixelX);

            positions.add(chp);
        }

    }
    final double ERROR_PERCENTAJE = 0.04;

    private int ExistCorrectPositionRight(int j) {
        for (int i=0; i<finalsContourns.size(); ++i)
        {
            Rect r = Imgproc.boundingRect(finalsContourns.get(i));
            float pos = (float)(r.x - InitialPixelX + r.width)/(float)(charsPlateLength);
            //Log.d("posi", "i="+i+" " +pos+ "]");
            if (Math.abs(pos - correctRightPositions.get(j)) <= ERROR_PERCENTAJE)
                return Math.round(pos * charsPlateLength) + InitialPixelX;
        }
        return -1;
    }

    private int ExistCorrectPositionLeft(int j) {
        for (int i=0; i<finalsContourns.size(); ++i)
        {
            Rect r = Imgproc.boundingRect(finalsContourns.get(i));
            float pos = (float) (r.x - InitialPixelX)/(float)charsPlateLength;
            //Log.d("posi", "i="+i + " ["+pos);
            if (Math.abs(pos - correctLeftPositions.get(j)) <= ERROR_PERCENTAJE)
                return Math.round(pos * charsPlateLength) + InitialPixelX;
        }
        return -1;
    }
/*
    public void CalculateHistrograms()
    {
        Core.reduce(CurrentImage, VerticalHistogram, 1, Core.REDUCE_SUM, CvType.CV_32SC1); //Is good the orientation?
        CurrentImage = VerticalHistogram;

    }*/
    private boolean aspectRatioCheck(Rect br) {
        final double MIN_RATIO = 0.3;// estaba en 0.3
        final double MAX_RATIO = 2.4;

        double ratio = (double)br.width / (double)br.height;
        if (ratio > MIN_RATIO && ratio < MAX_RATIO)
            return true;
        else
            return false;
    }

    private boolean areaCheck(Rect br) {
        final double MAX_AREA_PERCENTAJE = 0.2;
        final double MIN_AREA_PERCENTAJE = 0.03;
        double areaPercentaje = br.area() / CurrentImage.size().area();
        if (areaPercentaje > MIN_AREA_PERCENTAJE && areaPercentaje < MAX_AREA_PERCENTAJE)
            return true;
        else
            return false;
    }

    List<Mat> CroppedChars = new ArrayList<Mat>();
    public void CropChars() {
        final float PERCENTAJE_PLATE_X = 0.018F; //0.018 funciona bien
        final float PERCENTAJE_PLATE_Y = 0.035F;
        for(int i=0; i<positions.size(); ++i) {
            int extra_x = Math.max(Math.round(charsPlateLength*PERCENTAJE_PLATE_X), 1);
            int extra_y = Math.max(Math.round(charsPlateLength*PERCENTAJE_PLATE_Y), 1);
            int xStart = Math.max(positions.get(i).Start-extra_x, 0);
            int yStart = Math.max(InitialPixelY-extra_y,0);
            int xWidth = Math.min(positions.get(i).End - positions.get(i).Start+2*extra_x,CurrentImage.width()-xStart);
            int yWidth = Math.min(HeightChars+2*extra_y, CurrentImage.height() - yStart);
            Rect roi = new Rect(xStart, yStart, xWidth, yWidth);
            CroppedChars.add(new Mat(CurrentImage, roi)); //black and white image
            //CroppedChars.add(new Mat(OriginalImage.clone(), roi));  // gray scale image
        }
    }
    public void CropAll() {
        int extra = Math.max(Math.round(charsPlateLength*0.0144f), 1);
        int xStart = 0;//Math.max(positions.get(i).Start-extra, 0);
        int yStart = Math.max(InitialPixelY-extra,0);
        int xWidth = CurrentImage.width();//Math.min(positions.get(i).End - positions.get(i).Start+2*extra,CurrentImage.width()-xStart);
        int yWidth = Math.min(HeightChars+2*extra, CurrentImage.height() - yStart);
        Rect roi = new Rect(xStart, yStart, xWidth, yWidth);
        //CroppedChars.add(new Mat(CurrentImage.clone(), roi)); //black and white image
        CroppedChars.add(new Mat(OriginalImage.clone(), roi));  // gray scale image
    }
}
