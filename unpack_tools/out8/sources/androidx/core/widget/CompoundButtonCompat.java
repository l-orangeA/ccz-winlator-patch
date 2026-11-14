package androidx.core.widget;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.widget.CompoundButton;

/* loaded from: classes.dex */
public final class CompoundButtonCompat {
    public static void setButtonTintList(CompoundButton button, ColorStateList tint) {
        button.setButtonTintList(tint);
    }

    public static ColorStateList getButtonTintList(CompoundButton button) {
        return button.getButtonTintList();
    }

    public static void setButtonTintMode(CompoundButton button, PorterDuff.Mode tintMode) {
        button.setButtonTintMode(tintMode);
    }

    public static Drawable getButtonDrawable(CompoundButton button) {
        return button.getButtonDrawable();
    }
}
