package org.masonapps.autoapp;

import android.bluetooth.BluetoothAdapter;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.masonapps.autoapp.bluetooth.BluetoothActivity;
import org.masonapps.autoapp.database.AutoDatabase;
import org.masonapps.autoapp.database.AutoEntry;
import org.masonapps.autoapp.sections.AddVehicleFragment;
import org.masonapps.autoapp.sections.DTCFragment;
import org.masonapps.autoapp.sections.GraphDataFragment;
import org.masonapps.autoapp.sections.RawDataFragment;
import org.masonapps.autoapp.sections.VehicleListFragment;

import java.lang.ref.WeakReference;

public class MainActivity extends BluetoothActivity
        implements NavigationView.OnNavigationItemSelectedListener, AddVehicleFragment.OnVehicleAddedListener, VehicleListFragment.OnVehicleInteractionListener {

    public static final int FAB_COLOR_DISCONNECTED = Color.GRAY;
    private static final String VEHICLE_LIST_TAG = "vehicleList";
    private StringBuffer stringBuffer = new StringBuffer();
    private int index;
    private ProgressBar progressBar;
    private static final int COMMAND_NONE = -1;
    private static final int COMMAND_ECHO_OFF = 0;
    private static final int COMMAND_INFO = 1;
    private static final int COMMAND_AUTO_PROTOCOL = 2;
    private int currentCommand = COMMAND_NONE;
    private boolean confirmed = false;
    private FloatingActionButton fab;
    private Drawable disconnectedDrawable;
    private Drawable connectedDrawable;
    private AutoDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        database = new AutoDatabase(this);

        progressBar = (ProgressBar) findViewById(R.id.progressIndicator);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        if(savedInstanceState == null) {
            showRawDataFragment();
            setTitle("Raw Data");
        }

        disconnectedDrawable = DrawableCompat.wrap(getResources().getDrawable(R.drawable.ic_bluetooth_disabled));
        DrawableCompat.setTint(disconnectedDrawable, Color.LTGRAY);
        connectedDrawable = getResources().getDrawable(R.drawable.ic_bluetooth);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setImageDrawable(disconnectedDrawable);
        fab.setBackgroundTintList(ColorStateList.valueOf(FAB_COLOR_DISCONNECTED));
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isConnected()){
                    displayDisconnectDialog();
                } else {
                    if(BluetoothAdapter.getDefaultAdapter().isEnabled()){
                        displayDeviceListDialog();
                    } else {
                        requestEnableBluetooth();
                    }
                }
            }
        });
    }

    @Override
    protected void onPause() {
        database.close();
        super.onPause();
    }
    
    public WeakReference<AutoDatabase> getDatabaseWeakReference(){
        return new WeakReference<>(database);
    }

    private void displayDisconnectDialog() {
        
    }

    private void showRawDataFragment() {
        getSupportFragmentManager().beginTransaction().replace(R.id.container, new RawDataFragment()).commit();
    }

    public boolean sendATCommand(String command) {
        return write("AT" + command + "\r\n");
    }

    @Override
    public void connected() {
        fab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorAccent)));
        fab.setImageDrawable(connectedDrawable);
        sendATCommand("I");
        currentCommand = COMMAND_INFO;
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
        String out = "";
        while ((index = stringBuffer.indexOf(">")) != -1) {
            out += stringBuffer.substring(0, index) + "\n";
            stringBuffer.delete(0, index + 1);
        }
        if (!out.isEmpty()) {
            if (confirmed) {
                if (currentCommand == COMMAND_ECHO_OFF) {
                    if (out.toUpperCase().contains("OK")) {
                        Toast.makeText(getApplicationContext(), "echo disabled", Toast.LENGTH_SHORT).show();
                        sendATCommand("SP 0");
                        currentCommand = COMMAND_AUTO_PROTOCOL;
                    }
                } else if (currentCommand == COMMAND_AUTO_PROTOCOL) {
                    if (out.toUpperCase().contains("OK")) {
                        currentCommand = COMMAND_NONE;
                        Toast.makeText(getApplicationContext(), "auto ODB2 protocol set", Toast.LENGTH_SHORT).show();
                        setProgressVisibility(false);
                        for (OnBluetoothEventListener listener : listeners) {
                            listener.onConnected();
                        }
                    }
                } else if (currentCommand == COMMAND_NONE) {
                    for (OnBluetoothEventListener listener : listeners) {
                        listener.onReadLine(out);
                    }
                }
            } else {
                if (currentCommand == COMMAND_INFO) {
                    if (out.toUpperCase().contains("ELM32")) {
                        confirmed = true;
                        Toast.makeText(getApplicationContext(), "device compatibility confirmed", Toast.LENGTH_SHORT).show();
                        currentCommand = COMMAND_ECHO_OFF;
                        sendATCommand("E0");
                    }
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_add_vehicle) {
            new AddVehicleFragment().show(getSupportFragmentManager(), null);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_raw) {
            getSupportFragmentManager().beginTransaction().replace(R.id.container, new RawDataFragment()).commit();
            setTitle("Raw Data");
        } else if (id == R.id.nav_vehicles) {
            getSupportFragmentManager().beginTransaction().replace(R.id.container, new VehicleListFragment(), VEHICLE_LIST_TAG).commit();
            setTitle("Vehicles");
        } else if (id == R.id.nav_dtc) {
            getSupportFragmentManager().beginTransaction().replace(R.id.container, new DTCFragment()).commit();
            setTitle("Trouble Codes");
        } else if (id == R.id.nav_voltage) {
            getSupportFragmentManager().beginTransaction().replace(R.id.container, new GraphDataFragment()).commit();
            setTitle("Voltage");
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public boolean sendCommand(String s) {
        return write(s + "\r\n");
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    @Override
    public void vehicleAdded(AutoEntry entry) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(VEHICLE_LIST_TAG);
        if(fragment != null && fragment instanceof VehicleListFragment){
            ((VehicleListFragment)fragment).notifyVehicleAdded(entry);
        }
    }

    @Override
    public void onDeleteClicked(AutoEntry entry) {
        database.deleteEntry(entry);
    }

    @Override
    public void onEditClicked(AutoEntry entry) {
    }

    @Override
    public void onSetClicked(AutoEntry entry) {
    }
}
