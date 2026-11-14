package org.apache.commons.compress.utils;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/* loaded from: classes.dex */
public class CountingInputStream extends FilterInputStream {
    private long bytesRead;

    public CountingInputStream(InputStream in) {
        super(in);
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public int read() throws IOException {
        int r = ((FilterInputStream) this).in.read();
        if (r >= 0) {
            count(1L);
        }
        return r;
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public int read(byte[] b, int off, int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        int r = ((FilterInputStream) this).in.read(b, off, len);
        if (r >= 0) {
            count(r);
        }
        return r;
    }

    protected final void count(long read) {
        if (read != -1) {
            this.bytesRead += read;
        }
    }
}
