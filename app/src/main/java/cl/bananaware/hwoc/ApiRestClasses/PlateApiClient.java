package cl.bananaware.hwoc.ApiRestClasses;

import android.app.DownloadManager;
import android.content.Context;
import android.util.Log;

import com.loopj.android.http.*;
import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;

import cl.bananaware.hwoc.ImageViewer;
import cz.msebera.android.httpclient.Header;


/**
 * Created by fergu on 21-10-2016.
 */
public class PlateApiClient {
    private final String TOKEN_KEY = "AnonymousToken";
    private final String TOKEN_VALUE = "65e0e866-03d6-428b-a20d-76be1673561e";
    private HashMap<String, String> apiMethods = new HashMap<String, String>() {{
        put("getStolenPlates", "https://api.backand.com/1/objects/plates");
        put("putReportImage", "https://api.backand.com/1/objects/action/reports?name=uploadImage");
        put("putReport", "https://api.backand.com/1/objects/reports");
    }};
    AsyncHttpClient client;
    private Context context;

    public interface StolenPlatesArrived{
        void callback(List<Plate> plates);
    }

    public interface InsertReportResponse{
        void callback(Boolean success);
    }

    ArrayList<StolenPlatesArrived> StolenPlatesArrivedObservers = new ArrayList<StolenPlatesArrived>();
    ArrayList<InsertReportResponse> InsertReportResultObservers = new ArrayList<InsertReportResponse>();


    public PlateApiClient(Context context)
    {
        this.context = context;
        connect();
    }

    private void connect()
    {
        if (!ImageViewer.USE_API_DEBUG)
            return;
        client = new AsyncHttpClient();
        client.addHeader(TOKEN_KEY, TOKEN_VALUE);
        //client.addHeader("Content-Type", "multipart/form-data; charset=utf-8");
        client.addHeader("Content-Type", "application/json");
        client.setTimeout(20 * 100);

    }
    public void getStolenPlatesRequest(StolenPlatesArrived observer){
        if (!ImageViewer.USE_API_DEBUG)
            return;

        RequestParams params = new RequestParams();
        StolenPlatesArrivedObservers.add(observer);
        client.get(apiMethods.get("getStolenPlates"), params ,new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject jo) {
                try {
                    List<Plate> plates = Plate.FromJSONArray(jo.getJSONArray("data"));
                    for (Plate p : plates)
                    {
                        Log.d("pos", p.plate);
                    }
                    for (StolenPlatesArrived ev :StolenPlatesArrivedObservers){
                        ev.callback(plates);
                    }

                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    Log.d("pos", "error");
                    e.printStackTrace();

                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                // Hide Progress Dialog
                // When Http response code is '404'
                if(statusCode == 404){
                    Log.d("pos", "Requested resource not found");
                }
                // When Http response code is '500'
                else if(statusCode == 500){
                    Log.d("pos", "Something went wrong at server end");
                }
                // When Http response code other than 404, 500
                else{
                    Log.d("pos", "Unexpected Error occcured! [Most common Error: Device might not be connected to Internet or remote server is not up and running]");

                }
            }

        });
    }

    public void InsertReport(final Report report, InsertReportResponse observer) {
        if (!ImageViewer.USE_API_DEBUG)
            return;

        RequestParams params = new RequestParams();
        params.put("filedata", report.image);
        //params.put("plate", report.plate);
        //params.put("position", new JSONArray(report.position));
        //params.put("image", new ByteArrayInputStream(report.image2), report.plate + ".jpg");
        //params.put("date", "2016-11-04 21:21:56Z");
        params.setUseJsonStreamer(true);

        InsertReportResultObservers.add(observer);
        client.post(apiMethods.get("putReportImage"), params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject jo) {
                // called when response HTTP status is "200 OK"

                RequestParams params = new RequestParams();
                params.put("plate", report.plate);
                params.put("position", new JSONArray(report.position));
                try {
                    params.put("image", jo.getString("url"));
                }
                catch (Exception e){}
                params.put("date", report.date);
                params.setUseJsonStreamer(true);

                client.post(apiMethods.get("putReport"), params, new JsonHttpResponseHandler() {

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        // called when response HTTP status is "200 OK"
                        Log.i("API RESULT", Integer.toString(statusCode));
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {
                        Log.i("API RESULT", Integer.toString(statusCode));
                    }

                });
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {
                Log.i("API RESULT", Integer.toString(statusCode));
            }
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject jo) {
                Log.i("API RESULT", Integer.toString(statusCode));
            }

        });
    }


}