package org.tukaani.xz;

/* loaded from: classes.dex */
interface FilterEncoder extends FilterCoder {
    long getFilterID();

    byte[] getFilterProps();

    FinishableOutputStream getOutputStream(FinishableOutputStream finishableOutputStream, ArrayCache arrayCache);

    boolean supportsFlushing();
}
