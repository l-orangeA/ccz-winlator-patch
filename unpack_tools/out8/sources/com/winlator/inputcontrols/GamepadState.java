package com.winlator.inputcontrols;

import java.nio.ByteBuffer;

/* loaded from: classes.dex */
public class GamepadState {
    public float thumbLX = 0.0f;
    public float thumbLY = 0.0f;
    public float thumbRX = 0.0f;
    public float thumbRY = 0.0f;
    public final boolean[] dpad = new boolean[4];
    public short buttons = 0;

    public byte getPovHat() {
        boolean[] zArr = this.dpad;
        if (zArr[0] && zArr[1]) {
            return (byte) 1;
        }
        if (zArr[1] && zArr[2]) {
            return (byte) 3;
        }
        if (zArr[2] && zArr[3]) {
            return (byte) 5;
        }
        if (zArr[3] && zArr[0]) {
            return (byte) 7;
        }
        if (zArr[0]) {
            return (byte) 0;
        }
        if (zArr[1]) {
            return (byte) 2;
        }
        if (zArr[2]) {
            return (byte) 4;
        }
        return zArr[3] ? (byte) 6 : (byte) -1;
    }

    public void writeTo(ByteBuffer buffer) {
        buffer.putShort(this.buttons);
        buffer.put(getPovHat());
        buffer.putShort((short) (this.thumbLX * 32767.0f));
        buffer.putShort((short) (this.thumbLY * 32767.0f));
        buffer.putShort((short) (this.thumbRX * 32767.0f));
        buffer.putShort((short) (this.thumbRY * 32767.0f));
    }

    public void setPressed(int buttonIdx, boolean pressed) {
        int flag = 1 << buttonIdx;
        if (pressed) {
            this.buttons = (short) (this.buttons | flag);
        } else {
            this.buttons = (short) (this.buttons & (~flag));
        }
    }

    public boolean isPressed(int buttonIdx) {
        return (this.buttons & (1 << buttonIdx)) != 0;
    }

    public byte getDPadX() {
        boolean[] zArr = this.dpad;
        return (byte) (zArr[1] ? 1 : zArr[3] ? -1 : 0);
    }

    public byte getDPadY() {
        boolean[] zArr = this.dpad;
        int i = 0;
        if (zArr[0]) {
            i = -1;
        } else if (zArr[2]) {
            i = 1;
        }
        return (byte) i;
    }

    public void copy(GamepadState other) {
        this.thumbLX = other.thumbLX;
        this.thumbLY = other.thumbLY;
        this.thumbRX = other.thumbRX;
        this.thumbRY = other.thumbRY;
        this.buttons = other.buttons;
        System.arraycopy(other.dpad, 0, this.dpad, 0, 4);
    }
}
