package androidx.core.view;

import android.content.Context;
import android.view.PointerIcon;

/* loaded from: classes.dex */
public final class PointerIconCompat {
    private Object mPointerIcon;

    private PointerIconCompat(Object pointerIcon) {
        this.mPointerIcon = pointerIcon;
    }

    public Object getPointerIcon() {
        return this.mPointerIcon;
    }

    public static PointerIconCompat getSystemIcon(Context context, int style) {
        return new PointerIconCompat(PointerIcon.getSystemIcon(context, style));
    }
}
