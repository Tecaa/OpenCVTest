package cl.bananaware.hwoc.ImageProcessing;

import android.renderscript.Sampler;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by fergu on 31-10-2016.
 */
public class LinearRegression {
    int MAXN = 30*100;
    int n = 0;
    double[] x = new double[MAXN];
    double[] y = new double[MAXN];

    // first pass: read in data, compute xbar and ybar
    double sumx = 0.0, sumy = 0.0, sumx2 = 0.0;

    List<Integer> bannedIndex;
    Double beta1;
    Double beta0;

    public void AddPair(Double x_, Double y_) {
            x[n] = x_;
            y[n] = y_;
            sumx  += x[n];
            sumx2 += x[n] * x[n];
            sumy  += y[n];
            n++;

    }


    double xbar;
    double ybar;
    private void Compute()
    {
        xbar = sumx / (n - bannedIndex.size());
        ybar = sumy / (n - bannedIndex.size());

        // second pass: compute summary statistics
        double xxbar = 0.0, yybar = 0.0, xybar = 0.0;
        for (int i = 0; i < n; i++) {
            if (bannedIndex.contains(i))
                continue;
            xxbar += (x[i] - xbar) * (x[i] - xbar);
            yybar += (y[i] - ybar) * (y[i] - ybar);
            xybar += (x[i] - xbar) * (y[i] - ybar);
        }

        beta1 = xybar / xxbar;
        beta0 = ybar - beta1 * xbar;
    }


    public double PredictValue(Double x)
    {
        return beta1 * x + beta0;
    }

    public void AddPairs(List<Point> l) {
        for (int i=0; i<l.size(); ++i) {
            AddPair(l.get(i).x, l.get(i).y);
        }

    }



    public void CalculateCoeficients()
    {
        bannedIndex = new ArrayList<Integer>();
        Compute();
        Bans();
        Compute();


    }

    private void Bans() {

        if (n < 6)
            return;

        List<ValueError> errs = new ArrayList<ValueError>();
        for (int i = 0; i < n; i++) {
            double fit = beta1*x[i] + beta0;
            //rss += (fit - y[i]) * (fit - y[i]);
            errs.add(new ValueError(x[i], y[i], (fit - y[i]) * (fit - y[i]), i));
            //errs.add(new ValueError(x[i], y[i], (fit - ybar) * (fit - ybar), i));
        }
        Collections.sort(errs);
        DelPair(errs.get(0));
        DelPair(errs.get(1));
        DelPair(errs.get(2));
        DelPair(errs.get(3));


    }

    private void DelPair(ValueError valueError) {
        bannedIndex.add(valueError.index);
        sumx  -= x[valueError.index];
        sumx2 -= x[valueError.index] * x[valueError.index];
        sumy  -= y[valueError.index];
    }

    private class ValueError implements Comparable<ValueError>{
        double x_value;
        double y_value;
        Double error;
        int index;
        public ValueError(double x,double y, double e, int i)
        {
            x_value = x;
            y_value = y;
            error = e;
            index = i;
        }

        @Override
        public int compareTo(ValueError another) {
            return -this.error.compareTo(another.error);
        }
    }

}
