package org.masonapps.autoapp.sections;


import android.os.Bundle;
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


    private GraphView voltageGraph;
//    private GraphView rpmGraph;
    private boolean connected = false;
    private float voltage = 0f;
    private float rpm = 0f;
    
    private final BluetoothActivity.OnBluetoothEventListener listener = new BluetoothActivity.OnBluetoothEventListener() {
        @Override
        public void onReadLine(String line) {
            if(line.toUpperCase().contains("V")){
                try{
                    voltage = Float.parseFloat(line.substring(0, line.indexOf("V")));
                    voltageGraph.updateValue(voltage);
                    if(voltage > voltageGraph.getMax()){
                        voltageGraph.setMax(voltageGraph.getMax() + 3f);
                    }
                }catch (NumberFormatException ignored){}
//                rpmTimer.schedule(rpmTask, 20);
            }
//            else if(line.toUpperCase().contains("0C")){
//                try{
//                    line = line.replaceAll("\\s", "");
//                    if(line.length() > 4) {
//                        rpm = Math.round(Integer.parseInt(line.substring(line.length() - 4), 16) / 4f);
//                        rpmGraph.updateValue(rpm);
//                    }
//                }catch (NumberFormatException ignored){}
//                voltageTimer.schedule(voltageTask, 50);
//            }
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
        voltageTimer = new Timer();
//        rpmTimer = new Timer();
        voltageTask = new CommandTask("ATRV");
        voltageTimer.schedule(voltageTask, 500, 120);
//        rpmTask = new CommandTask("01 0C");
//        rpmTimer.schedule(rpmTask, 0);
    }

    private Timer voltageTimer;
//    private Timer rpmTimer;
    private CommandTask voltageTask;
//    private CommandTask rpmTask;

    public GraphDataFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_graph_data, container, false);
        voltageGraph = (GraphView) view.findViewById(R.id.voltageGraph);
//        rpmGraph = (GraphView) view.findViewById(R.id.rpmGraph);
        return view;
    }



    @Override
    public void onResume() {
        super.onResume();
        final MainActivity activity = (MainActivity) getActivity();
        connected = activity.isDeviceCompatible();
        activity.addOnBluetoothEventListener(listener);
        if(connected) startCommunication();
    }

    @Override
    public void onPause() {
        ((MainActivity) getActivity()).removeOnBluetoothEventListener(listener);
        stopCommunication();
        voltageTimer = null;
//        rpmTimer = null;
        voltageTask = null;
//        rpmTask = null;
        super.onPause();
    }

    private void stopCommunication() {
        if(voltageTimer != null) voltageTimer.cancel();
//        rpmTimer.cancel();
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
