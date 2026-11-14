package com.winlator.win32;

import android.graphics.Bitmap;
import com.winlator.core.StreamUtils;
import com.winlator.core.StringUtils;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;

/* loaded from: classes.dex */
public class PEParser {
    private final File peFile;
    private int resourcesRVA = 0;
    private int resourcesOffset = 0;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public interface ImageResourceEntry {
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class ImageResourceDirectoryEntry implements ImageResourceEntry {
        private final boolean dataIsDirectory;
        private ImageResourceDirectory directory;
        private final int name;
        private final boolean nameIsString;
        private final int offsetToData;

        private ImageResourceDirectoryEntry(ByteBuffer data) {
            int field1 = data.getInt();
            int field2 = data.getInt();
            this.name = field1 & Integer.MAX_VALUE;
            this.nameIsString = ((field1 >> 31) & 1) != 0;
            this.offsetToData = Integer.MAX_VALUE & field2;
            this.dataIsDirectory = ((field2 >> 31) & 1) != 0;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class ImageResourceDataEntry implements ImageResourceEntry {
        private final int codePage;
        private final int offsetToData;
        private final int reserved;
        private final int size;

        private ImageResourceDataEntry(ByteBuffer data) {
            this.offsetToData = data.getInt();
            this.size = data.getInt();
            this.codePage = data.getInt();
            this.reserved = data.getInt();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class ImageResourceDirectory {
        private final int characteristics;
        private final ArrayList<ImageResourceEntry> entries;
        private final short majorVersion;
        private final short minorVersion;
        private final short numberOfIdEntries;
        private final short numberOfNamedEntries;
        private final int timeDateStamp;

        private ImageResourceDirectory(ByteBuffer data, int level) {
            this.entries = new ArrayList<>();
            this.characteristics = data.getInt();
            this.timeDateStamp = data.getInt();
            this.majorVersion = data.getShort();
            this.minorVersion = data.getShort();
            short s = data.getShort();
            this.numberOfNamedEntries = s;
            short s2 = data.getShort();
            this.numberOfIdEntries = s2;
            int numberOfEntries = s + s2;
            for (int i = 0; i < numberOfEntries; i++) {
                ImageResourceDirectoryEntry directoryEntry = new ImageResourceDirectoryEntry(data);
                if ((directoryEntry.name == 3 && directoryEntry.dataIsDirectory) || (level > 0 && directoryEntry.dataIsDirectory)) {
                    int oldPosition = data.position();
                    data.position(directoryEntry.offsetToData);
                    directoryEntry.directory = new ImageResourceDirectory(data, level + 1);
                    data.position(oldPosition);
                    this.entries.add(0, directoryEntry);
                } else if (level > 0) {
                    int oldPosition2 = data.position();
                    data.position(directoryEntry.offsetToData);
                    ImageResourceDataEntry dataEntry = new ImageResourceDataEntry(data);
                    data.position(oldPosition2);
                    this.entries.add(0, dataEntry);
                }
            }
        }
    }

    private PEParser(File peFile) {
        this.peFile = peFile;
    }

    private ByteBuffer readIconData(int iconOffset, int iconSize) {
        try {
            InputStream inStream = new BufferedInputStream(new FileInputStream(this.peFile), 65536);
            try {
                byte[] iconBytes = new byte[iconSize];
                StreamUtils.skip(inStream, iconOffset);
                int bytesRead = inStream.read(iconBytes);
                ByteBuffer order = bytesRead != -1 ? ByteBuffer.wrap(iconBytes).order(ByteOrder.LITTLE_ENDIAN) : null;
                inStream.close();
                return order;
            } finally {
            }
        } catch (IOException e) {
            return null;
        }
    }

    private ImageResourceDirectory readImageResourceDirectory() {
        try {
            InputStream inStream = new BufferedInputStream(new FileInputStream(this.peFile), 65536);
            try {
                ByteBuffer allocate = ByteBuffer.allocate(64);
                ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;
                ByteBuffer dosHeader = allocate.order(byteOrder);
                int filePosition = 0 + inStream.read(dosHeader.array());
                short magicNumber = dosHeader.getShort();
                if (magicNumber == 23117) {
                    dosHeader.position(60);
                    int fileHeaderOffset = dosHeader.getInt() + 4;
                    int filePosition2 = filePosition + StreamUtils.skip(inStream, fileHeaderOffset - filePosition);
                    ByteBuffer fileHeader = ByteBuffer.allocate(20).order(byteOrder);
                    int filePosition3 = filePosition2 + inStream.read(fileHeader.array());
                    Short.toUnsignedInt(fileHeader.getShort());
                    short numberOfSections = fileHeader.getShort();
                    fileHeader.position(fileHeader.position() + 12);
                    short sizeofOptionalHeader = fileHeader.getShort();
                    int filePosition4 = filePosition3 + StreamUtils.skip(inStream, sizeofOptionalHeader);
                    int i = 0;
                    this.resourcesRVA = 0;
                    this.resourcesOffset = 0;
                    int resourcesSize = 0;
                    ByteBuffer sectionHeader = ByteBuffer.allocate(40).order(byteOrder);
                    byte[] nameBytes = new byte[8];
                    byte i2 = 0;
                    while (true) {
                        if (i2 >= numberOfSections) {
                            break;
                        }
                        sectionHeader.position(i);
                        filePosition4 += inStream.read(sectionHeader.array());
                        sectionHeader.get(nameBytes);
                        String name = StringUtils.fromANSIString(nameBytes);
                        if (!name.equals(".rsrc")) {
                            i2 = (byte) (i2 + 1);
                            i = 0;
                        } else {
                            sectionHeader.getInt();
                            this.resourcesRVA = sectionHeader.getInt();
                            resourcesSize = sectionHeader.getInt();
                            this.resourcesOffset = sectionHeader.getInt();
                            break;
                        }
                    }
                    int i3 = this.resourcesOffset;
                    if (i3 > 0) {
                        int skip = filePosition4 + StreamUtils.skip(inStream, i3 - filePosition4);
                        ByteBuffer resourcesBuffer = ByteBuffer.allocate(resourcesSize).order(ByteOrder.LITTLE_ENDIAN);
                        inStream.read(resourcesBuffer.array(), 0, resourcesBuffer.limit());
                        ImageResourceDirectory imageResourceDirectory = new ImageResourceDirectory(resourcesBuffer, 0);
                        inStream.close();
                        return imageResourceDirectory;
                    }
                    inStream.close();
                    return null;
                }
                inStream.close();
                return null;
            } finally {
            }
        } catch (IOException e) {
            return null;
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:10:0x0047, code lost:
    
        if (r3 == r22) goto L20;
     */
    /* JADX WARN: Code restructure failed: missing block: B:11:0x004a, code lost:
    
        r11 = false;
     */
    /* JADX WARN: Code restructure failed: missing block: B:12:0x0056, code lost:
    
        r9 = r11;
     */
    /* JADX WARN: Code restructure failed: missing block: B:13:0x0057, code lost:
    
        if (r9 == false) goto L55;
     */
    /* JADX WARN: Code restructure failed: missing block: B:16:0x0065, code lost:
    
        return android.graphics.BitmapFactory.decodeByteArray(r7.array(), 0, r7.limit());
     */
    /* JADX WARN: Code restructure failed: missing block: B:23:0x0053, code lost:
    
        if (r23 == (r8.outWidth >= 32)) goto L20;
     */
    /* JADX WARN: Code restructure failed: missing block: B:32:0x009a, code lost:
    
        if (r3 == r22) goto L38;
     */
    /* JADX WARN: Code restructure failed: missing block: B:33:0x00a9, code lost:
    
        r10 = false;
     */
    /* JADX WARN: Code restructure failed: missing block: B:34:0x00aa, code lost:
    
        r9 = r10;
     */
    /* JADX WARN: Code restructure failed: missing block: B:35:0x00ab, code lost:
    
        if (r9 == false) goto L53;
     */
    /* JADX WARN: Code restructure failed: missing block: B:36:0x00ad, code lost:
    
        r7.position(r8);
        r10 = com.winlator.win32.MSBitmap.decodeBuffer(r12, r12, r15, r7);
     */
    /* JADX WARN: Code restructure failed: missing block: B:37:0x00b4, code lost:
    
        if (r10 == null) goto L54;
     */
    /* JADX WARN: Code restructure failed: missing block: B:39:0x00b6, code lost:
    
        return r10;
     */
    /* JADX WARN: Code restructure failed: missing block: B:41:0x00b7, code lost:
    
        continue;
     */
    /* JADX WARN: Code restructure failed: missing block: B:42:0x00a5, code lost:
    
        if (r15 < 8) goto L40;
     */
    /* JADX WARN: Code restructure failed: missing block: B:43:0x00a7, code lost:
    
        r10 = true;
     */
    /* JADX WARN: Code restructure failed: missing block: B:46:0x00a2, code lost:
    
        if (r23 == (r12 >= 32)) goto L38;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private android.graphics.Bitmap decodeIcon(int r22, boolean r23, java.util.ArrayList<com.winlator.win32.PEParser.ImageResourceDataEntry> r24) {
        /*
            r21 = this;
            r0 = r21
            r1 = r22
            r2 = r23
            r3 = 0
        L7:
            int r4 = r24.size()
            if (r3 >= r4) goto Lbb
            r4 = r24
            java.lang.Object r5 = r4.get(r3)
            com.winlator.win32.PEParser$ImageResourceDataEntry r5 = (com.winlator.win32.PEParser.ImageResourceDataEntry) r5
            int r6 = com.winlator.win32.PEParser.ImageResourceDataEntry.access$700(r5)
            int r7 = r0.resourcesRVA
            int r6 = r6 - r7
            int r7 = r0.resourcesOffset
            int r6 = r6 + r7
            int r7 = com.winlator.win32.PEParser.ImageResourceDataEntry.access$800(r5)
            java.nio.ByteBuffer r7 = r0.readIconData(r6, r7)
            if (r7 == 0) goto Lb7
            boolean r8 = com.winlator.core.ImageUtils.isPNGData(r7)
            r9 = 32
            r10 = 0
            r11 = 1
            if (r8 == 0) goto L67
            android.graphics.BitmapFactory$Options r8 = new android.graphics.BitmapFactory$Options
            r8.<init>()
            r8.inJustDecodeBounds = r11
            byte[] r12 = r7.array()
            int r13 = r7.limit()
            android.graphics.BitmapFactory.decodeByteArray(r12, r10, r13, r8)
            if (r1 < 0) goto L4c
            if (r3 != r1) goto L4a
        L49:
            goto L56
        L4a:
            r11 = r10
            goto L56
        L4c:
            int r12 = r8.outWidth
            if (r12 < r9) goto L52
            r9 = r11
            goto L53
        L52:
            r9 = r10
        L53:
            if (r2 != r9) goto L4a
            goto L49
        L56:
            r9 = r11
            if (r9 == 0) goto L66
            byte[] r11 = r7.array()
            int r12 = r7.limit()
            android.graphics.Bitmap r10 = android.graphics.BitmapFactory.decodeByteArray(r11, r10, r12)
            return r10
        L66:
            goto Lb7
        L67:
            int r8 = r7.getInt()
            int r12 = r7.getInt()
            int r13 = r7.getInt()
            short r14 = r7.getShort()
            short r15 = r7.getShort()
            int r16 = r7.getInt()
            int r17 = r7.getInt()
            int r18 = r7.getInt()
            int r19 = r7.getInt()
            int r20 = r7.getInt()
            r10 = 8
            if (r15 != r10) goto L98
            if (r16 != 0) goto L97
            if (r20 == 0) goto L98
        L97:
            goto Lb7
        L98:
            if (r1 < 0) goto L9d
            if (r3 != r1) goto La9
        L9c:
            goto La5
        L9d:
            if (r12 < r9) goto La1
            r9 = r11
            goto La2
        La1:
            r9 = 0
        La2:
            if (r2 != r9) goto La9
            goto L9c
        La5:
            if (r15 < r10) goto La9
            r10 = r11
            goto Laa
        La9:
            r10 = 0
        Laa:
            r9 = r10
            if (r9 == 0) goto Lb7
            r7.position(r8)
            android.graphics.Bitmap r10 = com.winlator.win32.MSBitmap.decodeBuffer(r12, r12, r15, r7)
            if (r10 == 0) goto Lb7
            return r10
        Lb7:
            int r3 = r3 + 1
            goto L7
        Lbb:
            r4 = r24
            r3 = 0
            return r3
        */
        throw new UnsupportedOperationException("Method not decompiled: com.winlator.win32.PEParser.decodeIcon(int, boolean, java.util.ArrayList):android.graphics.Bitmap");
    }

    private Bitmap extractIcon(int iconIndex) {
        ImageResourceDirectory rootDirectory;
        if (!this.peFile.isFile() || (rootDirectory = readImageResourceDirectory()) == null) {
            return null;
        }
        ArrayList<ImageResourceDataEntry> dataEntries = new ArrayList<>();
        Stack<ImageResourceDirectory> stack = new Stack<>();
        stack.push(rootDirectory);
        while (!stack.isEmpty()) {
            ImageResourceDirectory directory = stack.pop();
            Iterator it = directory.entries.iterator();
            while (it.hasNext()) {
                ImageResourceEntry entry = (ImageResourceEntry) it.next();
                if (entry instanceof ImageResourceDirectoryEntry) {
                    stack.push(((ImageResourceDirectoryEntry) entry).directory);
                } else if (entry instanceof ImageResourceDataEntry) {
                    dataEntries.add((ImageResourceDataEntry) entry);
                }
            }
        }
        if (iconIndex < 0) {
            Bitmap bitmap = decodeIcon(-1, true, dataEntries);
            if (bitmap != null) {
                return bitmap;
            }
            Bitmap bitmap2 = decodeIcon(-1, false, dataEntries);
            if (bitmap2 != null) {
                return bitmap2;
            }
            return null;
        }
        return decodeIcon(iconIndex, true, dataEntries);
    }

    public static Bitmap extractIcon(File peFile) {
        return extractIcon(peFile, -1);
    }

    public static Bitmap extractIcon(File peFile, int iconIndex) {
        return new PEParser(peFile).extractIcon(iconIndex);
    }
}
