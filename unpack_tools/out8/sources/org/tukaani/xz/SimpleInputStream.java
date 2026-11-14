package org.tukaani.xz;

import java.io.IOException;
import java.io.InputStream;
import org.tukaani.xz.simple.SimpleFilter;

/* loaded from: classes.dex */
class SimpleInputStream extends InputStream {
    private InputStream in;
    private final SimpleFilter simpleFilter;
    private final byte[] filterBuf = new byte[4096];
    private int pos = 0;
    private int filtered = 0;
    private int unfiltered = 0;
    private boolean endReached = false;
    private IOException exception = null;
    private final byte[] tempBuf = new byte[1];

    /* JADX INFO: Access modifiers changed from: package-private */
    public SimpleInputStream(InputStream inputStream, SimpleFilter simpleFilter) {
        if (inputStream == null) {
            throw new NullPointerException();
        }
        if (simpleFilter == null) {
            throw new AssertionError();
        }
        this.in = inputStream;
        this.simpleFilter = simpleFilter;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int getMemoryUsage() {
        return 5;
    }

    @Override // java.io.InputStream
    public int available() throws IOException {
        if (this.in == null) {
            throw new XZIOException("Stream closed");
        }
        IOException iOException = this.exception;
        if (iOException == null) {
            return this.filtered;
        }
        throw iOException;
    }

    @Override // java.io.InputStream, java.io.Closeable, java.lang.AutoCloseable
    public void close() throws IOException {
        InputStream inputStream = this.in;
        if (inputStream != null) {
            try {
                inputStream.close();
            } finally {
                this.in = null;
            }
        }
    }

    @Override // java.io.InputStream
    public int read() throws IOException {
        if (read(this.tempBuf, 0, 1) == -1) {
            return -1;
        }
        return this.tempBuf[0] & 255;
    }

    /* JADX WARN: Code restructure failed: missing block: B:45:0x0092, code lost:
    
        if (r1 <= 0) goto L38;
     */
    /* JADX WARN: Code restructure failed: missing block: B:47:0x0096, code lost:
    
        return -1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:48:?, code lost:
    
        return r1;
     */
    @Override // java.io.InputStream
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public int read(byte[] r10, int r11, int r12) throws java.io.IOException {
        /*
            r9 = this;
            if (r11 < 0) goto La4
            if (r12 < 0) goto La4
            int r0 = r11 + r12
            if (r0 < 0) goto La4
            int r1 = r10.length
            if (r0 > r1) goto La4
            r0 = 0
            if (r12 != 0) goto Lf
            return r0
        Lf:
            java.io.InputStream r1 = r9.in
            if (r1 == 0) goto L9c
            java.io.IOException r1 = r9.exception
            if (r1 != 0) goto L9b
            r1 = r0
        L18:
            int r2 = r9.filtered     // Catch: java.io.IOException -> L97
            int r2 = java.lang.Math.min(r2, r12)     // Catch: java.io.IOException -> L97
            byte[] r3 = r9.filterBuf     // Catch: java.io.IOException -> L97
            int r4 = r9.pos     // Catch: java.io.IOException -> L97
            java.lang.System.arraycopy(r3, r4, r10, r11, r2)     // Catch: java.io.IOException -> L97
            int r3 = r9.pos     // Catch: java.io.IOException -> L97
            int r3 = r3 + r2
            r9.pos = r3     // Catch: java.io.IOException -> L97
            int r4 = r9.filtered     // Catch: java.io.IOException -> L97
            int r4 = r4 - r2
            r9.filtered = r4     // Catch: java.io.IOException -> L97
            int r11 = r11 + r2
            int r12 = r12 - r2
            int r1 = r1 + r2
            int r2 = r3 + r4
            int r5 = r9.unfiltered     // Catch: java.io.IOException -> L97
            int r2 = r2 + r5
            r6 = 4096(0x1000, float:5.74E-42)
            if (r2 != r6) goto L43
            byte[] r2 = r9.filterBuf     // Catch: java.io.IOException -> L97
            int r4 = r4 + r5
            java.lang.System.arraycopy(r2, r3, r2, r0, r4)     // Catch: java.io.IOException -> L97
            r9.pos = r0     // Catch: java.io.IOException -> L97
        L43:
            r2 = -1
            if (r12 == 0) goto L92
            boolean r3 = r9.endReached     // Catch: java.io.IOException -> L97
            if (r3 == 0) goto L4b
            goto L92
        L4b:
            int r3 = r9.filtered     // Catch: java.io.IOException -> L97
            if (r3 != 0) goto L8c
            int r4 = r9.pos     // Catch: java.io.IOException -> L97
            int r5 = r4 + r3
            int r7 = r9.unfiltered     // Catch: java.io.IOException -> L97
            int r5 = r5 + r7
            int r6 = r6 - r5
            java.io.InputStream r5 = r9.in     // Catch: java.io.IOException -> L97
            byte[] r8 = r9.filterBuf     // Catch: java.io.IOException -> L97
            int r4 = r4 + r3
            int r4 = r4 + r7
            int r3 = r5.read(r8, r4, r6)     // Catch: java.io.IOException -> L97
            if (r3 != r2) goto L6d
            r2 = 1
            r9.endReached = r2     // Catch: java.io.IOException -> L97
            int r2 = r9.unfiltered     // Catch: java.io.IOException -> L97
            r9.filtered = r2     // Catch: java.io.IOException -> L97
            r9.unfiltered = r0     // Catch: java.io.IOException -> L97
            goto L18
        L6d:
            int r2 = r9.unfiltered     // Catch: java.io.IOException -> L97
            int r2 = r2 + r3
            r9.unfiltered = r2     // Catch: java.io.IOException -> L97
            org.tukaani.xz.simple.SimpleFilter r3 = r9.simpleFilter     // Catch: java.io.IOException -> L97
            byte[] r4 = r9.filterBuf     // Catch: java.io.IOException -> L97
            int r5 = r9.pos     // Catch: java.io.IOException -> L97
            int r2 = r3.code(r4, r5, r2)     // Catch: java.io.IOException -> L97
            r9.filtered = r2     // Catch: java.io.IOException -> L97
            int r3 = r9.unfiltered     // Catch: java.io.IOException -> L97
            if (r2 > r3) goto L86
            int r3 = r3 - r2
            r9.unfiltered = r3     // Catch: java.io.IOException -> L97
            goto L18
        L86:
            java.lang.AssertionError r10 = new java.lang.AssertionError     // Catch: java.io.IOException -> L97
            r10.<init>()     // Catch: java.io.IOException -> L97
            throw r10     // Catch: java.io.IOException -> L97
        L8c:
            java.lang.AssertionError r10 = new java.lang.AssertionError     // Catch: java.io.IOException -> L97
            r10.<init>()     // Catch: java.io.IOException -> L97
            throw r10     // Catch: java.io.IOException -> L97
        L92:
            if (r1 <= 0) goto L95
            goto L96
        L95:
            r1 = r2
        L96:
            return r1
        L97:
            r10 = move-exception
            r9.exception = r10
            throw r10
        L9b:
            throw r1
        L9c:
            org.tukaani.xz.XZIOException r10 = new org.tukaani.xz.XZIOException
            java.lang.String r11 = "Stream closed"
            r10.<init>(r11)
            throw r10
        La4:
            java.lang.IndexOutOfBoundsException r10 = new java.lang.IndexOutOfBoundsException
            r10.<init>()
            throw r10
        */
        throw new UnsupportedOperationException("Method not decompiled: org.tukaani.xz.SimpleInputStream.read(byte[], int, int):int");
    }
}
