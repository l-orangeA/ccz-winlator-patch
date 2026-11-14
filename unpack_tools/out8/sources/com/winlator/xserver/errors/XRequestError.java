package com.winlator.xserver.errors;

import com.winlator.xconnector.XOutputStream;
import com.winlator.xconnector.XStreamLock;
import com.winlator.xserver.XClient;
import java.io.IOException;

/* loaded from: classes.dex */
public class XRequestError extends Exception {
    private final byte code;
    private final int data;

    public XRequestError(int code, int data) {
        this.code = (byte) code;
        this.data = data;
    }

    public void sendError(XClient client, byte opcode) throws IOException {
        XOutputStream outputStream = client.getOutputStream();
        XStreamLock lock = outputStream.lock();
        try {
            outputStream.writeByte((byte) 0);
            outputStream.writeByte(this.code);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(this.data);
            outputStream.writeShort(client.getRequestData());
            outputStream.writeByte(opcode);
            outputStream.writePad(21);
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
