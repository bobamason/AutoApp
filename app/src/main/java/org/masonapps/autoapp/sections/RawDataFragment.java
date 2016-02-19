package org.masonapps.autoapp.sections;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;

import org.masonapps.autoapp.MainActivity;
import org.masonapps.autoapp.R;
import org.masonapps.autoapp.bluetooth.BluetoothActivity;

public class RawDataFragment extends Fragment {
    
    public StringBuffer stringBuffer = new StringBuffer();
    private ScrollView scrollView;
    private TextView textView;
    private EditText editText;

    public RawDataFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_raw_data, container, false);
        scrollView = (ScrollView) view.findViewById(R.id.scrollViewRawData);
        textView = (TextView) view.findViewById(R.id.textViewRawData);
        editText = (EditText) view.findViewById(R.id.editTextRawData);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == KeyEvent.KEYCODE_ENTER) {
                    attemptSendMessage();
                    return true;
                }
                return false;
            }
        });
        final ImageButton button = (ImageButton) view.findViewById(R.id.sendButtonRawData);
        Drawable drawable = DrawableCompat.wrap(button.getDrawable());
        DrawableCompat.setTint(drawable.mutate(), button.getContext().getResources().getColor(R.color.colorAccent));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptSendMessage();
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity)getActivity()).addOnBluetoothEventListener(listener);
    }

    @Override
    public void onPause() {
        ((MainActivity)getActivity()).removeOnBluetoothEventListener(listener);
        super.onPause();
    }

    private void attemptSendMessage() {
        final Editable text = editText.getText();
        text.append("\r\n");
        boolean sent = ((MainActivity)getActivity()).write(text.toString());
        stringBuffer.append(">>> ").append(sent ? text : "unable to send\n");
        textView.setText(stringBuffer);
        scrollView.fullScroll(View.FOCUS_DOWN);
        editText.setText("");
    }

    private final BluetoothActivity.OnBluetoothEventListener listener = new BluetoothActivity.OnBluetoothEventListener() {
        @Override
        public void onReadLine(String line) {
            stringBuffer.append("\t").append(line).append("\n");
            textView.setText(stringBuffer);
            scrollView.fullScroll(View.FOCUS_DOWN);
        }

        @Override
        public void onConnected() {
        }

        @Override
        public void onDisconnected() {
        }
    };
}
