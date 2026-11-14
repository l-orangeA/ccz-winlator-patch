package com.winlator.xserver;

import android.graphics.Bitmap;
import java.nio.ByteBuffer;

/* loaded from: classes.dex */
public class Pixmap extends XResource {
    public final Drawable drawable;

    private static native void toBitmap(ByteBuffer byteBuffer, ByteBuffer byteBuffer2, Bitmap bitmap);

    public Pixmap(Drawable drawable) {
        super(drawable.id);
        this.drawable = drawable;
    }

    public Bitmap toBitmap(Pixmap maskPixmap) {
        ByteBuffer maskData = maskPixmap != null ? maskPixmap.drawable.getData() : null;
        Drawable drawable = this.drawable;
        Bitmap bitmap = Bitmap.createBitmap(drawable.width, drawable.height, Bitmap.Config.ARGB_8888);
        toBitmap(this.drawable.getData(), maskData, bitmap);
        return bitmap;
    }
}
