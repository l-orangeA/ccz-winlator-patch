package com.winlator.xserver;

/* loaded from: classes.dex */
public abstract class IDGenerator {
    private static int id = 0;

    public static int generate() {
        int i = id + 1;
        id = i;
        return i;
    }
}
