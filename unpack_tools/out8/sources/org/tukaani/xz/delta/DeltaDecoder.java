package org.tukaani.xz.delta;

/* loaded from: classes.dex */
public class DeltaDecoder extends DeltaCoder {
    public DeltaDecoder(int i) {
        super(i);
    }

    public void decode(byte[] bArr, int i, int i2) {
        int i3 = i2 + i;
        while (i < i3) {
            byte b = bArr[i];
            byte[] bArr2 = this.history;
            int i4 = this.distance;
            int i5 = this.pos;
            bArr[i] = (byte) (b + bArr2[(i4 + i5) & 255]);
            this.pos = i5 - 1;
            bArr2[i5 & 255] = bArr[i];
            i++;
        }
    }
}
