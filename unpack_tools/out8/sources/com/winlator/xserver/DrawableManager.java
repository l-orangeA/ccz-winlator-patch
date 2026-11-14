package com.winlator.xserver;

import android.util.SparseArray;
import com.winlator.core.Callback;
import com.winlator.renderer.Texture;
import com.winlator.widget.XServerView;
import com.winlator.xserver.XResourceManager;
import java.util.Objects;

/* loaded from: classes.dex */
public class DrawableManager extends XResourceManager implements XResourceManager.OnResourceLifecycleListener {
    private final SparseArray<Drawable> drawables = new SparseArray<>();
    private final XServer xServer;

    public DrawableManager(XServer xServer) {
        this.xServer = xServer;
        xServer.pixmapManager.addOnResourceLifecycleListener(this);
    }

    public Drawable getDrawable(int id) {
        return this.drawables.get(id);
    }

    public Drawable createDrawable(int id, short width, short height, byte depth) {
        return createDrawable(id, width, height, this.xServer.pixmapManager.getVisualForDepth(depth));
    }

    public Drawable createDrawable(int id, short width, short height, Visual visual) {
        if (id == 0) {
            return new Drawable(id, width, height, visual);
        }
        if (this.drawables.indexOfKey(id) >= 0) {
            return null;
        }
        Drawable drawable = new Drawable(id, width, height, visual);
        this.drawables.put(id, drawable);
        return drawable;
    }

    public void removeDrawable(int id) {
        Drawable drawable = this.drawables.get(id);
        Texture texture = drawable.getTexture();
        if (texture != null) {
            XServerView xServerView = this.xServer.getRenderer().xServerView;
            Objects.requireNonNull(texture);
            xServerView.queueEvent(new DrawableManager$$ExternalSyntheticLambda0(texture));
        }
        Callback<Drawable> onDestroyListener = drawable.getOnDestroyListener();
        if (onDestroyListener != null) {
            onDestroyListener.call(drawable);
        }
        drawable.setOnDrawListener(null);
        this.drawables.remove(id);
    }

    @Override // com.winlator.xserver.XResourceManager.OnResourceLifecycleListener
    public void onFreeResource(XResource resource) {
        if (resource instanceof Pixmap) {
            removeDrawable(((Pixmap) resource).drawable.id);
        }
    }

    public Visual getVisual() {
        return this.xServer.pixmapManager.visual;
    }
}
