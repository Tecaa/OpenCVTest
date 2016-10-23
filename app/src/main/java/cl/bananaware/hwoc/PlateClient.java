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

import com.loopj.android.http.RequestParams;

import java.sql.Time;
import java.util.Date;
import java.util.List;

import cl.bananaware.hwoc.ApiRestClasses.Plate;
import cl.bananaware.hwoc.ApiRestClasses.PlateApiClient;
import cl.bananaware.hwoc.ApiRestClasses.Report;

/**
 * Created by fergu on 21-10-2016.
 */
public class PlateClient {
    private static final int ACCEPTABLE_CONFIDENCE = 60;
    // Acquire a reference to the system Location Manager
    Context context;
    Location currentLocation;
    PlateApiClient plateApiClient;
    LocationManager locationManager;
    List<Plate> stolenPlates;
    PlateRecognizer plateRecognizer;
    public PlateClient(Context cont) {
        context = cont;
        plateApiClient = new PlateApiClient();
        plateApiClient.getStolenPlatesRequest(new RequestParams(), new PlateApiClient.StolenPlatesArrived() {
            @Override
            public void callback(List<Plate> plates) {
                stolenPlates = plates;
            }
        });

        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        plateRecognizer = new PlateRecognizer();

// Define a listener that responds to location updates
        /*
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                currentLocation = location;
                Log.d("pos", currentLocation.toString());
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

// Register the listener with the Location Manager to receive location updates
        if (ActivityCompat.checkSelfPermission(this.context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this.context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);*/
    }

    Bitmap image;
    // Toma una captura y almacena la posicion del gps y la imagen.
    public void ProcessCapture(Bitmap img) {
        Location location = GetPosition();
        Log.d("api", location.toString());
        Plate plate = ImageProcess(img);
        if (plate != null) {
            InsertReport(location, image, plate);
        }
    }

    private void InsertReport(Location location, Bitmap image, Plate plate) {
        Date currentDate = new Date(System.currentTimeMillis());
        Report report = new Report(location, plate, currentDate, image);
        plateApiClient.InsertReport(report);
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
    private Plate ImageProcess(Bitmap bmp)
    {
        PlateRecognizer.PlateResult plateResult = plateRecognizer.Recognize(bmp);
        if (plateResult.Confidence >= ACCEPTABLE_CONFIDENCE) {
            Plate p = new Plate(plateResult.Plate);
            if (stolenPlates.contains(p))
                return p;
        }
        return null;
    }
}