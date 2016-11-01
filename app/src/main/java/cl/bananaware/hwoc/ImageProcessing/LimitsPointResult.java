package cl.bananaware.hwoc.ImageProcessing;

import org.opencv.core.Point;

/**
 * Created by fergu on 31-10-2016.
 */
public class LimitsPointResult {
    Point g1;
    Point g2;
    Point l1;
    Point l2;

    Double x_min;
    Double x_max;
    public LimitsPointResult(Point g1, Point g2, Point l1, Point l2, Double x_min, Double x_max)
    {
        this.g1= g1;
        this.g2= g2;
        this.l1 = l1;
        this.l2 = l2;
        this.x_min = x_min;
        this.x_max = x_max;
    }
}
