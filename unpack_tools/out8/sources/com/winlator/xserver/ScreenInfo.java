package com.winlator.xserver;

/* loaded from: classes.dex */
public class ScreenInfo {
    public final short height;
    public final short width;

    public ScreenInfo(String value) {
        String[] parts = value.split("x");
        this.width = Short.parseShort(parts[0]);
        this.height = Short.parseShort(parts[1]);
    }

    public short getWidthInMillimeters() {
        return (short) (this.width / 10);
    }

    public short getHeightInMillimeters() {
        return (short) (this.height / 10);
    }

    public String toString() {
        return ((int) this.width) + "x" + ((int) this.height);
    }
}
