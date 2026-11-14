package com.winlator.xserver.events;

import com.winlator.xconnector.XOutputStream;
import com.winlator.xconnector.XStreamLock;
import com.winlator.xserver.Window;
import java.io.IOException;

/* loaded from: classes.dex */
public class ConfigureNotify extends Event {
    private final Window aboveSibling;
    private final short borderWidth;
    private final Window event;
    private final short height;
    private final boolean overrideRedirect;
    private final short width;
    private final Window window;
    private final short x;
    private final short y;

    public ConfigureNotify(Window event, Window window, Window aboveSibling, int x, int y, int width, int height, int borderWidth, boolean overrideRedirect) {
        super(22);
        this.event = event;
        this.window = window;
        this.aboveSibling = aboveSibling;
        this.x = (short) x;
        this.y = (short) y;
        this.width = (short) width;
        this.height = (short) height;
        this.borderWidth = (short) borderWidth;
        this.overrideRedirect = overrideRedirect;
    }

    @Override // com.winlator.xserver.events.Event
    public void send(short sequenceNumber, XOutputStream outputStream) throws IOException {
        XStreamLock lock = outputStream.lock();
        try {
            outputStream.writeByte(this.code);
            outputStream.writeByte((byte) 0);
            outputStream.writeShort(sequenceNumber);
            outputStream.writeInt(this.event.id);
            outputStream.writeInt(this.window.id);
            Window window = this.aboveSibling;
            outputStream.writeInt(window != null ? window.id : 0);
            outputStream.writeShort(this.x);
            outputStream.writeShort(this.y);
            outputStream.writeShort(this.width);
            outputStream.writeShort(this.height);
            outputStream.writeShort(this.borderWidth);
            outputStream.writeByte((byte) (this.overrideRedirect ? 1 : 0));
            outputStream.writePad(5);
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
