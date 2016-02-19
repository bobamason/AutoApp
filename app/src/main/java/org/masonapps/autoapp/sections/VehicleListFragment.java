package org.masonapps.autoapp.sections;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.masonapps.autoapp.MainActivity;
import org.masonapps.autoapp.R;
import org.masonapps.autoapp.database.AutoDatabase;
import org.masonapps.autoapp.database.AutoEntry;

import java.lang.ref.WeakReference;
import java.util.ArrayList;


public class VehicleListFragment extends Fragment {

    private int mColumnCount = 1;
    private RecyclerView recyclerView;
    private OnVehicleInteractionListener listener;

    public VehicleListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_vehicle_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            recyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }
            recyclerView.setAdapter(new VehicleListAdapter(new ArrayList<AutoEntry>(), listener));
            new LoadVehicleListTask(((MainActivity)getActivity()).getDatabaseWeakReference()).execute();
            final VehicleListAdapter adapter = new VehicleListAdapter(new ArrayList<AutoEntry>(), listener);
            recyclerView.setAdapter(adapter);
        }
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnVehicleInteractionListener) {
            listener = (OnVehicleInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnVehicleInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    public void notifyVehicleAdded(AutoEntry entry) {
        if(recyclerView != null){
            final VehicleListAdapter adapter = (VehicleListAdapter) recyclerView.getAdapter();
            adapter.autoEntries.add(0, entry);
            adapter.notifyItemInserted(0);
        }
    }

    public interface OnVehicleInteractionListener {
        void onDeleteClicked(AutoEntry entry);
        void onEditClicked(AutoEntry entry);
        void onSetClicked(AutoEntry entry);
    }

    private class LoadVehicleListTask extends AsyncTask<Void, Void, ArrayList<AutoEntry>> {

        private final WeakReference<AutoDatabase> databaseWeakReference;

        LoadVehicleListTask(WeakReference<AutoDatabase> databaseWeakReference){
            this.databaseWeakReference = databaseWeakReference;
        }

        @Override
        protected void onPreExecute() {
            ((MainActivity)getActivity()).setProgressVisibility(true);
        }

        @Override
        protected ArrayList<AutoEntry> doInBackground(Void... params) {
            AutoDatabase db = databaseWeakReference.get();
            if(db == null)
                return null;
            return db.getAllEntries();
        }

        @Override
        protected void onPostExecute(ArrayList<AutoEntry> autoEntries) {
            if(recyclerView != null) {
                final VehicleListAdapter adapter = (VehicleListAdapter) recyclerView.getAdapter();
                adapter.autoEntries.clear();
                adapter.autoEntries.addAll(autoEntries);
                adapter.notifyDataSetChanged();
            }
            final MainActivity activity = (MainActivity) getActivity();
            if(activity != null)
                activity.setProgressVisibility(false);
        }
    }
}
