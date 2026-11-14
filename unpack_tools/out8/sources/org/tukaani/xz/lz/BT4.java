package org.tukaani.xz.lz;

import org.tukaani.xz.ArrayCache;

/* loaded from: classes.dex */
final class BT4 extends LZEncoder {
    private int cyclicPos;
    private final int cyclicSize;
    private final int depthLimit;
    private final Hash234 hash;
    private int lzPos;
    private final Matches matches;
    private final int[] tree;

    /* JADX INFO: Access modifiers changed from: package-private */
    public BT4(int i, int i2, int i3, int i4, int i5, int i6, ArrayCache arrayCache) {
        super(i, i2, i3, i4, i5, arrayCache);
        this.cyclicPos = -1;
        int i7 = i + 1;
        this.cyclicSize = i7;
        this.lzPos = i7;
        this.hash = new Hash234(i, arrayCache);
        this.tree = arrayCache.getIntArray(i7 * 2, false);
        this.matches = new Matches(i4 - 1);
        this.depthLimit = i6 <= 0 ? (i4 / 2) + 16 : i6;
    }

    private int movePos() {
        int movePos = movePos(this.niceLen, 4);
        if (movePos != 0) {
            int i = this.lzPos + 1;
            this.lzPos = i;
            if (i == Integer.MAX_VALUE) {
                int i2 = Integer.MAX_VALUE - this.cyclicSize;
                this.hash.normalize(i2);
                LZEncoder.normalize(this.tree, this.cyclicSize * 2, i2);
                this.lzPos -= i2;
            }
            int i3 = this.cyclicPos + 1;
            this.cyclicPos = i3;
            if (i3 == this.cyclicSize) {
                this.cyclicPos = 0;
            }
        }
        return movePos;
    }

    /* JADX WARN: Code restructure failed: missing block: B:10:0x0034, code lost:
    
        if (r9[(r10 + r8) - r6] == r9[r10 + r8]) goto L14;
     */
    /* JADX WARN: Code restructure failed: missing block: B:11:0x0036, code lost:
    
        r8 = r8 + 1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:12:0x0038, code lost:
    
        if (r8 != r13) goto L18;
     */
    /* JADX WARN: Code restructure failed: missing block: B:13:0x0047, code lost:
    
        r9 = r12.buf;
        r10 = r12.readPos;
     */
    /* JADX WARN: Code restructure failed: missing block: B:14:0x0053, code lost:
    
        if (r9[(r10 + r8) - r6] == r9[r10 + r8]) goto L33;
     */
    /* JADX WARN: Code restructure failed: missing block: B:18:0x003a, code lost:
    
        r13 = r12.tree;
        r13[r1] = r13[r0];
        r13[r2] = r13[r0 + 1];
     */
    /* JADX WARN: Code restructure failed: missing block: B:19:0x0046, code lost:
    
        return;
     */
    /* JADX WARN: Code restructure failed: missing block: B:21:0x0055, code lost:
    
        r9 = r12.buf;
        r10 = r12.readPos;
     */
    /* JADX WARN: Code restructure failed: missing block: B:22:0x0065, code lost:
    
        if ((r9[(r10 + r8) - r6] & 255) >= (r9[r10 + r8] & 255)) goto L23;
     */
    /* JADX WARN: Code restructure failed: missing block: B:23:0x0067, code lost:
    
        r5 = r12.tree;
        r5[r1] = r14;
        r0 = r0 + 1;
        r14 = r5[r0];
        r1 = r0;
        r5 = r8;
     */
    /* JADX WARN: Code restructure failed: missing block: B:26:0x0072, code lost:
    
        r4 = r12.tree;
        r4[r2] = r14;
        r14 = r4[r0];
        r2 = r0;
        r4 = r8;
     */
    /* JADX WARN: Code restructure failed: missing block: B:29:0x007c, code lost:
    
        r13 = r12.tree;
        r13[r2] = 0;
        r13[r1] = 0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:30:0x0082, code lost:
    
        return;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private void skip(int r13, int r14) {
        /*
            r12 = this;
            int r0 = r12.depthLimit
            int r1 = r12.cyclicPos
            int r2 = r1 << 1
            int r2 = r2 + 1
            int r1 = r1 << 1
            r3 = 0
            r4 = r3
            r5 = r4
        Ld:
            int r6 = r12.lzPos
            int r6 = r6 - r14
            int r7 = r0 + (-1)
            if (r0 == 0) goto L7c
            int r0 = r12.cyclicSize
            if (r6 < r0) goto L19
            goto L7c
        L19:
            int r8 = r12.cyclicPos
            int r9 = r8 - r6
            if (r6 <= r8) goto L20
            goto L21
        L20:
            r0 = r3
        L21:
            int r9 = r9 + r0
            int r0 = r9 << 1
            int r8 = java.lang.Math.min(r4, r5)
            byte[] r9 = r12.buf
            int r10 = r12.readPos
            int r11 = r10 + r8
            int r11 = r11 - r6
            r11 = r9[r11]
            int r10 = r10 + r8
            r9 = r9[r10]
            if (r11 != r9) goto L55
        L36:
            int r8 = r8 + 1
            if (r8 != r13) goto L47
            int[] r13 = r12.tree
            r14 = r13[r0]
            r13[r1] = r14
            int r0 = r0 + 1
            r14 = r13[r0]
            r13[r2] = r14
            return
        L47:
            byte[] r9 = r12.buf
            int r10 = r12.readPos
            int r11 = r10 + r8
            int r11 = r11 - r6
            r11 = r9[r11]
            int r10 = r10 + r8
            r9 = r9[r10]
            if (r11 == r9) goto L36
        L55:
            byte[] r9 = r12.buf
            int r10 = r12.readPos
            int r11 = r10 + r8
            int r11 = r11 - r6
            r6 = r9[r11]
            r6 = r6 & 255(0xff, float:3.57E-43)
            int r10 = r10 + r8
            r9 = r9[r10]
            r9 = r9 & 255(0xff, float:3.57E-43)
            if (r6 >= r9) goto L72
            int[] r5 = r12.tree
            r5[r1] = r14
            int r0 = r0 + 1
            r14 = r5[r0]
            r1 = r0
            r5 = r8
            goto L7a
        L72:
            int[] r4 = r12.tree
            r4[r2] = r14
            r14 = r4[r0]
            r2 = r0
            r4 = r8
        L7a:
            r0 = r7
            goto Ld
        L7c:
            int[] r13 = r12.tree
            r13[r2] = r3
            r13[r1] = r3
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: org.tukaani.xz.lz.BT4.skip(int, int):void");
    }

    /* JADX WARN: Code restructure failed: missing block: B:45:0x00e6, code lost:
    
        if (r15[(r2 + r14) - r12] == r15[r2 + r14]) goto L48;
     */
    /* JADX WARN: Code restructure failed: missing block: B:46:0x00e8, code lost:
    
        r14 = r14 + 1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:47:0x00ea, code lost:
    
        if (r14 >= r1) goto L73;
     */
    /* JADX WARN: Code restructure failed: missing block: B:48:0x00ec, code lost:
    
        r2 = r17.buf;
        r9 = r17.readPos;
     */
    /* JADX WARN: Code restructure failed: missing block: B:49:0x00f8, code lost:
    
        if (r2[(r9 + r14) - r12] == r2[r9 + r14]) goto L74;
     */
    /* JADX WARN: Code restructure failed: missing block: B:51:0x00fa, code lost:
    
        if (r14 <= r10) goto L58;
     */
    /* JADX WARN: Code restructure failed: missing block: B:52:0x00fc, code lost:
    
        r2 = r17.matches;
        r9 = r2.len;
        r10 = r2.count;
        r9[r10] = r14;
        r2.dist[r10] = r12 - 1;
        r2.count = r10 + 1;
     */
    /* JADX WARN: Code restructure failed: missing block: B:53:0x010e, code lost:
    
        if (r14 < r3) goto L57;
     */
    /* JADX WARN: Code restructure failed: missing block: B:54:0x011c, code lost:
    
        r10 = r14;
     */
    /* JADX WARN: Code restructure failed: missing block: B:55:0x011f, code lost:
    
        r2 = r17.buf;
        r15 = r17.readPos;
     */
    /* JADX WARN: Code restructure failed: missing block: B:56:0x0130, code lost:
    
        if ((r2[(r15 + r14) - r12] & 255) >= (r2[r15 + r14] & 255)) goto L62;
     */
    /* JADX WARN: Code restructure failed: missing block: B:57:0x0132, code lost:
    
        r2 = r17.tree;
        r2[r5] = r6;
        r4 = r4 + 1;
        r6 = r2[r4];
        r5 = r4;
        r11 = r14;
     */
    /* JADX WARN: Code restructure failed: missing block: B:60:0x013e, code lost:
    
        r2 = r17.tree;
        r2[r7] = r6;
        r6 = r2[r4];
        r7 = r4;
        r8 = r14;
     */
    /* JADX WARN: Code restructure failed: missing block: B:63:0x0110, code lost:
    
        r1 = r17.tree;
        r1[r5] = r1[r4];
        r1[r7] = r1[r4 + 1];
     */
    /* JADX WARN: Code restructure failed: missing block: B:64:0x011b, code lost:
    
        return r2;
     */
    /* JADX WARN: Code restructure failed: missing block: B:69:0x014b, code lost:
    
        r1 = r17.tree;
        r1[r7] = 0;
        r1[r5] = 0;
     */
    /* JADX WARN: Removed duplicated region for block: B:20:0x0075  */
    /* JADX WARN: Removed duplicated region for block: B:23:0x008b A[ADDED_TO_REGION, LOOP:0: B:23:0x008b->B:26:0x009b, LOOP_START, PHI: r8
  0x008b: PHI (r8v10 int) = (r8v4 int), (r8v11 int) binds: [B:22:0x0089, B:26:0x009b] A[DONT_GENERATE, DONT_INLINE]] */
    /* JADX WARN: Removed duplicated region for block: B:35:0x00b2  */
    @Override // org.tukaani.xz.lz.LZEncoder
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public org.tukaani.xz.lz.Matches getMatches() {
        /*
            Method dump skipped, instructions count: 340
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: org.tukaani.xz.lz.BT4.getMatches():org.tukaani.xz.lz.Matches");
    }

    @Override // org.tukaani.xz.lz.LZEncoder
    public void putArraysToCache(ArrayCache arrayCache) {
        arrayCache.putArray(this.tree);
        this.hash.putArraysToCache(arrayCache);
        super.putArraysToCache(arrayCache);
    }

    @Override // org.tukaani.xz.lz.LZEncoder
    public void skip(int i) {
        while (true) {
            int i2 = i - 1;
            if (i <= 0) {
                return;
            }
            int i3 = this.niceLen;
            int movePos = movePos();
            if (movePos < i3) {
                if (movePos == 0) {
                    i = i2;
                } else {
                    i3 = movePos;
                }
            }
            this.hash.calcHashes(this.buf, this.readPos);
            int hash4Pos = this.hash.getHash4Pos();
            this.hash.updateTables(this.lzPos);
            skip(i3, hash4Pos);
            i = i2;
        }
    }
}
