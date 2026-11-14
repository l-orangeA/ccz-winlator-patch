package org.tukaani.xz;

/* loaded from: classes.dex */
public class ArrayCache {
    private static volatile ArrayCache defaultCache;
    private static final ArrayCache dummyCache;

    static {
        ArrayCache arrayCache = new ArrayCache();
        dummyCache = arrayCache;
        defaultCache = arrayCache;
    }

    public static ArrayCache getDefaultCache() {
        return defaultCache;
    }

    public byte[] getByteArray(int i, boolean z) {
        return new byte[i];
    }

    public int[] getIntArray(int i, boolean z) {
        return new int[i];
    }

    public void putArray(byte[] bArr) {
    }

    public void putArray(int[] iArr) {
    }
}
