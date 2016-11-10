package cl.bananaware.hwoc;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cl.bananaware.hwoc.ApiRestClasses.Plate;
import cl.bananaware.hwoc.ApiRestClasses.PlateApiClient;
import cl.bananaware.hwoc.ApiRestClasses.Report;
import cl.bananaware.hwoc.ImageProcessing.PlateRecognizer;
import cl.bananaware.hwoc.ImageProcessing.PlateResult;

/**
 * Created by fergu on 21-10-2016.
 */
public class PlateProcessSystem{

    // Acquire a reference to the system Location Manager
    Context context;
    Location currentLocation;
    PlateApiClient plateApiClient;
    List<Plate> stolenPlates;
    PlateRecognizer plateRecognizer;
    public PlateResult LastPlateReaded;

    public PlateProcessSystem(Context cont) {
        context = cont;
        stolenPlates = new ArrayList<Plate>();
        // Test data


        plateApiClient = new PlateApiClient(context);
        plateApiClient.getStolenPlatesRequest(new PlateApiClient.StolenPlatesArrived() {
            @Override
            public void callback(List<Plate> plates) {
                stolenPlates = plates;
                stolenPlates.add(new Plate("CGVL87"));
            }
        });

        // Acquire a reference to the system Location Manager



        //plateRecognizer = new PlateRecognizer();
    }

    Bitmap image;
    Mat m_image;
    // Toma una captura y almacena la posicion del gps y la imagen.
    public PlateResult ProcessCapture(Bitmap img, Boolean justProcess) {
        Mat m = new Mat();
        image = img;
        Utils.bitmapToMat(image, m);
        return InnerProcessCapture(m, justProcess);
    }

    public PlateResult ProcessCapture(Mat mat, Boolean justProcess) {
        return InnerProcessCapture(mat, justProcess);
    }

    private PlateResult InnerProcessCapture(Mat mat, Boolean justProcess) {
        m_image = mat;
        PlateResult plate = ImageProcess(mat, justProcess);
        Location location = MainActivity.locationController.GetCurrentLocation();

        if (plate != null) {
            InsertReport(location, image, new Plate(plate.Plate));
        }
        return plate;
    }
    private void InsertReport(Location location, Bitmap image, Plate plate) {
        Date currentDate = new Date(System.currentTimeMillis());
        Report report = new Report(location, plate, currentDate, image);
        plateApiClient.InsertReport(report, new PlateApiClient.InsertReportResponse() {
            @Override
            public void callback(Boolean correct) {
                Log.d("test", correct.toString());
            }
        });
    }


    // Se procesa la captura realizada
    private PlateResult ImageProcess(Mat mat, boolean justProcess)
    {
        plateRecognizer = new PlateRecognizer();
        plateRecognizer.InitDebug(debugHWOC);
        LastPlateReaded = plateRecognizer.Recognize(m_image);

        if (justProcess)
            return LastPlateReaded;


        Plate p = new Plate(LastPlateReaded.Plate);
        if (stolenPlates.contains(p))
            return LastPlateReaded;

        return null;
    }

    public void InitDebug(DebugHWOC debugHWOC) {
        this.debugHWOC = debugHWOC;
        //plateRecognizer.InitDebug(debugHWOC);
    }
    private DebugHWOC debugHWOC;

}