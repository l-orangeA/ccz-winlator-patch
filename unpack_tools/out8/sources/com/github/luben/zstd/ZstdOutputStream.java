package com.github.luben.zstd;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/* loaded from: classes.dex */
public class ZstdOutputStream extends FilterOutputStream {
    private ZstdOutputStreamNoFinalizer inner;

    public ZstdOutputStream(OutputStream outStream, int level) throws IOException {
        this(outStream, NoPool.INSTANCE);
        this.inner.setLevel(level);
    }

    public ZstdOutputStream(OutputStream outStream, BufferPool bufferPool) throws IOException {
        super(outStream);
        this.inner = new ZstdOutputStreamNoFinalizer(outStream, bufferPool);
    }

    protected void finalize() throws Throwable {
        close();
    }

    @Override // java.io.FilterOutputStream, java.io.OutputStream
    public void write(byte[] src, int offset, int len) throws IOException {
        this.inner.write(src, offset, len);
    }

    @Override // java.io.FilterOutputStream, java.io.OutputStream
    public void write(int i) throws IOException {
        this.inner.write(i);
    }

    @Override // java.io.FilterOutputStream, java.io.OutputStream, java.io.Flushable
    public void flush() throws IOException {
        this.inner.flush();
    }

    @Override // java.io.FilterOutputStream, java.io.OutputStream, java.io.Closeable, java.lang.AutoCloseable
    public void close() throws IOException {
        this.inner.close();
    }
}
