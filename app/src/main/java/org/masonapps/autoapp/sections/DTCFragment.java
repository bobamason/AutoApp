package org.masonapps.autoapp.sections;


import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.masonapps.autoapp.MainActivity;
import org.masonapps.autoapp.R;
import org.masonapps.autoapp.bluetooth.BluetoothActivity;
import org.masonapps.autoapp.obd2.ODB2Parser;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class DTCFragment extends Fragment {


    private static final int COMMAND_NONE = -1;
    private static final int COMMAND_FETCH = 3;
    private TroubleCodeAdapter adapter;
    private int currentCommand = COMMAND_NONE;
    private boolean connected = false;
    private Button fetchButton;
    private Button clearButton;
    private final BluetoothActivity.OnBluetoothEventListener listener = new BluetoothActivity.OnBluetoothEventListener() {
        @Override
        public void onReadLine(String line) {
            switch (currentCommand) {
                case COMMAND_NONE:
                    break;
                case COMMAND_FETCH:
                    hideProgressBar();
                    try {
                        if (adapter != null) {
                            adapter.clear();
                            adapter.addAll(ODB2Parser.parseTroubleCodes(line));
                        }
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                        final Activity activity = getActivity();
                        if (activity != null)
                            Toast.makeText(activity.getApplicationContext(), "unable to read response: " + line, Toast.LENGTH_SHORT).show();
                    }
                    currentCommand = COMMAND_NONE;
                    break;
            }
        }

        @Override
        public void onConnected() {
            connected = true;
            fetchButton.setEnabled(true);
            clearButton.setEnabled(true);
        }

        @Override
        public void onDisconnected() {
            connected = false;
            fetchButton.setEnabled(false);
            clearButton.setEnabled(false);
        }
    };

    public DTCFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_dtc, container, false);

        fetchButton = view.findViewById(R.id.fectchCodesButton);
        fetchButton.setOnClickListener(v -> {
            if (connected) {
                if (getActivity() != null) {
                    ((MainActivity) getActivity()).sendCommand("03");
                    currentCommand = COMMAND_FETCH;
                    showProgressBar();
                }
            }
        });

        clearButton = view.findViewById(R.id.clearCodesButton);
        clearButton.setOnClickListener(v -> {
            if (connected && getActivity() != null) {
                new AlertDialog.Builder(getActivity())
                        .setTitle("Are you sure?")
                        .setIcon(android.R.drawable.stat_sys_warning)
                        .setMessage("All trouble codes will be permanently erased.")
                        .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            clearTroubleCodes();
                            dialog.dismiss();
                        })
                        .create().show();
            }
        });

        final RecyclerView recyclerView = view.findViewById(R.id.troubleCodeRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new TroubleCodeAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);
//        recyclerView.setHasFixedSize(false);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        return view;
    }

    private void clearTroubleCodes() {
        boolean sent = false;
        if (getActivity() != null) {
            sent = ((MainActivity) getActivity()).sendCommand("04");
        }
        if (sent) {
            adapter.clear();
            Toast.makeText(getActivity().getApplicationContext(), "diagnostic trouble codes cleared", Toast.LENGTH_SHORT).show();
        }
    }

    private void hideProgressBar() {
        final FragmentActivity activity = getActivity();
        if (activity instanceof MainActivity)
            ((MainActivity) activity).setProgressVisibility(false);
    }

    private void showProgressBar() {
        final FragmentActivity activity = getActivity();
        if (activity instanceof MainActivity)
            ((MainActivity) activity).setProgressVisibility(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        final FragmentActivity activity = getActivity();
        if (activity instanceof MainActivity) {
            connected = ((MainActivity) activity).isDeviceCompatible();
            ((MainActivity) activity).addOnBluetoothEventListener(listener);
        }
    }

    @Override
    public void onPause() {
        if (getActivity() instanceof MainActivity)
            ((MainActivity) getActivity()).removeOnBluetoothEventListener(listener);
        super.onPause();
    }

    public static class TroubleCodeAdapter extends RecyclerView.Adapter<TroubleCodeAdapter.TroubleCodeViewHolder> {

        private final ArrayList<String> codes;

        public TroubleCodeAdapter(ArrayList<String> codes) {
            this.codes = codes;
        }

        @Override
        public TroubleCodeAdapter.TroubleCodeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new TroubleCodeViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_trouble_code, parent, false));
        }

        @Override
        public int getItemCount() {
            return codes.size();
        }

        @Override
        public void onBindViewHolder(TroubleCodeAdapter.TroubleCodeViewHolder holder, int position) {
            holder.bind(codes.get(position));
        }

        public void addAll(ArrayList<String> list) {
            int start = codes.size();
            codes.addAll(list);
            notifyItemRangeInserted(start, list.size());
        }

        public void add(String code) {
            int pos = codes.size();
            codes.add(code);
            notifyItemInserted(pos);
        }

        public void clear() {
            codes.clear();
            notifyDataSetChanged();
        }

        public static class TroubleCodeViewHolder extends RecyclerView.ViewHolder {

            private final TextView textView;
            private View.OnClickListener onSearchClickedListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Context context = v.getContext();
                    final Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
                    intent.putExtra(SearchManager.QUERY, "diagnostic trouble code " + textView.getText().toString());
                    if (intent.resolveActivity(context.getPackageManager()) != null) {
                        context.startActivity(intent);
                    } else {
                        Toast.makeText(context.getApplicationContext(), "unable to perform web search", Toast.LENGTH_SHORT).show();
                    }
                }
            };

            public TroubleCodeViewHolder(View itemView) {
                super(itemView);
                textView = (TextView) itemView.findViewById(R.id.troubleCodeTextView);
                ImageButton searchButton = (ImageButton) itemView.findViewById(R.id.troubleCodeSearchButton);
                Drawable drawable = DrawableCompat.wrap(searchButton.getDrawable());
                DrawableCompat.setTint(drawable.mutate(), itemView.getContext().getResources().getColor(R.color.colorAccent));
                searchButton.setOnClickListener(onSearchClickedListener);
            }

            public void bind(String text) {
                textView.setText(text);
            }
        }
    }
}
