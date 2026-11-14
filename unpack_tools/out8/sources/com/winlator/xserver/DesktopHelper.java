package com.winlator.xserver;

import androidx.collection.ArrayMap;
import com.winlator.xserver.Pointer;
import com.winlator.xserver.Property;
import com.winlator.xserver.WindowManager;
import com.winlator.xserver.XServer;
import java.util.Map;

/* loaded from: classes.dex */
public abstract class DesktopHelper {
    public static void attachTo(final XServer xServer) {
        setupXResources(xServer);
        xServer.pointer.addOnPointerMotionListener(new Pointer.OnPointerMotionListener() { // from class: com.winlator.xserver.DesktopHelper.1
            @Override // com.winlator.xserver.Pointer.OnPointerMotionListener
            public void onPointerButtonPress(Pointer.Button button) {
                DesktopHelper.updateFocusedWindow(XServer.this);
            }
        });
        xServer.windowManager.addOnWindowModificationListener(new WindowManager.OnWindowModificationListener() { // from class: com.winlator.xserver.DesktopHelper.2
            @Override // com.winlator.xserver.WindowManager.OnWindowModificationListener
            public void onMapWindow(Window window) {
                DesktopHelper.setFocusedWindow(XServer.this, window);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void updateFocusedWindow(XServer xServer) {
        WindowManager windowManager;
        Window window;
        XLock lock = xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.INPUT_DEVICE);
        try {
            Window focusedWindow = xServer.windowManager.getFocusedWindow();
            Window child = xServer.windowManager.findPointWindow(xServer.pointer.getClampedX(), xServer.pointer.getClampedY());
            if (child == null && focusedWindow != (window = (windowManager = xServer.windowManager).rootWindow)) {
                windowManager.setFocus(window, WindowManager.FocusRevertTo.NONE);
            } else if (child != null && child != focusedWindow) {
                setFocusedWindow(xServer, child);
            }
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

    /* JADX INFO: Access modifiers changed from: private */
    public static void setFocusedWindow(XServer xServer, Window window) {
        if (window.isApplicationWindow()) {
            Window parent = window.getParent();
            WindowManager windowManager = xServer.windowManager;
            boolean parentIsRoot = parent == windowManager.rootWindow;
            windowManager.setFocus(window, parentIsRoot ? WindowManager.FocusRevertTo.POINTER_ROOT : WindowManager.FocusRevertTo.PARENT);
            xServer.getWinHandler().bringToFront(window.getClassName(), window.getHandle());
        }
    }

    private static void setupXResources(XServer xServer) {
        int atom = Atom.getId("RESOURCE_MANAGER");
        int type = Atom.getId("STRING");
        ArrayMap<String, String> values = new ArrayMap<>();
        values.put("size", "20");
        values.put("theme", "dmz");
        values.put("theme_core", "true");
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            sb.append("Xcursor");
            sb.append('.');
            sb.append(entry.getKey());
            sb.append(':');
            sb.append('\t');
            sb.append(entry.getValue());
            sb.append('\n');
        }
        byte[] data = sb.toString().getBytes(XServer.LATIN1_CHARSET);
        xServer.windowManager.rootWindow.modifyProperty(atom, type, Property.Format.BYTE_ARRAY, Property.Mode.APPEND, data);
    }
}
