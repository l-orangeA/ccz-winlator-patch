package com.winlator.xserver;

import android.util.SparseArray;
import java.nio.IntBuffer;

/* loaded from: classes.dex */
public class CursorManager extends XResourceManager {
    private final SparseArray<Cursor> cursors = new SparseArray<>();
    private final DrawableManager drawableManager;

    public CursorManager(DrawableManager drawableManager) {
        this.drawableManager = drawableManager;
    }

    public Cursor getCursor(int id) {
        return this.cursors.get(id);
    }

    public Cursor createCursor(int id, short x, short y, Pixmap sourcePixmap, Pixmap maskPixmap) {
        if (this.cursors.indexOfKey(id) >= 0) {
            return null;
        }
        DrawableManager drawableManager = this.drawableManager;
        Drawable drawable = sourcePixmap.drawable;
        Drawable drawable2 = drawableManager.createDrawable(0, drawable.width, drawable.height, drawable.visual);
        Cursor cursor = new Cursor(id, x, y, drawable2, sourcePixmap.drawable, maskPixmap != null ? maskPixmap.drawable : null);
        this.cursors.put(id, cursor);
        triggerOnCreateResourceListener(cursor);
        return cursor;
    }

    public void freeCursor(int id) {
        triggerOnFreeResourceListener(this.cursors.get(id));
        this.cursors.remove(id);
    }

    private static boolean isEmptyMaskImage(Drawable maskImage) {
        IntBuffer maskData = maskImage.getData().asIntBuffer();
        for (int i = 0; i < maskData.capacity(); i++) {
            if (maskData.get(i) != 0) {
                return false;
            }
        }
        return true;
    }

    public void recolorCursor(Cursor cursor, byte foreRed, byte foreGreen, byte foreBlue, byte backRed, byte backGreen, byte backBlue) {
        Drawable drawable = cursor.maskImage;
        if (drawable != null) {
            boolean visible = !isEmptyMaskImage(drawable);
            cursor.setVisible(visible);
            if (visible) {
                cursor.cursorImage.drawAlphaMaskedBitmap(foreRed, foreGreen, foreBlue, backRed, backGreen, backBlue, cursor.sourceImage, cursor.maskImage);
            }
        }
    }
}
