package org.tukaani.xz;

import java.io.InputStream;
import org.tukaani.xz.simple.ARM;
import org.tukaani.xz.simple.ARMThumb;
import org.tukaani.xz.simple.IA64;
import org.tukaani.xz.simple.PowerPC;
import org.tukaani.xz.simple.SPARC;
import org.tukaani.xz.simple.SimpleFilter;
import org.tukaani.xz.simple.X86;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class BCJDecoder extends BCJCoder implements FilterDecoder {
    private final long filterID;
    private final int startOffset;

    /* JADX INFO: Access modifiers changed from: package-private */
    public BCJDecoder(long j, byte[] bArr) throws UnsupportedOptionsException {
        if (!BCJCoder.isBCJFilterID(j)) {
            throw new AssertionError();
        }
        this.filterID = j;
        if (bArr.length == 0) {
            this.startOffset = 0;
            return;
        }
        if (bArr.length != 4) {
            throw new UnsupportedOptionsException("Unsupported BCJ filter properties");
        }
        int i = 0;
        for (int i2 = 0; i2 < 4; i2++) {
            i |= (bArr[i2] & 255) << (i2 * 8);
        }
        this.startOffset = i;
    }

    @Override // org.tukaani.xz.FilterDecoder
    public InputStream getInputStream(InputStream inputStream, ArrayCache arrayCache) {
        SimpleFilter sparc;
        long j = this.filterID;
        if (j == 4) {
            sparc = new X86(false, this.startOffset);
        } else if (j == 5) {
            sparc = new PowerPC(false, this.startOffset);
        } else if (j == 6) {
            sparc = new IA64(false, this.startOffset);
        } else if (j == 7) {
            sparc = new ARM(false, this.startOffset);
        } else if (j == 8) {
            sparc = new ARMThumb(false, this.startOffset);
        } else {
            if (j != 9) {
                throw new AssertionError();
            }
            sparc = new SPARC(false, this.startOffset);
        }
        return new SimpleInputStream(inputStream, sparc);
    }

    @Override // org.tukaani.xz.FilterDecoder
    public int getMemoryUsage() {
        return SimpleInputStream.getMemoryUsage();
    }
}
