package androidx.transition;

import android.animation.TypeEvaluator;
import android.graphics.Rect;

/* loaded from: classes.dex */
class RectEvaluator implements TypeEvaluator<Rect> {
    private Rect mRect;

    @Override // android.animation.TypeEvaluator
    public Rect evaluate(float fraction, Rect startValue, Rect endValue) {
        int left = startValue.left + ((int) ((endValue.left - r0) * fraction));
        int top = startValue.top + ((int) ((endValue.top - r1) * fraction));
        int right = startValue.right + ((int) ((endValue.right - r2) * fraction));
        int bottom = startValue.bottom + ((int) ((endValue.bottom - r3) * fraction));
        Rect rect = this.mRect;
        if (rect == null) {
            return new Rect(left, top, right, bottom);
        }
        rect.set(left, top, right, bottom);
        return this.mRect;
    }
}
