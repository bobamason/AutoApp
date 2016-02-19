package org.masonapps.autoapp.web;

import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Bob on 12/1/2015.
 */
public class CarQueryApi {

    public static final String BASE_URL = "http://www.carqueryapi.com/api/0.3/?callback=?&cmd=";

    public static String getYearsURL() {
        return String.format("%sgetYears", BASE_URL);
    }


    public static String getMakesURL(int year) {
        return String.format("%sgetMakes&year=%d", BASE_URL, year);
    }

    public static String getModelsURL(int year, String make) {
        return String.format("%sgetModels&make=%s&year=%d", BASE_URL, make.replace(' ', '+').toLowerCase(), year);
    }

    public static String getTrimsURL(int year, String make, String model) {
        return String.format("%sgetTrims&make=%s&model=%s&year=%d", BASE_URL, make.replace(' ', '+').toLowerCase(), model.replace(' ', '+').toLowerCase(), year);
    }

    public static String getInfoURL(int modelId) {
        return String.format("%sgetModel&model=%d", BASE_URL, modelId);
    }

    public static ArrayList<String> listYears(JSONObject json) throws JSONException {
        final ArrayList<String> list = new ArrayList<>();
        int min, max;
        JSONObject yearsObj = json.getJSONObject("Years");
        min = yearsObj.optInt("min_year");
        max = yearsObj.optInt("max_year");
        for (int i = min; i < max; i++) {
            list.add(String.valueOf(i));
        }
        return list;
    }

    public static ArrayList<String> listMakes(JSONObject json) throws JSONException {
        final ArrayList<String> list = new ArrayList<>();
        JSONArray makes = json.getJSONArray("Makes");
        for (int i = 0; i < makes.length(); i++) {
            list.add(makes.getJSONObject(i).getString("make_display"));
        }
        return list;
    }

    public static ArrayList<String> listModels(JSONObject json) throws JSONException {
        final ArrayList<String> list = new ArrayList<>();
        JSONArray models = json.getJSONArray("Models");
        for (int i = 0; i < models.length(); i++) {
            list.add(models.getJSONObject(i).getString("model_name"));
        }
        return list;
    }

    public static ArrayList<String> listTrims(JSONObject json) throws JSONException {
        final ArrayList<String> list = new ArrayList<>();
        JSONArray trims = json.getJSONArray("Trims");
        for (int i = 0; i < trims.length(); i++) {
            final String trim = trims.getJSONObject(i).getString("model_trim");
            list.add(trim.isEmpty() ? "None" : trim);
        }
        return list;
    }

    public static ArrayList<Integer> listModelIDs(JSONObject json) throws JSONException {
        final ArrayList<Integer> list = new ArrayList<>();
        JSONArray trims = json.getJSONArray("Trims");
        for (int i = 0; i < trims.length(); i++) {
            final int id = trims.getJSONObject(i).optInt("model_id");
            list.add(id);
        }
        return list;
    }
    
    public static @Nullable JSONObject extractJSON(String string){
        int start = -1;
        int end = -1;

        for (int i = 0; i < string.length(); i++) {
            if(start == -1 && string.charAt(i) == '{'){
                start = i;
            }
            if (string.charAt(i) == '}'){
                end = i + 1;
            }
        }
        
        if(start >= 0 && end > start) {
            try {
                return new JSONObject(string.substring(start, end));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
