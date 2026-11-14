package com.github.luben.zstd;

import com.github.luben.zstd.util.Native;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/* loaded from: classes.dex */
public class ZstdInputStreamNoFinalizer extends FilterInputStream {
    private static final int srcBuffSize;
    private final BufferPool bufferPool;
    private long dstPos;
    private boolean frameFinished;
    private boolean isClosed;
    private boolean isContinuous;
    private boolean needRead;
    private final byte[] src;
    private final ByteBuffer srcByteBuffer;
    private long srcPos;
    private long srcSize;
    private final long stream;

    private static native long createDStream();

    private native int decompressStream(long j, byte[] bArr, int i, byte[] bArr2, int i2);

    private static native int freeDStream(long j);

    private native int initDStream(long j);

    public static native long recommendedDInSize();

    public static native long recommendedDOutSize();

    static {
        Native.load();
        srcBuffSize = (int) recommendedDInSize();
    }

    public ZstdInputStreamNoFinalizer(InputStream inStream) throws IOException {
        this(inStream, NoPool.INSTANCE);
    }

    public ZstdInputStreamNoFinalizer(InputStream inStream, BufferPool bufferPool) throws IOException {
        super(inStream);
        this.dstPos = 0L;
        this.srcPos = 0L;
        this.srcSize = 0L;
        this.needRead = true;
        this.isContinuous = false;
        this.frameFinished = true;
        this.isClosed = false;
        this.bufferPool = bufferPool;
        int i = srcBuffSize;
        ByteBuffer byteBuffer = bufferPool.get(i);
        this.srcByteBuffer = byteBuffer;
        if (byteBuffer == null) {
            throw new IOException("Cannot get ByteBuffer of size " + i + " from the BufferPool");
        }
        this.src = Zstd.extractArray(byteBuffer);
        synchronized (this) {
            long createDStream = createDStream();
            this.stream = createDStream;
            initDStream(createDStream);
        }
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public synchronized int read(byte[] dst, int offset, int len) throws IOException {
        if (offset >= 0) {
            if (len <= dst.length - offset) {
                if (len == 0) {
                    return 0;
                }
                int result = 0;
                while (result == 0) {
                    result = readInternal(dst, offset, len);
                }
                return result;
            }
        }
        throw new IndexOutOfBoundsException("Requested length " + len + " from offset " + offset + " in buffer of size " + dst.length);
    }

    /* JADX WARN: Code restructure failed: missing block: B:55:0x00c3, code lost:
    
        return (int) (r3 - r13);
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    int readInternal(byte[] r12, int r13, int r14) throws java.io.IOException {
        /*
            Method dump skipped, instructions count: 244
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.github.luben.zstd.ZstdInputStreamNoFinalizer.readInternal(byte[], int, int):int");
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public synchronized int read() throws IOException {
        byte[] oneByte = new byte[1];
        int result = 0;
        while (result == 0) {
            result = readInternal(oneByte, 0, 1);
        }
        if (result == 1) {
            return oneByte[0] & 255;
        }
        return -1;
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public synchronized int available() throws IOException {
        if (this.isClosed) {
            throw new IOException("Stream closed");
        }
        if (!this.needRead) {
            return 1;
        }
        return ((FilterInputStream) this).in.available();
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public boolean markSupported() {
        return false;
    }

    @Override // java.io.FilterInputStream, java.io.InputStream
    public synchronized long skip(long numBytes) throws IOException {
        if (this.isClosed) {
            throw new IOException("Stream closed");
        }
        if (numBytes <= 0) {
            return 0L;
        }
        int bufferLen = (int) recommendedDOutSize();
        if (bufferLen > numBytes) {
            bufferLen = (int) numBytes;
        }
        ByteBuffer buf = this.bufferPool.get(bufferLen);
        long toSkip = numBytes;
        try {
            byte[] data = Zstd.extractArray(buf);
            while (toSkip > 0) {
                try {
                    int read = read(data, 0, (int) Math.min(bufferLen, toSkip));
                    if (read < 0) {
                        break;
                    }
                    toSkip -= read;
                } catch (Throwable th) {
                    th = th;
                    this.bufferPool.release(buf);
                    throw th;
                }
            }
            this.bufferPool.release(buf);
            return numBytes - toSkip;
        } catch (Throwable th2) {
            th = th2;
        }
    }

    @Override // java.io.FilterInputStream, java.io.InputStream, java.io.Closeable, java.lang.AutoCloseable
    public synchronized void close() throws IOException {
        if (this.isClosed) {
            return;
        }
        this.isClosed = true;
        this.bufferPool.release(this.srcByteBuffer);
        freeDStream(this.stream);
        ((FilterInputStream) this).in.close();
    }
}
