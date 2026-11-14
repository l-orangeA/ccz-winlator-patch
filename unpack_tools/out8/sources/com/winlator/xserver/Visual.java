package com.winlator.xserver;

/* loaded from: classes.dex */
public class Visual {
    public final byte bitsPerRGBValue;
    public final int blueMask;
    public final byte depth;
    public final boolean displayable;
    public final int greenMask;
    public final int id;
    public final int redMask;
    public final Type type = Type.TRUE_COLOR;
    public final short colormapEntries = 256;

    /* loaded from: classes.dex */
    public enum Type {
        STATIC_GRAY,
        GRAYSCALE,
        STATIC_COLOR,
        PSEUDO_COLOR,
        TRUE_COLOR,
        DIRECT_COLOR
    }

    public Visual(int id, boolean displayable, int depth, int bitsPerRGBValue, int redMask, int greenMask, int blueMask) {
        this.id = id;
        this.displayable = displayable;
        this.depth = (byte) depth;
        this.bitsPerRGBValue = (byte) bitsPerRGBValue;
        this.redMask = redMask;
        this.greenMask = greenMask;
        this.blueMask = blueMask;
    }
}
