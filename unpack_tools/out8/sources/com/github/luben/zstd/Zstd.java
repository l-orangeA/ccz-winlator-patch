package com.github.luben.zstd;

import com.github.luben.zstd.util.Native;
import java.nio.ByteBuffer;

/* loaded from: classes.dex */
public class Zstd {
    public static native String getErrorName(long j);

    public static native boolean isError(long j);

    public static native int setCompressionLevel(long j, int i);

    static {
        Native.load();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static final byte[] extractArray(ByteBuffer buffer) {
        if (!buffer.hasArray() || buffer.arrayOffset() != 0) {
            throw new IllegalArgumentException("provided ByteBuffer lacks array or has non-zero arrayOffset");
        }
        return buffer.array();
    }
}
