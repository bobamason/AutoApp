package org.masonapps.autoapp.sections;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
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
            if (line.toUpperCase().contains("0C")) {
                try{
                    line = line.replaceAll("\\s", "");
                    if (line.length() > 4) {
                        final int value = Math.round(Integer.parseInt(line.substring(line.length() - 4), 16) / 4f);
                        graphView.updateValue(value);
                    }
                }catch (NumberFormatException ignored){}
                timer.schedule(task, 50);
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
        task = new CommandTask("01 0C");
        timer.schedule(task, 500, 120);
    }

    public GraphDataFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_graph_data, container, false);
        graphView = view.findViewById(R.id.graphView);
        graphView.setMin(0);
        graphView.setMax(6000);
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

    private class CommandTask extends TimerTask{

        private final String command;

        public CommandTask(String command) {
            super();
            this.command = command;
        }

        @Override
        public void run() {
            ((MainActivity)getActivity()).write(command + "\r\n");
        }
    }
}
