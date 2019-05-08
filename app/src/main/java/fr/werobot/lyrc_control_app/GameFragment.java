package fr.werobot.lyrc_control_app;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class GameFragment extends Fragment implements ServiceConnection, SerialListener {

    private View view;
    private LinearLayout flagOne;
    private LinearLayout flagTwo;

    private enum Connected {False, Pending, True}

    private String deviceAddress;
    private String newline = "\n";

    private TextView receiveText;

    private SerialSocket socket;
    private SerialService service;
    private boolean initialStart = true;
    private Connected connected = Connected.False;

    private Integer joystickAngle = 0;
    private Integer joystickStrength = 0;

    public GameFragment() {
    }

    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        deviceAddress = getArguments().getString("device");
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.registerOrientationListener(this::onOrientationChange);
        } else {
            System.out.println("ERR: Cannot register orientation listener because activity is null");
        }
    }

    private void onOrientationChange(int orientation) {
        this.updateOrientationMessage(orientation);
    }

    @Override
    public void onDestroy() {
        if (connected != Connected.False)
            disconnect();
        getActivity().stopService(new Intent(getActivity(), SerialService.class));
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (service != null)
            service.attach(this);
        else
            getActivity().startService(new Intent(getActivity(), SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    @Override
    public void onStop() {
        if (service != null && !getActivity().isChangingConfigurations())
            service.detach();
        super.onStop();
    }

    @SuppressWarnings("deprecation")
    // onAttach(context) was added with API 23. onAttach(activity) works for all API versions
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try {
            getActivity().unbindService(this);
        } catch (Exception ignored) {
        }
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (initialStart && service != null) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        if (initialStart && isResumed()) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    /*
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_game, container, false);
        receiveText = view.findViewById(R.id.receive_text);                          // TextView performance decreases with number of spans
        receiveText.setTextColor(getResources().getColor(R.color.colorRecieveText)); // set as default color to reduce number of spans
        receiveText.setMovementMethod(ScrollingMovementMethod.getInstance());
        view.findViewById(R.id.buttonAction1).setOnClickListener(v -> buttonAction1Call());
//        view.findViewById(R.id.buttonAction2).setOnClickListener(v -> buttonAction2Call());
        JoystickView joystick = view.findViewById(R.id.joystick);
        joystick.setOnMoveListener(this::joystickUpdate);
        this.updateOrientationMessage(getResources().getConfiguration().orientation);
        this.flagOne = view.findViewById(R.id.flag_one);
//        LinearLayout flagTwo = view.findViewById(R.id.flag_two);
//        String redColor = "#c0392b";
//        String greenColor = "#27AE60";
        ((ToggleButton) view.findViewById(R.id.main_arm_button)).setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                this.send("OMA");
//                flagOne.setBackgroundColor(Color.parseColor(greenColor));
            } else {
                this.send("CMA");
//                flagOne.setBackgroundColor(Color.parseColor(redColor));
            }
        });
        ((ToggleButton) view.findViewById(R.id.secondary_arm_button)).setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                this.send("OSA");
            } else {
                this.send("CSA");
            }
        });
        return view;
    }

    private void updateOrientationMessage(int orientation) {
        TextView textView = view.findViewById(R.id.game_portrait_text);
        View mainLayout = view.findViewById(R.id.game_main_layout);
        View textLayout = view.findViewById(R.id.game_portrait_layout);
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            mainLayout.setVisibility(LinearLayout.GONE);
            textLayout.setVisibility(LinearLayout.VISIBLE);
            textView.setText("<tourne ton tel stp>");
        } else {
            mainLayout.setVisibility(LinearLayout.VISIBLE);
            textLayout.setVisibility(LinearLayout.GONE);
            textView.setText("");
        }
    }

    private void joystickUpdate(int angle, int strength) {
        if (angle != joystickAngle || strength != joystickStrength) {
            joystickAngle = angle;
            joystickStrength = strength;
            int direction = 4;
            if (joystickAngle > 45 && joystickAngle < 3 * 45) {
                direction = 1;
            } else if (joystickAngle > 3 * 45 && joystickAngle < 5 * 45) {
                direction = 3;
            } else if (joystickAngle > 5 * 45 && joystickAngle < 7 * 45) {
                direction = 2;
            }
            int strengthTo255 = strength * 255 / 100;
            String data = "JOY#" + direction + "#" + strengthTo255;
            System.out.println(data);
            this.send(data);
        }
    }


    private void buttonAction1Call() {
        this.send("PING");
    }

    private void buttonAction2Call() {
        this.send("HELLO");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_terminal, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.clear) {
            receiveText.setText("");
            Toast.makeText(getActivity(), "Console cleared", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.newline) {
            String[] newlineNames = getResources().getStringArray(R.array.newline_names);
            String[] newlineValues = getResources().getStringArray(R.array.newline_values);
            int pos = java.util.Arrays.asList(newlineValues).indexOf(newline);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Newline");
            builder.setSingleChoiceItems(newlineNames, pos, (dialog, item1) -> {
                newline = newlineValues[item1];
                dialog.dismiss();
            });
            builder.create().show();
            return true;
        } else if (id == R.id.game) {
            Toast.makeText(getActivity(), "It's time to game now!", Toast.LENGTH_SHORT).show();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /*
     * Serial + UI
     */
    private void connect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            String deviceName = device.getName() != null ? device.getName() : device.getAddress();
            status("connecting...");
            connected = Connected.Pending;
            socket = new SerialSocket();
            service.connect(this, "Connected to " + deviceName);
            socket.connect(getContext(), service, device);
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    private void disconnect() {
        connected = Connected.False;
        service.disconnect();
        socket.disconnect();
        socket = null;
    }

    private void send(String str) {
        if (connected != Connected.True) {
            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            SpannableStringBuilder spn = new SpannableStringBuilder("> " + str + '\n');
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            receiveText.append(spn);
            byte[] data = (str + newline).getBytes();
            socket.write(data);
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }

    private void receive(byte[] data) {
        String toShow = new String(data);
        receiveText.append(toShow);
    }

    private void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str + '\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        receiveText.append(spn);
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        status("connected");
        connected = Connected.True;
    }

    @Override
    public void onSerialConnectError(Exception e) {
        status("connection failed: " + e.getMessage());
        disconnect();
    }

    @Override
    public void onSerialRead(byte[] data) {
        receive(data);
    }

    @Override
    public void onSerialIoError(Exception e) {
        status("connection lost: " + e.getMessage());
        disconnect();
    }

}
