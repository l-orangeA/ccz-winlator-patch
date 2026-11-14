package com.winlator.inputcontrols;

import android.view.KeyEvent;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class ExternalControllerBinding {
    private Binding binding = Binding.NONE;
    private short keyCode;

    public int getKeyCodeForAxis() {
        return this.keyCode;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = (short) keyCode;
    }

    public Binding getBinding() {
        return this.binding;
    }

    public void setBinding(Binding binding) {
        this.binding = binding;
    }

    public JSONObject toJSONObject() {
        try {
            JSONObject controllerBindingJSONObject = new JSONObject();
            controllerBindingJSONObject.put("keyCode", (int) this.keyCode);
            controllerBindingJSONObject.put("binding", this.binding.name());
            return controllerBindingJSONObject;
        } catch (JSONException e) {
            return null;
        }
    }

    public String toString() {
        short s = this.keyCode;
        switch (s) {
            case -8:
                return "AXIS RZ+";
            case -7:
                return "AXIS RZ-";
            case -6:
                return "AXIS Z+";
            case -5:
                return "AXIS Z-";
            case -4:
                return "AXIS Y+";
            case -3:
                return "AXIS Y-";
            case -2:
                return "AXIS X+";
            case -1:
                return "AXIS X-";
            default:
                return KeyEvent.keyCodeToString(s).replace("KEYCODE_", "").replace("_", " ");
        }
    }

    public static int getKeyCodeForAxis(int axis, byte sign) {
        switch (axis) {
            case 0:
                return sign > 0 ? -2 : -1;
            case 1:
                return sign > 0 ? -3 : -4;
            case 11:
                return sign > 0 ? -6 : -5;
            case 14:
                return sign > 0 ? -7 : -8;
            case 15:
                return sign > 0 ? 22 : 21;
            case 16:
                return sign > 0 ? 20 : 19;
            default:
                return 0;
        }
    }
}
