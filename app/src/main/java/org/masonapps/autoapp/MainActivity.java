package org.masonapps.autoapp;

import android.bluetooth.BluetoothAdapter;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.graphics.drawable.DrawableCompat;
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
import org.masonapps.autoapp.sections.BTConnectionFragment;
import org.masonapps.autoapp.sections.DTCFragment;
import org.masonapps.autoapp.sections.GraphDataFragment;
import org.masonapps.autoapp.sections.RawDataFragment;

public class MainActivity extends BluetoothActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final int FAB_COLOR_DISCONNECTED = Color.GRAY;
    private static final String COMMAND_INFO = "I";
    private static final String COMMAND_ECHO_OFF = "E0";
    private static final String COMMAND_AUTO_PROTOCOL = "SP 0";
    private StringBuffer stringBuffer = new StringBuffer();
    private ProgressBar progressBar;
    @Nullable
    private String currentCommand = null;
    private boolean confirmed = false;
    private FloatingActionButton fab;
    private Drawable disconnectedDrawable;
    private Drawable connectedDrawable;

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

        if (savedInstanceState == null)
            showDTCFragment();

//        if(isConnected())
//            showDTCFragment();
//        else
//            showBTConnectionFragment();

        setUpFAB();
    }

    private void setUpFAB() {
        disconnectedDrawable = DrawableCompat.wrap(getResources().getDrawable(R.drawable.ic_bluetooth_disabled));
        DrawableCompat.setTint(disconnectedDrawable, Color.LTGRAY);
        connectedDrawable = getResources().getDrawable(R.drawable.ic_bluetooth);
        fab = findViewById(R.id.fab);
        fab.setImageDrawable(disconnectedDrawable);
        fab.setBackgroundTintList(ColorStateList.valueOf(FAB_COLOR_DISCONNECTED));
        fab.setOnClickListener(v -> {
            if (isConnected()) {
                displayDisconnectDialog();
            } else {
                if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                    displayDeviceListDialog();
                } else {
                    requestEnableBluetooth();
                }
            }
        });
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

    private void showBTConnectionFragment() {
        getSupportFragmentManager().beginTransaction().replace(R.id.container, new BTConnectionFragment()).commit();
        setTitle(R.string.connect_bt);
    }

    public boolean sendATCommand(String command) {
        final boolean success = write("AT" + command + "\r\n");
        if (success)
            currentCommand = command;
        return success;
    }

    @Override
    public void connected() {
        fab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorAccent)));
        fab.setImageDrawable(connectedDrawable);
        showDTCFragment();
        sendATCommand(COMMAND_INFO);
    }

    @Override
    public void connecting() {
        setProgressVisibility(true);
        fab.setBackgroundTintList(ColorStateList.valueOf(FAB_COLOR_DISCONNECTED));
        fab.setImageDrawable(disconnectedDrawable);
    }

    @Override
    public void disconnected() {
        setProgressVisibility(true);
        fab.setBackgroundTintList(ColorStateList.valueOf(FAB_COLOR_DISCONNECTED));
        fab.setImageDrawable(disconnectedDrawable);
        showBTConnectionFragment();
        attemptToConnect();
        for (OnBluetoothEventListener listener : listeners) {
            listener.onDisconnected();
        }
    }

    public void setProgressVisibility(boolean visible) {
        if(progressBar != null)
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
            if (confirmed) {
                if (currentCommand != null && currentCommand.equals(COMMAND_ECHO_OFF)) {
                    if (s.contains("OK")) {
                        Toast.makeText(getApplicationContext(), "echo disabled", Toast.LENGTH_SHORT).show();
                        sendATCommand(COMMAND_AUTO_PROTOCOL);
                    }
                } else if (currentCommand != null && currentCommand.equals(COMMAND_AUTO_PROTOCOL)) {
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
                if (currentCommand != null && currentCommand.equals(COMMAND_INFO)) {
                    if (s.contains("ELM32")) {
                        confirmed = true;
                        Toast.makeText(getApplicationContext(), "device compatibility confirmed", Toast.LENGTH_SHORT).show();
                        sendATCommand(COMMAND_ECHO_OFF);
                    }
                }
            }
            currentCommand = null;
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

    public boolean isConfirmed() {
        return confirmed;
    }
}
