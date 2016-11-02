package cl.bananaware.hwoc.ImageProcessing;

import android.graphics.Bitmap;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import cl.bananaware.hwoc.DebugHWOC;
import cl.bananaware.hwoc.ImageViewer;
import cl.bananaware.hwoc.MainActivity;
import cl.bananaware.hwoc.R;
import cl.bananaware.hwoc.TimeProfiler;

/**
 * Created by fergu on 08-09-2016.
 */
public class PlateRecognizer {
    private DebugHWOC debugHWOC;
    public List<Mat> finalCandidates;
    public List<Mat> firstProcessSteps;
    public List<Mat> secondProcessSteps;


    private final String GROUP1 = "ABCDEFGHIJKLNPRSTUVXYZWM";
    private final String GROUP2 = "BCDFGHJKLPRSTVXYZW0123456789";
    private final String GROUP3 = "0123456789";
    private final int ACCEPTABLE_CONFIDENCE = 45;

    public void InitDebug(DebugHWOC d)
    {
        debugHWOC = d;
    }
    public PlateResult Recognize(Mat m_)
    {
        CleanAll();
        TimeProfiler.CheckPoint(1);

        List<Mat> matss = GetPreImages(m_);

        for (int mt = 0; mt<matss.size()/2; ++mt) {


            /*if (1 == 1)
                return new PlateResult();*/


            CandidatesFinder candidatesFinder = new CandidatesFinder(matss.get(mt+3),matss.get(mt));
            //CandidatesFinder candidatesFinder = new CandidatesFinder(m_);
            GetPreMultiDilationImage(candidatesFinder);


            String lastPlate = "";
            boolean correctPlate = false;


            int b = 0;
            for (ImageSize is : ImageSize.values()) {
                if (!ImageViewer.GOOD_SIZE) {/*
                if (is == ImageSize.PEQUEÑA)
                    continue;
                if (is == ImageSize.GRANDE)
                    continue;*/
                }

                candidatesFinder.SetSize(is);
                List<RotatedRect> outlines = GetBlueCandidates(candidatesFinder, b);

                //STEP 3: loop
                for (int i = 0; i < outlines.size(); ++i) {
                    TimeProfiler.CheckPoint(15, b, i);
                    CandidateSelector candidateSelector =
                            new CandidateSelector(candidatesFinder.OriginalEqualizedImage, candidatesFinder.OriginalImageRealSize, outlines.get(i));
                    //STEP 4:

                    candidateSelector.CalculateBounds();
                    candidateSelector.TruncateBounds();
                    TimeProfiler.CheckPoint(16, b, i);

                    AddImage(firstProcessSteps, R.drawable.ipp, 8);
                    AddCountournedImage(firstProcessSteps,
                            candidateSelector.OriginalEqualizedImage, candidateSelector.CandidateRect.boundingRect(), 9);
                    AddCountournedImage(firstProcessSteps,
                            candidateSelector.OriginalEqualizedImage, candidateSelector.CandidateRect, 9);


                    if (!candidateSelector.PercentajeAreaCandidateCheck()) {
                        AddImage(firstProcessSteps, R.drawable.percentaje_area_candidate_check, 10);
                        Log.d("filter", "i=" + i + "!PercentajeAreaCandidateCheck");
                        TimeProfiler.CheckPoint(17, b, i);
                        continue;
                    }
                    TimeProfiler.CheckPoint(17, b, i);

                    // INTENTAR OTBTENER TAMAÑO REAL ACA
                    //Mat img = candidateSelector.GetFinalImage(true);
                    candidateSelector.CropExtraRotatedRect(false);   //STEP 5:
                    TimeProfiler.CheckPoint(18, b, i);
                    AddStep(firstProcessSteps, candidateSelector.CurrentImage, 9);
                    candidateSelector.Equalize();
                    TimeProfiler.CheckPoint(19, b, i);

                    ////////////////////////////// START INVENCION MIA //////////////////////////////

                    AddStep(firstProcessSteps, candidateSelector.CurrentImage, 10);
                    candidateSelector.Sobel();
                    TimeProfiler.CheckPoint(20, b, i);
                    AddStep(firstProcessSteps, candidateSelector.CurrentImage, 11);
                    candidateSelector.GaussianBlur();
                    TimeProfiler.CheckPoint(21, b, i);
                    AddStep(firstProcessSteps, candidateSelector.CurrentImage, 12);
                    candidateSelector.Dilate();
                    TimeProfiler.CheckPoint(22, b, i);
                    AddStep(firstProcessSteps, candidateSelector.CurrentImage, 13);


                    candidateSelector.Erode();
                    TimeProfiler.CheckPoint(23, b, i);
                    AddStep(firstProcessSteps, candidateSelector.CurrentImage, 14);
                    ////////////////////////////// END INVENCION MIA //////////////////////////////


                    //STEP 6:
                    candidateSelector.OtsusThreshold();
                    TimeProfiler.CheckPoint(24, b, i);
                    AddStep(firstProcessSteps, candidateSelector.CurrentImage, 15);

                    //STEP 7:
                    candidateSelector.FindOutlines();
                    TimeProfiler.CheckPoint(25, b, i);

                    //STEP 8:
                    candidateSelector.FindMaxAreaCandidatePro();

                    TimeProfiler.CheckPoint(26, b, i);
                    //STEP 9:
                    if (!candidateSelector.FindMinAreaRectInMaxArea()) {
                        AddImage(firstProcessSteps, R.drawable.find_min_area_rect_in_max_area, 16);
                        TimeProfiler.CheckPoint(27, b, i);
                        Log.d("filter", "i=" + i + " !FindMinAreaRectInMaxArea");
                        continue;
                    }
                    TimeProfiler.CheckPoint(27, b, i);

                    AddCountournedImage(firstProcessSteps, candidateSelector.CurrentImage,
                            candidateSelector.MinAreaRect, 17);


                    //STEP 10 and 11

                    CandidateSelector.CheckError checkError = candidateSelector.DoChecks();
                    TimeProfiler.CheckPoint(28, b, i);
                    if (checkError != null) {

                        AddImage(firstProcessSteps, checkError.getValue(), 18);
                        Log.d("filter", "i=" + i + "!DoChecks");
                        continue;
                    }

                    Mat img = candidateSelector.GetFinalImage(true);
                    AddStep(firstProcessSteps, img, 19);
                    TimeProfiler.CheckPoint(29, b, i);


                    //NOTA: ACA RECIEN OBTENER IMAGEN TAMAÑO REAL.
                    AddImage(secondProcessSteps, R.drawable.qpp, 19);


                    //if (ImageViewer.SHOW_PROCESS_DEBUG)
                    img = img.clone();

                    CharacterSeparator characterSeparator = new CharacterSeparator(img);
                    AddStep(secondProcessSteps, characterSeparator.CurrentImage, 20);

                    characterSeparator.AdaptiveThreshold();
                    TimeProfiler.CheckPoint(30, b, i);
                    AddStep(secondProcessSteps, characterSeparator.CurrentImage, 20);
                    characterSeparator.FindCountourns(1);

                    TimeProfiler.CheckPoint(31, b, i);
                    AddStepWithContourns(secondProcessSteps, characterSeparator.CurrentImage,
                            characterSeparator.contourns, null, 20);

                    int cant = characterSeparator.FilterCountourns();
                    if (cant <= 3) {
                        TimeProfiler.CheckPoint(32, b, i);
                        AddStep(secondProcessSteps, characterSeparator.ImageWithContournsPreFiltred, 21);
                        AddStep(secondProcessSteps, characterSeparator.ImageWithContourns, 21);
                        AddImage(secondProcessSteps, R.drawable.filter_countourns, 22);

                        Log.d("filter", "q=" /*+ q*/ + " !FilterCountourns");

                        //Segundo intento
                        if (characterSeparator.isBiggerBoundingArea) {
                            characterSeparator.CutImage();
                            characterSeparator.FindCountourns(2);
                            if (characterSeparator.FilterCountourns() == 0) {
                                AddStep(secondProcessSteps, characterSeparator.CurrentImage, 9);
                                AddStep(secondProcessSteps, characterSeparator.ImageWithContournsPreFiltred, 21);
                                AddStep(secondProcessSteps, characterSeparator.ImageWithContourns, 21);
                                AddImage(secondProcessSteps, R.drawable.filter_countourns, 22);
                                continue;
                            }
                        } else
                            continue;

                    }
                    TimeProfiler.CheckPoint(32, b, i);

                    AddStep(secondProcessSteps, characterSeparator.ImageWithContournsPreFiltred, 21);
                    AddStep(secondProcessSteps, characterSeparator.ImageWithContourns, 23);
                    AddStep(secondProcessSteps, characterSeparator.CleanedImage, 24);

                    characterSeparator.CalculatePlateLength();
                    characterSeparator.CalculateCharsPositions();
                    TimeProfiler.CheckPoint(33, b, i);


                    if (ImageViewer.CHARS)
                        characterSeparator.CropChars();
                    else
                        characterSeparator.CropAll();

                    TimeProfiler.CheckPoint(34, b, i);
                    String whiteList = "";
                    int finalConfidence = 0;

                    if (ImageViewer.CHARS) {
                        for (int n = 0; n < characterSeparator.CroppedChars.size(); ++n) {

                            AddImage(secondProcessSteps, R.drawable.npp, 25);
                            AddStep(secondProcessSteps, characterSeparator.CroppedChars.get(n), 26);

                            switch (n) {
                                case 0://1
                                    whiteList = GROUP1;
                                    break;
                                case 2://3

                                    whiteList = GROUP2;
                                    break;
                                case 4://5

                                    whiteList = GROUP3;
                                    break;
                            }
                            MainActivity.baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, whiteList);


                            Mat m = characterSeparator.CroppedChars.get(n);
                            Bitmap temp = Bitmap.createBitmap(m.cols(), m.rows(), Bitmap.Config.ARGB_8888);


                            Utils.matToBitmap(m, temp);
                            MainActivity.baseApi.setImage(temp);
                            String recognizedText = MainActivity.baseApi.getUTF8Text();
                            Log.d("output", "n=" + n + " text:" + recognizedText);


                        }
                    } else {
                        AddImage(secondProcessSteps, R.drawable.npp, 25);
                        AddStep(secondProcessSteps, characterSeparator.CroppedChars.get(0), 25);
                        for (int n = 0; n < 3; ++n) {


                            switch (n) {
                                case 0://1
                                    whiteList = "ABCDEFGHIJKLNPRSTUVXYZW";
                                    break;
                                case 1://3

                                    whiteList = "BCDFGHJKLPRSTVXYZW0123456789";
                                    break;
                                case 2://5

                                    whiteList = "0123456789";
                                    break;
                            }
                            MainActivity.baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, whiteList);


                            //Mat m = characterSeparator.CroppedChars.get(0);
                            Mat m = characterSeparator.GetCharsGroupN(n);
                            Bitmap temp = Bitmap.createBitmap(m.cols(), m.rows(), Bitmap.Config.ARGB_8888);
                            AddStep(secondProcessSteps, m, 25);

                            Utils.matToBitmap(m, temp);
                            MainActivity.baseApi.setImage(temp);
                            String recognizedText = MainActivity.baseApi.getUTF8Text();
                            int[] confidences = MainActivity.baseApi.wordConfidences();
                            if (confidences.length != 0) {
                                finalConfidence += confidences[0];// confidences[Math.min(n, confidences.length - 1)];
                                Log.d("output", "n=" + n + " text:" + recognizedText + "\tconfidence: " + confidences[Math.min(n, confidences.length - 1)]);
                            }


                            lastPlate += recognizedText;//process(recognizedText, n);
                            TimeProfiler.CheckPoint(35, b, i);

                        }

                    }
                    correctPlate = isCorrectPlate(lastPlate);
                    finalConfidence /= 3;
                    if (correctPlate && aceptableConfidence(finalConfidence))
                        return new PlateResult(lastPlate, finalConfidence);

                    lastPlate = "";
                }
                b++;
            }
        }
        return new PlateResult();
    }

    private List<Mat> GetPreImages(Mat m_) {






        //IDEA: SUBDIVIDIR LA IMAGEN EN LAS "MAYORES MANCHAS BLANCAS Y APLICAR TODO EL ALGORITMO SOBRE LAS SUBDIVICIONES
        // HASTA ENCONTRAR UNA PATENTE".
        // OTRA OPCION ES NO RECORTAR SINO QUE GENERAR "ZONAS ACEPTADAS (ZONAS BLANCAS)" EN DONDE
        // SI SE ACEPTA QUE HAYAN CANDIDATOS A SER PATENTE Y LOS DE LAS OTRAS ZONAS SE IGNORAN.


        Mat x = new Mat();
        x = m_.clone();
        double factor = CandidatesFinder.Resize(x, 400);
        Mat x_gray = x.clone();
        Imgproc.cvtColor(x, x, Imgproc.COLOR_RGB2GRAY);

        Core.bitwise_not( x, x );
        Imgproc.threshold(x, x, 255, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

        //AddStep(firstProcessSteps, x, 2);


        final float EROTION_AMPLIFIER= 1;//1.9f; //estaba en 1. 1.5 funciona tb
        Mat element = Imgproc.getStructuringElement( Imgproc.MORPH_RECT, new Size( 10*EROTION_AMPLIFIER, 10*EROTION_AMPLIFIER ));
        Imgproc.erode( x, x, element);

        //AddStep(firstProcessSteps, x, 2);

        Mat element2 = Imgproc.getStructuringElement( Imgproc.MORPH_RECT, new Size( 30, 30 ));
        Imgproc.dilate( x, x, element2);

        //AddStep(firstProcessSteps, x, 2);

        List<MatOfPoint> cs = new ArrayList<MatOfPoint>();
        Imgproc.findContours(x, cs, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE );

        //AddStepWithContourns(firstProcessSteps, x, cs, null, 0);
        List<Rect> biggers = new ArrayList<Rect>();
        biggers.add(0, new Rect());
        biggers.add(1, new Rect());
        biggers.add(2, new Rect());
        List<Rect> biggersOriginal = new ArrayList<Rect>();
        biggersOriginal.add(0, new Rect());
        biggersOriginal.add(1, new Rect());
        biggersOriginal.add(2, new Rect());
        for(int i=0; i<cs.size(); ++i)
        {
            Rect bb = Imgproc.boundingRect(cs.get(i));
            Rect bb2 = new Rect((int)(bb.x / factor), (int)(bb.y / factor),
                    (int)(bb.width / factor), (int)(bb.height / factor));
            if (bb.area() > biggers.get(0).size().area()) {
                biggers.add(0, bb);
                biggers.remove(3);
                biggersOriginal.add(0, bb2);
                biggersOriginal.remove(3);
            }
            else if (bb.area() > biggers.get(1).size().area()) {
                biggers.add(1, bb);
                biggers.remove(3);

                biggersOriginal.add(1, bb2);
                biggersOriginal.remove(3);
            }
            else if (bb.area() > biggers.get(2).size().area()) {
                biggers.add(2, bb);
                biggers.remove(3);
                biggersOriginal.add(2, bb2);
                biggersOriginal.remove(3);
            }
        }
        List<Mat> mats = new ArrayList<Mat>();
        List<Mat> matsOriginal = new ArrayList<Mat>();
        for (int i=0; i< biggers.size(); ++i)
        {
            mats.add(x_gray.submat(biggers.get(i)));
            matsOriginal.add(m_.submat(biggersOriginal.get(i)));

            AddStep(firstProcessSteps, mats.get(mats.size()-1), 2);
            AddStep(firstProcessSteps, matsOriginal.get(mats.size()-1), 2);
        }

        mats.addAll(matsOriginal);
        return mats;
    }

    private boolean aceptableConfidence(int finalConfidence) {
        if (finalConfidence >= ACCEPTABLE_CONFIDENCE)
            return true;
        else
            return false;
    }

    private List<RotatedRect> GetBlueCandidates(CandidatesFinder candidatesFinder, int b) {

        candidatesFinder.SetPreMultiDilationImage();
        candidatesFinder.Erode2();
        AddStep(firstProcessSteps, candidatesFinder.CurrentImage, 8);
        candidatesFinder.Dilate2();
        TimeProfiler.CheckPoint(9, b);
        AddStep(firstProcessSteps, candidatesFinder.CurrentImage, 8);
        TimeProfiler.CheckPoint(10, b);
        candidatesFinder.OtsusThreshold();
        AddStep(firstProcessSteps, candidatesFinder.CurrentImage, 9);
        TimeProfiler.CheckPoint(11, b);
        //STEP 1: start Finding outlines in the binary image
        candidatesFinder.FindOutlines();
        TimeProfiler.CheckPoint(12, b);

        //STEP 2: start selecting outlines
        candidatesFinder.OutlinesSelection();
        TimeProfiler.CheckPoint(13, b);
        AddStepWithContourns(firstProcessSteps, candidatesFinder.CurrentImage,
                candidatesFinder.LastGreenCandidates, null, 10);
        AddStepWithContourns(firstProcessSteps, candidatesFinder.CurrentImage,
                candidatesFinder.LastGreenCandidates, candidatesFinder.LastBlueCandidates, 10);
        candidatesFinder.OutlinesFilter();
        TimeProfiler.CheckPoint(14, b);
        AddStepWithContourns(firstProcessSteps, candidatesFinder.CurrentImage,
                candidatesFinder.LastGreenCandidates, candidatesFinder.LastBlueCandidates, 11);


        return candidatesFinder.LastBlueCandidatesMAR;
    }

    private void GetPreMultiDilationImage(CandidatesFinder candidatesFinder) {



        candidatesFinder.ToGrayScale();
        TimeProfiler.CheckPoint(2);



        candidatesFinder.EqualizeHistOriginalImage(false);
        TimeProfiler.CheckPoint(3);
        AddStep(firstProcessSteps, candidatesFinder.CurrentImage, 1);


        candidatesFinder.Dilate();

        AddStep(firstProcessSteps, candidatesFinder.CurrentImage, 2);
        TimeProfiler.CheckPoint(4);

        candidatesFinder.Erode();

        AddStep(firstProcessSteps, candidatesFinder.CurrentImage, 3);
        TimeProfiler.CheckPoint(5);


        candidatesFinder.Substraction();

        AddStep(firstProcessSteps, candidatesFinder.CurrentImage, 4);
        TimeProfiler.CheckPoint(6);



        candidatesFinder.Sobel();
        AddStep(firstProcessSteps, candidatesFinder.CurrentImage, 5);
        TimeProfiler.CheckPoint(7);



        candidatesFinder.GaussianBlur();
        AddStep(firstProcessSteps, candidatesFinder.PreMultiDilationImage, 6);
        TimeProfiler.CheckPoint(8);
    }

    private void AddCountournedImage(List<Mat> firstProcessSteps, Mat originalEqualizedImage, RotatedRect candidateRect, int i) {
        if (debugHWOC != null)
            debugHWOC.AddCountournedImage(firstProcessSteps,
                    originalEqualizedImage, candidateRect, i);
    }

    private void AddCountournedImage(List<Mat> firstProcessSteps, Mat originalEqualizedImage, Rect boundingCandidateRect, int i) {
        if (debugHWOC != null)
            debugHWOC.AddCountournedImage(firstProcessSteps,
                    originalEqualizedImage, boundingCandidateRect, i);
    }

    private void AddImage(List<Mat> firstProcessSteps, int j, int i) {
        if (debugHWOC != null)
            debugHWOC.AddImage(firstProcessSteps, j , i);
    }

    private void AddStepWithContourns(List<Mat> firstProcessSteps, Mat currentImage,
                                      List<MatOfPoint> lastGreenCandidates,
                                      List<MatOfPoint> lastBlueCandidates, int i) {
        if (debugHWOC != null)
            debugHWOC.AddStepWithContourns(firstProcessSteps, currentImage,
                    lastGreenCandidates, lastBlueCandidates, i);
    }

    private void AddStep(List<Mat> firstProcessSteps, Mat currentImage, int i) {
        if (debugHWOC != null) {

            if (currentImage.size().width <= 0 || currentImage.size().height <= 0)
            {
                Log.e("ERROR", "ERROR IMAGEN SIN SUFICIENTE ANCHO O ALTO");
                return;
            }
            debugHWOC.AddStep(firstProcessSteps, currentImage, i);
        }
    }

    private boolean isCorrectPlate(String lastPlate) {
        if (lastPlate.length() == 6
                && GROUP1.contains(String.valueOf(lastPlate.charAt(0)))
                && GROUP1.contains(String.valueOf(lastPlate.charAt(1)))
                && GROUP2.contains(String.valueOf(lastPlate.charAt(2)))
                && GROUP2.contains(String.valueOf(lastPlate.charAt(3)))
                && GROUP3.contains(String.valueOf(lastPlate.charAt(4)))
                && GROUP3.contains(String.valueOf(lastPlate.charAt(5)))
                )
            return true;
        else
            return false;
    }

    private String process(String recognizedText, int n) {
        if (hasSixChars(recognizedText)) {
            return recognizedText.replace(" ", "").substring(n*2, n*2+2);
        }
        else if (hasThreeGroups(recognizedText))
        {
            int index;
            switch (n)
            {
                case 0:
                    return recognizedText.substring(0,2);
                case 1:
                    index = recognizedText.indexOf(" ")+1;
                    return recognizedText.substring(index,Math.min(index+2, recognizedText.length()));
                case 2:
                default:
                    index = recognizedText.length();
                    return recognizedText.substring(Math.max(index-2,0),index);
            }

        }
        else if (hasTwoGroups(recognizedText))
        {


            int index;
            switch (n)
            {
                case 0:
                    return recognizedText.substring(0,Math.min(2, recognizedText.length()));
                case 1:
                    String[] groups = recognizedText.replace("  ", " ").replace("   ", " ").split(" ");
                    if (groups[0].length() > groups[1].length()) {
                        index = recognizedText.indexOf(" ");
                        return recognizedText.substring(Math.max(index-2,0),index);
                    }
                    else{
                        index = recognizedText.indexOf(" ")+1;
                        return recognizedText.substring(index,Math.min(index+2, recognizedText.length()));
                    }

                case 2:
                default:
                    index = recognizedText.length();
                    return recognizedText.substring(Math.max(index-2,0),index);
            }

        }
        else
        {
            return "";/*
            int index;
            switch (n)
            {
                case 0:
                    return recognizedText.substring(0,Math.min(2, recognizedText.length()));
                case 1:

                    index = recognizedText.length() /2-1;
                    return recognizedText.substring(Math.max(index, 0),Math.min(index+2, recognizedText.length()));

                case 2:
                default:
                    index = recognizedText.length();
                    return recognizedText.substring(Math.max(index-2,0),index);
            }*/
        }

    }

    private boolean hasTwoGroups(String recognizedText) {
        return recognizedText.replace("   ", " ").replace("  ", " ").split(" ").length == 2;
    }
    private boolean hasThreeGroups(String recognizedText) {
        return recognizedText.replace("   ", " ").replace("  ", " ").split(" ").length == 3;
    }

    private boolean hasSixChars(String recognizedText) {
        return recognizedText.replace(" ", "").length() == 6;
    }

    private void CleanAll() {
        finalCandidates = new ArrayList<Mat>();
        firstProcessSteps = new ArrayList<Mat>();
        secondProcessSteps = new ArrayList<Mat>();
    }
}
