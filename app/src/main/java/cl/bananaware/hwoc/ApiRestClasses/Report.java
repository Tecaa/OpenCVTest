package cl.bananaware.hwoc.ApiRestClasses;

import android.graphics.Bitmap;
import android.location.Location;
import android.provider.ContactsContract;
import android.view.WindowManager;

import java.security.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by fergu on 23-10-2016.
 */
public class Report {
    /*Location Location;
    Plate Plate;
    Date Time;
    Bitmap Image;
    */
    List<Float> position;
    String plate;
    String date;
    String image;

    public Report(Location location, Plate plate, Date date, Bitmap image) {/*
        Location = location;
        Plate = plate;
        Time = date;
        Image = image;*/
        SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        this.position = new ArrayList<Float>();
        position.add(30.2f);//(float) location.getLatitude();
        position.add(24.2f);//(float) location.getLongitude();
        this.plate = plate.plate;
        this.date = s.format(date);//"2016-11-04T21:21:56.298Z";
        this.image = "image";
    }

    public String asdf()

    {
        return "{" +
                "  \"position\": [" +
                "    30.2," +
                "    24.2" +
                "  ]," +
                "  \"plate\": \"CGVL87\"," +
                "  \"date\": \"2016-11-04T21:21:56.298Z\"," +
                "  \"image\": \"text\"" +
                "}";
    }
    /*public String toString() {
        SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");*/
        /*return " { position: [30.2, 24.2"
                + " ],"
                + " plate: '" + this.Plate.plate + "',"
                + " date: '" + s.format(this.Time) + "',"
                + " image: " + "'text' }";*/
/*
        return " {" +
                "    'position': '[" +
                "      13.2," +
                "      13.2" +
                "    ]'," +
                "    'plate': 'text'," +
                "    'date': '2016-11-04T21:21:56.298Z'," +
                "    'image': 'text'" +
                "  }";*/
        /*
        return "position: [" + this.Location.getLatitude() + "," + this.Location.getLongitude()
                + "],"
                + "plate: '" + this.Plate.plate + "',"
                + "date: '" + Time.toString() + "'"
                + "image: " + "'text'";*/
    //}
}
