package org.masonapps.autoapp.database;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

/**
 * Created by Bob on 11/27/2015.
 */
public class AutoEntry {

    public static final String DTC_ERRORS = "dtcErrors";
    public static final String DTC_WARNINGS = "dtcWarnings";
    public static final String DTC_OLD = "dtcOld";
    public long id = -1;
    public int year = -1;
    public String make = "";
    public String model = "";
    private boolean scannedForDTC = false;
    private ArrayList<String> dtcsError = new ArrayList<>();
    private ArrayList<String> dtcsWarnings = new ArrayList<>();
    private ArrayList<String> dtcsCleared = new ArrayList<>();
    public int modelId;
    public String trim;

    public AutoEntry() {
    }
    
    

    public boolean hasScannedForDTC() {
        return scannedForDTC;
    }

    public void setDTCsError(String string) {
        dtcsError.clear();
        parseArray(string, dtcsError);
    }

    public void setDTCsWarning(String string) {
        dtcsWarnings.clear();
        parseArray(string, dtcsWarnings);
    }


    public void setDTCsCleared(String string) {
        dtcsCleared.clear();
        parseArray(string, dtcsCleared);
    }
    
    public String getDtcErrors(){
        return arrayToString(dtcsError);
    }
    
    public String getDtcWarnings(){
        return arrayToString(dtcsWarnings);
    }
    
    public String getDtcsCleared(){
        return arrayToString(dtcsCleared);
    }
    
    private static void parseArray(String string, ArrayList<String> array){
        try {
            final JSONArray jsonArray = new JSONArray(string);
            for (int i = 0; i < jsonArray.length(); i++) {
                array.add(jsonArray.getString(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    private static String arrayToString(ArrayList<String> array){
        final JSONArray jsonArray = new JSONArray();
        for (String s : array) {
            jsonArray.put(s);
        }
        return jsonArray.toString();
    }
}
