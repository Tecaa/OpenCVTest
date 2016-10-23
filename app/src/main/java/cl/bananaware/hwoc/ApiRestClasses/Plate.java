package cl.bananaware.hwoc.ApiRestClasses;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fergu on 21-10-2016.
 */
public class Plate {
    public String plate;
    public Plate()
    {
        plate = "";
    }
    public Plate(String plate)
    {
        this.plate = plate;
    }
    public static List<Plate> FromJSONArray(JSONArray jsonArray) throws JSONException {

        List<Plate> plates = new ArrayList<Plate>();
        for (int i=0; i<jsonArray.length(); ++i) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            Plate p = new Plate();
            p.plate = jsonObject.getString("plate");
            plates.add(p);
        }
        return plates;
    }

    public static List<Plate> FromApiResponseArray(String response) throws JSONException {
        JSONArray array = new JSONArray(response);
        return Plate.FromJSONArray(array);
    }
}
