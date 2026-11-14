package org.apache.commons.compress.archivers;

import java.io.IOException;
import java.io.InputStream;

/* loaded from: classes.dex */
public abstract class ArchiveInputStream extends InputStream {
    private final byte[] single = new byte[1];
    private long bytesRead = 0;

    public abstract boolean canReadEntryData(ArchiveEntry archiveEntry);

    public abstract ArchiveEntry getNextEntry() throws IOException;

    @Override // java.io.InputStream
    public int read() throws IOException {
        int num = read(this.single, 0, 1);
        if (num == -1) {
            return -1;
        }
        return this.single[0] & 255;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void count(int read) {
        count(read);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void count(long read) {
        if (read != -1) {
            this.bytesRead += read;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void pushedBackBytes(long pushedBack) {
        this.bytesRead -= pushedBack;
    }

    public long getBytesRead() {
        return this.bytesRead;
    }
}
