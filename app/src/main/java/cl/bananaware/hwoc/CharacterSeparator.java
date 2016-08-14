package cl.bananaware.hwoc;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
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
    public Mat ImageWithContourns;
    private Mat VerticalHistogram;
    private List<MatOfPoint> contourns = new ArrayList<MatOfPoint>();
    private List<MatOfPoint> finalsContourns = new ArrayList<MatOfPoint>();
    private List<Double> correctLeftPositions = new ArrayList<Double>();
    private List<Double> correctRightPositions = new ArrayList<Double>();
    public CharacterSeparator(Mat mat) {
        CurrentImage = mat;
        ImageWithContourns = new Mat();
        VerticalHistogram = new Mat(new Size(1, CurrentImage.width()), CvType.CV_32SC1);
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


        Imgproc.cvtColor(CurrentImage, ImageWithContourns, Imgproc.COLOR_GRAY2RGB);
        for (int cId = 0; cId < contourns.size(); cId++) {
            Imgproc.drawContours(ImageWithContourns, contourns, cId, new Scalar(0, 255, 0), 1);
        }

    }

    public boolean FilterCountourns() {

        for (int i=0; i<contourns.size(); ++i)
        {
            Rect boundingRect = Imgproc.boundingRect(contourns.get(i));
            if (areaCheck(boundingRect) && aspectRatioCheck(boundingRect))
            {
                finalsContourns.add(contourns.get(i));
            }
                
        }

        for (int cId = 0; cId < finalsContourns.size(); cId++) {
            Imgproc.drawContours(ImageWithContourns, finalsContourns, cId, new Scalar(255, 0, 0), 1);
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
            Log.d("posi", "i="+i+" " +pos+ "]");
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
            Log.d("posi", "i="+i + " ["+pos);
            if (Math.abs(pos - correctLeftPositions.get(j)) <= ERROR_PERCENTAJE)
                return Math.round(pos * charsPlateLength) + InitialPixelX;
        }
        return -1;
    }

    public void CalculateHistrograms()
    {
        Core.reduce(CurrentImage, VerticalHistogram, 1, Core.REDUCE_SUM, CvType.CV_32SC1); //Is good the orientation?
        CurrentImage = VerticalHistogram;

    }
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
        for(int i=0; i<positions.size(); ++i) {
            int extra = Math.max(Math.round(charsPlateLength*0.0144f), 1);
            int xStart = Math.max(positions.get(i).Start-extra, 0);
            int yStart = Math.max(InitialPixelY-extra,0);
            int xWidth = Math.min(positions.get(i).End - positions.get(i).Start+2*extra,CurrentImage.width()-xStart);
            int yWidth = Math.min(HeightChars+2*extra, CurrentImage.height() - yStart);
            Rect roi = new Rect(xStart, yStart, xWidth, yWidth);
            Log.d("posi", "RealX [0," + CurrentImage.width()+ "] Corte ["+positions.get(i).Start + "," + positions.get(i).End + "]");
            CroppedChars.add(new Mat(CurrentImage.clone(), roi));
        }
    }
}
