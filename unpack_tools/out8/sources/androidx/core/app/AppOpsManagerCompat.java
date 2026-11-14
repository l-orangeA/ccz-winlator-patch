package androidx.core.app;

import android.app.AppOpsManager;
import android.content.Context;

/* loaded from: classes.dex */
public final class AppOpsManagerCompat {
    public static String permissionToOp(String permission) {
        return AppOpsManager.permissionToOp(permission);
    }

    public static int noteProxyOpNoThrow(Context context, String op, String proxiedPackageName) {
        AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(AppOpsManager.class);
        return appOpsManager.noteProxyOpNoThrow(op, proxiedPackageName);
    }
}
