package cl.bananaware.hwoc;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fergu on 04-11-2016.
 */
public class LocationController implements LocationListener {

    private static final float METERS_MIN = 2;
    LocationManager locationManager;
    Context context;
    private Location currentLocation;
    private Location lastLocationReported;
    private List<String> lastLocationPlates;
    public LocationController(Context cont)
    {
        lastLocationPlates = new ArrayList<String>();
        this.context = cont;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(cont, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(cont, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            ; // no se pudieron dar permisos
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
        Log.d("loc", "requestLocation");
    }

    public Location GetCurrentLocation()
    {
        return currentLocation;
    }
    @Override
    public void onLocationChanged(Location location) {
        Log.d("loc", "onLocatinoChange");
        Log.d("loc", "location change " + location.getLatitude());
        this.currentLocation = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public boolean IsAwayEnough(Location location, String plate) {
        if (lastLocationPlates.contains(plate)) {
            float distance = lastLocationReported.distanceTo(location);
            Log.d("distance", String.valueOf(distance));
            if (lastLocationReported == null || distance >= METERS_MIN) {
                lastLocationReported = location;
                lastLocationPlates.clear();
                lastLocationPlates.add(plate);
                return true;
            }
            else
            {
                return false;
            }
        }
        else
        {
            lastLocationReported = location;
            lastLocationPlates.add(plate);
            return true;
        }
    }
}