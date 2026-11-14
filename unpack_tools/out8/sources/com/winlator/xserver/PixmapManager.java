package com.winlator.xserver;

import android.graphics.Bitmap;
import android.util.SparseArray;
import com.winlator.xserver.Window;

/* loaded from: classes.dex */
public class PixmapManager extends XResourceManager {
    private final SparseArray<Pixmap> pixmaps = new SparseArray<>();
    public final PixmapFormat[] supportedPixmapFormats;
    public final Visual[] supportedVisuals;
    public final Visual visual;

    public PixmapManager() {
        Visual visual = new Visual(IDGenerator.generate(), true, 32, 24, 16711680, 65280, 255);
        this.visual = visual;
        this.supportedVisuals = new Visual[]{visual, new Visual(IDGenerator.generate(), false, 1, 1, 0, 0, 0)};
        this.supportedPixmapFormats = new PixmapFormat[]{new PixmapFormat(1, 1, 32), new PixmapFormat(24, 32, 32), new PixmapFormat(32, 32, 32)};
    }

    public Pixmap getPixmap(int id) {
        return this.pixmaps.get(id);
    }

    public Pixmap createPixmap(Drawable drawable) {
        if (this.pixmaps.indexOfKey(drawable.id) >= 0) {
            return null;
        }
        Pixmap pixmap = new Pixmap(drawable);
        this.pixmaps.put(drawable.id, pixmap);
        triggerOnCreateResourceListener(pixmap);
        return pixmap;
    }

    public void freePixmap(int id) {
        triggerOnFreeResourceListener(this.pixmaps.get(id));
        this.pixmaps.remove(id);
    }

    public Visual getVisualForDepth(byte depth) {
        Visual visual = this.visual;
        if (depth == visual.depth) {
            return visual;
        }
        for (Visual visual2 : this.supportedVisuals) {
            if (depth == visual2.depth) {
                return visual2;
            }
        }
        return null;
    }

    public Visual getVisual(int id) {
        Visual visual = this.visual;
        if (id == visual.id) {
            return visual;
        }
        for (Visual visual2 : this.supportedVisuals) {
            if (id == visual2.id && visual2.displayable) {
                return visual2;
            }
        }
        return null;
    }

    public Bitmap getWindowIcon(Window window) {
        int colorPixmapId = window.getWMHintsValue(Window.WMHints.ICON_PIXMAP);
        int maskPixmapId = window.getWMHintsValue(Window.WMHints.ICON_MASK);
        Pixmap colorPixmap = colorPixmapId != 0 ? getPixmap(colorPixmapId) : null;
        Pixmap maskPixmap = maskPixmapId != 0 ? getPixmap(maskPixmapId) : null;
        if (colorPixmap != null) {
            return colorPixmap.toBitmap(maskPixmap);
        }
        return null;
    }
}
