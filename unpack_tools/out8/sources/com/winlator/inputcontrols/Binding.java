package com.winlator.inputcontrols;

import com.winlator.xserver.Pointer;
import com.winlator.xserver.XKeycode;
import java.util.ArrayList;

/* loaded from: classes.dex */
public enum Binding {
    NONE,
    MOUSE_LEFT_BUTTON,
    MOUSE_MIDDLE_BUTTON,
    MOUSE_RIGHT_BUTTON,
    MOUSE_MOVE_LEFT,
    MOUSE_MOVE_RIGHT,
    MOUSE_MOVE_UP,
    MOUSE_MOVE_DOWN,
    MOUSE_SCROLL_UP,
    MOUSE_SCROLL_DOWN,
    KEY_UP,
    KEY_RIGHT,
    KEY_DOWN,
    KEY_LEFT,
    KEY_ENTER,
    KEY_ESC,
    KEY_BKSP,
    KEY_DEL,
    KEY_TAB,
    KEY_SPACE,
    KEY_CTRL_L,
    KEY_CTRL_R,
    KEY_SHIFT_L,
    KEY_SHIFT_R,
    KEY_ALT_L,
    KEY_ALT_R,
    KEY_HOME,
    KEY_PRTSCN,
    KEY_PG_UP,
    KEY_PG_DOWN,
    KEY_CAPS_LOCK,
    KEY_NUM_LOCK,
    KEY_0,
    KEY_1,
    KEY_2,
    KEY_3,
    KEY_4,
    KEY_5,
    KEY_6,
    KEY_7,
    KEY_8,
    KEY_9,
    KEY_A,
    KEY_B,
    KEY_C,
    KEY_D,
    KEY_E,
    KEY_F,
    KEY_G,
    KEY_H,
    KEY_I,
    KEY_J,
    KEY_K,
    KEY_L,
    KEY_M,
    KEY_N,
    KEY_O,
    KEY_P,
    KEY_Q,
    KEY_R,
    KEY_S,
    KEY_T,
    KEY_U,
    KEY_V,
    KEY_W,
    KEY_X,
    KEY_Y,
    KEY_Z,
    KEY_BRACKET_LEFT,
    KEY_BRACKET_RIGHT,
    KEY_BACKSLASH,
    KEY_SLASH,
    KEY_SEMICOLON,
    KEY_COMMA,
    KEY_PERIOD,
    KEY_APOSTROPHE,
    KEY_KP_ADD,
    KEY_MINUS,
    KEY_F1,
    KEY_F2,
    KEY_F3,
    KEY_F4,
    KEY_F5,
    KEY_F6,
    KEY_F7,
    KEY_F8,
    KEY_F9,
    KEY_F10,
    KEY_F11,
    KEY_F12,
    KEY_KP_0,
    KEY_KP_1,
    KEY_KP_2,
    KEY_KP_3,
    KEY_KP_4,
    KEY_KP_5,
    KEY_KP_6,
    KEY_KP_7,
    KEY_KP_8,
    KEY_KP_9,
    GAMEPAD_BUTTON_A,
    GAMEPAD_BUTTON_B,
    GAMEPAD_BUTTON_X,
    GAMEPAD_BUTTON_Y,
    GAMEPAD_BUTTON_L1,
    GAMEPAD_BUTTON_R1,
    GAMEPAD_BUTTON_SELECT,
    GAMEPAD_BUTTON_START,
    GAMEPAD_BUTTON_L3,
    GAMEPAD_BUTTON_R3,
    GAMEPAD_BUTTON_L2,
    GAMEPAD_BUTTON_R2,
    GAMEPAD_LEFT_THUMB_UP,
    GAMEPAD_LEFT_THUMB_RIGHT,
    GAMEPAD_LEFT_THUMB_DOWN,
    GAMEPAD_LEFT_THUMB_LEFT,
    GAMEPAD_RIGHT_THUMB_UP,
    GAMEPAD_RIGHT_THUMB_RIGHT,
    GAMEPAD_RIGHT_THUMB_DOWN,
    GAMEPAD_RIGHT_THUMB_LEFT,
    GAMEPAD_DPAD_UP,
    GAMEPAD_DPAD_RIGHT,
    GAMEPAD_DPAD_DOWN,
    GAMEPAD_DPAD_LEFT;

    public final XKeycode keycode;

    Binding() {
        XKeycode keycode;
        try {
            keycode = XKeycode.valueOf(name());
        } catch (IllegalArgumentException e) {
            XKeycode keycode2 = XKeycode.KEY_NONE;
            String name = name();
            if (name.equals("KEY_PG_UP")) {
                XKeycode keycode3 = XKeycode.KEY_PRIOR;
                keycode = keycode3;
            } else if (!name.equals("KEY_PG_DOWN")) {
                keycode = keycode2;
            } else {
                XKeycode keycode4 = XKeycode.KEY_NEXT;
                keycode = keycode4;
            }
        }
        this.keycode = keycode;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.winlator.inputcontrols.Binding$1, reason: invalid class name */
    /* loaded from: classes.dex */
    public static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$winlator$inputcontrols$Binding;

        static {
            int[] iArr = new int[Binding.values().length];
            $SwitchMap$com$winlator$inputcontrols$Binding = iArr;
            try {
                iArr[Binding.KEY_SHIFT_L.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$winlator$inputcontrols$Binding[Binding.KEY_SHIFT_R.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$winlator$inputcontrols$Binding[Binding.KEY_CTRL_L.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$winlator$inputcontrols$Binding[Binding.KEY_CTRL_R.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$winlator$inputcontrols$Binding[Binding.KEY_ALT_L.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$winlator$inputcontrols$Binding[Binding.KEY_ALT_R.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$winlator$inputcontrols$Binding[Binding.KEY_BRACKET_LEFT.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$com$winlator$inputcontrols$Binding[Binding.KEY_BRACKET_RIGHT.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$com$winlator$inputcontrols$Binding[Binding.KEY_BACKSLASH.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$com$winlator$inputcontrols$Binding[Binding.KEY_SLASH.ordinal()] = 10;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$com$winlator$inputcontrols$Binding[Binding.KEY_SEMICOLON.ordinal()] = 11;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$com$winlator$inputcontrols$Binding[Binding.KEY_COMMA.ordinal()] = 12;
            } catch (NoSuchFieldError e12) {
            }
            try {
                $SwitchMap$com$winlator$inputcontrols$Binding[Binding.KEY_PERIOD.ordinal()] = 13;
            } catch (NoSuchFieldError e13) {
            }
            try {
                $SwitchMap$com$winlator$inputcontrols$Binding[Binding.KEY_APOSTROPHE.ordinal()] = 14;
            } catch (NoSuchFieldError e14) {
            }
            try {
                $SwitchMap$com$winlator$inputcontrols$Binding[Binding.KEY_MINUS.ordinal()] = 15;
            } catch (NoSuchFieldError e15) {
            }
            try {
                $SwitchMap$com$winlator$inputcontrols$Binding[Binding.KEY_KP_ADD.ordinal()] = 16;
            } catch (NoSuchFieldError e16) {
            }
            try {
                $SwitchMap$com$winlator$inputcontrols$Binding[Binding.MOUSE_LEFT_BUTTON.ordinal()] = 17;
            } catch (NoSuchFieldError e17) {
            }
            try {
                $SwitchMap$com$winlator$inputcontrols$Binding[Binding.MOUSE_MIDDLE_BUTTON.ordinal()] = 18;
            } catch (NoSuchFieldError e18) {
            }
            try {
                $SwitchMap$com$winlator$inputcontrols$Binding[Binding.MOUSE_RIGHT_BUTTON.ordinal()] = 19;
            } catch (NoSuchFieldError e19) {
            }
            try {
                $SwitchMap$com$winlator$inputcontrols$Binding[Binding.MOUSE_SCROLL_UP.ordinal()] = 20;
            } catch (NoSuchFieldError e20) {
            }
            try {
                $SwitchMap$com$winlator$inputcontrols$Binding[Binding.MOUSE_SCROLL_DOWN.ordinal()] = 21;
            } catch (NoSuchFieldError e21) {
            }
        }
    }

    @Override // java.lang.Enum
    public String toString() {
        switch (AnonymousClass1.$SwitchMap$com$winlator$inputcontrols$Binding[ordinal()]) {
            case 1:
                return "L SHIFT";
            case 2:
                return "R SHIFT";
            case 3:
                return "L CTRL";
            case 4:
                return "R CTRL";
            case 5:
                return "L ALT";
            case 6:
                return "R ALT";
            case 7:
                return "[";
            case 8:
                return "]";
            case 9:
                return "\\";
            case 10:
                return "/";
            case 11:
                return ";";
            case 12:
                return ",";
            case 13:
                return ".";
            case 14:
                return "'";
            case 15:
                return "-";
            case 16:
                return "+";
            default:
                return super.toString().replaceAll("^(MOUSE_)|(KEY_)|(GAMEPAD_)", "").replace("KP_", "NUMPAD_").replace("_", " ");
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public static Binding fromString(String name) {
        char c;
        switch (name.hashCode()) {
            case -96200823:
                if (name.equals("KEY_ALT")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 1312809067:
                if (name.equals("KEY_CTRL")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 2056785506:
                if (name.equals("KEY_SHIFT")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                return KEY_CTRL_L;
            case 1:
                return KEY_SHIFT_L;
            case 2:
                return KEY_ALT_L;
            default:
                return valueOf(name);
        }
    }

    public Pointer.Button getPointerButton() {
        switch (AnonymousClass1.$SwitchMap$com$winlator$inputcontrols$Binding[ordinal()]) {
            case 17:
                return Pointer.Button.BUTTON_LEFT;
            case 18:
                return Pointer.Button.BUTTON_MIDDLE;
            case 19:
                return Pointer.Button.BUTTON_RIGHT;
            case 20:
                return Pointer.Button.BUTTON_SCROLL_UP;
            case 21:
                return Pointer.Button.BUTTON_SCROLL_DOWN;
            default:
                return null;
        }
    }

    public boolean isMouse() {
        return name().startsWith("MOUSE_");
    }

    public boolean isKeyboard() {
        return name().startsWith("KEY_") || this == NONE;
    }

    public boolean isGamepad() {
        return name().startsWith("GAMEPAD_");
    }

    public boolean isMouseMove() {
        return this == MOUSE_MOVE_UP || this == MOUSE_MOVE_RIGHT || this == MOUSE_MOVE_DOWN || this == MOUSE_MOVE_LEFT;
    }

    public static String[] mouseBindingLabels() {
        ArrayList<String> names = new ArrayList<>();
        for (Binding binding : values()) {
            if (binding.isMouse()) {
                names.add(binding.toString());
            }
        }
        return (String[]) names.toArray(new String[0]);
    }

    public static String[] keyboardBindingLabels() {
        ArrayList<String> labels = new ArrayList<>();
        for (Binding binding : values()) {
            if (binding.isKeyboard()) {
                labels.add(binding.toString());
            }
        }
        return (String[]) labels.toArray(new String[0]);
    }

    public static String[] gamepadBindingLabels() {
        ArrayList<String> names = new ArrayList<>();
        for (Binding binding : values()) {
            if (binding.isGamepad()) {
                names.add(binding.toString());
            }
        }
        return (String[]) names.toArray(new String[0]);
    }

    public static Binding[] mouseBindingValues() {
        ArrayList<Binding> labels = new ArrayList<>();
        for (Binding binding : values()) {
            if (binding.isMouse()) {
                labels.add(binding);
            }
        }
        return (Binding[]) labels.toArray(new Binding[0]);
    }

    public static Binding[] keyboardBindingValues() {
        ArrayList<Binding> values = new ArrayList<>();
        for (Binding binding : values()) {
            if (binding.isKeyboard()) {
                values.add(binding);
            }
        }
        return (Binding[]) values.toArray(new Binding[0]);
    }

    public static Binding[] gamepadBindingValues() {
        ArrayList<Binding> labels = new ArrayList<>();
        for (Binding binding : values()) {
            if (binding.isGamepad()) {
                labels.add(binding);
            }
        }
        return (Binding[]) labels.toArray(new Binding[0]);
    }
}
