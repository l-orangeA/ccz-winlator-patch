package com.winlator.xserver.events;

import com.winlator.xserver.Bitmask;
import com.winlator.xserver.Window;
import com.winlator.xserver.events.PointerWindowEvent;

/* loaded from: classes.dex */
public class EnterNotify extends PointerWindowEvent {
    public EnterNotify(PointerWindowEvent.Detail detail, Window root, Window event, Window child, short rootX, short rootY, short eventX, short eventY, Bitmask state, PointerWindowEvent.Mode mode, boolean sameScreenAndFocus) {
        super(7, detail, root, event, child, rootX, rootY, eventX, eventY, state, mode, sameScreenAndFocus);
    }
}
