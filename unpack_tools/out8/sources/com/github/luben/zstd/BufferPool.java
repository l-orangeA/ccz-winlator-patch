package com.github.luben.zstd;

import java.nio.ByteBuffer;

/* loaded from: classes.dex */
public interface BufferPool {
    ByteBuffer get(int i);

    void release(ByteBuffer byteBuffer);
}
