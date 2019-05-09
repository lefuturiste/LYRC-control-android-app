package fr.werobot.lyrc_control_app;

import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.HashMap;

class ControllerHelper {

    private ControllerInputEventListener controllerInputEventListener;

    private HashMap<String, String> keyBindings = new HashMap<>();

    ControllerHelper() {
        keyBindings.put("B", "cross");
        keyBindings.put("C", "cricle");
        keyBindings.put("A", "square");
        keyBindings.put("X", "triangle");
        keyBindings.put("R2", "options");
        keyBindings.put("L2", "share");
        keyBindings.put("Z", "right_1");
        keyBindings.put("Y", "left_1");
        keyBindings.put("MODE", "ps");
        keyBindings.put("START", "right_thumb");
        keyBindings.put("SELECT", "left_thumb");
        keyBindings.put("THUMBL", "pad");
        keyBindings.put("R1", "right_2");
        keyBindings.put("L1", "left_2");
    }

    /* EVENTS INPUT */
    boolean onGenericMotionEvent(MotionEvent event) {
        if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK &&
                event.getAction() == MotionEvent.ACTION_MOVE) {

            final int historySize = event.getHistorySize();

            for (int i = 0; i < historySize; i++) {
                processJoystickInput(event, i);
            }

            processJoystickInput(event, -1);
        }
        return true;
    }

    boolean onKeyDown(int keyCode) {
//        System.out.println(KeyEvent.keyCodeToString(keyCode));
        String keyId = bindKeyCode(keyCode);
        if (keyId != null) {
            if (controllerInputEventListener != null) {
                controllerInputEventListener.run(keyId, 0, 0);
            }
            return true;
        }
        return false;
    }

    private String bindKeyCode(int keyCode) {
        String keyCodeLabel = KeyEvent.keyCodeToString(keyCode);
        keyCodeLabel = keyCodeLabel.replace("KEYCODE_BUTTON_", "");
        return keyBindings.containsKey(keyCodeLabel) ? keyBindings.get(keyCodeLabel) : null;
    }

    private ArrayList<Integer> getGameControllerIds() {
        ArrayList<Integer> gameControllerDeviceIds = new ArrayList<>();
        int[] deviceIds = InputDevice.getDeviceIds();
        for (int deviceId : deviceIds) {
            InputDevice dev = InputDevice.getDevice(deviceId);
            int sources = dev.getSources();

            if (((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD)
                    || ((sources & InputDevice.SOURCE_JOYSTICK)
                    == InputDevice.SOURCE_JOYSTICK)) {
                if (!gameControllerDeviceIds.contains(deviceId)) {
                    gameControllerDeviceIds.add(deviceId);
                }
            }
        }
        return gameControllerDeviceIds;
    }

    boolean haveControllerConnected() {
        return this.getGameControllerIds().size() != 0;
    }

    int getControllerId() {
        return this.getGameControllerIds().get(0);
    }

    private void processJoystickInput(MotionEvent event, int historyPos) {
        InputDevice inputDevice = event.getDevice();

        String axisType;

        float x = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_X, historyPos);
        float y = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_Y, historyPos);
        if (x != 0 || y != 0) {
            axisType = "left";
        } else {
            x = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_Z, historyPos);
            y = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_RZ, historyPos);
            if (x != 0 || y != 0) {
                axisType = "right";
            } else {
                x = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_HAT_X, historyPos);
                y = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_HAT_Y, historyPos);
                if (x != 0 || y != 0) {
                    axisType = "hat";
                } else {
//                    x = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_LTRIGGER, historyPos);
//                    y = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_RTRIGGER, historyPos);
//                    if (x != 0 || y != 0) {
//                        axisType = "shoulder";
//                    } else {
//                        axisType = "reset";
//                    }
                    axisType = "reset";
                }
            }
        }

        if (controllerInputEventListener != null) {
            controllerInputEventListener.run(axisType, x, y);
        }
    }

    private static float getCenteredAxis(MotionEvent event, InputDevice device, int axis, int historyPos) {
        final InputDevice.MotionRange range = device.getMotionRange(axis, event.getSource());

        if (range != null) {
            final float flat = range.getFlat();
            final float value =
                    historyPos < 0 ? event.getAxisValue(axis) :
                            event.getHistoricalAxisValue(axis, historyPos);
            if (Math.abs(value) > flat) {
                return value;
            }
        }
        return 0;
    }

    public interface ControllerInputEventListener {
        void run(String keyId, float x, float y);
    }

    void registerOnControllerInputEventListener(ControllerInputEventListener controllerInputEventListener) {
        this.controllerInputEventListener = controllerInputEventListener;
    }
}
