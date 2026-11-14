package com.winlator.core;

import com.winlator.math.Mathf;
import com.winlator.xserver.XServer;
import java.util.Timer;
import java.util.TimerTask;

/* loaded from: classes.dex */
public class CursorLocker extends TimerTask {
    private short maxDistance;
    private final XServer xServer;
    private float damping = 0.25f;
    private boolean enabled = true;
    private final Object pauseLock = new Object();

    public CursorLocker(XServer xServer) {
        this.xServer = xServer;
        this.maxDistance = (short) (xServer.screenInfo.width * 0.05f);
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(this, 0L, 16L);
    }

    public void setEnabled(boolean enabled) {
        if (enabled) {
            synchronized (this.pauseLock) {
                this.enabled = true;
                this.pauseLock.notifyAll();
            }
            return;
        }
        this.enabled = enabled;
    }

    @Override // java.util.TimerTask, java.lang.Runnable
    public void run() {
        synchronized (this.pauseLock) {
            if (!this.enabled) {
                try {
                    this.pauseLock.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        short x = this.xServer.pointer.getX();
        short s = this.maxDistance;
        short x2 = (short) Mathf.clamp((int) x, -s, this.xServer.screenInfo.width + s);
        short y = this.xServer.pointer.getY();
        short s2 = this.maxDistance;
        short y2 = (short) Mathf.clamp((int) y, -s2, this.xServer.screenInfo.height + s2);
        if (x2 < 0) {
            this.xServer.pointer.setX((short) Math.ceil(x2 * this.damping));
        } else {
            XServer xServer = this.xServer;
            if (x2 >= xServer.screenInfo.width) {
                xServer.pointer.setX((short) Math.floor(r3 + ((x2 - r3) * this.damping)));
            }
        }
        if (y2 < 0) {
            this.xServer.pointer.setY((short) Math.ceil(y2 * this.damping));
            return;
        }
        XServer xServer2 = this.xServer;
        if (y2 >= xServer2.screenInfo.height) {
            xServer2.pointer.setY((short) Math.floor(r3 + ((y2 - r3) * this.damping)));
        }
    }
}
