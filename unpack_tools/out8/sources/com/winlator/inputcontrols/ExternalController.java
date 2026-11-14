package com.winlator.inputcontrols;

import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import androidx.constraintlayout.widget.R$styleable;
import java.util.ArrayList;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class ExternalController {
    private String id;
    private String name;
    private int deviceId = -1;
    private final ArrayList<ExternalControllerBinding> controllerBindings = new ArrayList<>();
    public final GamepadState state = new GamepadState();
    private boolean processTriggerButtonOnMotionEvent = true;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getDeviceId() {
        if (this.deviceId == -1) {
            int[] deviceIds = InputDevice.getDeviceIds();
            int length = deviceIds.length;
            int i = 0;
            while (true) {
                if (i < length) {
                    int deviceId = deviceIds[i];
                    InputDevice device = InputDevice.getDevice(deviceId);
                    if (device == null || !device.getDescriptor().equals(this.id)) {
                        i++;
                    } else {
                        this.deviceId = deviceId;
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        return this.deviceId;
    }

    public boolean isConnected() {
        for (int deviceId : InputDevice.getDeviceIds()) {
            InputDevice device = InputDevice.getDevice(deviceId);
            if (device != null && device.getDescriptor().equals(this.id)) {
                return true;
            }
        }
        return false;
    }

    public ExternalControllerBinding getControllerBinding(int keyCode) {
        Iterator<ExternalControllerBinding> it = this.controllerBindings.iterator();
        while (it.hasNext()) {
            ExternalControllerBinding controllerBinding = it.next();
            if (controllerBinding.getKeyCodeForAxis() == keyCode) {
                return controllerBinding;
            }
        }
        return null;
    }

    public ExternalControllerBinding getControllerBindingAt(int index) {
        return this.controllerBindings.get(index);
    }

    public void addControllerBinding(ExternalControllerBinding controllerBinding) {
        if (getControllerBinding(controllerBinding.getKeyCodeForAxis()) == null) {
            this.controllerBindings.add(controllerBinding);
        }
    }

    public int getPosition(ExternalControllerBinding controllerBinding) {
        return this.controllerBindings.indexOf(controllerBinding);
    }

    public void removeControllerBinding(ExternalControllerBinding controllerBinding) {
        this.controllerBindings.remove(controllerBinding);
    }

    public int getControllerBindingCount() {
        return this.controllerBindings.size();
    }

    public JSONObject toJSONObject() {
        try {
            if (this.controllerBindings.isEmpty()) {
                return null;
            }
            JSONObject controllerJSONObject = new JSONObject();
            controllerJSONObject.put("id", this.id);
            controllerJSONObject.put("name", this.name);
            JSONArray controllerBindingsJSONArray = new JSONArray();
            Iterator<ExternalControllerBinding> it = this.controllerBindings.iterator();
            while (it.hasNext()) {
                ExternalControllerBinding controllerBinding = it.next();
                controllerBindingsJSONArray.put(controllerBinding.toJSONObject());
            }
            controllerJSONObject.put("controllerBindings", controllerBindingsJSONArray);
            return controllerJSONObject;
        } catch (JSONException e) {
            return null;
        }
    }

    public boolean equals(Object obj) {
        return obj instanceof ExternalController ? ((ExternalController) obj).id.equals(this.id) : super.equals(obj);
    }

    private void processJoystickInput(MotionEvent event, int historyPos) {
        boolean z = false;
        this.state.thumbLX = getCenteredAxis(event, 0, historyPos);
        this.state.thumbLY = getCenteredAxis(event, 1, historyPos);
        this.state.thumbRX = getCenteredAxis(event, 11, historyPos);
        this.state.thumbRY = getCenteredAxis(event, 14, historyPos);
        if (historyPos == -1) {
            float axisX = getCenteredAxis(event, 15, historyPos);
            float axisY = getCenteredAxis(event, 16, historyPos);
            GamepadState gamepadState = this.state;
            gamepadState.dpad[0] = axisY == -1.0f && Math.abs(gamepadState.thumbLY) < 0.15f;
            GamepadState gamepadState2 = this.state;
            gamepadState2.dpad[1] = axisX == 1.0f && Math.abs(gamepadState2.thumbLX) < 0.15f;
            GamepadState gamepadState3 = this.state;
            gamepadState3.dpad[2] = axisY == 1.0f && Math.abs(gamepadState3.thumbLY) < 0.15f;
            GamepadState gamepadState4 = this.state;
            boolean[] zArr = gamepadState4.dpad;
            if (axisX == -1.0f && Math.abs(gamepadState4.thumbLX) < 0.15f) {
                z = true;
            }
            zArr[3] = z;
        }
    }

    private void processTriggerButton(MotionEvent event) {
        this.state.setPressed(10, event.getAxisValue(17) == 1.0f || event.getAxisValue(23) == 1.0f);
        this.state.setPressed(11, event.getAxisValue(18) == 1.0f || event.getAxisValue(22) == 1.0f);
    }

    public boolean updateStateFromMotionEvent(MotionEvent event) {
        if (isJoystickDevice(event)) {
            if (this.processTriggerButtonOnMotionEvent) {
                processTriggerButton(event);
            }
            int historySize = event.getHistorySize();
            for (int i = 0; i < historySize; i++) {
                processJoystickInput(event, i);
            }
            processJoystickInput(event, -1);
            return true;
        }
        return false;
    }

    public boolean updateStateFromKeyEvent(KeyEvent event) {
        boolean z = false;
        boolean pressed = event.getAction() == 0;
        int keyCode = event.getKeyCode();
        int buttonIdx = getButtonIdxByKeyCode(keyCode);
        if (buttonIdx != -1) {
            if (buttonIdx == 10 || buttonIdx == 11) {
                this.processTriggerButtonOnMotionEvent = false;
            }
            this.state.setPressed(buttonIdx, pressed);
            return true;
        }
        switch (keyCode) {
            case 19:
                GamepadState gamepadState = this.state;
                gamepadState.dpad[0] = pressed && Math.abs(gamepadState.thumbLY) < 0.15f;
                return true;
            case 20:
                GamepadState gamepadState2 = this.state;
                boolean[] zArr = gamepadState2.dpad;
                if (pressed && Math.abs(gamepadState2.thumbLY) < 0.15f) {
                    z = true;
                }
                zArr[2] = z;
                return true;
            case 21:
                GamepadState gamepadState3 = this.state;
                boolean[] zArr2 = gamepadState3.dpad;
                if (pressed && Math.abs(gamepadState3.thumbLX) < 0.15f) {
                    z = true;
                }
                zArr2[3] = z;
                return true;
            case 22:
                GamepadState gamepadState4 = this.state;
                boolean[] zArr3 = gamepadState4.dpad;
                if (pressed && Math.abs(gamepadState4.thumbLX) < 0.15f) {
                    z = true;
                }
                zArr3[1] = z;
                return true;
            default:
                return false;
        }
    }

    public static ArrayList<ExternalController> getControllers() {
        int[] deviceIds = InputDevice.getDeviceIds();
        ArrayList<ExternalController> controllers = new ArrayList<>();
        for (int i = deviceIds.length - 1; i >= 0; i--) {
            InputDevice device = InputDevice.getDevice(deviceIds[i]);
            if (isGameController(device)) {
                ExternalController controller = new ExternalController();
                controller.setId(device.getDescriptor());
                controller.setName(device.getName());
                controllers.add(controller);
            }
        }
        return controllers;
    }

    public static ExternalController getController(String id) {
        Iterator<ExternalController> it = getControllers().iterator();
        while (it.hasNext()) {
            ExternalController controller = it.next();
            if (controller.getId().equals(id)) {
                return controller;
            }
        }
        return null;
    }

    public static ExternalController getController(int deviceId) {
        int[] deviceIds = InputDevice.getDeviceIds();
        for (int i = deviceIds.length - 1; i >= 0; i--) {
            if (deviceIds[i] == deviceId || deviceId == 0) {
                InputDevice device = InputDevice.getDevice(deviceIds[i]);
                if (isGameController(device)) {
                    ExternalController controller = new ExternalController();
                    controller.setId(device.getDescriptor());
                    controller.setName(device.getName());
                    controller.deviceId = deviceIds[i];
                    return controller;
                }
            }
        }
        return null;
    }

    public static boolean isGameController(InputDevice device) {
        if (device == null) {
            return false;
        }
        int sources = device.getSources();
        if (device.isVirtual()) {
            return false;
        }
        return (sources & 1025) == 1025 || (sources & 16777232) == 16777232;
    }

    public static float getCenteredAxis(MotionEvent event, int axis, int historyPos) {
        if (axis == 15 || axis == 16) {
            float value = event.getAxisValue(axis);
            if (Math.abs(value) == 1.0f) {
                return value;
            }
            return 0.0f;
        }
        InputDevice device = event.getDevice();
        InputDevice.MotionRange range = device.getMotionRange(axis, event.getSource());
        if (range != null) {
            float flat = range.getFlat();
            float value2 = historyPos < 0 ? event.getAxisValue(axis) : event.getHistoricalAxisValue(axis, historyPos);
            if (Math.abs(value2) > flat) {
                return value2;
            }
            return 0.0f;
        }
        return 0.0f;
    }

    public static boolean isJoystickDevice(MotionEvent event) {
        return (event.getSource() & 16777232) == 16777232 && event.getAction() == 2;
    }

    public static int getButtonIdxByKeyCode(int keyCode) {
        switch (keyCode) {
            case R$styleable.Constraint_layout_goneMarginBottom /* 96 */:
                return 0;
            case R$styleable.Constraint_layout_goneMarginEnd /* 97 */:
                return 1;
            case R$styleable.Constraint_layout_goneMarginLeft /* 98 */:
            case R$styleable.Constraint_layout_goneMarginTop /* 101 */:
            default:
                return -1;
            case R$styleable.Constraint_layout_goneMarginRight /* 99 */:
                return 2;
            case R$styleable.Constraint_layout_goneMarginStart /* 100 */:
                return 3;
            case R$styleable.Constraint_motionProgress /* 102 */:
                return 4;
            case R$styleable.Constraint_motionStagger /* 103 */:
                return 5;
            case R$styleable.Constraint_pathMotionArc /* 104 */:
                return 10;
            case 105:
                return 11;
            case R$styleable.Constraint_transitionEasing /* 106 */:
                return 8;
            case R$styleable.Constraint_transitionPathRotate /* 107 */:
                return 9;
            case R$styleable.Constraint_visibilityMode /* 108 */:
                return 7;
            case 109:
                return 6;
        }
    }
}
