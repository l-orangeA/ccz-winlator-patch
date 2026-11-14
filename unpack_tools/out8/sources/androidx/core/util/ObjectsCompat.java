package androidx.core.util;

import java.util.Objects;

/* loaded from: classes.dex */
public class ObjectsCompat {
    public static boolean equals(Object a, Object b) {
        return Objects.equals(a, b);
    }

    public static int hash(Object... values) {
        return Objects.hash(values);
    }
}
