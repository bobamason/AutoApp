package org.masonapps.autoapp.sections;


import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.masonapps.autoapp.MainActivity;
import org.masonapps.autoapp.R;
import org.masonapps.autoapp.database.AutoDatabase;
import org.masonapps.autoapp.database.AutoEntry;
import org.masonapps.autoapp.web.CarQueryApi;
import org.masonapps.autoapp.web.VolleySingleton;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class AddVehicleFragment extends DialogFragment {
    
    private static final String SELECT_YEAR = "Select Year";
    private static final String SELECT_MAKE = "Select Make";
    private static final String SELECT_MODEL = "Select Model";
    private static final String SELECT_TRIM = "Select Trim";
    private Spinner yearSpinner;
    private Spinner makeSpinner;
    private Spinner modelSpinner;
    private Spinner trimSpinner;
    private ProgressBar progressBar;
    private int year = -1;
    private String make = "";
    private String model = "";
    private String trim = "";
    private ArrayList<Integer> modelIDs = new ArrayList<>();
    private boolean busy = false;
    private OnVehicleAddedListener listener;


    public AddVehicleFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnVehicleAddedListener) {
            listener = (OnVehicleAddedListener) activity;
        } else {
            throw new RuntimeException(activity.toString()
                    + " must implement OnVehicleAddedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_add_vehicle, null);
        initUI(view);
        builder.setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (isReady() && !busy) {
                            AutoEntry entry = new AutoEntry();
                            entry.year = year;
                            entry.model = model;
                            entry.make = make;
                            entry.trim = trim.isEmpty() ? "None" : trim;
                            entry.modelId = modelID;
                            final JSONArray blankArray = new JSONArray();
                            entry.setDTCsError(blankArray.toString());
                            entry.setDTCsWarning(blankArray.toString());
                            entry.setDTCsCleared(blankArray.toString());
                            new AddVehicleTask(((MainActivity) getActivity()).getDatabaseWeakReference()).execute(entry);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        return builder.create();
    }

    private boolean isReady() {
        return year != -1 && !make.isEmpty() && !model.isEmpty() && modelID != -1;
    }

    private void makeToast(String msg){
        Toast.makeText(getActivity().getApplicationContext(), msg, Toast.LENGTH_LONG).show();
    }
    
    private void setProgressVisibility(boolean visible){
        busy = visible;
        try {
            progressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
            yearSpinner.setEnabled(!visible);
            makeSpinner.setEnabled(!visible);
            modelSpinner.setEnabled(!visible);
            trimSpinner.setEnabled(!visible);
        } catch (NullPointerException ignored){}
    }

    private int modelID = -1;
    private final Spinner.OnItemSelectedListener onItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            final String text = ((TextView) view).getText().toString();
            switch (parent.getId()){
                case R.id.yearSpinner:
                    if(!SELECT_YEAR.equals(text)) {
                        try {
                            year = Integer.parseInt(text);
                        } catch (NumberFormatException ignored) {
                        }
                        fetchMakes();
                        clearModels();
                        clearTrims();
                    } else {
                        clearMakes();
                        clearModels();
                        clearTrims();
                    }
                    break;
                case R.id.makeSpinner:
                    if(!SELECT_MAKE.equals(text)) {
                        make = text;
                        fetchModels();
                        clearTrims();
                    } else {
                        clearModels();
                        clearTrims();
                    }
                    break;
                case R.id.modelSpinner:
                    if(!SELECT_MODEL.equals(text)) {
                        model = text;
                        fetchTrims();
                    } else {
                        clearTrims();
                    }
                    break;
                case R.id.trimSpinner:
                    if(!SELECT_TRIM.equals(text)) {
                        trim = text;
                        modelID = modelIDs.get(position - 1);
                    } else {
                        modelID = modelIDs.get(0);
                    }
                    break;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    private void initUI(View view) {
        yearSpinner = (Spinner) view.findViewById(R.id.yearSpinner);
        yearSpinner.setOnItemSelectedListener(onItemSelectedListener);
        yearSpinner.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, new ArrayList<String>()));
        
        makeSpinner = (Spinner) view.findViewById(R.id.makeSpinner);
        makeSpinner.setOnItemSelectedListener(onItemSelectedListener);
        makeSpinner.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, new ArrayList<String>()));
        
        modelSpinner = (Spinner) view.findViewById(R.id.modelSpinner);
        modelSpinner.setOnItemSelectedListener(onItemSelectedListener);
        modelSpinner.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, new ArrayList<String>()));
        
        trimSpinner = (Spinner) view.findViewById(R.id.trimSpinner);
        trimSpinner.setOnItemSelectedListener(onItemSelectedListener);
        trimSpinner.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, new ArrayList<String>()));
        
        progressBar = (ProgressBar) view.findViewById(R.id.addVehicleProgress);
        fetchYears();
    }

    private void fetchYears() {
        setProgressVisibility(true);
        ((ArrayAdapter<String>) yearSpinner.getAdapter()).clear();
        year = -1;
        VolleySingleton.getInstance(getActivity()).addToRequestQueue(new StringRequest(Request.Method.GET, CarQueryApi.getYearsURL(), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                JSONObject json = CarQueryApi.extractJSON(response);
                if(json == null) return;
                try {
                    ArrayList<String> years = CarQueryApi.listYears(json);
                    ArrayAdapter<String> adapter = (ArrayAdapter<String>) yearSpinner.getAdapter();
                    adapter.add(SELECT_YEAR);
                    adapter.addAll(years);
                } catch (JSONException e) {
                    e.printStackTrace();
                    makeToast(e.getMessage());
                } catch (NullPointerException ignored){}
                setProgressVisibility(false);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                makeToast(error.getMessage());
                setProgressVisibility(false);
            }
        }));
    }

    private void fetchMakes() {
        if(year == -1){
            return;
        }
        setProgressVisibility(true);
        ((ArrayAdapter<String>) makeSpinner.getAdapter()).clear();
        make = "";
        VolleySingleton.getInstance(getActivity()).addToRequestQueue(new StringRequest(Request.Method.GET, CarQueryApi.getMakesURL(year), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                JSONObject json = CarQueryApi.extractJSON(response);
                if(json == null) return;
                try {
                    ArrayList<String> makes = CarQueryApi.listMakes(json);
                    ArrayAdapter<String> adapter = (ArrayAdapter<String>) makeSpinner.getAdapter();
                    adapter.add(SELECT_MAKE);
                    adapter.addAll(makes);
                } catch (JSONException e) {
                    e.printStackTrace();
                    makeToast(e.getMessage());
                } catch (NullPointerException ignored){}
                setProgressVisibility(false);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                makeToast(error.getMessage());
                setProgressVisibility(false);
            }
        }));
    }

    private void fetchModels() {
        if(year == -1 || make.isEmpty()){
            return;
        }
        setProgressVisibility(true);
        ((ArrayAdapter<String>) modelSpinner.getAdapter()).clear();
        model = "";
        VolleySingleton.getInstance(getActivity()).addToRequestQueue(new StringRequest(Request.Method.GET, CarQueryApi.getModelsURL(year, make), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                JSONObject json = CarQueryApi.extractJSON(response);
                if(json == null) return;
                try {
                    ArrayList<String> models = CarQueryApi.listModels(json);
                    ArrayAdapter<String> adapter = (ArrayAdapter<String>) modelSpinner.getAdapter();
                    adapter.add(SELECT_MODEL);
                    adapter.addAll(models);
                } catch (JSONException e) {
                    e.printStackTrace();
                    makeToast(e.getMessage());
                } catch (NullPointerException ignored){}
                setProgressVisibility(false);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                makeToast(error.getMessage());
                setProgressVisibility(false);
            }
        }));
    }

    private void fetchTrims() {
        if(year == -1 || make.isEmpty() || model.isEmpty()){
            return;
        }
        setProgressVisibility(true);
        ((ArrayAdapter<String>) trimSpinner.getAdapter()).clear();
        modelIDs.clear();
        trim = "";
        VolleySingleton.getInstance(getActivity()).addToRequestQueue(new StringRequest(Request.Method.GET, CarQueryApi.getTrimsURL(year, make, model), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                JSONObject json = CarQueryApi.extractJSON(response);
                if(json == null) return;
                try {
                    modelIDs.addAll(CarQueryApi.listModelIDs(json));
                    ArrayList<String> trims = CarQueryApi.listTrims(json);
                    ArrayAdapter<String> adapter = (ArrayAdapter<String>) trimSpinner.getAdapter();
                    adapter.add(SELECT_TRIM);
                    adapter.addAll(trims);
                } catch (JSONException e) {
                    e.printStackTrace();
                    makeToast(e.getMessage());
                } catch (NullPointerException ignored){}
                setProgressVisibility(false);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                makeToast(error.getMessage());
                setProgressVisibility(false);
            }
        }));
    }
    
    private void clearMakes() {
        ((ArrayAdapter<String>) makeSpinner.getAdapter()).clear();
        make = "";
    }
    
    private void clearModels() {
        ((ArrayAdapter<String>) modelSpinner.getAdapter()).clear();
        model = "";
    }

    private void clearTrims() {
        ((ArrayAdapter<String>) trimSpinner.getAdapter()).clear();
        trim = "";
        modelIDs.clear();
        modelID = -1;
    }
    
    private class AddVehicleTask extends AsyncTask<AutoEntry, Void, AutoEntry>{

        private final WeakReference<AutoDatabase> databaseWeakReference;

        AddVehicleTask(WeakReference<AutoDatabase> databaseWeakReference){
            this.databaseWeakReference = databaseWeakReference;
        }

        @Override
        protected void onPreExecute() {
            setProgressVisibility(true);
        }

        @Override
        protected AutoEntry doInBackground(AutoEntry... params) {
            AutoDatabase db = databaseWeakReference.get();
            final AutoEntry entry = params[0];
            if(db != null){
                db.insertEntry(entry);
            }
            return entry;
        }

        @Override
        protected void onPostExecute(AutoEntry entry) {
            setProgressVisibility(false);
            AddVehicleFragment.this.dismiss();
            if(listener != null && entry.id != -1){
                listener.vehicleAdded(entry);
            }
        }
    }
    
    public interface OnVehicleAddedListener{
        void vehicleAdded(AutoEntry entry);
    }
}