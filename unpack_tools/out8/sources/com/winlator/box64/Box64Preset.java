package com.winlator.box64;

/* loaded from: classes.dex */
public class Box64Preset {
    public final String id;
    public final String name;

    public Box64Preset(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public boolean isCustom() {
        return this.id.startsWith("CUSTOM");
    }

    public String toString() {
        return this.name;
    }
}
