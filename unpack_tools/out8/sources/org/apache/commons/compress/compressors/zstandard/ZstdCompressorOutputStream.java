package org.apache.commons.compress.compressors.zstandard;

import com.github.luben.zstd.ZstdOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;

/* loaded from: classes.dex */
public class ZstdCompressorOutputStream extends CompressorOutputStream {
    private final ZstdOutputStream encOS;

    public ZstdCompressorOutputStream(OutputStream outStream, int level) throws IOException {
        this.encOS = new ZstdOutputStream(outStream, level);
    }

    @Override // java.io.OutputStream, java.io.Closeable, java.lang.AutoCloseable
    public void close() throws IOException {
        this.encOS.close();
    }

    @Override // java.io.OutputStream
    public void write(int b) throws IOException {
        this.encOS.write(b);
    }

    @Override // java.io.OutputStream
    public void write(byte[] buf, int off, int len) throws IOException {
        this.encOS.write(buf, off, len);
    }

    public String toString() {
        return this.encOS.toString();
    }

    @Override // java.io.OutputStream, java.io.Flushable
    public void flush() throws IOException {
        this.encOS.flush();
    }
}
