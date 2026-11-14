package androidx.core.view.accessibility;

import android.view.View;
import android.view.accessibility.AccessibilityRecord;

/* loaded from: classes.dex */
public class AccessibilityRecordCompat {
    public static void setSource(AccessibilityRecord record, View root, int virtualDescendantId) {
        record.setSource(root, virtualDescendantId);
    }

    public static void setMaxScrollX(AccessibilityRecord record, int maxScrollX) {
        record.setMaxScrollX(maxScrollX);
    }

    public static void setMaxScrollY(AccessibilityRecord record, int maxScrollY) {
        record.setMaxScrollY(maxScrollY);
    }
}
