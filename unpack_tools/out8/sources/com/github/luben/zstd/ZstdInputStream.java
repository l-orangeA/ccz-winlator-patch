package com.github.luben.zstd;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/* loaded from: classes.dex */
public class ZstdInputStream extends FilterInputStream {
    private ZstdInputStreamNoFinalizer inner;

    public ZstdInputStream(InputStream inStream) throws IOException {
        super(inStream);
        this.inner = new ZstdInputStreamNoFinalizer(inStream);
    }

    protected void finalize() throws Throwable {
        close();
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public int read(byte[] dst, int offset, int len) throws IOException {
        return this.inner.read(dst, offset, len);
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public int read() throws IOException {
        return this.inner.read();
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public int available() throws IOException {
        return this.inner.available();
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public long skip(long numBytes) throws IOException {
        return this.inner.skip(numBytes);
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public boolean markSupported() {
        return this.inner.markSupported();
    }

    @Override // java.io.FilterInputStream, java.io.InputStream, java.io.Closeable, java.lang.AutoCloseable
    public void close() throws IOException {
        this.inner.close();
    }
}
