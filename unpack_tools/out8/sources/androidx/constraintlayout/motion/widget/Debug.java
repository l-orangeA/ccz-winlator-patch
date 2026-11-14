package androidx.constraintlayout.motion.widget;

import android.content.Context;
import android.view.View;

/* loaded from: classes.dex */
public class Debug {
    public static String getName(View view) {
        try {
            Context context = view.getContext();
            return context.getResources().getResourceEntryName(view.getId());
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}
