package cl.bananaware.hwoc;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fergu on 08-09-2016.
 */
public class PlateRecognizer {
    private DebugHWOC debugHWOC;
    List<Mat> finalCandidates;
    List<Mat> firstProcessSteps;
    List<Mat> secondProcessSteps;
    public void InitDebug(DebugHWOC d)
    {
        debugHWOC = d;
    }
    public String Recognize(Bitmap bmp)
    {
        TimeProfiler.CheckPoint(1);
        CandidatesFinder candidatesFinder;
        candidatesFinder = new CandidatesFinder(bmp);


        candidatesFinder.ToGrayScale();

        boolean fueGaussianBlureada = false;
        candidatesFinder.EqualizeHistOriginalImage(false);

        debugHWOC.AddStep(firstProcessSteps, candidatesFinder.CurrentImage, 1);


        if (EXPERIMENTAL_EQUALITATION) {
            Imgproc.GaussianBlur(candidatesFinder.OriginalEqualizedImage, candidatesFinder.CurrentImage, new Size(25, 25), 25);
            fueGaussianBlureada = true;

        }


        candidatesFinder.Dilate();

        debugHWOC.AddStep(firstProcessSteps, candidatesFinder.CurrentImage, 2);

        candidatesFinder.Erode();

        debugHWOC.AddStep(firstProcessSteps, candidatesFinder.CurrentImage, 3);

        TimeProfiler.CheckPoint(2);


        candidatesFinder.Substraction();

        debugHWOC.AddStep(firstProcessSteps, candidatesFinder.CurrentImage, 4);




        candidatesFinder.Sobel();
        debugHWOC.AddStep(firstProcessSteps, candidatesFinder.CurrentImage, 5);




        candidatesFinder.GaussianBlur();
        debugHWOC.AddStep(firstProcessSteps, candidatesFinder.PreMultiDilationImage, 6);

        List<RotatedRect> outlines = new ArrayList<RotatedRect>();
        TimeProfiler.CheckPoint(3);
        String plate = "";
        String finalPlate = "";

        for (ImageSize is : ImageSize.values()) {
            if (!ImageViewer.GOOD_SIZE) {/*
                if (is == ImageSize.PEQUEÑA)
                    continue;
                if (is == ImageSize.GRANDE)
                    continue;*/
            }
            candidatesFinder.Dilate2(is);
            candidatesFinder.Erode2();

            candidatesFinder.OtsusThreshold();


            //STEP 1: start Finding outlines in the binary image
            candidatesFinder.FindOutlines();


            //STEP 2: start selecting outlines
            candidatesFinder.OutlinesSelection();
            debugHWOC.AddStepWithContourns(firstProcessSteps, candidatesFinder.CurrentImage,
                    candidatesFinder.LastGreenCandidates, candidatesFinder.LastBlueCandidates, 7);
            candidatesFinder.OutlinesFilter();

            debugHWOC.AddStepWithContourns(firstProcessSteps, candidatesFinder.CurrentImage,
                    candidatesFinder.LastGreenCandidates, candidatesFinder.LastBlueCandidates, 7);

            //    outlines.addAll(candidatesFinder.LastBlueCandidatesMAR);

            outlines = candidatesFinder.LastBlueCandidatesMAR;

            TimeProfiler.CheckPoint(4);
            //STEP 3: loop
            for (int i = 0; i < outlines.size(); ++i) {
                CandidateSelector candidateSelector =
                        new CandidateSelector(candidatesFinder.OriginalEqualizedImage, candidatesFinder.OriginalImageRealSize, outlines.get(i));
                //STEP 4:

                candidateSelector.CalculateBounds();
                candidateSelector.TruncateBounds();

                debugHWOC.AddImage(firstProcessSteps, R.drawable.ipp, 8);
                debugHWOC.AddCountournedImage(firstProcessSteps,
                        candidateSelector.OriginalEqualizedImage, candidateSelector.CandidateRect, 9);


                if (!candidateSelector.PercentajeAreaCandidateCheck()) {
                    debugHWOC.AddImage(firstProcessSteps, R.drawable.percentaje_area_candidate_check, 10);
                    Log.d("filter", "i=" + i + "!PercentajeAreaCandidateCheck");
                    continue;
                }

                candidateSelector.CropExtraRotatedRect(false);   //STEP 5:
//            candidateSelector.CropExtraBoundget(i).angle);
                debugHWOC.AddStep(firstProcessSteps, candidateSelector.CurrentImage, 9);
                candidateSelector.Equalize();


                ////////////////////////////// START INVENCION MIA //////////////////////////////

                debugHWOC.AddStep(firstProcessSteps, candidateSelector.CurrentImage, 10);
                candidateSelector.Sobel();
                debugHWOC.AddStep(firstProcessSteps, candidateSelector.CurrentImage, 11);
                candidateSelector.GaussianBlur();
                debugHWOC.AddStep(firstProcessSteps, candidateSelector.CurrentImage, 12);
                candidateSelector.Dilate();
                debugHWOC.AddStep(firstProcessSteps, candidateSelector.CurrentImage, 13);


                candidateSelector.Erode();
                debugHWOC.AddStep(firstProcessSteps, candidateSelector.CurrentImage, 14);
                ////////////////////////////// END INVENCION MIA //////////////////////////////


                //STEP 6:
                candidateSelector.OtsusThreshold();

                debugHWOC.AddStep(firstProcessSteps, candidateSelector.CurrentImage, 15);

                //STEP 7:
                candidateSelector.FindOutlines();


                //STEP 8:
                candidateSelector.FindMaxAreaCandidatePro();


                //STEP 9:
                if (!candidateSelector.FindMinAreaRectInMaxArea()) {
                    debugHWOC.AddImage(firstProcessSteps, R.drawable.find_min_area_rect_in_max_area, 16);
                    Log.d("filter", "i=" + i + " !FindMinAreaRectInMaxArea");
                    continue;
                }


                debugHWOC.AddCountournedImage(firstProcessSteps, candidateSelector.CurrentImage,
                        candidateSelector.MinAreaRect, 17);


                //STEP 10 and 11

                CandidateSelector.CheckError checkError = candidateSelector.DoChecks();
                if (checkError != null) {

                    debugHWOC.AddImage(firstProcessSteps, checkError.getValue(), 18);
                    Log.d("filter", "i=" + i + "!DoChecks");
                    continue;
                }


                // Paso 11 sin rotación.
                //Mat sinCortar = candidatesFinder.OriginalImage.clone();
                //Rect roi2 = new Rect(rects.get(i).boundingRect().x + mr2.boundingRect().x,
                //        rects.get(i).boundingRect().y + mr2.boundingRect().y, (int)mr2.boundingRect().width, (int)mr2.boundingRect().height);
                //Mat cropped2 = new Mat(sinCortar, roi2);


                // Apply Gray Scale, Skew Correction, Fixed Size.
                // hacer reequalizacion????
                //Mat resizeimage = new Mat();
                //Size sz = new Size(100,100);
                //Imgproc.resize( croppedimage, resizeimage, sz );
                //
                //

                //debugHWOC.AddStep(firstProcessSteps, candidateSelector.CurrentImage.clone());
                //        debugHWOC.AddStep(firstProcessSteps, candidateSelector.OriginalEqualizedImage.clone());
                //debugHWOC.AddStep(firstProcessSteps, candidateSelector.GetFinalImage(true).clone());

                //InitializeGallery(R.id.gallery1, firstProcessSteps);
                //if(i==i)
//                return;


                //finalCandidates.add(candidateSelector.GetFinalImage(true));
                Mat img = candidateSelector.GetFinalImage(true);
                //debugHWOC.AddStep(firstProcessSteps, finalCandidates.get(finalCandidates.size() - 1), 19);
                debugHWOC.AddStep(firstProcessSteps, img, 19);


                TimeProfiler.CheckPoint(5);


                //NOTA: ACA RECIEN OBTENER IMAGEN TAMAÑO REAL.
                debugHWOC.AddImage(secondProcessSteps, R.drawable.qpp, 19);
                //Mat img = finalCandidates.get(q);


                if (SHOW_PROCESS_DEBUG)
                    img = img.clone();

                CharacterSeparator characterSeparator = new CharacterSeparator(img);
                debugHWOC.AddStep(secondProcessSteps, characterSeparator.CurrentImage, 20);

                characterSeparator.AdaptiveThreshold();

                characterSeparator.FindCountourns();


                if (!characterSeparator.FilterCountourns()) {

                    debugHWOC.AddStep(secondProcessSteps, characterSeparator.ImageWithContourns, 21);
                    debugHWOC.AddImage(secondProcessSteps, R.drawable.filter_countourns, 22);

                    Log.d("filter", "q=" /*+ q*/ + " !FilterCountourns");
                    continue;
                }


                debugHWOC.AddStep(secondProcessSteps, characterSeparator.ImageWithContourns, 23);
                debugHWOC.AddStep(secondProcessSteps, characterSeparator.CleanedImage, 24);

                characterSeparator.CalculatePlateLength();

                characterSeparator.CalculateCharsPositions();

                //characterSeparator.CalculateHistrograms();
                if (CHARS)
                    characterSeparator.CropChars();
                else
                    characterSeparator.CropAll();


                String whiteList = "";
                String lastPlate = "";
                if (CHARS) {
                    for (int n = 0; n < characterSeparator.CroppedChars.size(); ++n) {

                        debugHWOC.AddImage(secondProcessSteps, R.drawable.npp, 25);
                        debugHWOC.AddStep(secondProcessSteps, characterSeparator.CroppedChars.get(n), 26);

                        switch (n) {
                            case 0://1
                                whiteList = GROUP1;
                                break;
                            case 2://3
                                plate += "-";
                                whiteList = GROUP2;
                                break;
                            case 4://5
                                plate += "-";
                                whiteList = GROUP3;
                                break;
                        }
                        MainActivity.baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, whiteList);


                        Mat m = characterSeparator.CroppedChars.get(n);
                        Bitmap bmp = Bitmap.createBitmap(m.cols(), m.rows(), Bitmap.Config.ARGB_8888);


                        Utils.matToBitmap(m, bmp);
                        MainActivity.baseApi.setImage(bmp);
                        String recognizedText = MainActivity.baseApi.getUTF8Text();
                        Log.d("output", "n=" + n + " text:" + recognizedText);
                        plate += recognizedText;

                    }
                } else {
                    debugHWOC.AddImage(secondProcessSteps, R.drawable.npp, 25);
                    for (int n = 0; n < 3; ++n) {


                        switch (n) {
                            case 0://1
                                whiteList = "ABCDEFGHIJKLNPRSTUVXYZW";
                                break;
                            case 1://3
                                plate += "-";
                                whiteList = "BCDFGHJKLPRSTVXYZW0123456789";
                                break;
                            case 2://5
                                plate += "-";
                                whiteList = "0123456789";
                                break;
                        }
                        MainActivity.baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, whiteList);


                        Mat m = characterSeparator.CroppedChars.get(0);
                        Bitmap bmp = Bitmap.createBitmap(m.cols(), m.rows(), Bitmap.Config.ARGB_8888);


                        Utils.matToBitmap(m, bmp);
                        MainActivity.baseApi.setImage(bmp);
                        String recognizedText = MainActivity.baseApi.getUTF8Text();
                        Log.d("output", "n=" + n + " text:" + recognizedText);
                        //recognizedText = recognizedText.trim();
                        plate += recognizedText;
                        lastPlate += process(recognizedText, n);//.substring(n*2, n*2+2);

                    }
                    finalPlate += lastPlate;
                }
                correctPlate = correctPlate(lastPlate);
                if (correctPlate)
                    break;
                plate += " || ";
                finalPlate += " || ";
            }
            if (correctPlate)
                break;
        }
    }
    private void CleanAll() {
        finalCandidates = new ArrayList<Mat>();
        firstProcessSteps = new ArrayList<Mat>();
        secondProcessSteps = new ArrayList<Mat>();
    }

}
