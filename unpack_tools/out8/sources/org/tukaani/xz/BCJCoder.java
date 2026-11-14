package org.tukaani.xz;

/* loaded from: classes.dex */
abstract class BCJCoder implements FilterCoder {
    public static boolean isBCJFilterID(long j) {
        return j >= 4 && j <= 9;
    }

    @Override // org.tukaani.xz.FilterCoder
    public boolean changesSize() {
        return false;
    }

    @Override // org.tukaani.xz.FilterCoder
    public boolean lastOK() {
        return false;
    }

    @Override // org.tukaani.xz.FilterCoder
    public boolean nonLastOK() {
        return true;
    }
}
