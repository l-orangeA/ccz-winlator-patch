package org.apache.commons.compress.archivers;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/* loaded from: classes.dex */
public abstract class ArchiveOutputStream extends OutputStream {
    private final byte[] oneByte = new byte[1];
    private long bytesWritten = 0;

    public abstract void closeArchiveEntry() throws IOException;

    public abstract ArchiveEntry createArchiveEntry(File file, String str) throws IOException;

    public abstract void putArchiveEntry(ArchiveEntry archiveEntry) throws IOException;

    @Override // java.io.OutputStream
    public void write(int b) throws IOException {
        byte[] bArr = this.oneByte;
        bArr[0] = (byte) (b & 255);
        write(bArr, 0, 1);
    }
}
