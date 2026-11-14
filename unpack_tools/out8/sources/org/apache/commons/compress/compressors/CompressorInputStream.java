package org.apache.commons.compress.compressors;

import java.io.InputStream;

/* loaded from: classes.dex */
public abstract class CompressorInputStream extends InputStream {
    private long bytesRead = 0;

    /* JADX INFO: Access modifiers changed from: protected */
    public void count(int read) {
        count(read);
    }

    protected void count(long read) {
        if (read != -1) {
            this.bytesRead += read;
        }
    }
}
