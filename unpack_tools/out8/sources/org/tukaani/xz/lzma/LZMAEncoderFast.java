package org.tukaani.xz.lzma;

import org.tukaani.xz.ArrayCache;
import org.tukaani.xz.lz.LZEncoder;
import org.tukaani.xz.lz.Matches;
import org.tukaani.xz.rangecoder.RangeEncoder;

/* loaded from: classes.dex */
final class LZMAEncoderFast extends LZMAEncoder {
    private Matches matches;

    /* JADX INFO: Access modifiers changed from: package-private */
    public LZMAEncoderFast(RangeEncoder rangeEncoder, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, ArrayCache arrayCache) {
        super(rangeEncoder, LZEncoder.getInstance(i4, Math.max(i5, 1), 272, i6, 273, i7, i8, arrayCache), i, i2, i3, i4, i6);
        this.matches = null;
    }

    private boolean changePair(int i, int i2) {
        return i < (i2 >>> 7);
    }

    /* JADX WARN: Code restructure failed: missing block: B:42:0x0096, code lost:
    
        if (r4 < 128) goto L41;
     */
    /* JADX WARN: Code restructure failed: missing block: B:43:0x0098, code lost:
    
        r9 = 1;
     */
    @Override // org.tukaani.xz.lzma.LZMAEncoder
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    int getNextSymbol() {
        /*
            Method dump skipped, instructions count: 274
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: org.tukaani.xz.lzma.LZMAEncoderFast.getNextSymbol():int");
    }
}
