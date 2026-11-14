package org.apache.commons.compress.archivers.tar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipEncoding;
import org.apache.commons.compress.archivers.zip.ZipEncodingHelper;
import org.apache.commons.compress.utils.ArchiveUtils;
import org.apache.commons.compress.utils.BoundedInputStream;
import org.apache.commons.compress.utils.IOUtils;

/* loaded from: classes.dex */
public class TarArchiveInputStream extends ArchiveInputStream {
    private final int blockSize;
    private TarArchiveEntry currEntry;
    private int currentSparseInputStreamIndex;
    final String encoding;
    private long entryOffset;
    private long entrySize;
    private Map<String, String> globalPaxHeaders;
    private final List<TarArchiveStructSparse> globalSparseHeaders;
    private boolean hasHitEOF;
    private final InputStream inputStream;
    private final boolean lenient;
    private final int recordSize;
    private final byte[] smallBuf;
    private List<InputStream> sparseInputStreams;
    private final ZipEncoding zipEncoding;

    public TarArchiveInputStream(InputStream is) {
        this(is, 10240, 512);
    }

    public TarArchiveInputStream(InputStream is, int blockSize, int recordSize) {
        this(is, blockSize, recordSize, null);
    }

    public TarArchiveInputStream(InputStream is, int blockSize, int recordSize, String encoding) {
        this(is, blockSize, recordSize, encoding, false);
    }

    public TarArchiveInputStream(InputStream is, int blockSize, int recordSize, String encoding, boolean lenient) {
        this.smallBuf = new byte[256];
        this.globalPaxHeaders = new HashMap();
        this.globalSparseHeaders = new ArrayList();
        this.inputStream = is;
        this.hasHitEOF = false;
        this.encoding = encoding;
        this.zipEncoding = ZipEncodingHelper.getZipEncoding(encoding);
        this.recordSize = recordSize;
        this.blockSize = blockSize;
        this.lenient = lenient;
    }

    @Override // java.io.InputStream, java.io.Closeable, java.lang.AutoCloseable
    public void close() throws IOException {
        List<InputStream> list = this.sparseInputStreams;
        if (list != null) {
            for (InputStream inputStream : list) {
                inputStream.close();
            }
        }
        this.inputStream.close();
    }

    @Override // java.io.InputStream
    public int available() throws IOException {
        if (isDirectory()) {
            return 0;
        }
        if (this.currEntry.getRealSize() - this.entryOffset > 2147483647L) {
            return Integer.MAX_VALUE;
        }
        return (int) (this.currEntry.getRealSize() - this.entryOffset);
    }

    @Override // java.io.InputStream
    public long skip(long n) throws IOException {
        long skipped;
        if (n <= 0 || isDirectory()) {
            return 0L;
        }
        long available = this.currEntry.getRealSize() - this.entryOffset;
        if (!this.currEntry.isSparse()) {
            skipped = IOUtils.skip(this.inputStream, Math.min(n, available));
        } else {
            long skipped2 = Math.min(n, available);
            skipped = skipSparse(skipped2);
        }
        count(skipped);
        this.entryOffset += skipped;
        return skipped;
    }

    private long skipSparse(long n) throws IOException {
        List<InputStream> list = this.sparseInputStreams;
        if (list == null || list.size() == 0) {
            return this.inputStream.skip(n);
        }
        long bytesSkipped = 0;
        while (bytesSkipped < n && this.currentSparseInputStreamIndex < this.sparseInputStreams.size()) {
            InputStream currentInputStream = this.sparseInputStreams.get(this.currentSparseInputStreamIndex);
            bytesSkipped += currentInputStream.skip(n - bytesSkipped);
            if (bytesSkipped < n) {
                this.currentSparseInputStreamIndex++;
            }
        }
        return bytesSkipped;
    }

    @Override // java.io.InputStream
    public boolean markSupported() {
        return false;
    }

    @Override // java.io.InputStream
    public synchronized void mark(int markLimit) {
    }

    @Override // java.io.InputStream
    public synchronized void reset() {
    }

    public TarArchiveEntry getNextTarEntry() throws IOException {
        if (isAtEOF()) {
            return null;
        }
        if (this.currEntry != null) {
            IOUtils.skip(this, Long.MAX_VALUE);
            skipRecordPadding();
        }
        byte[] headerBuf = getRecord();
        if (headerBuf == null) {
            this.currEntry = null;
            return null;
        }
        try {
            TarArchiveEntry tarArchiveEntry = new TarArchiveEntry(headerBuf, this.zipEncoding, this.lenient);
            this.currEntry = tarArchiveEntry;
            this.entryOffset = 0L;
            this.entrySize = tarArchiveEntry.getSize();
            if (this.currEntry.isGNULongLinkEntry()) {
                byte[] longLinkData = getLongNameData();
                if (longLinkData == null) {
                    return null;
                }
                this.currEntry.setLinkName(this.zipEncoding.decode(longLinkData));
            }
            if (this.currEntry.isGNULongNameEntry()) {
                byte[] longNameData = getLongNameData();
                if (longNameData == null) {
                    return null;
                }
                this.currEntry.setName(this.zipEncoding.decode(longNameData));
            }
            if (this.currEntry.isGlobalPaxHeader()) {
                readGlobalPaxHeaders();
            }
            if (this.currEntry.isPaxHeader()) {
                paxHeaders();
            } else if (!this.globalPaxHeaders.isEmpty()) {
                applyPaxHeadersToCurrentEntry(this.globalPaxHeaders, this.globalSparseHeaders);
            }
            if (this.currEntry.isOldGNUSparse()) {
                readOldGNUSparse();
            }
            this.entrySize = this.currEntry.getSize();
            return this.currEntry;
        } catch (IllegalArgumentException e) {
            throw new IOException("Error detected parsing the header", e);
        }
    }

    private void skipRecordPadding() throws IOException {
        if (isDirectory()) {
            return;
        }
        long j = this.entrySize;
        if (j > 0) {
            int i = this.recordSize;
            if (j % i != 0) {
                long numRecords = (j / i) + 1;
                long padding = (i * numRecords) - j;
                long skipped = IOUtils.skip(this.inputStream, padding);
                count(skipped);
            }
        }
    }

    protected byte[] getLongNameData() throws IOException {
        ByteArrayOutputStream longName = new ByteArrayOutputStream();
        while (true) {
            int length = read(this.smallBuf);
            if (length < 0) {
                break;
            }
            longName.write(this.smallBuf, 0, length);
        }
        getNextEntry();
        if (this.currEntry == null) {
            return null;
        }
        byte[] longNameData = longName.toByteArray();
        int length2 = longNameData.length;
        while (length2 > 0 && longNameData[length2 - 1] == 0) {
            length2--;
        }
        if (length2 != longNameData.length) {
            byte[] l = new byte[length2];
            System.arraycopy(longNameData, 0, l, 0, length2);
            return l;
        }
        return longNameData;
    }

    private byte[] getRecord() throws IOException {
        byte[] headerBuf = readRecord();
        setAtEOF(isEOFRecord(headerBuf));
        if (isAtEOF() && headerBuf != null) {
            tryToConsumeSecondEOFRecord();
            consumeRemainderOfLastBlock();
            return null;
        }
        return headerBuf;
    }

    protected boolean isEOFRecord(byte[] record) {
        return record == null || ArchiveUtils.isArrayZero(record, this.recordSize);
    }

    protected byte[] readRecord() throws IOException {
        byte[] record = new byte[this.recordSize];
        int readNow = IOUtils.readFully(this.inputStream, record);
        count(readNow);
        if (readNow != this.recordSize) {
            return null;
        }
        return record;
    }

    private void readGlobalPaxHeaders() throws IOException {
        this.globalPaxHeaders = parsePaxHeaders(this, this.globalSparseHeaders);
        getNextEntry();
    }

    private void paxHeaders() throws IOException {
        List<TarArchiveStructSparse> sparseHeaders = new ArrayList<>();
        Map<String, String> headers = parsePaxHeaders(this, sparseHeaders);
        if (headers.containsKey("GNU.sparse.map")) {
            sparseHeaders = parsePAX01SparseHeaders(headers.get("GNU.sparse.map"));
        }
        getNextEntry();
        applyPaxHeadersToCurrentEntry(headers, sparseHeaders);
        if (this.currEntry.isPaxGNU1XSparse()) {
            this.currEntry.setSparseHeaders(parsePAX1XSparseHeaders());
        }
        buildSparseInputStreams();
    }

    private List<TarArchiveStructSparse> parsePAX01SparseHeaders(String sparseMap) throws IOException {
        List<TarArchiveStructSparse> sparseHeaders = new ArrayList<>();
        String[] sparseHeaderStrings = sparseMap.split(",");
        for (int i = 0; i < sparseHeaderStrings.length; i += 2) {
            long sparseOffset = Long.parseLong(sparseHeaderStrings[i]);
            long sparseNumbytes = Long.parseLong(sparseHeaderStrings[i + 1]);
            sparseHeaders.add(new TarArchiveStructSparse(sparseOffset, sparseNumbytes));
        }
        return sparseHeaders;
    }

    private List<TarArchiveStructSparse> parsePAX1XSparseHeaders() throws IOException {
        List<TarArchiveStructSparse> sparseHeaders = new ArrayList<>();
        long[] readResult = readLineOfNumberForPax1X(this.inputStream);
        long sparseOffset = readResult[0];
        long bytesRead = 0 + readResult[1];
        while (true) {
            long sparseHeadersCount = sparseOffset - 1;
            if (sparseOffset > 0) {
                long[] readResult2 = readLineOfNumberForPax1X(this.inputStream);
                long sparseOffset2 = readResult2[0];
                long bytesRead2 = bytesRead + readResult2[1];
                long[] readResult3 = readLineOfNumberForPax1X(this.inputStream);
                long sparseNumbytes = readResult3[0];
                bytesRead = bytesRead2 + readResult3[1];
                sparseHeaders.add(new TarArchiveStructSparse(sparseOffset2, sparseNumbytes));
                sparseOffset = sparseHeadersCount;
            } else {
                int i = this.recordSize;
                long bytesToSkip = i - (bytesRead % i);
                IOUtils.skip(this.inputStream, bytesToSkip);
                return sparseHeaders;
            }
        }
    }

    private long[] readLineOfNumberForPax1X(InputStream inputStream) throws IOException {
        long result = 0;
        long bytesRead = 0;
        while (true) {
            int number = inputStream.read();
            if (number != 10) {
                bytesRead++;
                if (number == -1) {
                    throw new IOException("Unexpected EOF when reading parse information of 1.X PAX format");
                }
                result = (10 * result) + (number - 48);
            } else {
                return new long[]{result, bytesRead + 1};
            }
        }
    }

    Map<String, String> parsePaxHeaders(InputStream inputStream, List<TarArchiveStructSparse> sparseHeaders) throws IOException {
        int ch;
        byte[] rest;
        Map<String, String> headers = new HashMap<>(this.globalPaxHeaders);
        Long offset = null;
        do {
            int len = 0;
            int read = 0;
            while (true) {
                int read2 = inputStream.read();
                ch = read2;
                int i = -1;
                if (read2 == -1) {
                    break;
                }
                read++;
                if (ch == 10) {
                    break;
                }
                if (ch == 32) {
                    ByteArrayOutputStream coll = new ByteArrayOutputStream();
                    while (true) {
                        int read3 = inputStream.read();
                        ch = read3;
                        if (read3 == i) {
                            break;
                        }
                        read++;
                        if (ch == 61) {
                            String keyword = coll.toString("UTF-8");
                            int restLen = len - read;
                            if (restLen == 1) {
                                headers.remove(keyword);
                            } else {
                                byte[] rest2 = new byte[restLen];
                                int got = IOUtils.readFully(inputStream, rest2);
                                if (got != restLen) {
                                    throw new IOException("Failed to read Paxheader. Expected " + restLen + " bytes, read " + got);
                                }
                                String value = new String(rest2, 0, restLen - 1, "UTF-8");
                                headers.put(keyword, value);
                                if (keyword.equals("GNU.sparse.offset")) {
                                    if (offset != null) {
                                        rest = rest2;
                                        sparseHeaders.add(new TarArchiveStructSparse(offset.longValue(), 0L));
                                    } else {
                                        rest = rest2;
                                    }
                                    offset = Long.valueOf(value);
                                } else {
                                    rest = rest2;
                                }
                                if (keyword.equals("GNU.sparse.numbytes")) {
                                    if (offset != null) {
                                        sparseHeaders.add(new TarArchiveStructSparse(offset.longValue(), Long.parseLong(value)));
                                        offset = null;
                                    } else {
                                        throw new IOException("Failed to read Paxheader.GNU.sparse.offset is expected before GNU.sparse.numbytes shows up.");
                                    }
                                }
                            }
                        } else {
                            coll.write((byte) ch);
                            i = -1;
                        }
                    }
                } else {
                    len = (len * 10) + (ch - 48);
                }
            }
        } while (ch != -1);
        if (offset != null) {
            sparseHeaders.add(new TarArchiveStructSparse(offset.longValue(), 0L));
        }
        return headers;
    }

    private void applyPaxHeadersToCurrentEntry(Map<String, String> headers, List<TarArchiveStructSparse> sparseHeaders) {
        this.currEntry.updateEntryFromPaxHeaders(headers);
        this.currEntry.setSparseHeaders(sparseHeaders);
    }

    /* JADX WARN: Code restructure failed: missing block: B:10:0x000e, code lost:
    
        r4.currEntry = null;
     */
    /* JADX WARN: Code restructure failed: missing block: B:11:0x002a, code lost:
    
        buildSparseInputStreams();
     */
    /* JADX WARN: Code restructure failed: missing block: B:12:0x002d, code lost:
    
        return;
     */
    /* JADX WARN: Code restructure failed: missing block: B:2:0x0006, code lost:
    
        if (r4.currEntry.isExtended() != false) goto L4;
     */
    /* JADX WARN: Code restructure failed: missing block: B:3:0x0008, code lost:
    
        r0 = getRecord();
     */
    /* JADX WARN: Code restructure failed: missing block: B:4:0x000c, code lost:
    
        if (r0 != null) goto L7;
     */
    /* JADX WARN: Code restructure failed: missing block: B:5:0x0012, code lost:
    
        r1 = new org.apache.commons.compress.archivers.tar.TarArchiveSparseEntry(r0);
        r4.currEntry.getSparseHeaders().addAll(r1.getSparseHeaders());
     */
    /* JADX WARN: Code restructure failed: missing block: B:6:0x0028, code lost:
    
        if (r1.isExtended() != false) goto L13;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private void readOldGNUSparse() throws java.io.IOException {
        /*
            r4 = this;
            org.apache.commons.compress.archivers.tar.TarArchiveEntry r0 = r4.currEntry
            boolean r0 = r0.isExtended()
            if (r0 == 0) goto L2a
        L8:
            byte[] r0 = r4.getRecord()
            if (r0 != 0) goto L12
            r1 = 0
            r4.currEntry = r1
            goto L2a
        L12:
            org.apache.commons.compress.archivers.tar.TarArchiveSparseEntry r1 = new org.apache.commons.compress.archivers.tar.TarArchiveSparseEntry
            r1.<init>(r0)
            org.apache.commons.compress.archivers.tar.TarArchiveEntry r2 = r4.currEntry
            java.util.List r2 = r2.getSparseHeaders()
            java.util.List r3 = r1.getSparseHeaders()
            r2.addAll(r3)
            boolean r0 = r1.isExtended()
            if (r0 != 0) goto L8
        L2a:
            r4.buildSparseInputStreams()
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: org.apache.commons.compress.archivers.tar.TarArchiveInputStream.readOldGNUSparse():void");
    }

    private boolean isDirectory() {
        TarArchiveEntry tarArchiveEntry = this.currEntry;
        return tarArchiveEntry != null && tarArchiveEntry.isDirectory();
    }

    @Override // org.apache.commons.compress.archivers.ArchiveInputStream
    public ArchiveEntry getNextEntry() throws IOException {
        return getNextTarEntry();
    }

    private void tryToConsumeSecondEOFRecord() throws IOException {
        boolean marked = this.inputStream.markSupported();
        if (marked) {
            this.inputStream.mark(this.recordSize);
        }
        try {
            boolean shouldReset = !isEOFRecord(readRecord());
            if (!shouldReset || !marked) {
            }
        } finally {
            if (marked) {
                pushedBackBytes(this.recordSize);
                this.inputStream.reset();
            }
        }
    }

    @Override // java.io.InputStream
    public int read(byte[] buf, int offset, int numToRead) throws IOException {
        int totalRead;
        if (numToRead == 0) {
            return 0;
        }
        if (isAtEOF() || isDirectory()) {
            return -1;
        }
        TarArchiveEntry tarArchiveEntry = this.currEntry;
        if (tarArchiveEntry == null) {
            throw new IllegalStateException("No current tar entry");
        }
        if (tarArchiveEntry.isSparse()) {
            if (this.entryOffset >= this.currEntry.getRealSize()) {
                return -1;
            }
        } else if (this.entryOffset >= this.entrySize) {
            return -1;
        }
        int numToRead2 = Math.min(numToRead, available());
        if (this.currEntry.isSparse()) {
            totalRead = readSparse(buf, offset, numToRead2);
        } else {
            totalRead = this.inputStream.read(buf, offset, numToRead2);
        }
        if (totalRead == -1) {
            if (numToRead2 > 0) {
                throw new IOException("Truncated TAR archive");
            }
            setAtEOF(true);
        } else {
            count(totalRead);
            this.entryOffset += totalRead;
        }
        return totalRead;
    }

    private int readSparse(byte[] buf, int offset, int numToRead) throws IOException {
        List<InputStream> list = this.sparseInputStreams;
        if (list == null || list.size() == 0) {
            InputStream currentInputStream = this.inputStream;
            return currentInputStream.read(buf, offset, numToRead);
        }
        if (this.currentSparseInputStreamIndex >= this.sparseInputStreams.size()) {
            return -1;
        }
        InputStream currentInputStream2 = this.sparseInputStreams.get(this.currentSparseInputStreamIndex);
        int readLen = currentInputStream2.read(buf, offset, numToRead);
        if (this.currentSparseInputStreamIndex == this.sparseInputStreams.size() - 1) {
            return readLen;
        }
        if (readLen == -1) {
            this.currentSparseInputStreamIndex++;
            return readSparse(buf, offset, numToRead);
        }
        if (readLen < numToRead) {
            this.currentSparseInputStreamIndex++;
            int readLenOfNext = readSparse(buf, offset + readLen, numToRead - readLen);
            if (readLenOfNext == -1) {
                return readLen;
            }
            return readLen + readLenOfNext;
        }
        return readLen;
    }

    @Override // org.apache.commons.compress.archivers.ArchiveInputStream
    public boolean canReadEntryData(ArchiveEntry ae) {
        if (ae instanceof TarArchiveEntry) {
            TarArchiveEntry te = (TarArchiveEntry) ae;
            return !te.isSparse();
        }
        return false;
    }

    protected final boolean isAtEOF() {
        return this.hasHitEOF;
    }

    protected final void setAtEOF(boolean b) {
        this.hasHitEOF = b;
    }

    private void consumeRemainderOfLastBlock() throws IOException {
        long bytesRead = getBytesRead();
        int i = this.blockSize;
        long bytesReadOfLastBlock = bytesRead % i;
        if (bytesReadOfLastBlock > 0) {
            long skipped = IOUtils.skip(this.inputStream, i - bytesReadOfLastBlock);
            count(skipped);
        }
    }

    private void buildSparseInputStreams() throws IOException {
        this.currentSparseInputStreamIndex = -1;
        this.sparseInputStreams = new ArrayList();
        List<TarArchiveStructSparse> sparseHeaders = this.currEntry.getSparseHeaders();
        if (sparseHeaders != null && sparseHeaders.size() > 1) {
            Comparator<TarArchiveStructSparse> sparseHeaderComparator = new Comparator<TarArchiveStructSparse>() { // from class: org.apache.commons.compress.archivers.tar.TarArchiveInputStream.1
                @Override // java.util.Comparator
                public int compare(TarArchiveStructSparse p, TarArchiveStructSparse q) {
                    Long pOffset = Long.valueOf(p.getOffset());
                    Long qOffset = Long.valueOf(q.getOffset());
                    return pOffset.compareTo(qOffset);
                }
            };
            Collections.sort(sparseHeaders, sparseHeaderComparator);
        }
        if (sparseHeaders != null) {
            InputStream zeroInputStream = new TarArchiveSparseZeroInputStream();
            long offset = 0;
            for (TarArchiveStructSparse sparseHeader : sparseHeaders) {
                if (sparseHeader.getOffset() == 0 && sparseHeader.getNumbytes() == 0) {
                    break;
                }
                if (sparseHeader.getOffset() - offset < 0) {
                    throw new IOException("Corrupted struct sparse detected");
                }
                if (sparseHeader.getOffset() - offset > 0) {
                    this.sparseInputStreams.add(new BoundedInputStream(zeroInputStream, sparseHeader.getOffset() - offset));
                }
                if (sparseHeader.getNumbytes() > 0) {
                    this.sparseInputStreams.add(new BoundedInputStream(this.inputStream, sparseHeader.getNumbytes()));
                }
                offset = sparseHeader.getOffset() + sparseHeader.getNumbytes();
            }
        }
        if (this.sparseInputStreams.size() > 0) {
            this.currentSparseInputStreamIndex = 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class TarArchiveSparseZeroInputStream extends InputStream {
        private TarArchiveSparseZeroInputStream() {
        }

        @Override // java.io.InputStream
        public int read() throws IOException {
            return 0;
        }

        @Override // java.io.InputStream
        public long skip(long n) {
            return n;
        }
    }
}
