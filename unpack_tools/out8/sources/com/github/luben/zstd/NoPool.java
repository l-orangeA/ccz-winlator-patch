package com.github.luben.zstd;

import java.nio.ByteBuffer;

/* loaded from: classes.dex */
public class NoPool implements BufferPool {
    public static final BufferPool INSTANCE = new NoPool();

    private NoPool() {
    }

    @Override // com.github.luben.zstd.BufferPool
    public ByteBuffer get(int capacity) {
        return ByteBuffer.allocate(capacity);
    }

    @Override // com.github.luben.zstd.BufferPool
    public void release(ByteBuffer buffer) {
    }
}
