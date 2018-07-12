package org.masonapps.autoapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.masonapps.autoapp.bluetooth.BluetoothActivity;
import org.masonapps.autoapp.bluetooth.BluetoothService;
import org.masonapps.autoapp.obd2.ELM32X;
import org.masonapps.autoapp.sections.DTCFragment;
import org.masonapps.autoapp.sections.GraphDataFragment;
import org.masonapps.autoapp.sections.RawDataFragment;

public class MainActivity extends BluetoothActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String KEY_DEVICE_ADDRESS = "deviceAddress";
    private StringBuffer stringBuffer = new StringBuffer();
    private ProgressBar progressBar;
    @Nullable
    private String currentCommand = null;
    private boolean isDeviceCompatible = false;
    private FloatingActionButton connectButton;
    private Drawable disconnectedDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        progressBar = findViewById(R.id.progressIndicator);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        connectButton = findViewById(R.id.fab);
        connectButton.setOnClickListener(v -> {
            if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                connectToODB2Adapter();
            } else {
                requestEnableBluetooth();
            }
        });
        disconnectedDrawable = getResources().getDrawable(R.drawable.ic_bluetooth_disabled, getTheme());
        disconnectedDrawable = getResources().getDrawable(R.drawable.ic_bluetooth_disabled, getTheme());

        if (isConnected()) {
            hideConnectButton();
            if (savedInstanceState == null)
                showDTCFragment();
        } else {
            showConnectButton();
        }
    }

    private void connectToODB2Adapter() {
        final String savedAddress = PreferenceManager.getDefaultSharedPreferences(this).getString(KEY_DEVICE_ADDRESS, "");
        final BluetoothDevice savedDevice = BluetoothService.getPairedDeviceByAddress(savedAddress);
        if (savedDevice != null) {
            setCurrentBtDevice(savedDevice);
        } else
            displayDeviceListDialog();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void displayDisconnectDialog() {
        new AlertDialog.Builder(this)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {

                })
                .setNegativeButton(android.R.string.no, (dialog, which) -> dialog.dismiss())
                .setTitle("Disconnect")
                .setMessage("Are you sure you want to disconnect from the current device?")
                .create()
                .show();
    }

    private void showRawDataFragment() {
        getSupportFragmentManager().beginTransaction().replace(R.id.container, new RawDataFragment()).commit();
        setTitle(R.string.raw_data);
    }

    private void showDTCFragment() {
        getSupportFragmentManager().beginTransaction().replace(R.id.container, new DTCFragment()).commit();
        setTitle(R.string.trouble_codes);
    }

//    private void showBTConnectionFragment() {
//        getSupportFragmentManager().beginTransaction().replace(R.id.container, new BTConnectionFragment()).commit();
//        setTitle(R.string.connect_bt);
//    }

    public boolean sendATCommand(String command) {
        final boolean success = write("AT" + command + "\r\n");
        if (success)
            currentCommand = command;
        return success;
    }

    @Override
    public void connected() {
        showDTCFragment();
        sendATCommand(ELM32X.COMMAND_INFO);
    }

    @Override
    public void connecting() {
        setProgressVisibility(true);
    }

    @Override
    public void disconnected() {
        showConnectButton();
        attemptToConnect();
        for (OnBluetoothEventListener listener : listeners) {
            listener.onDisconnected();
        }
    }

    private void showConnectButton() {
        connectButton.setVisibility(View.VISIBLE);
        connectButton.setImageDrawable(disconnectedDrawable);
    }

    private void hideConnectButton() {
        connectButton.setVisibility(View.GONE);
    }

    public void setProgressVisibility(boolean visible) {
        if (progressBar != null)
            progressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onRead(String line) {
        stringBuffer.append(line);
        StringBuilder out = new StringBuilder();

        int index;
        while ((index = stringBuffer.indexOf(">")) != -1) {
            out.append(stringBuffer.substring(0, index)).append("\n");
            stringBuffer.delete(0, index + 1);
        }
        if (out.length() > 0) {
            final String s = out.toString().toUpperCase();
            if (isDeviceCompatible) {
                if (currentCommand != null && currentCommand.equals(ELM32X.COMMAND_ECHO_OFF)) {
                    if (s.contains("OK")) {
                        Toast.makeText(getApplicationContext(), "echo disabled", Toast.LENGTH_SHORT).show();
                        sendATCommand(ELM32X.COMMAND_AUTO_PROTOCOL);
                    }
                } else if (currentCommand != null && currentCommand.equals(ELM32X.COMMAND_AUTO_PROTOCOL)) {
                    if (s.contains("OK")) {
                        currentCommand = null;
                        Toast.makeText(getApplicationContext(), "auto ODB2 protocol set", Toast.LENGTH_SHORT).show();
                        setProgressVisibility(false);
                        for (OnBluetoothEventListener listener : listeners) {
                            listener.onConnected();
                        }
                    }
                } else {
                    for (OnBluetoothEventListener listener : listeners) {
                        listener.onReadLine(out.toString());
                    }
                }
            } else {
                if (currentCommand != null && currentCommand.equals(ELM32X.COMMAND_INFO)) {
                    if (ELM32X.checkInfoResponseCompatibility(s)) {
                        onCompatibilityConfirmed();
                    } else {
                        showConnectButton();
                        setCurrentBtDevice(null);
                        // TODO: 7/12/2018 disconnect 
                    }
                }
            }
            currentCommand = null;
        }
    }

    private void onCompatibilityConfirmed() {
        isDeviceCompatible = true;
        hideConnectButton();
        Toast.makeText(getApplicationContext(), "device compatibility confirmed", Toast.LENGTH_SHORT).show();
        sendATCommand(ELM32X.COMMAND_ECHO_OFF);
        if (getCurrentBtDevice() != null) {
            PreferenceManager.getDefaultSharedPreferences(this).edit().putString(KEY_DEVICE_ADDRESS, getCurrentBtDevice().getAddress()).apply();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.disconnect && isConnected()) {
            displayDisconnectDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_raw) {
            getSupportFragmentManager().beginTransaction().replace(R.id.container, new RawDataFragment()).commit();
            setTitle(getString(R.string.raw_data));
        } else if (id == R.id.nav_dtc) {
            getSupportFragmentManager().beginTransaction().replace(R.id.container, new DTCFragment()).commit();
            setTitle(getString(R.string.trouble_codes));
        } else if (id == R.id.nav_voltage) {
            getSupportFragmentManager().beginTransaction().replace(R.id.container, new GraphDataFragment()).commit();
            setTitle(getString(R.string.voltage));
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public boolean sendCommand(String s) {
        return write(s + "\r\n");
    }

    public boolean isDeviceCompatible() {
        return isDeviceCompatible;
    }
}
