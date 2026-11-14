package org.tukaani.xz.check;

/* loaded from: classes.dex */
public class None extends Check {
    public None() {
        this.size = 0;
        this.name = "None";
    }

    @Override // org.tukaani.xz.check.Check
    public byte[] finish() {
        return new byte[0];
    }

    @Override // org.tukaani.xz.check.Check
    public void update(byte[] bArr, int i, int i2) {
    }
}
