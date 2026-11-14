package com.winlator.renderer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import androidx.core.graphics.ColorUtils;
import com.winlatcn.R;
import com.winlator.math.Mathf;
import com.winlator.math.XForm;
import com.winlator.renderer.material.CursorMaterial;
import com.winlator.renderer.material.WindowMaterial;
import com.winlator.widget.XServerView;
import com.winlator.xserver.Bitmask;
import com.winlator.xserver.Cursor;
import com.winlator.xserver.Drawable;
import com.winlator.xserver.Pointer;
import com.winlator.xserver.ScreenInfo;
import com.winlator.xserver.Window;
import com.winlator.xserver.WindowManager;
import com.winlator.xserver.XLock;
import com.winlator.xserver.XServer;
import java.util.ArrayList;
import java.util.Iterator;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/* loaded from: classes.dex */
public class GLRenderer implements GLSurfaceView.Renderer, WindowManager.OnWindowModificationListener, Pointer.OnPointerMotionListener {
    private int cursorBackColor;
    private int cursorForeColor;
    private final CursorMaterial cursorMaterial;
    private float cursorScale;
    private boolean cursorVisible;
    public final EffectComposer effectComposer;
    private String forceFullscreenWMClass;
    private boolean fullscreen;
    private float magnifierZoom;
    protected final VertexAttribute quadVertices;
    private final ArrayList<RenderableWindow> renderableWindows;
    private final Drawable rootCursorDrawable;
    private boolean screenOffsetYRelativeToCursor;
    protected short surfaceHeight;
    protected short surfaceWidth;
    private final float[] tmpXForm1;
    private final float[] tmpXForm2;
    private boolean toggleFullscreen;
    private String[] unviewableWMClasses;
    public final ViewTransformation viewTransformation;
    protected boolean viewportNeedsUpdate;
    private final WindowMaterial windowMaterial;
    private final XServer xServer;
    public final XServerView xServerView;

    public GLRenderer(XServerView xServerView, XServer xServer) {
        VertexAttribute vertexAttribute = new VertexAttribute("position", 2);
        this.quadVertices = vertexAttribute;
        this.tmpXForm1 = XForm.getInstance();
        this.tmpXForm2 = XForm.getInstance();
        this.cursorMaterial = new CursorMaterial();
        this.windowMaterial = new WindowMaterial();
        this.viewTransformation = new ViewTransformation();
        this.renderableWindows = new ArrayList<>();
        this.forceFullscreenWMClass = null;
        this.fullscreen = false;
        this.toggleFullscreen = false;
        this.viewportNeedsUpdate = true;
        this.cursorVisible = true;
        this.cursorScale = 1.0f;
        this.cursorBackColor = 16777215;
        this.cursorForeColor = 0;
        this.screenOffsetYRelativeToCursor = false;
        this.unviewableWMClasses = null;
        this.magnifierZoom = 1.0f;
        this.effectComposer = new EffectComposer(this);
        this.xServerView = xServerView;
        this.xServer = xServer;
        this.rootCursorDrawable = createRootCursorDrawable();
        vertexAttribute.put(new float[]{0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f});
        xServer.windowManager.addOnWindowModificationListener(this);
        xServer.pointer.addOnPointerMotionListener(this);
    }

    @Override // android.opengl.GLSurfaceView.Renderer
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GPUImage.checkIsSupported();
        GLES20.glFrontFace(2305);
        GLES20.glDisable(2884);
        GLES20.glDisable(2929);
        GLES20.glDepthMask(false);
        GLES20.glEnable(3042);
        GLES20.glBlendFunc(770, 771);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    }

    @Override // android.opengl.GLSurfaceView.Renderer
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        this.surfaceWidth = (short) width;
        this.surfaceHeight = (short) height;
        ViewTransformation viewTransformation = this.viewTransformation;
        ScreenInfo screenInfo = this.xServer.screenInfo;
        viewTransformation.update(width, height, screenInfo.width, screenInfo.height);
    }

    @Override // android.opengl.GLSurfaceView.Renderer
    public void onDrawFrame(GL10 gl) {
        if (this.toggleFullscreen) {
            this.fullscreen = !this.fullscreen;
            this.toggleFullscreen = false;
            this.viewportNeedsUpdate = true;
        }
        if (this.effectComposer.hasEffects()) {
            this.effectComposer.render();
        } else {
            drawFrame();
        }
    }

    public void drawFrame() {
        if (this.viewportNeedsUpdate) {
            if (this.fullscreen) {
                GLES20.glViewport(0, 0, this.surfaceWidth, this.surfaceHeight);
            } else {
                ViewTransformation viewTransformation = this.viewTransformation;
                GLES20.glViewport(viewTransformation.viewOffsetX, viewTransformation.viewOffsetY, viewTransformation.viewWidth, viewTransformation.viewHeight);
            }
            this.viewportNeedsUpdate = false;
        }
        GLES20.glClear(16384);
        float pointerX = 0.0f;
        float pointerY = 0.0f;
        float magnifierZoom = !this.screenOffsetYRelativeToCursor ? this.magnifierZoom : 1.0f;
        if (magnifierZoom != 1.0f) {
            short s = this.xServer.screenInfo.width;
            pointerX = Mathf.clamp((this.xServer.pointer.getX() * magnifierZoom) - (s * 0.5f), 0.0f, s * Math.abs(1.0f - magnifierZoom));
        }
        if (this.screenOffsetYRelativeToCursor || magnifierZoom != 1.0f) {
            float scaleY = magnifierZoom != 1.0f ? Math.abs(1.0f - magnifierZoom) : 0.5f;
            XServer xServer = this.xServer;
            float offsetY = xServer.screenInfo.height * (this.screenOffsetYRelativeToCursor ? 0.25f : 0.5f);
            pointerY = Mathf.clamp((xServer.pointer.getY() * magnifierZoom) - offsetY, 0.0f, this.xServer.screenInfo.height * scaleY);
        }
        XForm.makeTransform(this.tmpXForm2, -pointerX, -pointerY, magnifierZoom, magnifierZoom, 0.0f);
        renderWindows();
        if (this.cursorVisible) {
            renderCursor();
        }
    }

    @Override // com.winlator.xserver.WindowManager.OnWindowModificationListener
    public void onMapWindow(Window window) {
        this.xServerView.queueEvent(new GLRenderer$$ExternalSyntheticLambda0(this));
        this.xServerView.requestRender();
    }

    @Override // com.winlator.xserver.WindowManager.OnWindowModificationListener
    public void onUnmapWindow(Window window) {
        this.xServerView.queueEvent(new GLRenderer$$ExternalSyntheticLambda0(this));
        this.xServerView.requestRender();
    }

    @Override // com.winlator.xserver.WindowManager.OnWindowModificationListener
    public void onChangeWindowZOrder(Window window) {
        this.xServerView.queueEvent(new GLRenderer$$ExternalSyntheticLambda0(this));
        this.xServerView.requestRender();
    }

    @Override // com.winlator.xserver.WindowManager.OnWindowModificationListener
    public void onUpdateWindowContent(Window window) {
        this.xServerView.requestRender();
    }

    @Override // com.winlator.xserver.WindowManager.OnWindowModificationListener
    public void onUpdateWindowGeometry(final Window window, boolean resized) {
        if (resized) {
            this.xServerView.queueEvent(new GLRenderer$$ExternalSyntheticLambda0(this));
        } else {
            this.xServerView.queueEvent(new Runnable() { // from class: com.winlator.renderer.GLRenderer$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    GLRenderer.this.lambda$onUpdateWindowGeometry$0(window);
                }
            });
        }
        this.xServerView.requestRender();
    }

    @Override // com.winlator.xserver.WindowManager.OnWindowModificationListener
    public void onUpdateWindowAttributes(Window window, Bitmask mask) {
        if (mask.isSet(16384)) {
            this.xServerView.requestRender();
        }
    }

    @Override // com.winlator.xserver.Pointer.OnPointerMotionListener
    public void onPointerMove(short x, short y) {
        this.xServerView.requestRender();
    }

    private void renderCursorDrawable(Drawable drawable, int x, int y) {
        synchronized (drawable.renderLock) {
            Texture texture = drawable.getTexture();
            texture.updateFromDrawable(drawable);
            float f = drawable.width;
            float f2 = this.cursorScale;
            XForm.set(this.tmpXForm1, x, y, f * f2, drawable.height * f2);
            float[] fArr = this.tmpXForm1;
            XForm.multiply(fArr, fArr, this.tmpXForm2);
            this.cursorMaterial.setUniformColor("backColor", this.cursorBackColor);
            this.cursorMaterial.setUniformColor("foreColor", this.cursorForeColor);
            GLES20.glActiveTexture(33984);
            GLES20.glBindTexture(3553, texture.getTextureId());
            this.cursorMaterial.setUniformInt("texture", 0);
            this.cursorMaterial.setUniformFloatArray("xform", this.tmpXForm1);
            GLES20.glDrawArrays(5, 0, this.quadVertices.count());
            GLES20.glBindTexture(3553, 0);
        }
    }

    private void renderWindowDrawable(Drawable drawable, int x, int y, boolean forceFullscreen) {
        synchronized (drawable.renderLock) {
            Texture texture = drawable.getTexture();
            texture.updateFromDrawable(drawable);
            if (forceFullscreen) {
                ScreenInfo screenInfo = this.xServer.screenInfo;
                short newHeight = (short) Math.min(screenInfo.height, (screenInfo.width / drawable.width) * drawable.height);
                short newWidth = (short) ((newHeight / drawable.height) * drawable.width);
                float[] fArr = this.tmpXForm1;
                ScreenInfo screenInfo2 = this.xServer.screenInfo;
                XForm.set(fArr, (screenInfo2.width - newWidth) * 0.5f, (screenInfo2.height - newHeight) * 0.5f, newWidth, newHeight);
            } else {
                XForm.set(this.tmpXForm1, x, y, drawable.width, drawable.height);
            }
            float[] fArr2 = this.tmpXForm1;
            XForm.multiply(fArr2, fArr2, this.tmpXForm2);
            GLES20.glActiveTexture(33984);
            GLES20.glBindTexture(3553, texture.getTextureId());
            this.windowMaterial.setUniformInt("texture", 0);
            this.windowMaterial.setUniformFloatArray("xform", this.tmpXForm1);
            GLES20.glDrawArrays(5, 0, this.quadVertices.count());
            GLES20.glBindTexture(3553, 0);
        }
    }

    private void renderWindows() {
        this.windowMaterial.use();
        WindowMaterial windowMaterial = this.windowMaterial;
        ScreenInfo screenInfo = this.xServer.screenInfo;
        windowMaterial.setUniformVec2("viewSize", screenInfo.width, screenInfo.height);
        this.quadVertices.bind(this.windowMaterial.programId);
        XLock lock = this.xServer.lock(XServer.Lockable.DRAWABLE_MANAGER);
        try {
            Iterator<RenderableWindow> it = this.renderableWindows.iterator();
            while (it.hasNext()) {
                RenderableWindow window = it.next();
                if (!window.content.isBlank()) {
                    renderWindowDrawable(window.content, window.rootX, window.rootY, window.forceFullscreen);
                }
            }
            if (lock != null) {
                lock.close();
            }
            this.quadVertices.disable();
        } catch (Throwable th) {
            if (lock != null) {
                try {
                    lock.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private void renderCursor() {
        this.cursorMaterial.use();
        CursorMaterial cursorMaterial = this.cursorMaterial;
        ScreenInfo screenInfo = this.xServer.screenInfo;
        cursorMaterial.setUniformVec2("viewSize", screenInfo.width, screenInfo.height);
        this.quadVertices.bind(this.cursorMaterial.programId);
        XLock lock = this.xServer.lock(XServer.Lockable.DRAWABLE_MANAGER);
        try {
            Window pointWindow = this.xServer.inputDeviceManager.getPointWindow();
            Cursor cursor = pointWindow != null ? pointWindow.attributes.getCursor() : null;
            short x = this.xServer.pointer.getClampedX();
            short y = this.xServer.pointer.getClampedY();
            if (cursor != null) {
                if (cursor.isVisible()) {
                    renderCursorDrawable(cursor.cursorImage, x - cursor.hotSpotX, y - cursor.hotSpotY);
                }
            } else {
                renderCursorDrawable(this.rootCursorDrawable, x, y);
            }
            if (lock != null) {
                lock.close();
            }
            this.quadVertices.disable();
        } catch (Throwable th) {
            if (lock != null) {
                try {
                    lock.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    public void toggleFullscreen() {
        this.toggleFullscreen = true;
        this.xServerView.requestRender();
    }

    private Drawable createRootCursorDrawable() {
        Context context = this.xServerView.getContext();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.cursor, options);
        return Drawable.fromBitmap(bitmap);
    }

    public void updateScene() {
        XLock lock = this.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.DRAWABLE_MANAGER);
        try {
            this.renderableWindows.clear();
            Window window = this.xServer.windowManager.rootWindow;
            collectRenderableWindows(window, window.getX(), this.xServer.windowManager.rootWindow.getY());
            if (lock != null) {
                lock.close();
            }
        } catch (Throwable th) {
            if (lock != null) {
                try {
                    lock.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private void collectRenderableWindows(Window window, int x, int y) {
        if (window.isRenderable()) {
            if (window != this.xServer.windowManager.rootWindow) {
                boolean viewable = true;
                boolean z = false;
                if (this.unviewableWMClasses != null) {
                    String wmClass = window.getClassName();
                    String[] strArr = this.unviewableWMClasses;
                    int length = strArr.length;
                    int i = 0;
                    while (true) {
                        if (i >= length) {
                            break;
                        }
                        String unviewableWMClass = strArr[i];
                        if (!wmClass.contains(unviewableWMClass)) {
                            i++;
                        } else {
                            if (window.attributes.isEnabled()) {
                                window.disableAllDescendants();
                            }
                            viewable = false;
                        }
                    }
                }
                if (viewable) {
                    if (this.forceFullscreenWMClass != null) {
                        short width = window.getWidth();
                        short height = window.getHeight();
                        boolean forceFullscreen = false;
                        if (width >= 320 && height >= 200) {
                            ScreenInfo screenInfo = this.xServer.screenInfo;
                            if (width < screenInfo.width && height < screenInfo.height) {
                                Window parent = window.getParent();
                                boolean parentHasWMClass = parent.getClassName().contains(this.forceFullscreenWMClass);
                                boolean hasWMClass = window.getClassName().contains(this.forceFullscreenWMClass);
                                if (hasWMClass) {
                                    if (!parentHasWMClass && window.getChildCount() == 0) {
                                        z = true;
                                    }
                                    forceFullscreen = z;
                                } else {
                                    short borderX = (short) (parent.getWidth() - width);
                                    short borderY = (short) (parent.getHeight() - height);
                                    if (parent.getChildCount() == 1 && borderX > 0 && borderY > 0 && borderX <= 12) {
                                        forceFullscreen = true;
                                        removeRenderableWindow(parent);
                                    }
                                }
                            }
                        }
                        this.renderableWindows.add(new RenderableWindow(window.getContent(), x, y, forceFullscreen));
                    } else {
                        this.renderableWindows.add(new RenderableWindow(window.getContent(), x, y));
                    }
                }
            }
            for (Window child : window.getChildren()) {
                collectRenderableWindows(child, child.getX() + x, child.getY() + y);
            }
        }
    }

    private void removeRenderableWindow(Window window) {
        for (int i = 0; i < this.renderableWindows.size(); i++) {
            if (this.renderableWindows.get(i).content == window.getContent()) {
                this.renderableWindows.remove(i);
                return;
            }
        }
    }

    /* renamed from: updateWindowPosition */
    public void lambda$onUpdateWindowGeometry$0(Window window) {
        Iterator<RenderableWindow> it = this.renderableWindows.iterator();
        while (it.hasNext()) {
            RenderableWindow renderableWindow = it.next();
            if (renderableWindow.content == window.getContent()) {
                renderableWindow.rootX = window.getRootX();
                renderableWindow.rootY = window.getRootY();
                return;
            }
        }
    }

    public void setCursorVisible(boolean cursorVisible) {
        this.cursorVisible = cursorVisible;
        this.xServerView.requestRender();
    }

    public void setCursorScale(float cursorScale) {
        this.cursorScale = cursorScale;
    }

    public void setCursorColor(int cursorColor) {
        this.cursorBackColor = cursorColor;
        this.cursorForeColor = ColorUtils.calculateLuminance(cursorColor) < 0.5d ? 16777215 : 0;
    }

    public void setScreenOffsetYRelativeToCursor(boolean screenOffsetYRelativeToCursor) {
        this.screenOffsetYRelativeToCursor = screenOffsetYRelativeToCursor;
        this.xServerView.requestRender();
    }

    public void setForceFullscreenWMClass(String forceFullscreenWMClass) {
        this.forceFullscreenWMClass = forceFullscreenWMClass;
    }

    public void setUnviewableWMClasses(String... unviewableWMNames) {
        this.unviewableWMClasses = unviewableWMNames;
    }

    public boolean isFullscreen() {
        return this.fullscreen;
    }

    public float getMagnifierZoom() {
        return this.magnifierZoom;
    }

    public void setMagnifierZoom(float magnifierZoom) {
        this.magnifierZoom = magnifierZoom;
        this.xServerView.requestRender();
    }
}
