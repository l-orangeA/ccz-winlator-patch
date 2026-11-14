package com.winlator.xserver.requests;

import com.winlator.xconnector.XInputStream;
import com.winlator.xconnector.XOutputStream;
import com.winlator.xconnector.XStreamLock;
import com.winlator.xserver.XClient;
import com.winlator.xserver.errors.XRequestError;
import java.io.IOException;

/* loaded from: classes.dex */
public abstract class CursorRequests {
    /* JADX WARN: Code restructure failed: missing block: B:12:0x003d, code lost:
    
        if (r1.height == r3.height) goto L17;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static void createCursor(com.winlator.xserver.XClient r24, com.winlator.xconnector.XInputStream r25, com.winlator.xconnector.XOutputStream r26) throws com.winlator.xserver.errors.XRequestError {
        /*
            r0 = r24
            int r7 = r25.readInt()
            int r8 = r25.readInt()
            int r9 = r25.readInt()
            boolean r1 = r0.isValidResourceId(r7)
            if (r1 == 0) goto La8
            com.winlator.xserver.XServer r1 = r0.xServer
            com.winlator.xserver.PixmapManager r1 = r1.pixmapManager
            com.winlator.xserver.Pixmap r10 = r1.getPixmap(r8)
            if (r10 == 0) goto La2
            com.winlator.xserver.XServer r1 = r0.xServer
            com.winlator.xserver.PixmapManager r1 = r1.pixmapManager
            com.winlator.xserver.Pixmap r11 = r1.getPixmap(r9)
            if (r11 == 0) goto L46
            com.winlator.xserver.Drawable r1 = r11.drawable
            com.winlator.xserver.Visual r2 = r1.visual
            byte r2 = r2.depth
            r3 = 1
            if (r2 != r3) goto L40
            short r2 = r1.width
            com.winlator.xserver.Drawable r3 = r10.drawable
            short r4 = r3.width
            if (r2 != r4) goto L40
            short r1 = r1.height
            short r2 = r3.height
            if (r1 != r2) goto L40
            goto L46
        L40:
            com.winlator.xserver.errors.BadMatch r1 = new com.winlator.xserver.errors.BadMatch
            r1.<init>()
            throw r1
        L46:
            short r1 = r25.readShort()
            byte r15 = (byte) r1
            short r1 = r25.readShort()
            byte r14 = (byte) r1
            short r1 = r25.readShort()
            byte r13 = (byte) r1
            short r1 = r25.readShort()
            byte r12 = (byte) r1
            short r1 = r25.readShort()
            byte r6 = (byte) r1
            short r1 = r25.readShort()
            byte r5 = (byte) r1
            short r20 = r25.readShort()
            short r21 = r25.readShort()
            com.winlator.xserver.XServer r1 = r0.xServer
            com.winlator.xserver.CursorManager r1 = r1.cursorManager
            r2 = r7
            r3 = r20
            r4 = r21
            r22 = r5
            r5 = r10
            r23 = r6
            r6 = r11
            com.winlator.xserver.Cursor r1 = r1.createCursor(r2, r3, r4, r5, r6)
            if (r1 == 0) goto L9c
            com.winlator.xserver.XServer r2 = r0.xServer
            com.winlator.xserver.CursorManager r2 = r2.cursorManager
            r3 = r12
            r12 = r2
            r2 = r13
            r13 = r1
            r4 = r14
            r14 = r15
            r5 = r15
            r15 = r4
            r16 = r2
            r17 = r3
            r18 = r23
            r19 = r22
            r12.recolorCursor(r13, r14, r15, r16, r17, r18, r19)
            r0.registerAsOwnerOfResource(r1)
            return
        L9c:
            com.winlator.xserver.errors.BadIdChoice r6 = new com.winlator.xserver.errors.BadIdChoice
            r6.<init>(r7)
            throw r6
        La2:
            com.winlator.xserver.errors.BadPixmap r1 = new com.winlator.xserver.errors.BadPixmap
            r1.<init>(r8)
            throw r1
        La8:
            com.winlator.xserver.errors.BadIdChoice r1 = new com.winlator.xserver.errors.BadIdChoice
            r1.<init>(r7)
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.winlator.xserver.requests.CursorRequests.createCursor(com.winlator.xserver.XClient, com.winlator.xconnector.XInputStream, com.winlator.xconnector.XOutputStream):void");
    }

    public static void freeCursor(XClient client, XInputStream inputStream, XOutputStream outputStream) throws XRequestError {
        client.xServer.cursorManager.freeCursor(inputStream.readInt());
    }

    public static void getPointerMapping(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        XStreamLock lock = outputStream.lock();
        try {
            byte[] buttonsMap = {1, 2, 3};
            byte length = (byte) buttonsMap.length;
            outputStream.writeByte((byte) 1);
            outputStream.writeByte(length);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt((length + 3) / 4);
            outputStream.writePad(24);
            outputStream.write(buttonsMap);
            outputStream.writePad(3 & (-length));
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
}
