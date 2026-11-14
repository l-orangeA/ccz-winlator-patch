package com.google.android.material.shape;

import android.graphics.RectF;

/* loaded from: classes.dex */
public class CornerTreatment {
    public void getCornerPath(ShapePath shapePath, float f, float f2, float f3) {
        throw null;
    }

    public void getCornerPath(ShapePath shapePath, float angle, float interpolation, RectF bounds, CornerSize size) {
        getCornerPath(shapePath, angle, interpolation, size.getCornerSize(bounds));
    }
}
