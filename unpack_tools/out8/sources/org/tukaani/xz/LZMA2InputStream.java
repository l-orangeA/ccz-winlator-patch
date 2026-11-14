package org.tukaani.xz;

import androidx.constraintlayout.widget.R$styleable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.tukaani.xz.lz.LZDecoder;
import org.tukaani.xz.lzma.LZMADecoder;
import org.tukaani.xz.rangecoder.RangeDecoderFromBuffer;

/* loaded from: classes.dex */
public class LZMA2InputStream extends InputStream {
    private final ArrayCache arrayCache;
    private DataInputStream in;
    private LZDecoder lz;
    private LZMADecoder lzma;
    private boolean needDictReset;
    private RangeDecoderFromBuffer rc;
    private int uncompressedSize = 0;
    private boolean isLZMAChunk = false;
    private boolean needProps = true;
    private boolean endReached = false;
    private IOException exception = null;
    private final byte[] tempBuf = new byte[1];

    /* JADX INFO: Access modifiers changed from: package-private */
    public LZMA2InputStream(InputStream inputStream, int i, byte[] bArr, ArrayCache arrayCache) {
        this.needDictReset = true;
        if (inputStream == null) {
            throw new NullPointerException();
        }
        this.arrayCache = arrayCache;
        this.in = new DataInputStream(inputStream);
        this.rc = new RangeDecoderFromBuffer(65536, arrayCache);
        this.lz = new LZDecoder(getDictSize(i), bArr, arrayCache);
        if (bArr == null || bArr.length <= 0) {
            return;
        }
        this.needDictReset = false;
    }

    private void decodeChunkHeader() throws IOException {
        int readUnsignedByte = this.in.readUnsignedByte();
        if (readUnsignedByte == 0) {
            this.endReached = true;
            putArraysToCache();
            return;
        }
        if (readUnsignedByte >= 224 || readUnsignedByte == 1) {
            this.needProps = true;
            this.needDictReset = false;
            this.lz.reset();
        } else if (this.needDictReset) {
            throw new CorruptedInputException();
        }
        if (readUnsignedByte < 128) {
            if (readUnsignedByte > 2) {
                throw new CorruptedInputException();
            }
            this.isLZMAChunk = false;
            this.uncompressedSize = this.in.readUnsignedShort() + 1;
            return;
        }
        this.isLZMAChunk = true;
        int i = (readUnsignedByte & 31) << 16;
        this.uncompressedSize = i;
        this.uncompressedSize = i + this.in.readUnsignedShort() + 1;
        int readUnsignedShort = this.in.readUnsignedShort() + 1;
        if (readUnsignedByte >= 192) {
            this.needProps = false;
            decodeProps();
        } else {
            if (this.needProps) {
                throw new CorruptedInputException();
            }
            if (readUnsignedByte >= 160) {
                this.lzma.reset();
            }
        }
        this.rc.prepareInputBuffer(this.in, readUnsignedShort);
    }

    private void decodeProps() throws IOException {
        int readUnsignedByte = this.in.readUnsignedByte();
        if (readUnsignedByte > 224) {
            throw new CorruptedInputException();
        }
        int i = readUnsignedByte / 45;
        int i2 = readUnsignedByte - ((i * 9) * 5);
        int i3 = i2 / 9;
        int i4 = i2 - (i3 * 9);
        if (i4 + i3 > 4) {
            throw new CorruptedInputException();
        }
        this.lzma = new LZMADecoder(this.lz, this.rc, i4, i3, i);
    }

    private static int getDictSize(int i) {
        if (i >= 4096 && i <= 2147483632) {
            return (i + 15) & (-16);
        }
        throw new IllegalArgumentException("Unsupported dictionary size " + i);
    }

    public static int getMemoryUsage(int i) {
        return (getDictSize(i) / 1024) + R$styleable.Constraint_pathMotionArc;
    }

    private void putArraysToCache() {
        LZDecoder lZDecoder = this.lz;
        if (lZDecoder != null) {
            lZDecoder.putArraysToCache(this.arrayCache);
            this.lz = null;
            this.rc.putArraysToCache(this.arrayCache);
            this.rc = null;
        }
    }

    @Override // java.io.InputStream
    public int available() throws IOException {
        DataInputStream dataInputStream = this.in;
        if (dataInputStream == null) {
            throw new XZIOException("Stream closed");
        }
        IOException iOException = this.exception;
        if (iOException == null) {
            return this.isLZMAChunk ? this.uncompressedSize : Math.min(this.uncompressedSize, dataInputStream.available());
        }
        throw iOException;
    }

    @Override // java.io.InputStream, java.io.Closeable, java.lang.AutoCloseable
    public void close() throws IOException {
        if (this.in != null) {
            putArraysToCache();
            try {
                this.in.close();
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
                if (this.uncompressedSize == 0) {
                    decodeChunkHeader();
                    if (this.endReached) {
                        if (i4 == 0) {
                            return -1;
                        }
                        return i4;
                    }
                }
                int min = Math.min(this.uncompressedSize, i2);
                if (this.isLZMAChunk) {
                    this.lz.setLimit(min);
                    this.lzma.decode();
                } else {
                    this.lz.copyUncompressed(this.in, min);
                }
                int flush = this.lz.flush(bArr, i);
                i += flush;
                i2 -= flush;
                i4 += flush;
                int i5 = this.uncompressedSize - flush;
                this.uncompressedSize = i5;
                if (i5 == 0 && (!this.rc.isFinished() || this.lz.hasPending())) {
                    throw new CorruptedInputException();
                }
            } catch (IOException e) {
                this.exception = e;
                throw e;
            }
        }
        return i4;
    }
}
