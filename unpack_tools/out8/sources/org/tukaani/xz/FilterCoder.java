package org.tukaani.xz;

/* loaded from: classes.dex */
interface FilterCoder {
    boolean changesSize();

    boolean lastOK();

    boolean nonLastOK();
}
