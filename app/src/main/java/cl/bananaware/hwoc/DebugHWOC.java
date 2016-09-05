package cl.bananaware.hwoc;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by fergu on 20-08-2016.
 */
public class DebugHWOC {
   // Map<Integer, Mat> images = new HashMap<Integer, Mat>();
    private Resources resources;
    public DebugHWOC(Bitmap q, Bitmap i, Bitmap n)
    {/*
        Mat m = new Mat();
        Utils.bitmapToMat(q, m);
        images.put(R.drawable.qpp, m.clone());
        Utils.bitmapToMat(i, m);
        images.put(R.drawable.ipp, m.clone());
        Utils.bitmapToMat(n, m);
        images.put(R.drawable.npp, m.clone());*/
    }

    public DebugHWOC(Resources res) {
        resources = res;
    }

    public void AddImage(List<Mat> process, int element, int level) {
        if (!ImageViewer.SHOW_PROCESS_DEBUG || ImageViewer.I_LEVEL < level)
            return;

        Mat m = new Mat();
        Utils.bitmapToMat(BitmapFactory.decodeResource(resources, element), m);
        process.add(m);
    }

    public void AddCountournedImage(List<Mat> process, Mat originalEqualizedImage,
                                    RotatedRect candidateRect, int level) {
        if (!ImageViewer.SHOW_PROCESS_DEBUG || ImageViewer.I_LEVEL < level)
            return;

        Mat temp = originalEqualizedImage;
        Imgproc.cvtColor(temp, temp, Imgproc.COLOR_GRAY2RGB);

        DebugHWOC.drawRotatedRectInMat(candidateRect, temp);
        process.add(temp);
    }

    public void AddStepWithContourns(List<Mat> list, Mat img, List<MatOfPoint> green,
                                     List<MatOfPoint> blue, int level) {
        if (!ImageViewer.SHOW_PROCESS_DEBUG || ImageViewer.I_LEVEL < level)
            return;

        list.add(PutContourns(img.clone(), green, blue));
    }

    public void AddStep(List<Mat> list, Mat img, int level) {
        if (!ImageViewer.SHOW_PROCESS_DEBUG || ImageViewer.I_LEVEL < level)
            return;
        list.add(img.clone());
    }
    private Mat PutContourns(Mat currentImage, List<MatOfPoint> lastGreenCandidates, List<MatOfPoint> lastBlueCandidates) {
        Imgproc.cvtColor(currentImage, currentImage, Imgproc.COLOR_GRAY2RGB); //Convert to gray scale
        if (lastGreenCandidates != null)
        {
            for (int cId = 0; cId < lastGreenCandidates.size(); cId++) {
                Imgproc.drawContours(currentImage, lastGreenCandidates, cId, new Scalar(0, 255, 0), 1);
            }
        }
        if (lastBlueCandidates != null)
        {
            for (int cId = 0; cId < lastBlueCandidates.size(); cId++) {
                Imgproc.drawContours(currentImage, lastBlueCandidates, cId, new Scalar(0, 0, 255), 6);
            }
        }
        return currentImage;
    }



    public static Mat drawRotatedRectInMat(RotatedRect rRect, Mat mat)
    {
        Point[] vertices = new Point[4];
        rRect.points(vertices);
        for (int j = 0; j < 4; j++){
            Imgproc.line(mat, vertices[j], vertices[(j+1)%4], new Scalar(255,0,0));
        }
        return mat;
    }

}
