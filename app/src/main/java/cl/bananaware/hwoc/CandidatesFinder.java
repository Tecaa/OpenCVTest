package cl.bananaware.hwoc;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

/**
 * Created by fergu on 06-08-2016.
 */
public class CandidatesFinder {
    public Mat OriginalImage;
    public Mat CurrentImage;
    public CandidatesFinder(Bitmap bm)
    {
        OriginalImage = new Mat();
        Utils.bitmapToMat(bm, OriginalImage);
        CurrentImage = OriginalImage;
    }
}
