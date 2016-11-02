package cl.bananaware.hwoc.ImageProcessing;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import cl.bananaware.hwoc.HeightCandidate;
import cl.bananaware.hwoc.ImageViewer;

/**
 * Created by fergu on 12-08-2016.
 */
public class CharacterSeparator {
    private static final int PADDING = 10;
    public Mat CurrentImage;
    public Mat CleanedImage;
    public Mat ImageWithContourns, ImageWithContournsPreFiltred;
    public Mat OriginalImage;
    //private Mat VerticalHistogram;
    public List<MatOfPoint> contourns;
    public List<MatOfPoint> finalsContourns;
    private List<Double> correctLeftPositions = new ArrayList<Double>();
    private List<Double> correctRightPositions = new ArrayList<Double>();
    public CharacterSeparator(Mat mat) {
        CurrentImage = mat;
        OriginalImage = mat.clone();
        ImageWithContourns = new Mat();
        ImageWithContournsPreFiltred = new Mat();
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

    public void FindCountourns(int time)
    {
        if (time == 1)
            Core.bitwise_not(CurrentImage, CurrentImage);

        Mat hierarchy = new Mat();
        contourns = new ArrayList<MatOfPoint>();
        Imgproc.findContours(CurrentImage.clone(),contourns,hierarchy,Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);


        if (ImageViewer.SHOW_PROCESS_DEBUG) {
            Imgproc.cvtColor(CurrentImage, ImageWithContourns, Imgproc.COLOR_GRAY2RGB);
            Imgproc.cvtColor(CurrentImage, ImageWithContournsPreFiltred, Imgproc.COLOR_GRAY2RGB);
            for (int cId = 0; cId < contourns.size(); cId++) {
                Imgproc.drawContours(ImageWithContourns, contourns, cId, new Scalar(0, 255, 0), 1);
                Imgproc.drawContours(ImageWithContournsPreFiltred, contourns, cId, new Scalar(0, 255, 0), 1);
            }
        }
    }

    Boolean isBiggerBoundingArea;
    public int FilterAndRotateCountourns(int time) {
        Mat image = CurrentImage.clone();
        if (time == 2 && PatentInclinationAngle != 0)
            rotatePlateImage(image, -PatentInclinationAngle);
        int sizeF = getFinalContourns(image);

        if (sizeF <= 3)
            return sizeF;

        relationFilter();
        MatOfPoint limits = GetLimits(finalsContourns);
        //rotatePlateImage(CurrentImage, -PatentInclinationAngle);

        /*sizeF = getFinalContourns(2);
        if (sizeF <= 3)
            return sizeF;*/
        //pts = getPoints();



        // REMOVER LO QUE ESTÁ SOBRE LAS ROJAS Y BAJO ELLAS
        // ADEMAS REMOVER LO QUE ES MUY  PEQUEÑO (CIRCULOS INTERIORES PARA EVITAR ERRORES)

        //if (pts.size() <= 0)
          //  return false;

        //MatOfPoint2f mpp = new MatOfPoint2f(pts.toArray(new Point[pts.size()]));
        //RotatedRect rr = Imgproc.minAreaRect(mpp);



        if (REMOVE_OUTSIDE) {
            //Point[] pointss = new Point[4];
            //rr.points(pointss);


            Mat mask = new Mat(image.size(), CvType.CV_8UC1, new Scalar(0));     // suppose img is your image Mat

            List<MatOfPoint> qqqq = new ArrayList<MatOfPoint>();
            //qqqq.add(new MatOfPoint(pointss));
            qqqq.add(limits);
            Imgproc.fillPoly(mask, qqqq, new Scalar(255));                           // <- do it here
            Mat maskedImage = new Mat(image.size(), CvType.CV_8UC1);  // Assuming you have 3 channel image
            maskedImage.setTo(new Scalar(0));  // Set all pixels to (180, 180, 180)
            image.copyTo(maskedImage, mask);  // Copy pixels within contour to maskedImage.
            //CurrentImage = C;
            CleanedImage = maskedImage.clone();
            CurrentImage = maskedImage.clone();


        }
        if (ImageViewer.SHOW_PROCESS_DEBUG) {
            for (int cId = 0; cId < finalsContourns.size(); cId++) {
                Imgproc.drawContours(ImageWithContourns, finalsContourns, cId, new Scalar(255, 0, 0), 1);
            }
        }
        return finalsContourns.size();

    }

    private void rotatePlateImage(Mat src, double angle)
    {

        Point pt = new Point(src.cols()/2., src.rows()/2.);
        Mat r = Imgproc.getRotationMatrix2D(pt, angle, 1.0);
        Imgproc.warpAffine(src, src, r, new Size(src.cols(), src.rows()));
    }


    double PatentInclinationAngle;
    private MatOfPoint GetLimits(List<MatOfPoint> finalsContourns) {
        List<Point> superior = new ArrayList<Point>();
        List<Point> inferior = new ArrayList<Point>();

        Double x_min = Double.MAX_VALUE;
        Double x_max = 0.0;
        for (int i=0; i<finalsContourns.size(); ++i)
        {
            //Rect temp = Imgproc.boundingRect(finalsContourns.get(i));
            RotatedRect temp = Imgproc.minAreaRect(CandidateSelector.mopToMop2f(finalsContourns.get(i)));
            LimitsPointResult res = GetLimitsPoints(temp);
            superior.add(res.g1);
            superior.add(res.g2);
            inferior.add(res.l1);
            inferior.add(res.l2);

            if (x_min > res.x_min) {
                x_min = res.x_min;
            }
            if (x_max < res.x_max) {
                x_max = res.x_max;
            }
        }

        LinearRegression lrSuperior = new LinearRegression();
        LinearRegression lrInferior = new LinearRegression();

        lrSuperior.AddPairs(superior);
        lrInferior.AddPairs(inferior);

        lrSuperior.CalculateCoeficients();
        lrInferior.CalculateCoeficients();

        //get 4 points
        Point[] points = new Point[4];
        points[0] = new Point(x_min, lrSuperior.PredictValue(x_min));
        points[1] = new Point(x_max, lrSuperior.PredictValue(x_max));
        points[2] = new Point(x_max, lrInferior.PredictValue(x_max));
        points[3] = new Point(x_min, lrInferior.PredictValue(x_min));
        /*
        points[0] = new Point(x_min, (x_min - y_iz_max.x)/(y_der_max.x - y_iz_max.x) * (y_der_max.y - y_iz_max.y) + y_iz_max.y);
        points[1] = new Point(x_max, (x_max - y_iz_max.x)/(y_der_max.x - y_iz_max.x) * (y_der_max.y - y_iz_max.y) + y_iz_max.y);
        points[2] = new Point(x_max, (x_max - y_iz_low.x)/(y_der_low.x - y_iz_low.x) * (y_der_low.y - y_iz_low.y) + y_iz_low.y);
        points[3] = new Point(x_min, (x_min - y_iz_low.x)/(y_der_low.x - y_iz_low.x) * (y_der_low.y - y_iz_low.y) + y_iz_low.y);*/
        PatentInclinationAngle = Math.atan2(-(points[1].y - points[0].y), points[1].x - points[0].x)*180.0 / Math.PI;
        return new MatOfPoint(points);
    }

    private Double GetRightValue(Point[] points_der) {
        Double right = points_der[0].x;
        for (int i=1; i< points_der.length; ++i) {
            if (points_der[i].x > right)
                right = points_der[i].x;
        }
        return  right;
    }

    private Double GetLeftValue(Point[] points_iz) {
        Double left = points_iz[0].x;
        for (int i=1; i< points_iz.length; ++i) {
            if (points_iz[i].x < left)
                left = points_iz[i].x;
        }
        return  left;
    }

    private LimitsPointResult GetLimitsPoints(RotatedRect rr) {
        Point[] ps = new Point[4];
        rr.points(ps);

        Point great1 = ps[0];
        Point great2 = ps[0];
        Point lower1 = ps[0];
        Point lower2 = ps[0];
        Double x_min = ps[0].x;
        Double x_max = ps[0].x;


        for (int i = 1; i< ps.length; ++i) {
            if (ps[i].y > great1.y) {
                great2 = great1;
                great1 = ps[i];
            }
            else if (ps[i].y > great2.y) {
                great2 = ps[i];
            }

            if (ps[i].y < lower1.y) {
                lower2 = lower1;
                lower1 = ps[i];
            }
            else if (ps[i].y < lower2.y) {
                lower2 = ps[i];
            }

            if (ps[i].x < x_min)
                x_min = ps[i].x;
            if (ps[i].x > x_max)
                x_max = ps[i].x;
        }
        return new LimitsPointResult(great1, great2, lower1, lower2, x_min, x_max);
    }

    CaracteristicaRelacionador alturas;
    CaracteristicaRelacionador anchuras;
    CaracteristicaRelacionador areas;
    private void relationAddValues(Rect r) {

        alturas.NuevoValor(r.height);
        anchuras.NuevoValor(r.width);
        areas.NuevoValor(r.area());
    }

    private void relationFilter() {
        List<MatOfPoint> dels = new ArrayList<MatOfPoint>();
        Log.d("size", "-----------------------------");
        for (int i=0; i<finalsContourns.size();++i){
            Rect r = Imgproc.boundingRect(finalsContourns.get(i));
            Log.d("size", "Altura " + r.height);
            Log.d("size", "\t\tAnchura " + r.width);
            Log.d("size", "\t\t\t\tArea " + r.area());
            boolean altura = alturas.EsMayoria(r.height);
            boolean anchura = anchuras.EsMayoria(r.width);
            boolean area = areas.EsMayoria(r.area());
            if (!altura || !anchura || !area)
                dels.add(finalsContourns.get(i));
        }
        finalsContourns.removeAll(dels);


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
                if (Math.abs(hcs.get(k).Start - r.y)/r.height <= ERROR && Math.abs(hcs.get(k).End - (r.y + r.height))/r.height <= ERROR) {
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
    Rect CropCharactersROI;
    public void CropAll() {
        int extra = Math.max(Math.round(charsPlateLength*0.0144f), 1);
        int xStart = Math.max(InitialPixelX-extra, 0);
        int yStart = Math.max(InitialPixelY-extra,0);
        int xWidth = Math.min(charsPlateLength +2*extra,CurrentImage.width()-xStart);
        int yWidth = Math.min(HeightChars+2*extra, CurrentImage.height() - yStart);
        CropCharactersROI = new Rect(xStart, yStart, xWidth, yWidth);
        //CroppedChars.add(new Mat(CurrentImage.clone(), roi)); //black and white image
        Core.copyMakeBorder(new Mat(CurrentImage, CropCharactersROI), CurrentImage, PADDING, PADDING, PADDING, PADDING, 0);
        CroppedChars.add(CurrentImage);  // gray scale image
        /*CroppedChars.add(CurrentImage);  // gray scale image
        CroppedChars.add(CurrentImage);  // gray scale image*/

    }

    public List<Point> getPoints() {
        List<Point> pts = new ArrayList<Point>();
        for (int i=0;  i<finalsContourns.size();++i) {
            MatOfPoint mop = finalsContourns.get(i);
            MatOfPoint2f mop2f = CandidateSelector.mopToMop2f(mop);
            RotatedRect rr = Imgproc.minAreaRect(mop2f);
            Point[] p = new Point[4];
            rr.points(p);

            for (int k = 0; k < p.length; ++k) {
                if (p[k].x < 0)
                    p[k].x = 0;
                if (p[k].x > CurrentImage.width())
                    p[k].x = CurrentImage.width();
                if (p[k].y < 0)
                    p[k].y = 0;
                if (p[k].y > CurrentImage.height())
                    p[k].y = CurrentImage.height();
            }

            pts.add(p[0]);
            pts.add(p[1]);
            pts.add(p[2]);
            pts.add(p[3]);
        }
        return pts;
    }

    public void CutImage() {
        Rect myROI = new Rect(0,0,CurrentImage.width(), (int)(CurrentImage.height()*0.8f));
        CurrentImage = CurrentImage.submat(myROI);
    }

    public Mat GetCharsGroupN(int n) {
        Rect r;

        int EXTRA = (int)(charsPlateLength * 0.01);
        int ancho = charsPlateLength/3;
        int base = InitialPixelX - CropCharactersROI.x + PADDING;
        switch (n)
        {
            case 0:
                r = new Rect(base - EXTRA, 0, ancho+2*EXTRA, CurrentImage.height());
                break;
            case 1:
                r = new Rect(base + ancho-EXTRA, 0, ancho+2*EXTRA, CurrentImage.height());
                break;
            case 2:
            default:
                r = new Rect(base + 2*ancho - EXTRA, 0, ancho+2*EXTRA, CurrentImage.height());
                break;
        }

        //Size s = CroppedChars.get(0).size();
        return CroppedChars.get(0).submat(r);
    }

    final boolean REMOVE_OUTSIDE = true;
    public int getFinalContourns(Mat image) {

        alturas = new CaracteristicaRelacionador(0.40);
        anchuras = new CaracteristicaRelacionador(0.40);
        areas = new CaracteristicaRelacionador(0.40);

        //List<Point> pts = new ArrayList<Point>();
        isBiggerBoundingArea = false;
        double biggerBoundingArea = 0;
        finalsContourns = new ArrayList<MatOfPoint>();

        for (int i=0; i<contourns.size(); ++i)
        {

            Rect boundingRect = Imgproc.boundingRect(contourns.get(i));
            double a = boundingRect.area();
            if (a >= biggerBoundingArea)
                biggerBoundingArea = a;



            //MatOfPoint mop = contourns.get(i);
            //MatOfPoint2f mop2f = CandidateSelector.mopToMop2f(mop);
            //RotatedRect rr = Imgproc.minAreaRect(mop2f);

            if (areaCheck(boundingRect) && aspectRatioCheck(boundingRect))
            {
                finalsContourns.add(contourns.get(i));

                relationAddValues(boundingRect);

            }
            if (REMOVE_OUTSIDE){
                if (boundingRect.area() < 0.025 * image.size().area()) {
                    Imgproc.drawContours(image, contourns, i,
                            new Scalar(0), -1); // This is a OpenCV function
                }
            }
        }

        if (ImageViewer.SHOW_PROCESS_DEBUG) {
            for (int cId = 0; cId < finalsContourns.size(); cId++) {
                Imgproc.drawContours(ImageWithContournsPreFiltred, finalsContourns, cId, new Scalar(255, 0, 0), 1);
            }
        }

        if (biggerBoundingArea / image.size().area() >= 0.75)
            isBiggerBoundingArea = true;


        return finalsContourns.size();
    }

    public class CaracteristicaContador
    {
        double valorCaracteristica;
        int frecuencia;

        public CaracteristicaContador(double valor)
        {
            frecuencia = 1;
            valorCaracteristica = valor;
        }
    }
    public class CaracteristicaRelacionador
    {
        List<CaracteristicaContador> caracteristicas;
        double porcentajeAceptabilidad;

        public CaracteristicaRelacionador(double porcentaje)
        {
            caracteristicas = new ArrayList<CaracteristicaContador>();
            porcentajeAceptabilidad = porcentaje;
        }
        public void NuevoValor(double valor)
        {
            boolean insertado = false;
            for (CaracteristicaContador cc:caracteristicas) {
                if (DentroRango(valor, cc.valorCaracteristica, porcentajeAceptabilidad)) {
                    ++cc.frecuencia;
                    insertado = true;
                    break;
                }
            }
            if (!insertado)
                caracteristicas.add(new CaracteristicaContador(valor));
        }
        public boolean EsMayoria(double valor)
        {
            int mayorFrecuencia = 0;
            int indiceMayorFrecuancia = 0;
            int indiceBusqueda = -1;

            for (int i=0; i<caracteristicas.size(); ++i) {
                if (mayorFrecuencia < caracteristicas.get(i).frecuencia)
                {
                    indiceMayorFrecuancia = i;
                    mayorFrecuencia = caracteristicas.get(i).frecuencia;
                }
                if (DentroRango(valor, caracteristicas.get(i).valorCaracteristica, porcentajeAceptabilidad)) {
                    if (indiceBusqueda == -1)
                        indiceBusqueda = i;
                    else if (indiceBusqueda != -1 && caracteristicas.get(indiceBusqueda).frecuencia < caracteristicas.get(i).frecuencia )
                        indiceBusqueda = i;
                }
            }
            return (indiceBusqueda == indiceMayorFrecuancia);
        }
        private boolean DentroRango(double valor, double valorCaracteristica, double porcentaje)
        {
            return (valor >= valorCaracteristica * (1-porcentajeAceptabilidad)
                    && valor <= valorCaracteristica * (1+porcentajeAceptabilidad));
        }
    }
}
