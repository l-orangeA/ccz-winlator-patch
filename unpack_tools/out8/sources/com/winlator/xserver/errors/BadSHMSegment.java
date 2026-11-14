package com.winlator.xserver.errors;

/* loaded from: classes.dex */
public class BadSHMSegment extends XRequestError {
    public BadSHMSegment(int id) {
        super(-128, id);
    }
}
