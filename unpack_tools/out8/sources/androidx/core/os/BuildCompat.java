package androidx.core.os;

import android.os.Build;

/* loaded from: classes.dex */
public class BuildCompat {
    @Deprecated
    public static boolean isAtLeastR() {
        return Build.VERSION.SDK_INT >= 30;
    }
}
