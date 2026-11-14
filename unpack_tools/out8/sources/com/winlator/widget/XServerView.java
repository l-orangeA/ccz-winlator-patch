package com.winlator.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.widget.FrameLayout;
import com.winlator.renderer.GLRenderer;
import com.winlator.xserver.XServer;

@SuppressLint({"ViewConstructor"})
/* loaded from: classes.dex */
public class XServerView extends GLSurfaceView {
    private final GLRenderer renderer;

    public XServerView(Context context, XServer xServer) {
        super(context);
        setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        setEGLContextClientVersion(3);
        setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        setPreserveEGLContextOnPause(true);
        GLRenderer gLRenderer = new GLRenderer(this, xServer);
        this.renderer = gLRenderer;
        setRenderer(gLRenderer);
        setRenderMode(0);
    }

    public GLRenderer getRenderer() {
        return this.renderer;
    }
}
