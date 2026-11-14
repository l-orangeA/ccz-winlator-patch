package org.tukaani.xz.rangecoder;

import java.util.Arrays;

/* loaded from: classes.dex */
public abstract class RangeCoder {
    public static final void initProbs(short[] sArr) {
        Arrays.fill(sArr, (short) 1024);
    }
}
