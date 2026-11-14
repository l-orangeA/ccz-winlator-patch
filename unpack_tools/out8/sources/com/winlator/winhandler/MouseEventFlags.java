package com.winlator.winhandler;

import com.winlator.xserver.Pointer;

/* loaded from: classes.dex */
public abstract class MouseEventFlags {

    /* renamed from: com.winlator.winhandler.MouseEventFlags$1, reason: invalid class name */
    /* loaded from: classes.dex */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$winlator$xserver$Pointer$Button;

        static {
            int[] iArr = new int[Pointer.Button.values().length];
            $SwitchMap$com$winlator$xserver$Pointer$Button = iArr;
            try {
                iArr[Pointer.Button.BUTTON_LEFT.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$winlator$xserver$Pointer$Button[Pointer.Button.BUTTON_MIDDLE.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$winlator$xserver$Pointer$Button[Pointer.Button.BUTTON_RIGHT.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$winlator$xserver$Pointer$Button[Pointer.Button.BUTTON_SCROLL_DOWN.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$winlator$xserver$Pointer$Button[Pointer.Button.BUTTON_SCROLL_UP.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
        }
    }

    public static int getFlagFor(Pointer.Button button, boolean isActionDown) {
        switch (AnonymousClass1.$SwitchMap$com$winlator$xserver$Pointer$Button[button.ordinal()]) {
            case 1:
                return isActionDown ? 2 : 4;
            case 2:
                return isActionDown ? 32 : 64;
            case 3:
                return isActionDown ? 8 : 16;
            case 4:
            case 5:
                return 2048;
            default:
                return 0;
        }
    }
}
