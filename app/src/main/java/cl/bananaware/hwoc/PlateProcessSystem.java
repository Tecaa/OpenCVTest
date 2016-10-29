package cl.bananaware.hwoc;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
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
public class PlateProcessSystem {

    // Acquire a reference to the system Location Manager
    Context context;
    Location currentLocation;
    PlateApiClient plateApiClient;
    LocationManager locationManager;
    List<Plate> stolenPlates;
    PlateRecognizer plateRecognizer;
    public PlateResult LastPlateReaded;
    public PlateProcessSystem(Context cont) {
        context = cont;
        stolenPlates = new ArrayList<Plate>();
        // Test data

        stolenPlates.add(new Plate("ZU3520"));

        plateApiClient = new PlateApiClient();
        plateApiClient.getStolenPlatesRequest(new PlateApiClient.StolenPlatesArrived() {
            @Override
            public void callback(List<Plate> plates) {
                stolenPlates = plates;
            }
        });

        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        plateRecognizer = new PlateRecognizer();
    }

    Bitmap image;
    Mat m_image;
    // Toma una captura y almacena la posicion del gps y la imagen.
    public PlateResult ProcessCapture(Bitmap img, Boolean justProcess) {
        m_image = new Mat();
        image = img;
        Utils.bitmapToMat(image, m_image);
        return ProcessCapture(m_image, justProcess);
    }

    public PlateResult ProcessCapture(Mat mat, Boolean justProcess) {
        PlateResult plate = ImageProcess(mat, justProcess);
        Location location = GetPosition();

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

    private Location GetPosition() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.


            return null;
        }
        return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

    }


    // Se procesa la captura realizada
    private PlateResult ImageProcess(Mat mat, boolean justProcess)
    {
        LastPlateReaded = plateRecognizer.Recognize(m_image);

        if (justProcess)
            return LastPlateReaded;


        Plate p = new Plate(LastPlateReaded.Plate);
        if (stolenPlates.contains(p))
            return LastPlateReaded;

        return null;
    }

    public void InitDebug(DebugHWOC debugHWOC) {
        plateRecognizer.InitDebug(debugHWOC);
    }


}