package com.winlator.xserver;

import androidx.constraintlayout.widget.R$styleable;

/* JADX WARN: Enum visitor error
jadx.core.utils.exceptions.JadxRuntimeException: Init of enum field 'KEY_MAX' uses external variables
	at jadx.core.dex.visitors.EnumVisitor.createEnumFieldByConstructor(EnumVisitor.java:451)
	at jadx.core.dex.visitors.EnumVisitor.processEnumFieldByField(EnumVisitor.java:372)
	at jadx.core.dex.visitors.EnumVisitor.processEnumFieldByWrappedInsn(EnumVisitor.java:337)
	at jadx.core.dex.visitors.EnumVisitor.extractEnumFieldsFromFilledArray(EnumVisitor.java:322)
	at jadx.core.dex.visitors.EnumVisitor.extractEnumFieldsFromInsn(EnumVisitor.java:262)
	at jadx.core.dex.visitors.EnumVisitor.extractEnumFieldsFromInvoke(EnumVisitor.java:293)
	at jadx.core.dex.visitors.EnumVisitor.extractEnumFieldsFromInsn(EnumVisitor.java:266)
	at jadx.core.dex.visitors.EnumVisitor.convertToEnum(EnumVisitor.java:151)
	at jadx.core.dex.visitors.EnumVisitor.visit(EnumVisitor.java:100)
 */
/* JADX WARN: Failed to restore enum class, 'enum' modifier and super class removed */
/* loaded from: classes.dex */
public final class XKeycode {
    private static final /* synthetic */ XKeycode[] $VALUES;
    public static final XKeycode KEY_DEL;
    public static final XKeycode KEY_MAX;
    public final byte id;
    public static final XKeycode KEY_NONE = new XKeycode("KEY_NONE", 0, 0);
    public static final XKeycode KEY_ESC = new XKeycode("KEY_ESC", 1, 9);
    public static final XKeycode KEY_1 = new XKeycode("KEY_1", 2, 10);
    public static final XKeycode KEY_2 = new XKeycode("KEY_2", 3, 11);
    public static final XKeycode KEY_3 = new XKeycode("KEY_3", 4, 12);
    public static final XKeycode KEY_4 = new XKeycode("KEY_4", 5, 13);
    public static final XKeycode KEY_5 = new XKeycode("KEY_5", 6, 14);
    public static final XKeycode KEY_6 = new XKeycode("KEY_6", 7, 15);
    public static final XKeycode KEY_7 = new XKeycode("KEY_7", 8, 16);
    public static final XKeycode KEY_8 = new XKeycode("KEY_8", 9, 17);
    public static final XKeycode KEY_9 = new XKeycode("KEY_9", 10, 18);
    public static final XKeycode KEY_0 = new XKeycode("KEY_0", 11, 19);
    public static final XKeycode KEY_MINUS = new XKeycode("KEY_MINUS", 12, 20);
    public static final XKeycode KEY_EQUAL = new XKeycode("KEY_EQUAL", 13, 21);
    public static final XKeycode KEY_BKSP = new XKeycode("KEY_BKSP", 14, 22);
    public static final XKeycode KEY_TAB = new XKeycode("KEY_TAB", 15, 23);
    public static final XKeycode KEY_Q = new XKeycode("KEY_Q", 16, 24);
    public static final XKeycode KEY_W = new XKeycode("KEY_W", 17, 25);
    public static final XKeycode KEY_E = new XKeycode("KEY_E", 18, 26);
    public static final XKeycode KEY_R = new XKeycode("KEY_R", 19, 27);
    public static final XKeycode KEY_T = new XKeycode("KEY_T", 20, 28);
    public static final XKeycode KEY_Y = new XKeycode("KEY_Y", 21, 29);
    public static final XKeycode KEY_U = new XKeycode("KEY_U", 22, 30);
    public static final XKeycode KEY_I = new XKeycode("KEY_I", 23, 31);
    public static final XKeycode KEY_O = new XKeycode("KEY_O", 24, 32);
    public static final XKeycode KEY_P = new XKeycode("KEY_P", 25, 33);
    public static final XKeycode KEY_BRACKET_LEFT = new XKeycode("KEY_BRACKET_LEFT", 26, 34);
    public static final XKeycode KEY_BRACKET_RIGHT = new XKeycode("KEY_BRACKET_RIGHT", 27, 35);
    public static final XKeycode KEY_ENTER = new XKeycode("KEY_ENTER", 28, 36);
    public static final XKeycode KEY_CTRL_L = new XKeycode("KEY_CTRL_L", 29, 37);
    public static final XKeycode KEY_A = new XKeycode("KEY_A", 30, 38);
    public static final XKeycode KEY_S = new XKeycode("KEY_S", 31, 39);
    public static final XKeycode KEY_D = new XKeycode("KEY_D", 32, 40);
    public static final XKeycode KEY_F = new XKeycode("KEY_F", 33, 41);
    public static final XKeycode KEY_G = new XKeycode("KEY_G", 34, 42);
    public static final XKeycode KEY_H = new XKeycode("KEY_H", 35, 43);
    public static final XKeycode KEY_J = new XKeycode("KEY_J", 36, 44);
    public static final XKeycode KEY_K = new XKeycode("KEY_K", 37, 45);
    public static final XKeycode KEY_L = new XKeycode("KEY_L", 38, 46);
    public static final XKeycode KEY_SEMICOLON = new XKeycode("KEY_SEMICOLON", 39, 47);
    public static final XKeycode KEY_APOSTROPHE = new XKeycode("KEY_APOSTROPHE", 40, 48);
    public static final XKeycode KEY_GRAVE = new XKeycode("KEY_GRAVE", 41, 49);
    public static final XKeycode KEY_SHIFT_L = new XKeycode("KEY_SHIFT_L", 42, 50);
    public static final XKeycode KEY_BACKSLASH = new XKeycode("KEY_BACKSLASH", 43, 51);
    public static final XKeycode KEY_Z = new XKeycode("KEY_Z", 44, 52);
    public static final XKeycode KEY_X = new XKeycode("KEY_X", 45, 53);
    public static final XKeycode KEY_C = new XKeycode("KEY_C", 46, 54);
    public static final XKeycode KEY_V = new XKeycode("KEY_V", 47, 55);
    public static final XKeycode KEY_B = new XKeycode("KEY_B", 48, 56);
    public static final XKeycode KEY_N = new XKeycode("KEY_N", 49, 57);
    public static final XKeycode KEY_M = new XKeycode("KEY_M", 50, 58);
    public static final XKeycode KEY_COMMA = new XKeycode("KEY_COMMA", 51, 59);
    public static final XKeycode KEY_PERIOD = new XKeycode("KEY_PERIOD", 52, 60);
    public static final XKeycode KEY_SLASH = new XKeycode("KEY_SLASH", 53, 61);
    public static final XKeycode KEY_SHIFT_R = new XKeycode("KEY_SHIFT_R", 54, 62);
    public static final XKeycode KEY_KP_MULTIPLY = new XKeycode("KEY_KP_MULTIPLY", 55, 63);
    public static final XKeycode KEY_ALT_L = new XKeycode("KEY_ALT_L", 56, 64);
    public static final XKeycode KEY_SPACE = new XKeycode("KEY_SPACE", 57, 65);
    public static final XKeycode KEY_CAPS_LOCK = new XKeycode("KEY_CAPS_LOCK", 58, 66);
    public static final XKeycode KEY_F1 = new XKeycode("KEY_F1", 59, 67);
    public static final XKeycode KEY_F2 = new XKeycode("KEY_F2", 60, 68);
    public static final XKeycode KEY_F3 = new XKeycode("KEY_F3", 61, 69);
    public static final XKeycode KEY_F4 = new XKeycode("KEY_F4", 62, 70);
    public static final XKeycode KEY_F5 = new XKeycode("KEY_F5", 63, 71);
    public static final XKeycode KEY_F6 = new XKeycode("KEY_F6", 64, 72);
    public static final XKeycode KEY_F7 = new XKeycode("KEY_F7", 65, 73);
    public static final XKeycode KEY_F8 = new XKeycode("KEY_F8", 66, 74);
    public static final XKeycode KEY_F9 = new XKeycode("KEY_F9", 67, 75);
    public static final XKeycode KEY_F10 = new XKeycode("KEY_F10", 68, 76);
    public static final XKeycode KEY_NUM_LOCK = new XKeycode("KEY_NUM_LOCK", 69, 77);
    public static final XKeycode KEY_SCROLL_LOCK = new XKeycode("KEY_SCROLL_LOCK", 70, 78);
    public static final XKeycode KEY_KP_7 = new XKeycode("KEY_KP_7", 71, 79);
    public static final XKeycode KEY_KP_8 = new XKeycode("KEY_KP_8", 72, 80);
    public static final XKeycode KEY_KP_9 = new XKeycode("KEY_KP_9", 73, 81);
    public static final XKeycode KEY_KP_SUBTRACT = new XKeycode("KEY_KP_SUBTRACT", 74, 82);
    public static final XKeycode KEY_KP_4 = new XKeycode("KEY_KP_4", 75, 83);
    public static final XKeycode KEY_KP_5 = new XKeycode("KEY_KP_5", 76, 84);
    public static final XKeycode KEY_KP_6 = new XKeycode("KEY_KP_6", 77, 85);
    public static final XKeycode KEY_KP_ADD = new XKeycode("KEY_KP_ADD", 78, 86);
    public static final XKeycode KEY_KP_1 = new XKeycode("KEY_KP_1", 79, 87);
    public static final XKeycode KEY_KP_2 = new XKeycode("KEY_KP_2", 80, 88);
    public static final XKeycode KEY_KP_3 = new XKeycode("KEY_KP_3", 81, 89);
    public static final XKeycode KEY_KP_0 = new XKeycode("KEY_KP_0", 82, 90);
    public static final XKeycode KEY_KP_DEL = new XKeycode("KEY_KP_DEL", 83, 91);
    public static final XKeycode KEY_F11 = new XKeycode("KEY_F11", 84, 95);
    public static final XKeycode KEY_F12 = new XKeycode("KEY_F12", 85, 96);
    public static final XKeycode KEY_KP_ENTER = new XKeycode("KEY_KP_ENTER", 86, R$styleable.Constraint_pathMotionArc);
    public static final XKeycode KEY_CTRL_R = new XKeycode("KEY_CTRL_R", 87, 105);
    public static final XKeycode KEY_KP_DIVIDE = new XKeycode("KEY_KP_DIVIDE", 88, R$styleable.Constraint_transitionEasing);
    public static final XKeycode KEY_PRTSCN = new XKeycode("KEY_PRTSCN", 89, R$styleable.Constraint_transitionPathRotate);
    public static final XKeycode KEY_ALT_R = new XKeycode("KEY_ALT_R", 90, R$styleable.Constraint_visibilityMode);
    public static final XKeycode KEY_HOME = new XKeycode("KEY_HOME", 91, 110);
    public static final XKeycode KEY_UP = new XKeycode("KEY_UP", 92, 111);
    public static final XKeycode KEY_PRIOR = new XKeycode("KEY_PRIOR", 93, 112);
    public static final XKeycode KEY_LEFT = new XKeycode("KEY_LEFT", 94, 113);
    public static final XKeycode KEY_RIGHT = new XKeycode("KEY_RIGHT", 95, androidx.appcompat.R$styleable.AppCompatTheme_viewInflaterClass);
    public static final XKeycode KEY_END = new XKeycode("KEY_END", 96, androidx.appcompat.R$styleable.AppCompatTheme_windowActionBar);
    public static final XKeycode KEY_DOWN = new XKeycode("KEY_DOWN", 97, androidx.appcompat.R$styleable.AppCompatTheme_windowActionBarOverlay);
    public static final XKeycode KEY_NEXT = new XKeycode("KEY_NEXT", 98, androidx.appcompat.R$styleable.AppCompatTheme_windowActionModeOverlay);
    public static final XKeycode KEY_INSERT = new XKeycode("KEY_INSERT", 99, androidx.appcompat.R$styleable.AppCompatTheme_windowFixedHeightMajor);

    private static /* synthetic */ XKeycode[] $values() {
        return new XKeycode[]{KEY_NONE, KEY_ESC, KEY_1, KEY_2, KEY_3, KEY_4, KEY_5, KEY_6, KEY_7, KEY_8, KEY_9, KEY_0, KEY_MINUS, KEY_EQUAL, KEY_BKSP, KEY_TAB, KEY_Q, KEY_W, KEY_E, KEY_R, KEY_T, KEY_Y, KEY_U, KEY_I, KEY_O, KEY_P, KEY_BRACKET_LEFT, KEY_BRACKET_RIGHT, KEY_ENTER, KEY_CTRL_L, KEY_A, KEY_S, KEY_D, KEY_F, KEY_G, KEY_H, KEY_J, KEY_K, KEY_L, KEY_SEMICOLON, KEY_APOSTROPHE, KEY_GRAVE, KEY_SHIFT_L, KEY_BACKSLASH, KEY_Z, KEY_X, KEY_C, KEY_V, KEY_B, KEY_N, KEY_M, KEY_COMMA, KEY_PERIOD, KEY_SLASH, KEY_SHIFT_R, KEY_KP_MULTIPLY, KEY_ALT_L, KEY_SPACE, KEY_CAPS_LOCK, KEY_F1, KEY_F2, KEY_F3, KEY_F4, KEY_F5, KEY_F6, KEY_F7, KEY_F8, KEY_F9, KEY_F10, KEY_NUM_LOCK, KEY_SCROLL_LOCK, KEY_KP_7, KEY_KP_8, KEY_KP_9, KEY_KP_SUBTRACT, KEY_KP_4, KEY_KP_5, KEY_KP_6, KEY_KP_ADD, KEY_KP_1, KEY_KP_2, KEY_KP_3, KEY_KP_0, KEY_KP_DEL, KEY_F11, KEY_F12, KEY_KP_ENTER, KEY_CTRL_R, KEY_KP_DIVIDE, KEY_PRTSCN, KEY_ALT_R, KEY_HOME, KEY_UP, KEY_PRIOR, KEY_LEFT, KEY_RIGHT, KEY_END, KEY_DOWN, KEY_NEXT, KEY_INSERT, KEY_DEL, KEY_MAX};
    }

    public static XKeycode valueOf(String name) {
        return (XKeycode) Enum.valueOf(XKeycode.class, name);
    }

    public static XKeycode[] values() {
        return (XKeycode[]) $VALUES.clone();
    }

    static {
        XKeycode xKeycode = new XKeycode("KEY_DEL", 100, androidx.appcompat.R$styleable.AppCompatTheme_windowFixedHeightMinor);
        KEY_DEL = xKeycode;
        KEY_MAX = new XKeycode("KEY_MAX", R$styleable.Constraint_layout_goneMarginTop, xKeycode.id);
        $VALUES = $values();
    }

    private XKeycode(String str, int i, int id) {
        this.id = (byte) id;
    }
}
