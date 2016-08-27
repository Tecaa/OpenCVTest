package cl.bananaware.hwoc;

import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by fergu on 22-08-2016.
 */
public final class TimeProfiler {
    //Map<TimeProfilerKey, Long> times = new HashMap<TimeProfilerKey, Long>();
    static List<TimeProfilerElement> times = new ArrayList<TimeProfilerElement>();

    private TimeProfiler(){
    }

    public static void ResetCheckPoints() {
        times.clear();
    }
    public static void CheckPoint(Double i) {
        CheckPoint(i, null);
    }

    public static void CheckPoint(Integer i) {
        CheckPoint(Double.valueOf(i), null);
    }

    public static void CheckPoint(Integer i, Integer index) {
        CheckPoint(Double.valueOf(i), index, null);
    }

    public static void CheckPoint(Double i, Integer index) {
        CheckPoint(i, index, null);
    }
    public static void CheckPoint(Integer i, Integer index, Integer index2) {
        CheckPoint(Double.valueOf(i), index, index2);
    }
    public static void CheckPoint(Double i, Integer index, Integer index2) {
        times.add(new TimeProfilerElement(i, index, index2, System.currentTimeMillis()));
    }

    public static String GetTotalTime() {
        return "[TOTAL] " + (times.get(times.size()-1).TimeMillis - times.get(0).TimeMillis) + " [ms]";
    }
    public static String GetTimes(boolean sort)
    {
        return GetTimes(sort, null, null, null);
    }
    public static String GetTimes(boolean sort, Integer count)
    {
        return GetTimes(sort, null, null, count);
    }
    public static String GetTimes(boolean sort, Integer from, Integer to)
    {
        return GetTimes(sort, (double)from, (double)to, null);
    }
    public static String GetTimes(boolean sort, Double from, Double to)
    {
        return GetTimes(sort, from, to, null);
    }
    public static String GetTimes(boolean sort, Double from, Double to, Integer count)
    {
        String output = "";
        List<TimeProfilerPair> tpp = new ArrayList<TimeProfilerPair>();
        for (int i=1; i<times.size(); ++i)
            tpp.add(new TimeProfilerPair(times.get(i-1), times.get(i)));

        if (sort) {
            Collections.sort(tpp, new Comparator<TimeProfilerPair>() {
                @Override
                public int compare(TimeProfilerPair lhs, TimeProfilerPair rhs) {
                    return (int) -((lhs.Last.TimeMillis - lhs.First.TimeMillis) - (rhs.Last.TimeMillis - rhs.First.TimeMillis));
                }
            });

        }

        from = (from == null ? -1 : from);
        to = (to == null ? Integer.MAX_VALUE : to);
        count = (count == null ? Integer.MAX_VALUE : count);

        double sum = 0;
        for (int i=0, j=0 ; i<tpp.size(); ++i) {
            TimeProfilerElement first = tpp.get(i).First;
            TimeProfilerElement last = tpp.get(i).Last;

            if (j >= count)
                break;

            if (first.PrincipalIndex < from || last.PrincipalIndex > to)
                continue;

            double delta = last.TimeMillis - first.TimeMillis;
            sum += delta;
            String line = "[" + i + "]" + " ";

            line += first.PrincipalIndex;
            line += (first.IterationIndex == null ? "" : "." + first.IterationIndex);
            line += (first.IterationIndex2 == null ? "" : "." + first.IterationIndex2);

            line += "\t-> ";

            line += last.PrincipalIndex;
            line += (last.IterationIndex == null ? "" : "." + last.IterationIndex);
            line += (last.IterationIndex2 == null ? "" : "." + last.IterationIndex2);

            line += "\t= ";
            line += delta;
            line += " [ms]";

            line += "\tSum: ";
            line += sum;
            line += " [ms]";

            output += line + "\n";
            ++j;
        }
        return output;
    }



    static class TimeProfilerElement {
        Double PrincipalIndex;
        Integer IterationIndex;
        Integer IterationIndex2;
        Long TimeMillis;

        public TimeProfilerElement(Double pi, Integer ii, Integer ii2, Long t)
        {
            PrincipalIndex = pi;
            IterationIndex = ii;
            IterationIndex2 = ii2;
            TimeMillis = t;
        }
        /*
        @Override
        public int hashCode() {
            // A big value that is supposed to be more than any value of
            // IterationIndex and IterationIndex2
            final int BIG_VALUE = 10000;
            Integer value1 = IterationIndex;
            if (IterationIndex == null)
                value1 = BIG_VALUE*2-1;

            Integer value2 = IterationIndex2;
            if (IterationIndex2 == null)
                value1 = BIG_VALUE-1;

            return (int)(PrincipalIndex * BIG_VALUE*2 + IterationIndex * BIG_VALUE + IterationIndex2);
        }

        @Override
        public boolean equals(Object o) {
            TimeProfilerElement oo = (TimeProfilerElement) o;
            return this.PrincipalIndex == oo.PrincipalIndex
                    && this.IterationIndex == oo.IterationIndex
                    && this.TimeMillis == oo.TimeMillis;
        }*/
    }
    static class TimeProfilerPair {
        TimeProfilerElement First;
        TimeProfilerElement Last;

        public TimeProfilerPair(TimeProfilerElement first, TimeProfilerElement last) {
            First = first;
            Last = last;
        }

    }
}
