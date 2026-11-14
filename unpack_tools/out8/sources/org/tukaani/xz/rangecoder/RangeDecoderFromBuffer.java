package org.tukaani.xz.rangecoder;

import java.io.DataInputStream;
import java.io.IOException;
import org.tukaani.xz.ArrayCache;
import org.tukaani.xz.CorruptedInputException;

/* loaded from: classes.dex */
public final class RangeDecoderFromBuffer extends RangeDecoder {
    private final byte[] buf;
    private int pos;

    public RangeDecoderFromBuffer(int i, ArrayCache arrayCache) {
        byte[] byteArray = arrayCache.getByteArray(i - 5, false);
        this.buf = byteArray;
        this.pos = byteArray.length;
    }

    public boolean isFinished() {
        return this.pos == this.buf.length && this.code == 0;
    }

    @Override // org.tukaani.xz.rangecoder.RangeDecoder
    public void normalize() throws IOException {
        int i = this.range;
        if (((-16777216) & i) == 0) {
            try {
                int i2 = this.code << 8;
                byte[] bArr = this.buf;
                int i3 = this.pos;
                this.pos = i3 + 1;
                this.code = i2 | (bArr[i3] & 255);
                this.range = i << 8;
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new CorruptedInputException();
            }
        }
    }

    public void prepareInputBuffer(DataInputStream dataInputStream, int i) throws IOException {
        if (i < 5) {
            throw new CorruptedInputException();
        }
        if (dataInputStream.readUnsignedByte() != 0) {
            throw new CorruptedInputException();
        }
        this.code = dataInputStream.readInt();
        this.range = -1;
        int i2 = i - 5;
        byte[] bArr = this.buf;
        int length = bArr.length - i2;
        this.pos = length;
        dataInputStream.readFully(bArr, length, i2);
    }

    public void putArraysToCache(ArrayCache arrayCache) {
        arrayCache.putArray(this.buf);
    }
}
