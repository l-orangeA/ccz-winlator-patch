package com.google.android.material.circularreveal;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.widget.FrameLayout;
import com.google.android.material.circularreveal.CircularRevealWidget;

/* loaded from: classes.dex */
public class CircularRevealFrameLayout extends FrameLayout implements CircularRevealWidget {
    @Override // com.google.android.material.circularreveal.CircularRevealWidget
    public void buildCircularRevealCache() {
        CircularRevealHelper circularRevealHelper = null;
        circularRevealHelper.buildCircularRevealCache();
        throw null;
    }

    @Override // com.google.android.material.circularreveal.CircularRevealWidget
    public void destroyCircularRevealCache() {
        CircularRevealHelper circularRevealHelper = null;
        circularRevealHelper.destroyCircularRevealCache();
        throw null;
    }

    @Override // com.google.android.material.circularreveal.CircularRevealWidget
    public CircularRevealWidget.RevealInfo getRevealInfo() {
        CircularRevealHelper circularRevealHelper = null;
        circularRevealHelper.getRevealInfo();
        throw null;
    }

    @Override // com.google.android.material.circularreveal.CircularRevealWidget
    public void setRevealInfo(CircularRevealWidget.RevealInfo revealInfo) {
        CircularRevealHelper circularRevealHelper = null;
        circularRevealHelper.setRevealInfo(revealInfo);
        throw null;
    }

    @Override // com.google.android.material.circularreveal.CircularRevealWidget
    public int getCircularRevealScrimColor() {
        CircularRevealHelper circularRevealHelper = null;
        circularRevealHelper.getCircularRevealScrimColor();
        throw null;
    }

    @Override // com.google.android.material.circularreveal.CircularRevealWidget
    public void setCircularRevealScrimColor(int color) {
        CircularRevealHelper circularRevealHelper = null;
        circularRevealHelper.setCircularRevealScrimColor(color);
        throw null;
    }

    public Drawable getCircularRevealOverlayDrawable() {
        CircularRevealHelper circularRevealHelper = null;
        circularRevealHelper.getCircularRevealOverlayDrawable();
        throw null;
    }

    @Override // com.google.android.material.circularreveal.CircularRevealWidget
    public void setCircularRevealOverlayDrawable(Drawable drawable) {
        CircularRevealHelper circularRevealHelper = null;
        circularRevealHelper.setCircularRevealOverlayDrawable(drawable);
        throw null;
    }

    @Override // android.view.View
    @SuppressLint({"MissingSuperCall"})
    public void draw(Canvas canvas) {
        super.draw(canvas);
    }

    @Override // android.view.View
    public boolean isOpaque() {
        return super.isOpaque();
    }
}
