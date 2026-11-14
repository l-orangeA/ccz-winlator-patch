package com.winlator.renderer;

import com.winlator.xserver.Drawable;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class RenderableWindow {
    final Drawable content;
    final boolean forceFullscreen;
    short rootX;
    short rootY;

    public RenderableWindow(Drawable content, int rootX, int rootY) {
        this(content, rootX, rootY, false);
    }

    public RenderableWindow(Drawable content, int rootX, int rootY, boolean forceFullscreen) {
        this.content = content;
        this.rootX = (short) rootX;
        this.rootY = (short) rootY;
        this.forceFullscreen = forceFullscreen;
    }
}
