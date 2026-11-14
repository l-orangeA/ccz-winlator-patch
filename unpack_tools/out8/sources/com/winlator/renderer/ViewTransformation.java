package com.winlator.renderer;

/* loaded from: classes.dex */
public class ViewTransformation {
    public float aspect;
    public float sceneOffsetX;
    public float sceneOffsetY;
    public float sceneScaleX;
    public float sceneScaleY;
    public int viewHeight;
    public int viewOffsetX;
    public int viewOffsetY;
    public int viewWidth;

    public void update(int outerWidth, int outerHeight, int innerWidth, int innerHeight) {
        this.aspect = Math.min(outerWidth / innerWidth, outerHeight / innerHeight);
        this.viewWidth = (int) Math.ceil(innerWidth * r0);
        this.viewHeight = (int) Math.ceil(innerHeight * this.aspect);
        float f = this.aspect;
        this.viewOffsetX = (int) ((outerWidth - (innerWidth * f)) * 0.5f);
        this.viewOffsetY = (int) ((outerHeight - (innerHeight * f)) * 0.5f);
        float f2 = (innerWidth * f) / outerWidth;
        this.sceneScaleX = f2;
        float f3 = (innerHeight * f) / outerHeight;
        this.sceneScaleY = f3;
        this.sceneOffsetX = (innerWidth - (innerWidth * f2)) * 0.5f;
        this.sceneOffsetY = (innerHeight - (innerHeight * f3)) * 0.5f;
    }
}
