package org.masonapps.autoapp.sections;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.masonapps.autoapp.MainActivity;
import org.masonapps.autoapp.R;
import org.masonapps.autoapp.bluetooth.BluetoothActivity;
import org.masonapps.autoapp.views.GraphView;

import java.util.Timer;
import java.util.TimerTask;

public class GraphDataFragment extends Fragment {


    private Timer timer;
    private CommandTask task;
    private GraphView graphView;
    private boolean connected = false;

    private final BluetoothActivity.OnBluetoothEventListener listener = new BluetoothActivity.OnBluetoothEventListener() {
        @Override
        public void onReadLine(String line) {
            Log.d("graph data", "line = " + line);
            try {
                graphView.setLabel("raw data = " + line);
                if (line.toUpperCase().contains("11")) {
                    line = line.replaceAll("\\s", "");
                    if (line.length() > 4) {
                        final int rawValue = Integer.parseInt(line.substring(4, 6), 16);
                        Log.d("graph data", "rawValue = " + rawValue);
                        final float value = rawValue / 255f * 100f;
//                        final float value = rawValue * 3f;
                        Log.d("graph data", "value = " + value);
                        graphView.updateValue(Math.round(value));
                    }
                }
                timer.schedule(task, 500);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConnected() {
            connected = true;
            startCommunication();
        }

        @Override
        public void onDisconnected() {
            connected = false;
            stopCommunication();
        }
    };

    private void startCommunication() {
        timer = new Timer();
        task = new CommandTask("0111");
//        task = new CommandTask("010A");
        timer.schedule(task, 500);
    }

    public GraphDataFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_graph_data, container, false);
        graphView = view.findViewById(R.id.graphView);
        graphView.setMin(0f);
        graphView.setMax(100f);
        graphView.setUnits("%");
        graphView.setLabel("Throttle Position");
//        graphView.setLabel("Fuel Pressure");
        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            final MainActivity activity = ((MainActivity) getActivity());
            connected = activity.isDeviceCompatible();
            activity.addOnBluetoothEventListener(listener);
        }
        if (connected) startCommunication();
    }

    @Override
    public void onPause() {
        if (getActivity() instanceof MainActivity)
            ((MainActivity) getActivity()).removeOnBluetoothEventListener(listener);
        stopCommunication();
        timer = null;
        task = null;
        super.onPause();
    }

    private void stopCommunication() {
        if (timer != null) timer.cancel();
    }

    private class CommandTask extends TimerTask {

        private final String command;

        public CommandTask(String command) {
            super();
            this.command = command;
        }

        @Override
        public void run() {
            try {
                if (getActivity() instanceof BluetoothActivity)
                    ((BluetoothActivity) getActivity()).write(command + "\r\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
