package org.apache.commons.compress.archivers.tar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipEncoding;
import org.apache.commons.compress.utils.ArchiveUtils;

/* loaded from: classes.dex */
public class TarArchiveEntry implements ArchiveEntry {
    private static final TarArchiveEntry[] EMPTY_TAR_ARCHIVE_ENTRIES = new TarArchiveEntry[0];
    private boolean checkSumOK;
    private int devMajor;
    private int devMinor;
    private final Map<String, String> extraPaxHeaders;
    private final File file;
    private long groupId;
    private String groupName;
    private boolean isExtended;
    private byte linkFlag;
    private String linkName;
    private String magic;
    private long modTime;
    private int mode;
    private String name;
    private boolean paxGNU1XSparse;
    private boolean paxGNUSparse;
    private final boolean preserveAbsolutePath;
    private long realSize;
    private long size;
    private List<TarArchiveStructSparse> sparseHeaders;
    private boolean starSparse;
    private long userId;
    private String userName;
    private String version;

    private TarArchiveEntry(boolean preserveAbsolutePath) {
        this.name = "";
        this.userId = 0L;
        this.groupId = 0L;
        this.size = 0L;
        this.linkName = "";
        this.magic = "ustar\u0000";
        this.version = "00";
        this.groupName = "";
        this.devMajor = 0;
        this.devMinor = 0;
        this.paxGNU1XSparse = false;
        this.extraPaxHeaders = new HashMap();
        String user = System.getProperty("user.name", "");
        this.userName = user.length() > 31 ? user.substring(0, 31) : user;
        this.file = null;
        this.preserveAbsolutePath = preserveAbsolutePath;
    }

    public TarArchiveEntry(String name, boolean preserveAbsolutePath) {
        this(preserveAbsolutePath);
        String name2 = normalizeFileName(name, preserveAbsolutePath);
        boolean isDir = name2.endsWith("/");
        this.name = name2;
        this.mode = isDir ? 16877 : 33188;
        this.linkFlag = isDir ? (byte) 53 : (byte) 48;
        this.modTime = new Date().getTime() / 1000;
        this.userName = "";
    }

    public TarArchiveEntry(String name, byte linkFlag) {
        this(name, linkFlag, false);
    }

    public TarArchiveEntry(String name, byte linkFlag, boolean preserveAbsolutePath) {
        this(name, preserveAbsolutePath);
        this.linkFlag = linkFlag;
        if (linkFlag == 76) {
            this.magic = "ustar ";
            this.version = " \u0000";
        }
    }

    public TarArchiveEntry(File file, String fileName) {
        this.name = "";
        this.userId = 0L;
        this.groupId = 0L;
        this.size = 0L;
        this.linkName = "";
        this.magic = "ustar\u0000";
        this.version = "00";
        this.groupName = "";
        this.devMajor = 0;
        this.devMinor = 0;
        this.paxGNU1XSparse = false;
        this.extraPaxHeaders = new HashMap();
        String normalizedName = normalizeFileName(fileName, false);
        this.file = file;
        if (file.isDirectory()) {
            this.mode = 16877;
            this.linkFlag = (byte) 53;
            int nameLength = normalizedName.length();
            if (nameLength == 0 || normalizedName.charAt(nameLength - 1) != '/') {
                this.name = normalizedName + "/";
            } else {
                this.name = normalizedName;
            }
        } else {
            this.mode = 33188;
            this.linkFlag = (byte) 48;
            this.size = file.length();
            this.name = normalizedName;
        }
        this.modTime = file.lastModified() / 1000;
        this.userName = "";
        this.preserveAbsolutePath = false;
    }

    public TarArchiveEntry(byte[] headerBuf, ZipEncoding encoding, boolean lenient) throws IOException {
        this(false);
        parseTarHeader(headerBuf, encoding, false, lenient);
    }

    public boolean equals(TarArchiveEntry it) {
        return it != null && getName().equals(it.getName());
    }

    public boolean equals(Object it) {
        if (it == null || getClass() != it.getClass()) {
            return false;
        }
        return equals((TarArchiveEntry) it);
    }

    public int hashCode() {
        return getName().hashCode();
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = normalizeFileName(name, this.preserveAbsolutePath);
    }

    public String getLinkName() {
        return this.linkName;
    }

    public void setLinkName(String link) {
        this.linkName = link;
    }

    public long getLongUserId() {
        return this.userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getLongGroupId() {
        return this.groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public void setModTime(long time) {
        this.modTime = time / 1000;
    }

    public void setModTime(Date time) {
        this.modTime = time.getTime() / 1000;
    }

    public Date getModTime() {
        return new Date(this.modTime * 1000);
    }

    public int getMode() {
        return this.mode;
    }

    public long getSize() {
        return this.size;
    }

    public void setSparseHeaders(List<TarArchiveStructSparse> sparseHeaders) {
        this.sparseHeaders = sparseHeaders;
    }

    public List<TarArchiveStructSparse> getSparseHeaders() {
        return this.sparseHeaders;
    }

    public boolean isPaxGNU1XSparse() {
        return this.paxGNU1XSparse;
    }

    public void setSize(long size) {
        if (size < 0) {
            throw new IllegalArgumentException("Size is out of range: " + size);
        }
        this.size = size;
    }

    public int getDevMajor() {
        return this.devMajor;
    }

    public void setDevMajor(int devNo) {
        if (devNo < 0) {
            throw new IllegalArgumentException("Major device number is out of range: " + devNo);
        }
        this.devMajor = devNo;
    }

    public int getDevMinor() {
        return this.devMinor;
    }

    public void setDevMinor(int devNo) {
        if (devNo < 0) {
            throw new IllegalArgumentException("Minor device number is out of range: " + devNo);
        }
        this.devMinor = devNo;
    }

    public boolean isExtended() {
        return this.isExtended;
    }

    public long getRealSize() {
        if (!isSparse()) {
            return this.size;
        }
        return this.realSize;
    }

    public boolean isGNUSparse() {
        return isOldGNUSparse() || isPaxGNUSparse();
    }

    public boolean isOldGNUSparse() {
        return this.linkFlag == 83;
    }

    public boolean isPaxGNUSparse() {
        return this.paxGNUSparse;
    }

    public boolean isStarSparse() {
        return this.starSparse;
    }

    public boolean isGNULongLinkEntry() {
        return this.linkFlag == 75;
    }

    public boolean isGNULongNameEntry() {
        return this.linkFlag == 76;
    }

    public boolean isPaxHeader() {
        byte b = this.linkFlag;
        return b == 120 || b == 88;
    }

    public boolean isGlobalPaxHeader() {
        return this.linkFlag == 103;
    }

    public boolean isDirectory() {
        File file = this.file;
        if (file != null) {
            return file.isDirectory();
        }
        if (this.linkFlag == 53) {
            return true;
        }
        return (isPaxHeader() || isGlobalPaxHeader() || !getName().endsWith("/")) ? false : true;
    }

    public boolean isSymbolicLink() {
        return this.linkFlag == 50;
    }

    public boolean isLink() {
        return this.linkFlag == 49;
    }

    public boolean isSparse() {
        return isGNUSparse() || isStarSparse();
    }

    public Map<String, String> getExtraPaxHeaders() {
        return Collections.unmodifiableMap(this.extraPaxHeaders);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void updateEntryFromPaxHeaders(Map<String, String> headers) {
        for (Map.Entry<String, String> ent : headers.entrySet()) {
            String key = ent.getKey();
            String val = ent.getValue();
            processPaxHeader(key, val, headers);
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private void processPaxHeader(String key, String val, Map<String, String> headers) {
        char c;
        switch (key.hashCode()) {
            case -1916861932:
                if (key.equals("SCHILY.devmajor")) {
                    c = '\t';
                    break;
                }
                c = 65535;
                break;
            case -1916619760:
                if (key.equals("SCHILY.devminor")) {
                    c = '\b';
                    break;
                }
                c = 65535;
                break;
            case -277496563:
                if (key.equals("GNU.sparse.realsize")) {
                    c = 11;
                    break;
                }
                c = 65535;
                break;
            case -160380561:
                if (key.equals("GNU.sparse.size")) {
                    c = '\n';
                    break;
                }
                c = 65535;
                break;
            case 102338:
                if (key.equals("gid")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 115792:
                if (key.equals("uid")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 3433509:
                if (key.equals("path")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 3530753:
                if (key.equals("size")) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case 98496370:
                if (key.equals("gname")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 104223930:
                if (key.equals("mtime")) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case 111425664:
                if (key.equals("uname")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case 530706950:
                if (key.equals("SCHILY.filetype")) {
                    c = '\f';
                    break;
                }
                c = 65535;
                break;
            case 1195018015:
                if (key.equals("linkpath")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                setName(val);
                return;
            case 1:
                setLinkName(val);
                return;
            case 2:
                setGroupId(Long.parseLong(val));
                return;
            case 3:
                setGroupName(val);
                return;
            case 4:
                setUserId(Long.parseLong(val));
                return;
            case 5:
                setUserName(val);
                return;
            case 6:
                setSize(Long.parseLong(val));
                return;
            case 7:
                setModTime((long) (Double.parseDouble(val) * 1000.0d));
                return;
            case '\b':
                setDevMinor(Integer.parseInt(val));
                return;
            case '\t':
                setDevMajor(Integer.parseInt(val));
                return;
            case '\n':
                fillGNUSparse0xData(headers);
                return;
            case 11:
                fillGNUSparse1xData(headers);
                return;
            case '\f':
                if ("sparse".equals(val)) {
                    fillStarSparseData(headers);
                    return;
                }
                return;
            default:
                this.extraPaxHeaders.put(key, val);
                return;
        }
    }

    public void writeEntryHeader(byte[] outbuf, ZipEncoding encoding, boolean starMode) throws IOException {
        int offset = TarUtils.formatNameBytes(this.name, outbuf, 0, 100, encoding);
        int offset2 = this.mode;
        int offset3 = writeEntryHeaderField(this.modTime, outbuf, writeEntryHeaderField(this.size, outbuf, writeEntryHeaderField(this.groupId, outbuf, writeEntryHeaderField(this.userId, outbuf, writeEntryHeaderField(offset2, outbuf, offset, 8, starMode), 8, starMode), 8, starMode), 12, starMode), 12, starMode);
        int c = 0;
        while (c < 8) {
            outbuf[offset3] = 32;
            c++;
            offset3++;
        }
        int c2 = offset3 + 1;
        outbuf[offset3] = this.linkFlag;
        int offset4 = TarUtils.formatNameBytes(this.linkName, outbuf, c2, 100, encoding);
        int offset5 = TarUtils.formatNameBytes(this.groupName, outbuf, TarUtils.formatNameBytes(this.userName, outbuf, TarUtils.formatNameBytes(this.version, outbuf, TarUtils.formatNameBytes(this.magic, outbuf, offset4, 6), 2), 32, encoding), 32, encoding);
        int offset6 = this.devMajor;
        for (int offset7 = writeEntryHeaderField(this.devMinor, outbuf, writeEntryHeaderField(offset6, outbuf, offset5, 8, starMode), 8, starMode); offset7 < outbuf.length; offset7++) {
            outbuf[offset7] = 0;
        }
        long chk = TarUtils.computeCheckSum(outbuf);
        TarUtils.formatCheckSumOctalBytes(chk, outbuf, offset3, 8);
    }

    private int writeEntryHeaderField(long value, byte[] outbuf, int offset, int length, boolean starMode) {
        if (!starMode && (value < 0 || value >= (1 << ((length - 1) * 3)))) {
            return TarUtils.formatLongOctalBytes(0L, outbuf, offset, length);
        }
        return TarUtils.formatLongOctalOrBinaryBytes(value, outbuf, offset, length);
    }

    private void parseTarHeader(byte[] header, ZipEncoding encoding, boolean oldStyle, boolean lenient) throws IOException {
        String parseName;
        String parseName2;
        String parseName3;
        String parseName4;
        int offset;
        String xstarPrefix;
        String prefix;
        if (oldStyle) {
            parseName = TarUtils.parseName(header, 0, 100);
        } else {
            parseName = TarUtils.parseName(header, 0, 100, encoding);
        }
        this.name = parseName;
        int offset2 = 0 + 100;
        this.mode = (int) parseOctalOrBinary(header, offset2, 8, lenient);
        this.userId = (int) parseOctalOrBinary(header, r0, 8, lenient);
        this.groupId = (int) parseOctalOrBinary(header, r0, 8, lenient);
        int offset3 = offset2 + 8 + 8 + 8;
        this.size = TarUtils.parseOctalOrBinary(header, offset3, 12);
        int offset4 = offset3 + 12;
        this.modTime = parseOctalOrBinary(header, offset4, 12, lenient);
        this.checkSumOK = TarUtils.verifyCheckSum(header);
        int offset5 = offset4 + 12 + 8;
        int offset6 = offset5 + 1;
        this.linkFlag = header[offset5];
        if (oldStyle) {
            parseName2 = TarUtils.parseName(header, offset6, 100);
        } else {
            parseName2 = TarUtils.parseName(header, offset6, 100, encoding);
        }
        this.linkName = parseName2;
        int offset7 = offset6 + 100;
        this.magic = TarUtils.parseName(header, offset7, 6);
        int offset8 = offset7 + 6;
        this.version = TarUtils.parseName(header, offset8, 2);
        int offset9 = offset8 + 2;
        if (oldStyle) {
            parseName3 = TarUtils.parseName(header, offset9, 32);
        } else {
            parseName3 = TarUtils.parseName(header, offset9, 32, encoding);
        }
        this.userName = parseName3;
        int offset10 = offset9 + 32;
        if (oldStyle) {
            parseName4 = TarUtils.parseName(header, offset10, 32);
        } else {
            parseName4 = TarUtils.parseName(header, offset10, 32, encoding);
        }
        this.groupName = parseName4;
        int offset11 = offset10 + 32;
        byte b = this.linkFlag;
        if (b == 51 || b == 52) {
            this.devMajor = (int) parseOctalOrBinary(header, offset11, 8, lenient);
            int offset12 = offset11 + 8;
            this.devMinor = (int) parseOctalOrBinary(header, offset12, 8, lenient);
            offset = offset12 + 8;
        } else {
            offset = offset11 + 16;
        }
        int type = evaluateType(header);
        switch (type) {
            case 2:
                int offset13 = offset + 12 + 12 + 12 + 4 + 1;
                this.sparseHeaders = new ArrayList();
                for (int i = 0; i < 4; i++) {
                    TarArchiveStructSparse sparseHeader = TarUtils.parseSparse(header, (i * 24) + offset13);
                    if (sparseHeader.getOffset() > 0 || sparseHeader.getNumbytes() > 0) {
                        this.sparseHeaders.add(sparseHeader);
                    }
                }
                int offset14 = offset13 + 96;
                this.isExtended = TarUtils.parseBoolean(header, offset14);
                int offset15 = offset14 + 1;
                this.realSize = TarUtils.parseOctal(header, offset15, 12);
                int i2 = offset15 + 12;
                return;
            case 3:
            default:
                if (oldStyle) {
                    prefix = TarUtils.parseName(header, offset, 155);
                } else {
                    prefix = TarUtils.parseName(header, offset, 155, encoding);
                }
                if (isDirectory() && !this.name.endsWith("/")) {
                    this.name += "/";
                }
                if (prefix.length() > 0) {
                    this.name = prefix + "/" + this.name;
                    return;
                }
                return;
            case 4:
                if (oldStyle) {
                    xstarPrefix = TarUtils.parseName(header, offset, 131);
                } else {
                    xstarPrefix = TarUtils.parseName(header, offset, 131, encoding);
                }
                if (xstarPrefix.length() > 0) {
                    this.name = xstarPrefix + "/" + this.name;
                    return;
                }
                return;
        }
    }

    private long parseOctalOrBinary(byte[] header, int offset, int length, boolean lenient) {
        if (lenient) {
            try {
                return TarUtils.parseOctalOrBinary(header, offset, length);
            } catch (IllegalArgumentException e) {
                return -1L;
            }
        }
        return TarUtils.parseOctalOrBinary(header, offset, length);
    }

    private static String normalizeFileName(String fileName, boolean preserveAbsolutePath) {
        String osname;
        int colon;
        if (!preserveAbsolutePath && (osname = System.getProperty("os.name").toLowerCase(Locale.ENGLISH)) != null) {
            if (osname.startsWith("windows")) {
                if (fileName.length() > 2) {
                    char ch1 = fileName.charAt(0);
                    char ch2 = fileName.charAt(1);
                    if (ch2 == ':' && ((ch1 >= 'a' && ch1 <= 'z') || (ch1 >= 'A' && ch1 <= 'Z'))) {
                        fileName = fileName.substring(2);
                    }
                }
            } else if (osname.contains("netware") && (colon = fileName.indexOf(58)) != -1) {
                fileName = fileName.substring(colon + 1);
            }
        }
        String fileName2 = fileName.replace(File.separatorChar, '/');
        while (!preserveAbsolutePath && fileName2.startsWith("/")) {
            fileName2 = fileName2.substring(1);
        }
        return fileName2;
    }

    private int evaluateType(byte[] header) {
        if (ArchiveUtils.matchAsciiBuffer("ustar ", header, 257, 6)) {
            return 2;
        }
        if (ArchiveUtils.matchAsciiBuffer("ustar\u0000", header, 257, 6)) {
            return ArchiveUtils.matchAsciiBuffer("tar\u0000", header, 508, 4) ? 4 : 3;
        }
        return 0;
    }

    void fillGNUSparse0xData(Map<String, String> headers) {
        this.paxGNUSparse = true;
        this.realSize = Integer.parseInt(headers.get("GNU.sparse.size"));
        if (headers.containsKey("GNU.sparse.name")) {
            this.name = headers.get("GNU.sparse.name");
        }
    }

    void fillGNUSparse1xData(Map<String, String> headers) {
        this.paxGNUSparse = true;
        this.paxGNU1XSparse = true;
        this.realSize = Integer.parseInt(headers.get("GNU.sparse.realsize"));
        this.name = headers.get("GNU.sparse.name");
    }

    void fillStarSparseData(Map<String, String> headers) {
        this.starSparse = true;
        if (headers.containsKey("SCHILY.realsize")) {
            this.realSize = Long.parseLong(headers.get("SCHILY.realsize"));
        }
    }
}
