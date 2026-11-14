package com.winlator.xserver.events;

import com.winlator.xconnector.XOutputStream;
import com.winlator.xconnector.XStreamLock;
import com.winlator.xserver.Bitmask;
import com.winlator.xserver.Window;
import java.io.IOException;

/* loaded from: classes.dex */
public class InputDeviceEvent extends Event {
    private final Window child;
    private final byte detail;
    private final Window event;
    private final short eventX;
    private final short eventY;
    private final Window root;
    private final short rootX;
    private final short rootY;
    private final Bitmask state;
    private final int timestamp;

    public InputDeviceEvent(int code, byte detail, Window root, Window event, Window child, short rootX, short rootY, short eventX, short eventY, Bitmask state) {
        super(code);
        this.detail = detail;
        this.timestamp = (int) System.currentTimeMillis();
        this.root = root;
        this.event = event;
        this.child = child;
        this.rootX = rootX;
        this.rootY = rootY;
        this.eventX = eventX;
        this.eventY = eventY;
        this.state = state;
    }

    @Override // com.winlator.xserver.events.Event
    public void send(short sequenceNumber, XOutputStream outputStream) throws IOException {
        XStreamLock lock = outputStream.lock();
        try {
            outputStream.writeByte(this.code);
            outputStream.writeByte(this.detail);
            outputStream.writeShort(sequenceNumber);
            outputStream.writeInt(this.timestamp);
            outputStream.writeInt(this.root.id);
            outputStream.writeInt(this.event.id);
            Window window = this.child;
            outputStream.writeInt(window != null ? window.id : 0);
            outputStream.writeShort(this.rootX);
            outputStream.writeShort(this.rootY);
            outputStream.writeShort(this.eventX);
            outputStream.writeShort(this.eventY);
            outputStream.writeShort((short) this.state.getBits());
            outputStream.writeByte((byte) 1);
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
}
