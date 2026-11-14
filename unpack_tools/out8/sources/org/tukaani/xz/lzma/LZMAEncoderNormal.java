package org.tukaani.xz.lzma;

import org.tukaani.xz.ArrayCache;
import org.tukaani.xz.lz.LZEncoder;
import org.tukaani.xz.lz.Matches;
import org.tukaani.xz.rangecoder.RangeEncoder;

/* loaded from: classes.dex */
final class LZMAEncoderNormal extends LZMAEncoder {
    private Matches matches;
    private final State nextState;
    private int optCur;
    private int optEnd;
    private final Optimum[] opts;
    private final int[] repLens;

    /* JADX INFO: Access modifiers changed from: package-private */
    public LZMAEncoderNormal(RangeEncoder rangeEncoder, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, ArrayCache arrayCache) {
        super(rangeEncoder, LZEncoder.getInstance(i4, Math.max(i5, 4096), 4096, i6, 273, i7, i8, arrayCache), i, i2, i3, i4, i6);
        this.opts = new Optimum[4096];
        this.optCur = 0;
        this.optEnd = 0;
        this.repLens = new int[4];
        this.nextState = new State();
        for (int i9 = 0; i9 < 4096; i9++) {
            this.opts[i9] = new Optimum();
        }
    }

    private void calc1BytePrices(int i, int i2, int i3, int i4) {
        boolean z;
        int i5 = this.lz.getByte(0);
        int i6 = this.lz.getByte(this.opts[this.optCur].reps[0] + 1);
        int price = this.opts[this.optCur].price + this.literalEncoder.getPrice(i5, i6, this.lz.getByte(1), i, this.opts[this.optCur].state);
        Optimum[] optimumArr = this.opts;
        int i7 = this.optCur;
        if (price < optimumArr[i7 + 1].price) {
            optimumArr[i7 + 1].set1(price, i7, -1);
            z = true;
        } else {
            z = false;
        }
        if (i6 == i5) {
            Optimum[] optimumArr2 = this.opts;
            int i8 = this.optCur;
            if (optimumArr2[i8 + 1].optPrev == i8 || optimumArr2[i8 + 1].backPrev != 0) {
                int shortRepPrice = getShortRepPrice(i4, optimumArr2[i8].state, i2);
                Optimum[] optimumArr3 = this.opts;
                int i9 = this.optCur;
                if (shortRepPrice <= optimumArr3[i9 + 1].price) {
                    optimumArr3[i9 + 1].set1(shortRepPrice, i9, 0);
                    z = true;
                }
            }
        }
        if (z || i6 == i5 || i3 <= 2) {
            return;
        }
        int matchLen = this.lz.getMatchLen(1, this.opts[this.optCur].reps[0], Math.min(this.niceLen, i3 - 1));
        if (matchLen >= 2) {
            this.nextState.set(this.opts[this.optCur].state);
            this.nextState.updateLiteral();
            int longRepAndLenPrice = price + getLongRepAndLenPrice(0, matchLen, this.nextState, (i + 1) & this.posMask);
            int i10 = this.optCur + 1 + matchLen;
            while (true) {
                int i11 = this.optEnd;
                if (i11 >= i10) {
                    break;
                }
                Optimum[] optimumArr4 = this.opts;
                int i12 = i11 + 1;
                this.optEnd = i12;
                optimumArr4[i12].reset();
            }
            Optimum[] optimumArr5 = this.opts;
            if (longRepAndLenPrice < optimumArr5[i10].price) {
                optimumArr5[i10].set2(longRepAndLenPrice, this.optCur, 0);
            }
        }
    }

    private int calcLongRepPrices(int i, int i2, int i3, int i4) {
        int i5;
        int min = Math.min(i3, this.niceLen);
        int i6 = 2;
        for (int i7 = 0; i7 < 4; i7++) {
            int matchLen = this.lz.getMatchLen(this.opts[this.optCur].reps[i7], min);
            if (matchLen >= 2) {
                while (true) {
                    int i8 = this.optEnd;
                    i5 = this.optCur;
                    if (i8 >= i5 + matchLen) {
                        break;
                    }
                    Optimum[] optimumArr = this.opts;
                    int i9 = i8 + 1;
                    this.optEnd = i9;
                    optimumArr[i9].reset();
                }
                int longRepPrice = getLongRepPrice(i4, i7, this.opts[i5].state, i2);
                for (int i10 = matchLen; i10 >= 2; i10--) {
                    int price = this.repLenEncoder.getPrice(i10, i2) + longRepPrice;
                    Optimum[] optimumArr2 = this.opts;
                    int i11 = this.optCur;
                    if (price < optimumArr2[i11 + i10].price) {
                        optimumArr2[i11 + i10].set1(price, i11, i7);
                    }
                }
                if (i7 == 0) {
                    i6 = matchLen + 1;
                }
                int i12 = i6;
                int matchLen2 = this.lz.getMatchLen(matchLen + 1, this.opts[this.optCur].reps[i7], Math.min(this.niceLen, (i3 - matchLen) - 1));
                if (matchLen2 >= 2) {
                    int price2 = longRepPrice + this.repLenEncoder.getPrice(matchLen, i2);
                    this.nextState.set(this.opts[this.optCur].state);
                    this.nextState.updateLongRep();
                    int i13 = i + matchLen;
                    int price3 = price2 + this.literalEncoder.getPrice(this.lz.getByte(matchLen, 0), this.lz.getByte(0), this.lz.getByte(matchLen, 1), i13, this.nextState);
                    this.nextState.updateLiteral();
                    int longRepAndLenPrice = price3 + getLongRepAndLenPrice(0, matchLen2, this.nextState, (i13 + 1) & this.posMask);
                    int i14 = this.optCur + matchLen + 1 + matchLen2;
                    while (true) {
                        int i15 = this.optEnd;
                        if (i15 >= i14) {
                            break;
                        }
                        Optimum[] optimumArr3 = this.opts;
                        int i16 = i15 + 1;
                        this.optEnd = i16;
                        optimumArr3[i16].reset();
                    }
                    Optimum[] optimumArr4 = this.opts;
                    if (longRepAndLenPrice < optimumArr4[i14].price) {
                        optimumArr4[i14].set3(longRepAndLenPrice, this.optCur, i7, matchLen, 0);
                    }
                }
                i6 = i12;
            }
        }
        return i6;
    }

    private void calcNormalMatchPrices(int i, int i2, int i3, int i4, int i5) {
        int i6;
        Matches matches;
        int[] iArr;
        int i7;
        int i8 = i5;
        Matches matches2 = this.matches;
        if (matches2.len[matches2.count - 1] > i3) {
            matches2.count = 0;
            while (true) {
                matches = this.matches;
                iArr = matches.len;
                i7 = matches.count;
                if (iArr[i7] >= i3) {
                    break;
                } else {
                    matches.count = i7 + 1;
                }
            }
            matches.count = i7 + 1;
            iArr[i7] = i3;
        }
        Matches matches3 = this.matches;
        if (matches3.len[matches3.count - 1] < i8) {
            return;
        }
        while (true) {
            int i9 = this.optEnd;
            i6 = this.optCur;
            Matches matches4 = this.matches;
            if (i9 >= matches4.len[matches4.count - 1] + i6) {
                break;
            }
            Optimum[] optimumArr = this.opts;
            int i10 = i9 + 1;
            this.optEnd = i10;
            optimumArr[i10].reset();
        }
        int normalMatchPrice = getNormalMatchPrice(i4, this.opts[i6].state);
        int i11 = 0;
        while (i8 > this.matches.len[i11]) {
            i11++;
        }
        while (true) {
            int i12 = this.matches.dist[i11];
            int matchAndLenPrice = getMatchAndLenPrice(normalMatchPrice, i12, i8, i2);
            Optimum[] optimumArr2 = this.opts;
            int i13 = this.optCur;
            if (matchAndLenPrice < optimumArr2[i13 + i8].price) {
                optimumArr2[i13 + i8].set1(matchAndLenPrice, i13, i12 + 4);
            }
            if (i8 == this.matches.len[i11]) {
                int matchLen = this.lz.getMatchLen(i8 + 1, i12, Math.min(this.niceLen, (i3 - i8) - 1));
                if (matchLen >= 2) {
                    this.nextState.set(this.opts[this.optCur].state);
                    this.nextState.updateMatch();
                    int i14 = i + i8;
                    int price = matchAndLenPrice + this.literalEncoder.getPrice(this.lz.getByte(i8, 0), this.lz.getByte(0), this.lz.getByte(i8, 1), i14, this.nextState);
                    this.nextState.updateLiteral();
                    int longRepAndLenPrice = price + getLongRepAndLenPrice(0, matchLen, this.nextState, (i14 + 1) & this.posMask);
                    int i15 = this.optCur + i8 + 1 + matchLen;
                    while (true) {
                        int i16 = this.optEnd;
                        if (i16 >= i15) {
                            break;
                        }
                        Optimum[] optimumArr3 = this.opts;
                        int i17 = i16 + 1;
                        this.optEnd = i17;
                        optimumArr3[i17].reset();
                    }
                    Optimum[] optimumArr4 = this.opts;
                    if (longRepAndLenPrice < optimumArr4[i15].price) {
                        optimumArr4[i15].set3(longRepAndLenPrice, this.optCur, i12 + 4, i8, 0);
                    }
                }
                i11++;
                if (i11 == this.matches.count) {
                    return;
                }
            }
            i8++;
        }
    }

    private int convertOpts() {
        int i = this.optCur;
        this.optEnd = i;
        int i2 = this.opts[i].optPrev;
        while (true) {
            Optimum[] optimumArr = this.opts;
            int i3 = this.optCur;
            Optimum optimum = optimumArr[i3];
            if (optimum.prev1IsLiteral) {
                optimumArr[i2].optPrev = i3;
                optimumArr[i2].backPrev = -1;
                int i4 = i2 - 1;
                this.optCur = i2;
                if (optimum.hasPrev2) {
                    optimumArr[i4].optPrev = i4 + 1;
                    optimumArr[i4].backPrev = optimum.backPrev2;
                    this.optCur = i4;
                    i2 = optimum.optPrev2;
                } else {
                    i2 = i4;
                }
            }
            int i5 = optimumArr[i2].optPrev;
            optimumArr[i2].optPrev = this.optCur;
            this.optCur = i2;
            if (i2 <= 0) {
                int i6 = optimumArr[0].optPrev;
                this.optCur = i6;
                this.back = optimumArr[i6].backPrev;
                return i6;
            }
            i2 = i5;
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:34:0x00dc  */
    /* JADX WARN: Removed duplicated region for block: B:42:0x0114  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private void updateOptStateAndReps() {
        /*
            Method dump skipped, instructions count: 302
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: org.tukaani.xz.lzma.LZMAEncoderNormal.updateOptStateAndReps():void");
    }

    @Override // org.tukaani.xz.lzma.LZMAEncoder
    int getNextSymbol() {
        int i;
        int i2 = this.optCur;
        int i3 = this.optEnd;
        if (i2 < i3) {
            Optimum[] optimumArr = this.opts;
            int i4 = optimumArr[i2].optPrev - i2;
            int i5 = optimumArr[i2].optPrev;
            this.optCur = i5;
            this.back = optimumArr[i5].backPrev;
            return i4;
        }
        if (i2 != i3) {
            throw new AssertionError();
        }
        this.optCur = 0;
        this.optEnd = 0;
        this.back = -1;
        if (this.readAhead == -1) {
            this.matches = getMatches();
        }
        int min = Math.min(this.lz.getAvail(), 273);
        if (min < 2) {
            return 1;
        }
        int i6 = 0;
        for (int i7 = 0; i7 < 4; i7++) {
            this.repLens[i7] = this.lz.getMatchLen(this.reps[i7], min);
            int[] iArr = this.repLens;
            if (iArr[i7] < 2) {
                iArr[i7] = 0;
            } else if (iArr[i7] > iArr[i6]) {
                i6 = i7;
            }
        }
        int[] iArr2 = this.repLens;
        int i8 = iArr2[i6];
        int i9 = this.niceLen;
        if (i8 >= i9) {
            this.back = i6;
            skip(iArr2[i6] - 1);
            return this.repLens[i6];
        }
        Matches matches = this.matches;
        int i10 = matches.count;
        if (i10 > 0) {
            i = matches.len[i10 - 1];
            int i11 = matches.dist[i10 - 1];
            if (i >= i9) {
                this.back = i11 + 4;
                skip(i - 1);
                return i;
            }
        } else {
            i = 0;
        }
        int i12 = this.lz.getByte(0);
        int i13 = this.lz.getByte(this.reps[0] + 1);
        if (i < 2 && i12 != i13 && this.repLens[i6] < 2) {
            return 1;
        }
        int pos = this.lz.getPos();
        int i14 = pos & this.posMask;
        this.opts[1].set1(this.literalEncoder.getPrice(i12, i13, this.lz.getByte(1), pos, this.state), 0, -1);
        int anyMatchPrice = getAnyMatchPrice(this.state, i14);
        int anyRepPrice = getAnyRepPrice(anyMatchPrice, this.state);
        if (i13 == i12) {
            int shortRepPrice = getShortRepPrice(anyRepPrice, this.state, i14);
            Optimum[] optimumArr2 = this.opts;
            if (shortRepPrice < optimumArr2[1].price) {
                optimumArr2[1].set1(shortRepPrice, 0, 0);
            }
        }
        int max = Math.max(i, this.repLens[i6]);
        this.optEnd = max;
        if (max < 2) {
            if (max != 0) {
                throw new AssertionError(this.optEnd);
            }
            this.back = this.opts[1].backPrev;
            return 1;
        }
        updatePrices();
        this.opts[0].state.set(this.state);
        System.arraycopy(this.reps, 0, this.opts[0].reps, 0, 4);
        for (int i15 = this.optEnd; i15 >= 2; i15--) {
            this.opts[i15].reset();
        }
        int i16 = 0;
        for (int i17 = 4; i16 < i17; i17 = 4) {
            int i18 = this.repLens[i16];
            if (i18 >= 2) {
                int longRepPrice = getLongRepPrice(anyRepPrice, i16, this.state, i14);
                do {
                    int price = this.repLenEncoder.getPrice(i18, i14) + longRepPrice;
                    Optimum[] optimumArr3 = this.opts;
                    if (price < optimumArr3[i18].price) {
                        optimumArr3[i18].set1(price, 0, i16);
                    }
                    i18--;
                } while (i18 >= 2);
            }
            i16++;
        }
        int max2 = Math.max(this.repLens[0] + 1, 2);
        if (max2 <= i) {
            int normalMatchPrice = getNormalMatchPrice(anyMatchPrice, this.state);
            int i19 = 0;
            while (max2 > this.matches.len[i19]) {
                i19++;
            }
            while (true) {
                int i20 = this.matches.dist[i19];
                int matchAndLenPrice = getMatchAndLenPrice(normalMatchPrice, i20, max2, i14);
                Optimum[] optimumArr4 = this.opts;
                if (matchAndLenPrice < optimumArr4[max2].price) {
                    optimumArr4[max2].set1(matchAndLenPrice, 0, i20 + 4);
                }
                Matches matches2 = this.matches;
                if (max2 == matches2.len[i19] && (i19 = i19 + 1) == matches2.count) {
                    break;
                }
                max2++;
            }
        }
        int min2 = Math.min(this.lz.getAvail(), 4095);
        while (true) {
            int i21 = this.optCur + 1;
            this.optCur = i21;
            if (i21 >= this.optEnd) {
                break;
            }
            Matches matches3 = getMatches();
            this.matches = matches3;
            int i22 = matches3.count;
            if (i22 > 0 && matches3.len[i22 - 1] >= this.niceLen) {
                break;
            }
            int i23 = min2 - 1;
            int i24 = pos + 1;
            int i25 = i24 & this.posMask;
            updateOptStateAndReps();
            Optimum[] optimumArr5 = this.opts;
            int i26 = this.optCur;
            int anyMatchPrice2 = optimumArr5[i26].price + getAnyMatchPrice(optimumArr5[i26].state, i25);
            int anyRepPrice2 = getAnyRepPrice(anyMatchPrice2, this.opts[this.optCur].state);
            calc1BytePrices(i24, i25, i23, anyRepPrice2);
            if (i23 >= 2) {
                int calcLongRepPrices = calcLongRepPrices(i24, i25, i23, anyRepPrice2);
                if (this.matches.count > 0) {
                    calcNormalMatchPrices(i24, i25, i23, anyMatchPrice2, calcLongRepPrices);
                }
            }
            min2 = i23;
            pos = i24;
        }
        return convertOpts();
    }

    @Override // org.tukaani.xz.lzma.LZMAEncoder, org.tukaani.xz.lzma.LZMACoder
    public void reset() {
        this.optCur = 0;
        this.optEnd = 0;
        super.reset();
    }
}
