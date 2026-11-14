package com.winlator.core;

import android.content.res.Resources;

/* loaded from: classes.dex */
public class UnitUtils {
    public static float dpToPx(float dp) {
        return Resources.getSystem().getDisplayMetrics().density * dp;
    }

    public static float pxToDp(float px) {
        return px / Resources.getSystem().getDisplayMetrics().density;
    }
}
