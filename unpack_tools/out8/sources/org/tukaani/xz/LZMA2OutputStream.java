package org.tukaani.xz;

import java.io.DataOutputStream;
import java.io.IOException;
import org.tukaani.xz.lz.LZEncoder;
import org.tukaani.xz.lzma.LZMAEncoder;
import org.tukaani.xz.rangecoder.RangeEncoderToBuffer;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class LZMA2OutputStream extends FinishableOutputStream {
    private final ArrayCache arrayCache;
    private boolean dictResetNeeded;
    private LZEncoder lz;
    private LZMAEncoder lzma;
    private FinishableOutputStream out;
    private final DataOutputStream outData;
    private final int props;
    private RangeEncoderToBuffer rc;
    private boolean stateResetNeeded = true;
    private boolean propsNeeded = true;
    private int pendingSize = 0;
    private boolean finished = false;
    private IOException exception = null;
    private final byte[] tempBuf = new byte[1];

    /* JADX INFO: Access modifiers changed from: package-private */
    public LZMA2OutputStream(FinishableOutputStream finishableOutputStream, LZMA2Options lZMA2Options, ArrayCache arrayCache) {
        this.dictResetNeeded = true;
        if (finishableOutputStream == null) {
            throw new NullPointerException();
        }
        this.arrayCache = arrayCache;
        this.out = finishableOutputStream;
        this.outData = new DataOutputStream(finishableOutputStream);
        this.rc = new RangeEncoderToBuffer(65536, arrayCache);
        int dictSize = lZMA2Options.getDictSize();
        LZMAEncoder lZMAEncoder = LZMAEncoder.getInstance(this.rc, lZMA2Options.getLc(), lZMA2Options.getLp(), lZMA2Options.getPb(), lZMA2Options.getMode(), dictSize, getExtraSizeBefore(dictSize), lZMA2Options.getNiceLen(), lZMA2Options.getMatchFinder(), lZMA2Options.getDepthLimit(), arrayCache);
        this.lzma = lZMAEncoder;
        this.lz = lZMAEncoder.getLZEncoder();
        byte[] presetDict = lZMA2Options.getPresetDict();
        if (presetDict != null && presetDict.length > 0) {
            this.lz.setPresetDict(dictSize, presetDict);
            this.dictResetNeeded = false;
        }
        this.props = (((lZMA2Options.getPb() * 5) + lZMA2Options.getLp()) * 9) + lZMA2Options.getLc();
    }

    private static int getExtraSizeBefore(int i) {
        if (65536 > i) {
            return 65536 - i;
        }
        return 0;
    }

    private void writeChunk() throws IOException {
        int finish = this.rc.finish();
        int uncompressedSize = this.lzma.getUncompressedSize();
        if (finish <= 0) {
            throw new AssertionError(finish);
        }
        if (uncompressedSize <= 0) {
            throw new AssertionError(uncompressedSize);
        }
        if (finish + 2 < uncompressedSize) {
            writeLZMA(uncompressedSize, finish);
        } else {
            this.lzma.reset();
            uncompressedSize = this.lzma.getUncompressedSize();
            if (uncompressedSize <= 0) {
                throw new AssertionError(uncompressedSize);
            }
            writeUncompressed(uncompressedSize);
        }
        this.pendingSize -= uncompressedSize;
        this.lzma.resetUncompressedSize();
        this.rc.reset();
    }

    private void writeEndMarker() throws IOException {
        if (this.finished) {
            throw new AssertionError();
        }
        IOException iOException = this.exception;
        if (iOException != null) {
            throw iOException;
        }
        this.lz.setFinishing();
        while (this.pendingSize > 0) {
            try {
                this.lzma.encodeForLZMA2();
                writeChunk();
            } catch (IOException e) {
                this.exception = e;
                throw e;
            }
        }
        this.out.write(0);
        this.finished = true;
        this.lzma.putArraysToCache(this.arrayCache);
        this.lzma = null;
        this.lz = null;
        this.rc.putArraysToCache(this.arrayCache);
        this.rc = null;
    }

    private void writeLZMA(int i, int i2) throws IOException {
        int i3 = i - 1;
        this.outData.writeByte((this.propsNeeded ? this.dictResetNeeded ? 224 : 192 : this.stateResetNeeded ? 160 : 128) | (i3 >>> 16));
        this.outData.writeShort(i3);
        this.outData.writeShort(i2 - 1);
        if (this.propsNeeded) {
            this.outData.writeByte(this.props);
        }
        this.rc.write(this.out);
        this.propsNeeded = false;
        this.stateResetNeeded = false;
        this.dictResetNeeded = false;
    }

    private void writeUncompressed(int i) throws IOException {
        while (true) {
            int i2 = 1;
            if (i <= 0) {
                this.stateResetNeeded = true;
                return;
            }
            int min = Math.min(i, 65536);
            DataOutputStream dataOutputStream = this.outData;
            if (!this.dictResetNeeded) {
                i2 = 2;
            }
            dataOutputStream.writeByte(i2);
            this.outData.writeShort(min - 1);
            this.lz.copyUncompressed(this.out, i, min);
            i -= min;
            this.dictResetNeeded = false;
        }
    }

    @Override // java.io.OutputStream, java.io.Closeable, java.lang.AutoCloseable
    public void close() throws IOException {
        if (this.out != null) {
            if (!this.finished) {
                try {
                    writeEndMarker();
                } catch (IOException e) {
                }
            }
            try {
                this.out.close();
            } catch (IOException e2) {
                if (this.exception == null) {
                    this.exception = e2;
                }
            }
            this.out = null;
        }
        IOException iOException = this.exception;
        if (iOException != null) {
            throw iOException;
        }
    }

    @Override // org.tukaani.xz.FinishableOutputStream
    public void finish() throws IOException {
        if (this.finished) {
            return;
        }
        writeEndMarker();
        try {
            this.out.finish();
        } catch (IOException e) {
            this.exception = e;
            throw e;
        }
    }

    @Override // java.io.OutputStream, java.io.Flushable
    public void flush() throws IOException {
        IOException iOException = this.exception;
        if (iOException != null) {
            throw iOException;
        }
        if (this.finished) {
            throw new XZIOException("Stream finished or closed");
        }
        try {
            this.lz.setFlushing();
            while (this.pendingSize > 0) {
                this.lzma.encodeForLZMA2();
                writeChunk();
            }
            this.out.flush();
        } catch (IOException e) {
            this.exception = e;
            throw e;
        }
    }

    @Override // java.io.OutputStream
    public void write(int i) throws IOException {
        byte[] bArr = this.tempBuf;
        bArr[0] = (byte) i;
        write(bArr, 0, 1);
    }

    @Override // java.io.OutputStream
    public void write(byte[] bArr, int i, int i2) throws IOException {
        int i3;
        if (i < 0 || i2 < 0 || (i3 = i + i2) < 0 || i3 > bArr.length) {
            throw new IndexOutOfBoundsException();
        }
        IOException iOException = this.exception;
        if (iOException != null) {
            throw iOException;
        }
        if (this.finished) {
            throw new XZIOException("Stream finished or closed");
        }
        while (i2 > 0) {
            try {
                int fillWindow = this.lz.fillWindow(bArr, i, i2);
                i += fillWindow;
                i2 -= fillWindow;
                this.pendingSize += fillWindow;
                if (this.lzma.encodeForLZMA2()) {
                    writeChunk();
                }
            } catch (IOException e) {
                this.exception = e;
                throw e;
            }
        }
    }
}
