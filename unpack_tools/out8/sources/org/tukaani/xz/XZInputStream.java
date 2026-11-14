package org.tukaani.xz;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/* loaded from: classes.dex */
public class XZInputStream extends InputStream {
    private final ArrayCache arrayCache;
    private boolean endReached;
    private IOException exception;
    private InputStream in;
    private final int memoryLimit;
    private final byte[] tempBuf;
    private final boolean verifyCheck;
    private SingleXZInputStream xzIn;

    public XZInputStream(InputStream inputStream, int i) throws IOException {
        this(inputStream, i, true);
    }

    public XZInputStream(InputStream inputStream, int i, boolean z) throws IOException {
        this(inputStream, i, z, ArrayCache.getDefaultCache());
    }

    public XZInputStream(InputStream inputStream, int i, boolean z, ArrayCache arrayCache) throws IOException {
        this.endReached = false;
        this.exception = null;
        this.tempBuf = new byte[1];
        this.arrayCache = arrayCache;
        this.in = inputStream;
        this.memoryLimit = i;
        this.verifyCheck = z;
        this.xzIn = new SingleXZInputStream(inputStream, i, z, arrayCache);
    }

    private void prepareNextStream() throws IOException {
        DataInputStream dataInputStream = new DataInputStream(this.in);
        byte[] bArr = new byte[12];
        while (dataInputStream.read(bArr, 0, 1) != -1) {
            dataInputStream.readFully(bArr, 1, 3);
            if (bArr[0] != 0 || bArr[1] != 0 || bArr[2] != 0 || bArr[3] != 0) {
                dataInputStream.readFully(bArr, 4, 8);
                try {
                    this.xzIn = new SingleXZInputStream(this.in, this.memoryLimit, this.verifyCheck, bArr, this.arrayCache);
                    return;
                } catch (XZFormatException e) {
                    throw new CorruptedInputException("Garbage after a valid XZ Stream");
                }
            }
        }
        this.endReached = true;
    }

    @Override // java.io.InputStream
    public int available() throws IOException {
        if (this.in == null) {
            throw new XZIOException("Stream closed");
        }
        IOException iOException = this.exception;
        if (iOException != null) {
            throw iOException;
        }
        SingleXZInputStream singleXZInputStream = this.xzIn;
        if (singleXZInputStream == null) {
            return 0;
        }
        return singleXZInputStream.available();
    }

    @Override // java.io.InputStream, java.io.Closeable, java.lang.AutoCloseable
    public void close() throws IOException {
        close(true);
    }

    public void close(boolean z) throws IOException {
        if (this.in != null) {
            SingleXZInputStream singleXZInputStream = this.xzIn;
            if (singleXZInputStream != null) {
                singleXZInputStream.close(false);
                this.xzIn = null;
            }
            if (z) {
                try {
                    this.in.close();
                } finally {
                    this.in = null;
                }
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

    @Override // java.io.InputStream
    public int read(byte[] bArr, int i, int i2) throws IOException {
        int i3;
        if (i < 0 || i2 < 0 || (i3 = i + i2) < 0 || i3 > bArr.length) {
            throw new IndexOutOfBoundsException();
        }
        int i4 = 0;
        if (i2 == 0) {
            return 0;
        }
        if (this.in == null) {
            throw new XZIOException("Stream closed");
        }
        IOException iOException = this.exception;
        if (iOException != null) {
            throw iOException;
        }
        if (this.endReached) {
            return -1;
        }
        while (i2 > 0) {
            try {
                if (this.xzIn == null) {
                    prepareNextStream();
                    if (this.endReached) {
                        if (i4 == 0) {
                            return -1;
                        }
                        return i4;
                    }
                }
                int read = this.xzIn.read(bArr, i, i2);
                if (read > 0) {
                    i4 += read;
                    i += read;
                    i2 -= read;
                } else if (read == -1) {
                    this.xzIn = null;
                }
            } catch (IOException e) {
                this.exception = e;
                if (i4 == 0) {
                    throw e;
                }
            }
        }
        return i4;
    }
}
