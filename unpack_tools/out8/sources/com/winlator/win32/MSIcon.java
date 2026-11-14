package com.winlator.win32;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.winlator.core.FileUtils;
import com.winlator.core.ImageUtils;
import com.winlator.win32.MSIcon;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/* loaded from: classes.dex */
public class MSIcon {

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class IconDirEntry {
        private short bitCount;
        private short colorPlanes;
        private int height;
        private int imageOffset;
        private int imageSize;
        private byte numberOfColors;
        private byte reserved;
        private int width;

        private IconDirEntry() {
        }
    }

    public static Bitmap decodeByteArray(byte[] bytes, int offset, int length) {
        ByteBuffer data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        data.position(offset);
        if (ImageUtils.isPNGData(data)) {
            return BitmapFactory.decodeByteArray(bytes, offset, length);
        }
        int bitmapOffset = data.getInt();
        int bmpWidth = data.getInt();
        data.getInt();
        data.getShort();
        short bitCount = data.getShort();
        data.position(offset + bitmapOffset);
        return MSBitmap.decodeBuffer(bmpWidth, bmpWidth, bitCount, data);
    }

    public static Bitmap decodeFile(File icoFile) {
        byte[] bytes;
        if (!icoFile.isFile() || (bytes = FileUtils.read(icoFile)) == null) {
            return null;
        }
        ByteBuffer data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        short reserved = data.getShort();
        short imageType = data.getShort();
        short numberOfImages = data.getShort();
        if (reserved != 0 || imageType != 1 || numberOfImages <= 0 || numberOfImages >= 32) {
            return null;
        }
        ArrayList<IconDirEntry> entries = new ArrayList<>();
        for (byte i = 0; i < numberOfImages; i = (byte) (i + 1)) {
            IconDirEntry entry = new IconDirEntry();
            entry.width = Byte.toUnsignedInt(data.get());
            entry.height = Byte.toUnsignedInt(data.get());
            entry.numberOfColors = data.get();
            entry.reserved = data.get();
            entry.colorPlanes = data.getShort();
            entry.bitCount = data.getShort();
            entry.imageSize = data.getInt();
            entry.imageOffset = data.getInt();
            entries.add(entry);
        }
        Collections.sort(entries, new Comparator() { // from class: com.winlator.win32.MSIcon$$ExternalSyntheticLambda0
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                int lambda$decodeFile$0;
                lambda$decodeFile$0 = MSIcon.lambda$decodeFile$0((MSIcon.IconDirEntry) obj, (MSIcon.IconDirEntry) obj2);
                return lambda$decodeFile$0;
            }
        });
        IconDirEntry firstEntry = entries.get(0);
        return decodeByteArray(data.array(), firstEntry.imageOffset, firstEntry.imageSize);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ int lambda$decodeFile$0(IconDirEntry a, IconDirEntry b) {
        int value = Short.compare(b.bitCount, a.bitCount);
        return value != 0 ? value : Integer.compare(b.width, a.width);
    }
}
