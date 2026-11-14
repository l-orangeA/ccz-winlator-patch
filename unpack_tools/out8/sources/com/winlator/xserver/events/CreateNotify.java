package com.winlator.xserver.events;

import com.winlator.xconnector.XOutputStream;
import com.winlator.xconnector.XStreamLock;
import com.winlator.xserver.Window;
import java.io.IOException;

/* loaded from: classes.dex */
public class CreateNotify extends Event {
    private final Window parent;
    private final Window window;

    public CreateNotify(Window parent, Window window) {
        super(16);
        this.parent = parent;
        this.window = window;
    }

    @Override // com.winlator.xserver.events.Event
    public void send(short sequenceNumber, XOutputStream outputStream) throws IOException {
        XStreamLock lock = outputStream.lock();
        try {
            outputStream.writeByte(this.code);
            outputStream.writeByte((byte) 0);
            outputStream.writeShort(sequenceNumber);
            outputStream.writeInt(this.parent.id);
            outputStream.writeInt(this.window.id);
            outputStream.writeShort(this.window.getX());
            outputStream.writeShort(this.window.getY());
            outputStream.writeShort(this.window.getWidth());
            outputStream.writeShort(this.window.getHeight());
            outputStream.writeShort(this.window.getBorderWidth());
            outputStream.writeByte((byte) (this.window.attributes.isOverrideRedirect() ? 1 : 0));
            outputStream.writePad(9);
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
