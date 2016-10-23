package cl.bananaware.hwoc.ApiRestClasses;

import android.graphics.Bitmap;
import android.location.Location;
import android.provider.ContactsContract;
import android.view.WindowManager;

import java.security.Timestamp;
import java.util.Date;

/**
 * Created by fergu on 23-10-2016.
 */
public class Report {
    Location Location;
    Plate Plate;
    Date Time;
    Bitmap Image;

    public Report(Location location, Plate plate, Date date, Bitmap image)
    {
        Location = location;
        Plate = plate;
        Time = date;
        Image = image;
    }
}
