package androidx.transition;

import android.graphics.Matrix;
import android.util.Log;
import android.view.View;
import java.lang.reflect.Field;

/* loaded from: classes.dex */
class ViewUtilsBase {
    private static Field sViewFlagsField;
    private static boolean sViewFlagsFieldFetched;

    public void clearNonTransitionAlpha(View view) {
        throw null;
    }

    public float getTransitionAlpha(View view) {
        throw null;
    }

    public void saveNonTransitionAlpha(View view) {
        throw null;
    }

    public void setLeftTopRightBottom(View view, int i, int i2, int i3, int i4) {
        throw null;
    }

    public void setTransitionAlpha(View view, float f) {
        throw null;
    }

    public void transformMatrixToGlobal(View view, Matrix matrix) {
        throw null;
    }

    public void transformMatrixToLocal(View view, Matrix matrix) {
        throw null;
    }

    public void setTransitionVisibility(View view, int visibility) {
        if (!sViewFlagsFieldFetched) {
            try {
                Field declaredField = View.class.getDeclaredField("mViewFlags");
                sViewFlagsField = declaredField;
                declaredField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                Log.i("ViewUtilsBase", "fetchViewFlagsField: ");
            }
            sViewFlagsFieldFetched = true;
        }
        Field field = sViewFlagsField;
        if (field != null) {
            try {
                int viewFlags = field.getInt(view);
                sViewFlagsField.setInt(view, (viewFlags & (-13)) | visibility);
            } catch (IllegalAccessException e2) {
            }
        }
    }
}
