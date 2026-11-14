package com.winlator.xserver.events;

import com.winlator.xserver.Bitmask;
import com.winlator.xserver.Window;

/* loaded from: classes.dex */
public class MotionNotify extends InputDeviceEvent {
    public MotionNotify(boolean z, Window window, Window window2, Window window3, short s, short s2, short s3, short s4, Bitmask bitmask) {
        super(6, z ? (byte) 1 : (byte) 0, window, window2, window3, s, s2, s3, s4, bitmask);
    }
}
