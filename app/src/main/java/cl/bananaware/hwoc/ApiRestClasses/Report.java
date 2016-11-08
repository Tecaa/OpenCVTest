package cl.bananaware.hwoc.ApiRestClasses;

import android.graphics.Bitmap;
import android.location.Location;
import android.provider.ContactsContract;
import android.util.Base64;
import android.view.WindowManager;

import java.io.ByteArrayOutputStream;
import java.security.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by fergu on 23-10-2016.
 */
public class Report {
    public final byte[] image2;
    /*Location Location;
        Plate Plate;
        Date Time;
        Bitmap Image;
        */
    List<Float> position;
    String plate;
    String date;
    String image;

    public Report(Location location, Plate plate, Date date, Bitmap image) {
        SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        this.position = new ArrayList<Float>();
        if (location == null)
        {
            position.add(0.0f);
            position.add(0.0f);
        }
        else
        {
            position.add((float) location.getLatitude());
            position.add((float) location.getLongitude());
        }

        this.plate = plate.plate;
        this.date = s.format(date);//"2016-11-04T21:21:56.298Z";

        this.image2 = encodeTobyteArray(scaleDown(image, 600, false));

        //this.image = encodeTobase64(scaleDown(image, 1000, false));
        //this.image = "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";
    }

    public static Bitmap scaleDown(Bitmap realImage, float maxImageSize,
                                   boolean filter) {
        float ratio = Math.min(
                (float) maxImageSize / realImage.getWidth(),
                (float) maxImageSize / realImage.getHeight());
        int width = Math.round((float) ratio * realImage.getWidth());
        int height = Math.round((float) ratio * realImage.getHeight());

        return Bitmap.createScaledBitmap(realImage, width,
                height, filter);
    }

    public static String encodeTobase64(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] b = baos.toByteArray();
        return Base64.encodeToString(b,Base64.DEFAULT);
    }

    public static byte[] encodeTobyteArray(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        return baos.toByteArray();
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
