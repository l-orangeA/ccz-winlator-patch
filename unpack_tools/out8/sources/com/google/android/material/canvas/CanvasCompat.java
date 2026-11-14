package com.google.android.material.canvas;

import android.graphics.Canvas;

/* loaded from: classes.dex */
public class CanvasCompat {
    public static int saveLayerAlpha(Canvas canvas, float left, float top, float right, float bottom, int alpha) {
        return canvas.saveLayerAlpha(left, top, right, bottom, alpha);
    }
}
