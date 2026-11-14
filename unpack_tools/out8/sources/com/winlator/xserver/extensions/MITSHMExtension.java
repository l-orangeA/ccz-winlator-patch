package com.winlator.xserver.extensions;

import com.winlator.xconnector.XInputStream;
import com.winlator.xconnector.XOutputStream;
import com.winlator.xconnector.XStreamLock;
import com.winlator.xserver.Drawable;
import com.winlator.xserver.GraphicsContext;
import com.winlator.xserver.XClient;
import com.winlator.xserver.XLock;
import com.winlator.xserver.XServer;
import com.winlator.xserver.errors.BadDrawable;
import com.winlator.xserver.errors.BadGraphicsContext;
import com.winlator.xserver.errors.BadImplementation;
import com.winlator.xserver.errors.BadSHMSegment;
import com.winlator.xserver.errors.XRequestError;
import java.io.IOException;
import java.nio.ByteBuffer;

/* loaded from: classes.dex */
public class MITSHMExtension implements Extension {
    @Override // com.winlator.xserver.extensions.Extension
    public String getName() {
        return "MIT-SHM";
    }

    @Override // com.winlator.xserver.extensions.Extension
    public byte getMajorOpcode() {
        return (byte) -101;
    }

    @Override // com.winlator.xserver.extensions.Extension
    public byte getFirstErrorId() {
        return Byte.MIN_VALUE;
    }

    @Override // com.winlator.xserver.extensions.Extension
    public byte getFirstEventId() {
        return (byte) 64;
    }

    private static void queryVersion(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        XStreamLock lock = outputStream.lock();
        try {
            outputStream.writeByte((byte) 1);
            outputStream.writeByte((byte) 0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeShort((short) 1);
            outputStream.writeShort((short) 1);
            outputStream.writeShort((short) 0);
            outputStream.writeShort((short) 0);
            outputStream.writeByte((byte) 0);
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

    private static void attach(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int xid = inputStream.readInt();
        int shmid = inputStream.readInt();
        inputStream.skip(4);
        client.xServer.getSHMSegmentManager().attach(xid, shmid);
    }

    private static void detach(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        client.xServer.getSHMSegmentManager().detach(inputStream.readInt());
    }

    private static void putImage(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int drawableId = inputStream.readInt();
        int gcId = inputStream.readInt();
        short totalWidth = inputStream.readShort();
        short totalHeight = inputStream.readShort();
        short srcX = inputStream.readShort();
        short srcY = inputStream.readShort();
        short srcWidth = inputStream.readShort();
        short srcHeight = inputStream.readShort();
        short dstX = inputStream.readShort();
        short dstY = inputStream.readShort();
        byte depth = inputStream.readByte();
        inputStream.skip(3);
        int shmseg = inputStream.readInt();
        inputStream.skip(4);
        Drawable drawable = client.xServer.drawableManager.getDrawable(drawableId);
        if (drawable == null) {
            throw new BadDrawable(drawableId);
        }
        GraphicsContext graphicsContext = client.xServer.graphicsContextManager.getGraphicsContext(gcId);
        if (graphicsContext == null) {
            throw new BadGraphicsContext(gcId);
        }
        ByteBuffer data = client.xServer.getSHMSegmentManager().getData(shmseg);
        if (data == null) {
            throw new BadSHMSegment(shmseg);
        }
        if (graphicsContext.getFunction() != GraphicsContext.Function.COPY) {
            throw new UnsupportedOperationException("GC Function other than COPY is not supported.");
        }
        drawable.drawImage(srcX, srcY, dstX, dstY, srcWidth, srcHeight, depth, data, totalWidth, totalHeight);
    }

    @Override // com.winlator.xserver.extensions.Extension
    public void handleRequest(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        XLock lock;
        int opcode = client.getRequestData();
        switch (opcode) {
            case 0:
                queryVersion(client, inputStream, outputStream);
                return;
            case 1:
                XLock lock2 = client.xServer.lock(XServer.Lockable.SHMSEGMENT_MANAGER);
                try {
                    attach(client, inputStream, outputStream);
                    if (lock2 != null) {
                        lock2.close();
                        return;
                    }
                    return;
                } finally {
                    if (lock2 != null) {
                        try {
                            lock2.close();
                        } catch (Throwable th) {
                            th.addSuppressed(th);
                        }
                    }
                }
            case 2:
                lock = client.xServer.lock(XServer.Lockable.SHMSEGMENT_MANAGER);
                try {
                    detach(client, inputStream, outputStream);
                    if (lock != null) {
                        lock.close();
                        return;
                    }
                    return;
                } finally {
                }
            case 3:
                lock = client.xServer.lock(XServer.Lockable.SHMSEGMENT_MANAGER, XServer.Lockable.DRAWABLE_MANAGER, XServer.Lockable.GRAPHIC_CONTEXT_MANAGER);
                try {
                    putImage(client, inputStream, outputStream);
                    if (lock != null) {
                        lock.close();
                        return;
                    }
                    return;
                } finally {
                }
            default:
                throw new BadImplementation();
        }
    }
}
