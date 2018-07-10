package org.masonapps.autoapp.sections;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.masonapps.autoapp.R;
import org.masonapps.autoapp.bluetooth.BluetoothActivity;

public class BTConnectionFragment extends Fragment {

    public BTConnectionFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_btconnection, container, false);
        view.findViewById(R.id.connect_bt_button).setOnClickListener(v -> {
            if (getActivity() instanceof BluetoothActivity) {
                final BluetoothActivity activity = (BluetoothActivity) getActivity();
                if (BluetoothAdapter.getDefaultAdapter().isEnabled())
                    activity.displayDeviceListDialog();
                else
                    activity.requestEnableBluetooth();
            }
        });
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
