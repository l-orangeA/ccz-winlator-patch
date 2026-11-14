package org.apache.commons.compress.archivers.tar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class TarArchiveSparseEntry {
    private final boolean isExtended;
    private List<TarArchiveStructSparse> sparseHeaders = new ArrayList();

    public TarArchiveSparseEntry(byte[] headerBuf) throws IOException {
        for (int i = 0; i < 21; i++) {
            TarArchiveStructSparse sparseHeader = TarUtils.parseSparse(headerBuf, (i * 24) + 0);
            if (sparseHeader.getOffset() > 0 || sparseHeader.getNumbytes() > 0) {
                this.sparseHeaders.add(sparseHeader);
            }
        }
        int offset = 0 + 504;
        this.isExtended = TarUtils.parseBoolean(headerBuf, offset);
    }

    public boolean isExtended() {
        return this.isExtended;
    }

    public List<TarArchiveStructSparse> getSparseHeaders() {
        return this.sparseHeaders;
    }
}
