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
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import cl.bananaware.hwoc.HeightCandidate;
import cl.bananaware.hwoc.ImageViewer;

/**
 * Created by fergu on 12-08-2016.
 */
public class CharacterSeparator {
    public Mat CurrentImage;
    public Mat CleanedImage;
    public Mat ImageWithContourns;
    public Mat OriginalImage;
    //private Mat VerticalHistogram;
    public List<MatOfPoint> contourns = new ArrayList<MatOfPoint>();
    public List<MatOfPoint> finalsContourns = new ArrayList<MatOfPoint>();
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

        alturas = new CaracteristicaRelacionador(0.3);
        anchuras = new CaracteristicaRelacionador(0.3);
        areas = new CaracteristicaRelacionador(0.3);

        final boolean REMOVE_OUTSIDE = true;
        List<Point> pts = new ArrayList<Point>();
        for (int i=0; i<contourns.size(); ++i)
        {
            Rect boundingRect = Imgproc.boundingRect(contourns.get(i));


            MatOfPoint mop = contourns.get(i);
            MatOfPoint2f mop2f = CandidateSelector.mopToMop2f(mop);
            RotatedRect rr = Imgproc.minAreaRect(mop2f);

            if (areaCheck(boundingRect) && aspectRatioCheck(boundingRect))
            {
                finalsContourns.add(contourns.get(i));
                /*
                if (min_x > boundingRect.x)
                    min_x = boundingRect.x;
                if (max_x < boundingRect.x + boundingRect.width)
                    max_x = boundingRect.x + boundingRect.width;
                if (min_y > boundingRect.y)
                    min_y = boundingRect.y;
                if (max_y < boundingRect.y + boundingRect.height)
                    max_y = boundingRect.y + boundingRect.height;*/


                relationAddValues(contourns.get(i));

            }
            if (REMOVE_OUTSIDE) {
                if (boundingRect.area() < 0.03 * CurrentImage.size().area()) {
                    Imgproc.drawContours(CurrentImage, contourns, i,
                            new Scalar(0), -1); // This is a OpenCV function
                }
            }
        }

        relationFilter();
        pts = getPoints();



        // REMOVER LO QUE ESTÁ SOBRE LAS ROJAS Y BAJO ELLAS
        // ADEMAS REMOVER LO QUE ES MUY  PEQUEÑO (CIRCULOS INTERIORES PARA EVITAR ERRORES)

        if (pts.size() <= 0)
            return false;

        MatOfPoint2f mpp = new MatOfPoint2f(pts.toArray(new Point[pts.size()]));
        RotatedRect rr = Imgproc.minAreaRect(mpp);

        if (REMOVE_OUTSIDE) {
            Point[] pointss = new Point[4];
            rr.points(pointss);


            Mat mask = new Mat(CurrentImage.size(), CvType.CV_8UC1, new Scalar(0));     // suppose img is your image Mat

            List<MatOfPoint> qqqq = new ArrayList<MatOfPoint>();
            qqqq.add(new MatOfPoint(pointss));
            Imgproc.fillPoly(mask, qqqq, new Scalar(255));                           // <- do it here
            Mat maskedImage = new Mat(CurrentImage.size(), CvType.CV_8UC1);  // Assuming you have 3 channel image
            maskedImage.setTo(new Scalar(0));  // Set all pixels to (180, 180, 180)
            CurrentImage.copyTo(maskedImage, mask);  // Copy pixels within contour to maskedImage.
            //CurrentImage = C;
            CleanedImage = maskedImage.clone();
            CurrentImage = maskedImage.clone();
        }
        if (ImageViewer.SHOW_PROCESS_DEBUG) {
            for (int cId = 0; cId < finalsContourns.size(); cId++) {
                Imgproc.drawContours(ImageWithContourns, finalsContourns, cId, new Scalar(255, 0, 0), 1);
            }
        }
        return finalsContourns.size() != 0;

    }
    CaracteristicaRelacionador alturas;
    CaracteristicaRelacionador anchuras;
    CaracteristicaRelacionador areas;
    private void relationAddValues(MatOfPoint matOfPoint) {

        alturas.NuevoValor(matOfPoint.height());
        anchuras.NuevoValor(matOfPoint.width());
        areas.NuevoValor(matOfPoint.size().area());
    }

    private void relationFilter() {
        List<MatOfPoint> copy = finalsContourns;
        for (int i=0; i<finalsContourns.size();++i){
            if (!alturas.EsMayoria(finalsContourns.get(i).height())
                || !anchuras.EsMayoria(finalsContourns.get(i).width())
                || !areas.EsMayoria(finalsContourns.get(i).size().area()))
                copy.remove(i);
        }
        finalsContourns = copy;
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
    public void CropAll() {
        int extra = Math.max(Math.round(charsPlateLength*0.0144f), 1);
        int xStart = Math.max(InitialPixelX-extra, 0);
        int yStart = Math.max(InitialPixelY-extra,0);
        int xWidth = Math.min(charsPlateLength +2*extra,CurrentImage.width()-xStart);
        int yWidth = Math.min(HeightChars+2*extra, CurrentImage.height() - yStart);
        Rect roi = new Rect(xStart, yStart, xWidth, yWidth);
        //CroppedChars.add(new Mat(CurrentImage.clone(), roi)); //black and white image
        Core.copyMakeBorder(new Mat(CurrentImage, roi), CurrentImage, 10, 10, 10, 10, 0);
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

    public class CaracteristicaContador
    {
        double valorCaracteristica;
        int frecuencia;

        public CaracteristicaContador(double valor)
        {
            frecuencia = 0;
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
