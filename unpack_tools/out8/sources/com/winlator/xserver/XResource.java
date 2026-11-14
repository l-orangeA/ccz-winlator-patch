package com.winlator.xserver;

/* loaded from: classes.dex */
public abstract class XResource {
    public final int id;

    public XResource(int id) {
        this.id = id;
    }

    public int hashCode() {
        return this.id;
    }
}
