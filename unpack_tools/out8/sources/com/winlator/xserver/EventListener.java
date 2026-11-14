package com.winlator.xserver;

import com.winlator.xserver.events.Event;
import java.io.IOException;

/* loaded from: classes.dex */
public class EventListener {
    public final XClient client;
    public final Bitmask eventMask;

    public EventListener(XClient client, Bitmask eventMask) {
        this.client = client;
        this.eventMask = eventMask;
    }

    public boolean isInterestedIn(int eventId) {
        return this.eventMask.isSet(eventId);
    }

    public boolean isInterestedIn(Bitmask mask) {
        return this.eventMask.intersects(mask);
    }

    public void sendEvent(Event event) {
        try {
            event.send(this.client.getSequenceNumber(), this.client.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
