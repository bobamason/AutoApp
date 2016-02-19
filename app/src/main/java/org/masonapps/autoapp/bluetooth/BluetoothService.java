package org.masonapps.autoapp.bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by Bob on 7/21/2015.
 */
public class BluetoothService extends Service{
    
    public static final UUID MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    public static final int MESSAGE_READ = 101;
    public static final int MESSAGE_WRITE = 102;
    public static final int MESSAGE_STATE_CHANGE = 103;
    public static final int MESSAGE_REG_CLIENT = 104;
    public static final int MESSAGE_UNREG_CLIENT = 105;
    public static final int MESSAGE_SET_DEVICE = 106;
    public static final int MESSAGE_CONNECT = 107;
    public static final int STATE_NONE = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;
    private static final String TAG = BluetoothService.class.getSimpleName();
    private ConnectThread connectThread = null;
    private CommunicationThread communicationThread = null;
    private int state;
    private Messenger messenger = new Messenger(new IncomingHandler());
    private ArrayList<Messenger> clients = new ArrayList<>();
    private BluetoothDevice device = null;
    private BluetoothAdapter bluetoothAdapter;

    @Override
    public void onCreate() {
        super.onCreate();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled()){
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        stop();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    protected void connect() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (communicationThread != null) {
            communicationThread.cancel();
            communicationThread = null;
        }
        connectThread = new ConnectThread();
        connectThread.start();
    }

    protected boolean write(String msg) {
        if (communicationThread == null) return false;
        CommunicationThread r;

        synchronized (this) {
            r = communicationThread;
        }

        return r.write(msg);
    }

    protected void stop() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (communicationThread != null) {
            communicationThread.cancel();
            communicationThread = null;
        }

        setState(STATE_NONE);
    }

    protected void connected(BluetoothSocket socket) {

        if (communicationThread != null) {
            communicationThread.cancel();
            communicationThread = null;
        }

        communicationThread = new CommunicationThread(socket);
        communicationThread.start();
    }

    protected void connectionFailed() {
        Log.e(TAG, "connection failed");
        setState(STATE_NONE);
//        connect();
    }

    protected void setState(int state) {
        this.state = state;
        try {
            messenger.send(Message.obtain(null, MESSAGE_STATE_CHANGE, state, -1));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    
    protected class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothService.MESSAGE_READ:
                    for (int i = clients.size() - 1; i >= 0; i--) {
                        try{
                            clients.get(i).send(Message.obtain(null, MESSAGE_READ, msg.obj));
                        } catch (RemoteException e){
                            clients.remove(i);
                        }
                    }
                    break;
                case BluetoothService.MESSAGE_WRITE:
                    write(msg.obj.toString());
                    break;
                case BluetoothService.MESSAGE_REG_CLIENT:
                    clients.add(msg.replyTo);
                    break;
                case BluetoothService.MESSAGE_UNREG_CLIENT:
                    clients.remove(msg.replyTo);
                    break;
                case BluetoothService.MESSAGE_SET_DEVICE:
                    device = (BluetoothDevice) msg.obj;
                    break;
                case BluetoothService.MESSAGE_CONNECT:
                    connect();
                    break;
                case BluetoothService.MESSAGE_STATE_CHANGE:
                    for (int i = clients.size() - 1; i >= 0; i--) {
                        try{
                            clients.get(i).send(Message.obtain(null, MESSAGE_STATE_CHANGE, msg.arg1, 0));
                        } catch (RemoteException e){
                            clients.remove(i);
                        }
                    }
                    break;
            }
        }
    }

    protected class ConnectThread extends Thread {

        private final BluetoothSocket socket;

        public ConnectThread() {
            BluetoothSocket tmpSocket = null;
            try {
                Log.d(TAG, "device name: " + device.getName() + " address: " + device.getAddress());
                ParcelUuid[] parcelUuids = device.getUuids();
                UUID uuid;
                if (parcelUuids != null && parcelUuids.length > 0) {
                    uuid = parcelUuids[0].getUuid();
                } else {
                    uuid = MODULE_UUID;
                }
                Log.d(TAG, "device uuid: " + uuid.toString());
                tmpSocket = device.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                Log.e(TAG, "failed to create socket", e);
            }
            socket = tmpSocket;
        }

        @Override
        public void run() {
            bluetoothAdapter.cancelDiscovery();
            if (socket != null) {
                try {
                    setState(STATE_CONNECTING);
                    socket.connect();
                    Log.d(TAG, "connecting...");
                } catch (IOException e) {
                    Log.e(TAG, "failed to connect socket", e);
                    try {
                        socket.close();
                    } catch (IOException e1) {
                        Log.e(TAG, "failed to close socket", e1);
                    }
                    connectionFailed();
                    return;
                }

                synchronized (BluetoothService.this) {
                    connectThread = null;
                }

                connected(socket);
                Log.d(TAG, "connected");
                setState(STATE_CONNECTED);
            }
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "failed to close socket", e);
            }
        }
    }

    protected class CommunicationThread extends Thread {

        private final BluetoothSocket socket;
        private OutputStream outputStream = null;
        private InputStream inputStream = null;
        private int numBytes;
        private byte[] bytes = new byte[1024];

        public CommunicationThread(BluetoothSocket socket) {
            this.socket = socket;
            InputStream tmpInStream = null;
            OutputStream tmpOutStream = null;
            try {
                tmpInStream = socket.getInputStream();
                tmpOutStream = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "failed to get streams", e);
            }
            inputStream = tmpInStream;
            outputStream = tmpOutStream;
        }

        @Override
        public void run() {
            String str;
            while (true) {
                try {
                    if (inputStream != null) {
                        numBytes = inputStream.read(bytes, 0, bytes.length);
                        if (numBytes != -1) {
                            str = new String(bytes, 0, numBytes);
                            messenger.send(Message.obtain(null, BluetoothService.MESSAGE_READ, str));
//                            Log.d(TAG, "reading: " + str);
                        }
                    }
                } catch (IOException e) {
                    connectionFailed();
                    Log.e(TAG, "failed to read stream", e);
                    break;
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        public boolean write(String msg) {
            boolean ok = false;
            try {
                if (outputStream != null) {
                    outputStream.write(msg.getBytes());
                    ok = true;
//                    Log.d(TAG, "writing: " + msg);
                }
            } catch (IOException e) {
                Log.e(TAG, "failed to write stream", e);
            }
            return ok;
        }


        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "failed to close socket", e);
            }
        }
    }
}
