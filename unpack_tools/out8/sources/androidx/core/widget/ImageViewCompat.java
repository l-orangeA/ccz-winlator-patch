package androidx.core.widget;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.widget.ImageView;

/* loaded from: classes.dex */
public class ImageViewCompat {
    public static ColorStateList getImageTintList(ImageView view) {
        return view.getImageTintList();
    }

    public static void setImageTintList(ImageView view, ColorStateList tintList) {
        view.setImageTintList(tintList);
    }

    public static PorterDuff.Mode getImageTintMode(ImageView view) {
        return view.getImageTintMode();
    }

    public static void setImageTintMode(ImageView view, PorterDuff.Mode mode) {
        view.setImageTintMode(mode);
    }
}
