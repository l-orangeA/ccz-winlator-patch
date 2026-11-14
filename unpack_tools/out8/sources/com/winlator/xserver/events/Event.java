package com.winlator.xserver.events;

import com.winlator.xconnector.XOutputStream;
import java.io.IOException;

/* loaded from: classes.dex */
public abstract class Event {
    protected final byte code;

    public abstract void send(short s, XOutputStream xOutputStream) throws IOException;

    public Event(int code) {
        this.code = (byte) code;
    }
}
