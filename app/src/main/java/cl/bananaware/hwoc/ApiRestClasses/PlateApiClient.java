package cl.bananaware.hwoc.ApiRestClasses;

import android.app.DownloadManager;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;

/**
 * Created by fergu on 21-10-2016.
 */
public class PlateApiClient {
    private final String TOKEN_KEY = "AnonymousToken";
    private final String TOKEN_VALUE = "65e0e866-03d6-428b-a20d-76be1673561e";
    private HashMap<String, String> apiMethods = new HashMap<String, String>() {{
        put("getStolenPlates","https://api.backand.com/1/objects/plates");
        put("putReport","https://api.backand.com/1/objects/reports");
    }};
    AsyncHttpClient client;


    public interface StolenPlatesArrived{
        void callback(List<Plate> plates);
    }

    public interface InsertReportResponse{
        void callback(Boolean success);
    }

    ArrayList<StolenPlatesArrived> StolenPlatesArrivedObservers = new ArrayList<StolenPlatesArrived>();
    ArrayList<InsertReportResponse> InsertReportResultObservers = new ArrayList<InsertReportResponse>();


    public PlateApiClient()
    {
        connect();
    }

    private void connect()
    {
        client = new AsyncHttpClient();
        client.addHeader(TOKEN_KEY, TOKEN_VALUE);
    }
    public void getStolenPlatesRequest(StolenPlatesArrived observer){
        RequestParams params = new RequestParams();
        StolenPlatesArrivedObservers.add(observer);
        client.get(apiMethods.get("getStolenPlates"), params ,new AsyncHttpResponseHandler() {
            // When the response returned by REST has Http response code '200'
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject jo = new JSONObject(response);
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
            // When the response returned by REST has Http response code other than '200'
            @Override
            public void onFailure(int statusCode, Throwable error,
                                  String content) {
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

    public void InsertReport(Report report, InsertReportResponse observer) {
        RequestParams param = new RequestParams();
        param.put("report", report);
        InsertReportResultObservers.add(observer);
        client.post(apiMethods.get("putReport") ,param, new AsyncHttpResponseHandler() {
            // When the response returned by REST has Http response code '200'
            @Override
            public void onSuccess(String response) {

                for (InsertReportResponse ev : InsertReportResultObservers) {
                    ev.callback(true);
                }
            }
            // When the response returned by REST has Http response code other than '200'
            @Override
            public void onFailure(int statusCode, Throwable error,
                                  String content) {
                // Hide Progress Dialog
                // When Http response code is '404'
                for (InsertReportResponse ev : InsertReportResultObservers) {
                    ev.callback(false);
                }
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


}