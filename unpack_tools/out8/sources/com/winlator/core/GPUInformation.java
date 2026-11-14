package com.winlator.core;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.collection.ArrayMap;
import androidx.preference.PreferenceManager;
import java.util.Locale;
import java.util.Objects;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

/* loaded from: classes.dex */
public abstract class GPUInformation {
    private static ArrayMap<String, String> loadGPUInformation(final Context context) {
        final Thread thread = Thread.currentThread();
        final ArrayMap<String, String> gpuInfo = new ArrayMap<>();
        gpuInfo.put("renderer", "");
        gpuInfo.put("vendor", "");
        gpuInfo.put("version", "");
        new Thread(new Runnable() { // from class: com.winlator.core.GPUInformation$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                GPUInformation.lambda$loadGPUInformation$0(ArrayMap.this, context, thread);
            }
        }).start();
        synchronized (thread) {
            try {
                thread.wait();
            } catch (InterruptedException e) {
            }
        }
        return gpuInfo;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$loadGPUInformation$0(ArrayMap gpuInfo, Context context, Thread thread) {
        int[] attribList = {12339, 1, 12352, 4, 12324, 8, 12323, 8, 12322, 8, 12321, 0, 12344};
        EGLConfig[] configs = new EGLConfig[1];
        int[] configCounts = new int[1];
        EGL10 egl = (EGL10) EGLContext.getEGL();
        EGLDisplay eglDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        int[] version = new int[2];
        egl.eglInitialize(eglDisplay, version);
        egl.eglChooseConfig(eglDisplay, attribList, configs, 1, configCounts);
        int[] attribList2 = {12440, 2, 12344};
        EGLContext eglContext = egl.eglCreateContext(eglDisplay, configs[0], EGL10.EGL_NO_CONTEXT, attribList2);
        EGLSurface eGLSurface = EGL10.EGL_NO_SURFACE;
        egl.eglMakeCurrent(eglDisplay, eGLSurface, eGLSurface, eglContext);
        GL10 gl = (GL10) eglContext.getGL();
        String gpuRenderer = Objects.toString(gl.glGetString(7937), "");
        String gpuVendor = Objects.toString(gl.glGetString(7936), "");
        String gpuVersion = Objects.toString(gl.glGetString(7938), "");
        gpuInfo.put("renderer", gpuRenderer);
        gpuInfo.put("vendor", gpuVendor);
        gpuInfo.put("version", gpuVersion);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putString("gpu_renderer", gpuRenderer).putString("gpu_vendor", gpuVendor).putString("gpu_version", gpuVersion).apply();
        synchronized (thread) {
            thread.notify();
        }
    }

    public static String getRenderer(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String value = preferences.getString("gpu_renderer", "");
        if (!value.isEmpty()) {
            return value;
        }
        ArrayMap<String, String> gpuInfo = loadGPUInformation(context);
        return gpuInfo.get("renderer");
    }

    public static boolean isAdreno6xx(Context context) {
        return getRenderer(context).toLowerCase(Locale.ENGLISH).matches(".*adreno[^6]+6[0-9]{2}.*");
    }
}
