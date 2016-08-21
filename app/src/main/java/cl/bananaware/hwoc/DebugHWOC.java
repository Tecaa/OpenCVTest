package cl.bananaware.hwoc;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
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

    public void AddImage(List<Mat> process, int element) {
        if (!ImageViewer.SHOW_PROCESS_DEBUG)
            return;

        Mat m = new Mat();
        Utils.bitmapToMat(BitmapFactory.decodeResource(resources, element), m);
        process.add(m);
    }

    public void AddCountournedImage(List<Mat> process, Mat originalEqualizedImage, RotatedRect candidateRect) {
        if (!ImageViewer.SHOW_PROCESS_DEBUG)
            return;

        Mat temp = originalEqualizedImage;
        Imgproc.cvtColor(temp, temp, Imgproc.COLOR_GRAY2RGB);

        DebugHWOC.drawRotatedRectInMat(candidateRect, temp);
        process.add(temp);
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
