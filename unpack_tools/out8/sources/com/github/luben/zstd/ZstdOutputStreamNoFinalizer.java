package com.github.luben.zstd;

import com.github.luben.zstd.util.Native;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/* loaded from: classes.dex */
public class ZstdOutputStreamNoFinalizer extends FilterOutputStream {
    private static final int dstSize;
    private final BufferPool bufferPool;
    private boolean closeFrameOnFlush;
    private final byte[] dst;
    private final ByteBuffer dstByteBuffer;
    private long dstPos;
    private boolean frameClosed;
    private boolean isClosed;
    private long srcPos;
    private final long stream;

    private native int compressStream(long j, byte[] bArr, int i, byte[] bArr2, int i2);

    private static native long createCStream();

    private native int endStream(long j, byte[] bArr, int i);

    private native int flushStream(long j, byte[] bArr, int i);

    private static native int freeCStream(long j);

    public static native long recommendedCOutSize();

    private native int resetCStream(long j);

    static {
        Native.load();
        dstSize = (int) recommendedCOutSize();
    }

    public ZstdOutputStreamNoFinalizer(OutputStream outStream, BufferPool bufferPool) throws IOException {
        super(outStream);
        this.srcPos = 0L;
        this.dstPos = 0L;
        this.isClosed = false;
        this.closeFrameOnFlush = false;
        this.frameClosed = true;
        this.stream = createCStream();
        this.bufferPool = bufferPool;
        int i = dstSize;
        ByteBuffer byteBuffer = bufferPool.get(i);
        this.dstByteBuffer = byteBuffer;
        if (byteBuffer == null) {
            throw new IOException("Cannot get ByteBuffer of size " + i + " from the BufferPool");
        }
        this.dst = Zstd.extractArray(byteBuffer);
    }

    public synchronized ZstdOutputStreamNoFinalizer setLevel(int level) throws IOException {
        if (!this.frameClosed) {
            throw new IOException("Change of parameter on initialized stream");
        }
        int size = Zstd.setCompressionLevel(this.stream, level);
        if (Zstd.isError(size)) {
            throw new IOException("Compression param: " + Zstd.getErrorName(size));
        }
        return this;
    }

    @Override // java.io.FilterOutputStream, java.io.OutputStream
    public synchronized void write(byte[] src, int offset, int len) throws IOException {
        if (this.isClosed) {
            throw new IOException("Stream closed");
        }
        if (this.frameClosed) {
            int size = resetCStream(this.stream);
            if (Zstd.isError(size)) {
                throw new IOException("Compression error: cannot create header: " + Zstd.getErrorName(size));
            }
            this.frameClosed = false;
        }
        int size2 = offset + len;
        this.srcPos = offset;
        while (this.srcPos < size2) {
            int size3 = compressStream(this.stream, this.dst, dstSize, src, size2);
            if (Zstd.isError(size3)) {
                throw new IOException("Compression error: " + Zstd.getErrorName(size3));
            }
            long j = this.dstPos;
            if (j > 0) {
                ((FilterOutputStream) this).out.write(this.dst, 0, (int) j);
            }
        }
    }

    @Override // java.io.FilterOutputStream, java.io.OutputStream
    public void write(int i) throws IOException {
        byte[] oneByte = {(byte) i};
        write(oneByte, 0, 1);
    }

    /* JADX WARN: Code restructure failed: missing block: B:10:0x001d, code lost:
    
        if (com.github.luben.zstd.Zstd.isError(r0) != false) goto L32;
     */
    /* JADX WARN: Code restructure failed: missing block: B:11:0x001f, code lost:
    
        ((java.io.FilterOutputStream) r6).out.write(r6.dst, 0, (int) r6.dstPos);
     */
    /* JADX WARN: Code restructure failed: missing block: B:12:0x0029, code lost:
    
        if (r0 > 0) goto L33;
     */
    /* JADX WARN: Code restructure failed: missing block: B:14:0x002b, code lost:
    
        r6.frameClosed = true;
     */
    /* JADX WARN: Code restructure failed: missing block: B:15:0x0068, code lost:
    
        ((java.io.FilterOutputStream) r6).out.flush();
     */
    /* JADX WARN: Code restructure failed: missing block: B:19:0x004a, code lost:
    
        throw new java.io.IOException("Compression error: " + com.github.luben.zstd.Zstd.getErrorName(r0));
     */
    /* JADX WARN: Code restructure failed: missing block: B:21:0x004b, code lost:
    
        r0 = flushStream(r6.stream, r6.dst, com.github.luben.zstd.ZstdOutputStreamNoFinalizer.dstSize);
     */
    /* JADX WARN: Code restructure failed: missing block: B:22:0x005a, code lost:
    
        if (com.github.luben.zstd.Zstd.isError(r0) != false) goto L35;
     */
    /* JADX WARN: Code restructure failed: missing block: B:23:0x005c, code lost:
    
        ((java.io.FilterOutputStream) r6).out.write(r6.dst, 0, (int) r6.dstPos);
     */
    /* JADX WARN: Code restructure failed: missing block: B:24:0x0066, code lost:
    
        if (r0 > 0) goto L36;
     */
    /* JADX WARN: Code restructure failed: missing block: B:29:0x0089, code lost:
    
        throw new java.io.IOException("Compression error: " + com.github.luben.zstd.Zstd.getErrorName(r0));
     */
    /* JADX WARN: Code restructure failed: missing block: B:8:0x000c, code lost:
    
        if (r6.closeFrameOnFlush != false) goto L9;
     */
    /* JADX WARN: Code restructure failed: missing block: B:9:0x000e, code lost:
    
        r0 = endStream(r6.stream, r6.dst, com.github.luben.zstd.ZstdOutputStreamNoFinalizer.dstSize);
     */
    @Override // java.io.FilterOutputStream, java.io.OutputStream, java.io.Flushable
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public synchronized void flush() throws java.io.IOException {
        /*
            r6 = this;
            monitor-enter(r6)
            boolean r0 = r6.isClosed     // Catch: java.lang.Throwable -> L94
            if (r0 != 0) goto L8c
            boolean r0 = r6.frameClosed     // Catch: java.lang.Throwable -> L94
            if (r0 != 0) goto L8a
            boolean r0 = r6.closeFrameOnFlush     // Catch: java.lang.Throwable -> L94
            r1 = 0
            if (r0 == 0) goto L4b
        Le:
            long r2 = r6.stream     // Catch: java.lang.Throwable -> L94
            byte[] r0 = r6.dst     // Catch: java.lang.Throwable -> L94
            int r4 = com.github.luben.zstd.ZstdOutputStreamNoFinalizer.dstSize     // Catch: java.lang.Throwable -> L94
            int r0 = r6.endStream(r2, r0, r4)     // Catch: java.lang.Throwable -> L94
            long r2 = (long) r0     // Catch: java.lang.Throwable -> L94
            boolean r2 = com.github.luben.zstd.Zstd.isError(r2)     // Catch: java.lang.Throwable -> L94
            if (r2 != 0) goto L2f
            java.io.OutputStream r2 = r6.out     // Catch: java.lang.Throwable -> L94
            byte[] r3 = r6.dst     // Catch: java.lang.Throwable -> L94
            long r4 = r6.dstPos     // Catch: java.lang.Throwable -> L94
            int r4 = (int) r4     // Catch: java.lang.Throwable -> L94
            r2.write(r3, r1, r4)     // Catch: java.lang.Throwable -> L94
            if (r0 > 0) goto Le
            r1 = 1
            r6.frameClosed = r1     // Catch: java.lang.Throwable -> L94
            goto L68
        L2f:
            java.io.IOException r1 = new java.io.IOException     // Catch: java.lang.Throwable -> L94
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L94
            r2.<init>()     // Catch: java.lang.Throwable -> L94
            java.lang.String r3 = "Compression error: "
            r2.append(r3)     // Catch: java.lang.Throwable -> L94
            long r3 = (long) r0     // Catch: java.lang.Throwable -> L94
            java.lang.String r3 = com.github.luben.zstd.Zstd.getErrorName(r3)     // Catch: java.lang.Throwable -> L94
            r2.append(r3)     // Catch: java.lang.Throwable -> L94
            java.lang.String r2 = r2.toString()     // Catch: java.lang.Throwable -> L94
            r1.<init>(r2)     // Catch: java.lang.Throwable -> L94
            throw r1     // Catch: java.lang.Throwable -> L94
        L4b:
            long r2 = r6.stream     // Catch: java.lang.Throwable -> L94
            byte[] r0 = r6.dst     // Catch: java.lang.Throwable -> L94
            int r4 = com.github.luben.zstd.ZstdOutputStreamNoFinalizer.dstSize     // Catch: java.lang.Throwable -> L94
            int r0 = r6.flushStream(r2, r0, r4)     // Catch: java.lang.Throwable -> L94
            long r2 = (long) r0     // Catch: java.lang.Throwable -> L94
            boolean r2 = com.github.luben.zstd.Zstd.isError(r2)     // Catch: java.lang.Throwable -> L94
            if (r2 != 0) goto L6e
            java.io.OutputStream r2 = r6.out     // Catch: java.lang.Throwable -> L94
            byte[] r3 = r6.dst     // Catch: java.lang.Throwable -> L94
            long r4 = r6.dstPos     // Catch: java.lang.Throwable -> L94
            int r4 = (int) r4     // Catch: java.lang.Throwable -> L94
            r2.write(r3, r1, r4)     // Catch: java.lang.Throwable -> L94
            if (r0 > 0) goto L4b
        L68:
            java.io.OutputStream r0 = r6.out     // Catch: java.lang.Throwable -> L94
            r0.flush()     // Catch: java.lang.Throwable -> L94
            goto L8a
        L6e:
            java.io.IOException r1 = new java.io.IOException     // Catch: java.lang.Throwable -> L94
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L94
            r2.<init>()     // Catch: java.lang.Throwable -> L94
            java.lang.String r3 = "Compression error: "
            r2.append(r3)     // Catch: java.lang.Throwable -> L94
            long r3 = (long) r0     // Catch: java.lang.Throwable -> L94
            java.lang.String r3 = com.github.luben.zstd.Zstd.getErrorName(r3)     // Catch: java.lang.Throwable -> L94
            r2.append(r3)     // Catch: java.lang.Throwable -> L94
            java.lang.String r2 = r2.toString()     // Catch: java.lang.Throwable -> L94
            r1.<init>(r2)     // Catch: java.lang.Throwable -> L94
            throw r1     // Catch: java.lang.Throwable -> L94
        L8a:
            monitor-exit(r6)
            return
        L8c:
            java.io.IOException r0 = new java.io.IOException     // Catch: java.lang.Throwable -> L94
            java.lang.String r1 = "Stream closed"
            r0.<init>(r1)     // Catch: java.lang.Throwable -> L94
            throw r0     // Catch: java.lang.Throwable -> L94
        L94:
            r0 = move-exception
            monitor-exit(r6)
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.github.luben.zstd.ZstdOutputStreamNoFinalizer.flush():void");
    }

    @Override // java.io.FilterOutputStream, java.io.OutputStream, java.io.Closeable, java.lang.AutoCloseable
    public synchronized void close() throws IOException {
        close(true);
    }

    /* JADX WARN: Code restructure failed: missing block: B:10:0x0019, code lost:
    
        if (com.github.luben.zstd.Zstd.isError(r1) != false) goto L24;
     */
    /* JADX WARN: Code restructure failed: missing block: B:11:0x001b, code lost:
    
        ((java.io.FilterOutputStream) r7).out.write(r7.dst, 0, (int) r7.dstPos);
     */
    /* JADX WARN: Code restructure failed: missing block: B:12:0x0026, code lost:
    
        if (r1 > 0) goto L25;
     */
    /* JADX WARN: Code restructure failed: missing block: B:18:0x0044, code lost:
    
        throw new java.io.IOException("Compression error: " + com.github.luben.zstd.Zstd.getErrorName(r1));
     */
    /* JADX WARN: Code restructure failed: missing block: B:19:0x0045, code lost:
    
        if (r8 == false) goto L17;
     */
    /* JADX WARN: Code restructure failed: missing block: B:20:0x0047, code lost:
    
        ((java.io.FilterOutputStream) r7).out.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:22:0x005b, code lost:
    
        return;
     */
    /* JADX WARN: Code restructure failed: missing block: B:8:0x0008, code lost:
    
        if (r7.frameClosed == false) goto L8;
     */
    /* JADX WARN: Code restructure failed: missing block: B:9:0x000a, code lost:
    
        r1 = endStream(r7.stream, r7.dst, com.github.luben.zstd.ZstdOutputStreamNoFinalizer.dstSize);
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private void close(boolean r8) throws java.io.IOException {
        /*
            r7 = this;
            boolean r0 = r7.isClosed
            if (r0 == 0) goto L5
            return
        L5:
            r0 = 1
            boolean r1 = r7.frameClosed     // Catch: java.lang.Throwable -> L5c
            if (r1 != 0) goto L45
        La:
            long r1 = r7.stream     // Catch: java.lang.Throwable -> L5c
            byte[] r3 = r7.dst     // Catch: java.lang.Throwable -> L5c
            int r4 = com.github.luben.zstd.ZstdOutputStreamNoFinalizer.dstSize     // Catch: java.lang.Throwable -> L5c
            int r1 = r7.endStream(r1, r3, r4)     // Catch: java.lang.Throwable -> L5c
            long r2 = (long) r1     // Catch: java.lang.Throwable -> L5c
            boolean r2 = com.github.luben.zstd.Zstd.isError(r2)     // Catch: java.lang.Throwable -> L5c
            if (r2 != 0) goto L29
            java.io.OutputStream r2 = r7.out     // Catch: java.lang.Throwable -> L5c
            byte[] r3 = r7.dst     // Catch: java.lang.Throwable -> L5c
            r4 = 0
            long r5 = r7.dstPos     // Catch: java.lang.Throwable -> L5c
            int r5 = (int) r5     // Catch: java.lang.Throwable -> L5c
            r2.write(r3, r4, r5)     // Catch: java.lang.Throwable -> L5c
            if (r1 > 0) goto La
            goto L45
        L29:
            java.io.IOException r2 = new java.io.IOException     // Catch: java.lang.Throwable -> L5c
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L5c
            r3.<init>()     // Catch: java.lang.Throwable -> L5c
            java.lang.String r4 = "Compression error: "
            r3.append(r4)     // Catch: java.lang.Throwable -> L5c
            long r4 = (long) r1     // Catch: java.lang.Throwable -> L5c
            java.lang.String r4 = com.github.luben.zstd.Zstd.getErrorName(r4)     // Catch: java.lang.Throwable -> L5c
            r3.append(r4)     // Catch: java.lang.Throwable -> L5c
            java.lang.String r3 = r3.toString()     // Catch: java.lang.Throwable -> L5c
            r2.<init>(r3)     // Catch: java.lang.Throwable -> L5c
            throw r2     // Catch: java.lang.Throwable -> L5c
        L45:
            if (r8 == 0) goto L4c
            java.io.OutputStream r1 = r7.out     // Catch: java.lang.Throwable -> L5c
            r1.close()     // Catch: java.lang.Throwable -> L5c
        L4c:
            r7.isClosed = r0
            com.github.luben.zstd.BufferPool r0 = r7.bufferPool
            java.nio.ByteBuffer r1 = r7.dstByteBuffer
            r0.release(r1)
            long r0 = r7.stream
            freeCStream(r0)
            return
        L5c:
            r1 = move-exception
            r7.isClosed = r0
            com.github.luben.zstd.BufferPool r0 = r7.bufferPool
            java.nio.ByteBuffer r2 = r7.dstByteBuffer
            r0.release(r2)
            long r2 = r7.stream
            freeCStream(r2)
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.github.luben.zstd.ZstdOutputStreamNoFinalizer.close(boolean):void");
    }
}
