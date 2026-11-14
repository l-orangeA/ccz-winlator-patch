package org.tukaani.xz.check;

import java.security.NoSuchAlgorithmException;
import org.tukaani.xz.UnsupportedOptionsException;

/* loaded from: classes.dex */
public abstract class Check {
    String name;
    int size;

    public static Check getInstance(int i) throws UnsupportedOptionsException {
        switch (i) {
            case 0:
                return new None();
            case 1:
                return new CRC32();
            case 4:
                return new CRC64();
            case 10:
                try {
                    return new SHA256();
                } catch (NoSuchAlgorithmException e) {
                    break;
                }
        }
        throw new UnsupportedOptionsException("Unsupported Check ID " + i);
    }

    public abstract byte[] finish();

    public String getName() {
        return this.name;
    }

    public int getSize() {
        return this.size;
    }

    public void update(byte[] bArr) {
        update(bArr, 0, bArr.length);
    }

    public abstract void update(byte[] bArr, int i, int i2);
}
