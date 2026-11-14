package com.winlator.xserver.requests;

import com.winlator.xconnector.XInputStream;
import com.winlator.xconnector.XOutputStream;
import com.winlator.xconnector.XStreamLock;
import com.winlator.xserver.Drawable;
import com.winlator.xserver.GraphicsContext;
import com.winlator.xserver.XClient;
import com.winlator.xserver.errors.BadDrawable;
import com.winlator.xserver.errors.BadGraphicsContext;
import com.winlator.xserver.errors.BadMatch;
import com.winlator.xserver.errors.XRequestError;
import java.io.IOException;
import java.nio.ByteBuffer;

/* loaded from: classes.dex */
public abstract class DrawRequests {

    /* loaded from: classes.dex */
    private enum CoordinateMode {
        ORIGIN,
        PREVIOUS
    }

    /* loaded from: classes.dex */
    public enum Format {
        BITMAP,
        XY_PIXMAP,
        Z_PIXMAP
    }

    public static void putImage(XClient client, XInputStream inputStream, XOutputStream outputStream) throws XRequestError {
        Format format = Format.values()[client.getRequestData()];
        int drawableId = inputStream.readInt();
        int gcId = inputStream.readInt();
        short width = inputStream.readShort();
        short height = inputStream.readShort();
        short dstX = inputStream.readShort();
        short dstY = inputStream.readShort();
        byte leftPad = inputStream.readByte();
        byte depth = inputStream.readByte();
        inputStream.skip(2);
        int length = client.getRemainingRequestLength();
        ByteBuffer data = inputStream.readByteBuffer(length);
        Drawable drawable = client.xServer.drawableManager.getDrawable(drawableId);
        if (drawable == null) {
            throw new BadDrawable(drawableId);
        }
        GraphicsContext graphicsContext = client.xServer.graphicsContextManager.getGraphicsContext(gcId);
        if (graphicsContext == null) {
            throw new BadGraphicsContext(gcId);
        }
        if (graphicsContext.getFunction() != GraphicsContext.Function.COPY && format != Format.Z_PIXMAP) {
            throw new UnsupportedOperationException("GC Function other than COPY is not supported.");
        }
        switch (AnonymousClass1.$SwitchMap$com$winlator$xserver$requests$DrawRequests$Format[format.ordinal()]) {
            case 1:
                if (leftPad != 0) {
                    throw new UnsupportedOperationException("PutImage.leftPad cannot be != 0.");
                }
                if (depth == 1) {
                    drawable.drawImage((short) 0, (short) 0, dstX, dstY, width, height, (byte) 1, data, width, height);
                    return;
                }
                throw new BadMatch();
            case 2:
                if (drawable.visual.depth != depth) {
                    throw new BadMatch();
                }
                return;
            case 3:
                if (leftPad == 0) {
                    drawable.drawImage((short) 0, (short) 0, dstX, dstY, width, height, depth, data, width, height);
                    return;
                }
                throw new BadMatch();
            default:
                return;
        }
    }

    /* renamed from: com.winlator.xserver.requests.DrawRequests$1, reason: invalid class name */
    /* loaded from: classes.dex */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$winlator$xserver$requests$DrawRequests$Format;

        static {
            int[] iArr = new int[Format.values().length];
            $SwitchMap$com$winlator$xserver$requests$DrawRequests$Format = iArr;
            try {
                iArr[Format.BITMAP.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$winlator$xserver$requests$DrawRequests$Format[Format.XY_PIXMAP.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$winlator$xserver$requests$DrawRequests$Format[Format.Z_PIXMAP.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    public static void getImage(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        Format format = Format.values()[client.getRequestData()];
        int drawableId = inputStream.readInt();
        short x = inputStream.readShort();
        short y = inputStream.readShort();
        short width = inputStream.readShort();
        short height = inputStream.readShort();
        inputStream.skip(4);
        if (format != Format.Z_PIXMAP) {
            throw new UnsupportedOperationException("Only Z_PIXMAP is supported.");
        }
        Drawable drawable = client.xServer.drawableManager.getDrawable(drawableId);
        if (drawable == null) {
            throw new BadDrawable(drawableId);
        }
        int visualId = client.xServer.pixmapManager.getPixmap(drawableId) == null ? drawable.visual.id : 0;
        ByteBuffer data = drawable.getImage(x, y, width, height);
        int length = data.limit();
        XStreamLock lock = outputStream.lock();
        try {
            outputStream.writeByte((byte) 1);
            outputStream.writeByte(drawable.visual.depth);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt((length + 3) / 4);
            outputStream.writeInt(visualId);
            outputStream.writePad(20);
            outputStream.write(data);
            if (((-length) & 3) > 0) {
                outputStream.writePad((-length) & 3);
            }
            if (lock != null) {
                lock.close();
            }
        } catch (Throwable th) {
            if (lock != null) {
                try {
                    lock.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    public static void copyArea(XClient client, XInputStream inputStream, XOutputStream outputStream) throws XRequestError {
        int srcDrawableId = inputStream.readInt();
        int dstDrawableId = inputStream.readInt();
        int gcId = inputStream.readInt();
        short srcX = inputStream.readShort();
        short srcY = inputStream.readShort();
        short dstX = inputStream.readShort();
        short dstY = inputStream.readShort();
        short width = inputStream.readShort();
        short height = inputStream.readShort();
        Drawable srcDrawable = client.xServer.drawableManager.getDrawable(srcDrawableId);
        if (srcDrawable == null) {
            throw new BadDrawable(srcDrawableId);
        }
        Drawable dstDrawable = client.xServer.drawableManager.getDrawable(dstDrawableId);
        if (dstDrawable == null) {
            throw new BadDrawable(dstDrawableId);
        }
        GraphicsContext graphicsContext = client.xServer.graphicsContextManager.getGraphicsContext(gcId);
        if (graphicsContext == null) {
            throw new BadGraphicsContext(gcId);
        }
        if (srcDrawable.visual.depth != dstDrawable.visual.depth) {
            throw new BadMatch();
        }
        dstDrawable.copyArea(srcX, srcY, dstX, dstY, width, height, srcDrawable, graphicsContext.getFunction());
    }

    public static void polyLine(XClient client, XInputStream inputStream, XOutputStream outputStream) throws XRequestError {
        CoordinateMode coordinateMode = CoordinateMode.values()[client.getRequestData()];
        int drawableId = inputStream.readInt();
        int gcId = inputStream.readInt();
        Drawable drawable = client.xServer.drawableManager.getDrawable(drawableId);
        if (drawable == null) {
            throw new BadDrawable(drawableId);
        }
        GraphicsContext graphicsContext = client.xServer.graphicsContextManager.getGraphicsContext(gcId);
        if (graphicsContext == null) {
            throw new BadGraphicsContext(gcId);
        }
        int length = client.getRemainingRequestLength();
        short[] points = new short[length / 2];
        int i = 0;
        while (length != 0) {
            int i2 = i + 1;
            points[i] = inputStream.readShort();
            i = i2 + 1;
            points[i2] = inputStream.readShort();
            length -= 4;
        }
        if (coordinateMode == CoordinateMode.ORIGIN && graphicsContext.getLineWidth() > 0) {
            drawable.drawLines(graphicsContext.getForeground(), graphicsContext.getLineWidth(), points);
        }
    }

    public static void polyFillRectangle(XClient client, XInputStream inputStream, XOutputStream outputStream) throws XRequestError {
        int drawableId = inputStream.readInt();
        int gcId = inputStream.readInt();
        Drawable drawable = client.xServer.drawableManager.getDrawable(drawableId);
        if (drawable == null) {
            throw new BadDrawable(drawableId);
        }
        GraphicsContext graphicsContext = client.xServer.graphicsContextManager.getGraphicsContext(gcId);
        if (graphicsContext == null) {
            throw new BadGraphicsContext(gcId);
        }
        for (int length = client.getRemainingRequestLength(); length != 0; length -= 8) {
            short x = inputStream.readShort();
            short y = inputStream.readShort();
            short width = inputStream.readShort();
            short height = inputStream.readShort();
            drawable.fillRect(x, y, width, height, graphicsContext.getBackground());
        }
    }
}
