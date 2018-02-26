package org.masonapps.autoapp.bluetooth;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by Bob on 10/20/2015.
 */
public abstract class BluetoothActivity extends AppCompatActivity {

    private static final String DEVICE_LIST_TAG = "deviceList";
    private static final int ENABLE_REQUEST_CODE = 1002;
    public ArrayList<OnBluetoothEventListener> listeners = new ArrayList<>();
    protected Messenger btService;
    private Messenger incomingMessenger = new Messenger(new IncomingHandler(new WeakReference<>(this)));
    private boolean serviceBound = false;
    private ArrayList<BluetoothDevice> devices = new ArrayList<>();
    private boolean isConnected = false;
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            btService = new Messenger(service);
            try {
                final Message msg = Message.obtain();
                msg.what = BluetoothService.MESSAGE_REG_CLIENT;
                msg.replyTo = incomingMessenger;
                btService.send(msg);
                serviceBound = true;
                displayDeviceListDialog();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            isConnected = false;
            btService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        init();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unBindService();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == ENABLE_REQUEST_CODE) {
            initialize();
        }
    }

    protected void init() {
        if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            initialize();
        } else {
            requestEnableBluetooth();
        }
    }

    public void requestEnableBluetooth() {
        startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), ENABLE_REQUEST_CODE);
    }

    private void initialize() {
        bindService(new Intent(BluetoothActivity.this, BluetoothService.class), connection, BIND_AUTO_CREATE);
    }

    private void unBindService() {
        if (btService != null) {
            try {
                final Message message = Message.obtain();
                message.replyTo = incomingMessenger;
                message.what = BluetoothService.MESSAGE_UNREG_CLIENT;
                btService.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            unbindService(connection);
        }
    }

    public void displayDeviceListDialog() {
        DialogFragment fragment = (DialogFragment) getSupportFragmentManager().findFragmentByTag(DEVICE_LIST_TAG);
        if (fragment != null) fragment.dismiss();
        devices.clear();
        devices.addAll(BluetoothAdapter.getDefaultAdapter().getBondedDevices());
        String[] deviceNames = new String[devices.size()];
        for (int i = 0; i < devices.size(); i++) {
            deviceNames[i] = devices.get(i).getName();
        }
        if (devices.size() > 0) setDevice(0);
        SelectDeviceDialog.newInstance(deviceNames, 0).show(getSupportFragmentManager(), DEVICE_LIST_TAG);
    }

    public void addOnBluetoothEventListener(OnBluetoothEventListener listener) {
        listeners.add(listener);
        if (isConnected)
            listener.onConnected();
        else
            listener.onDisconnected();
    }

    public void removeOnBluetoothEventListener(OnBluetoothEventListener listener) {
        listeners.remove(listener);
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setDevice(int which) {
        if (serviceBound) {
            try {
                btService.send(Message.obtain(null, BluetoothService.MESSAGE_SET_DEVICE, devices.get(which)));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void attemptToConnect() {
        if (serviceBound) {
            try {
                btService.send(Message.obtain(null, BluetoothService.MESSAGE_CONNECT));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void disconnect() {
        if (serviceBound) {
            try {
                btService.send(Message.obtain(null, BluetoothService.MESSAGE_DISCONNECT));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean write(String line) {
        if (isConnected) {
            try {
                btService.send(Message.obtain(null, BluetoothService.MESSAGE_WRITE, line));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return isConnected;
    }

    public abstract void connected();

    public abstract void connecting();

    public abstract void disconnected();

    public abstract void onRead(String line);

    public interface OnBluetoothEventListener {
        void onReadLine(String line);

        void onConnected();

        void onDisconnected();
    }

    protected static class IncomingHandler extends Handler {

        private final WeakReference<BluetoothActivity> activityWeakReference;

        public IncomingHandler(WeakReference<BluetoothActivity> activityWeakReference) {
            this.activityWeakReference = activityWeakReference;
        }

        @Override
        public void handleMessage(Message msg) {
            BluetoothActivity btActivity = activityWeakReference.get();
            if (btActivity == null) return;
            switch (msg.what) {
                case BluetoothService.MESSAGE_READ:
                    btActivity.onRead(msg.obj.toString());
                    break;
                case BluetoothService.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_NONE:
                            btActivity.isConnected = false;
                            btActivity.disconnected();
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            btActivity.isConnected = false;
                            btActivity.connecting();
                            break;
                        case BluetoothService.STATE_CONNECTED:
                            btActivity.isConnected = true;
                            btActivity.connected();
                            break;
                    }
                    break;
            }
        }
    }

    public static class SelectDeviceDialog extends DialogFragment {

        private static final String DEVICE_NAMES_KEY = "names";
        private static final String POSITION_KEY = "position";

        public static SelectDeviceDialog newInstance(String[] deviceNames, int position) {
            SelectDeviceDialog fragment = new SelectDeviceDialog();
            Bundle args = new Bundle();
            args.putStringArray(DEVICE_NAMES_KEY, deviceNames);
            args.putInt(POSITION_KEY, position);
            fragment.setArguments(args);
            return fragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final BluetoothActivity activity = (BluetoothActivity) getActivity();
            assert activity != null;
            AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                    .setTitle("Select Paired Device")
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> activity.attemptToConnect());
            final Bundle arguments = getArguments();
            if (arguments != null) {
                builder.setSingleChoiceItems(arguments.getStringArray(DEVICE_NAMES_KEY), arguments.getInt(POSITION_KEY), (dialog, which) -> activity.setDevice(which));
            }
            return builder.create();
        }
    }
}
