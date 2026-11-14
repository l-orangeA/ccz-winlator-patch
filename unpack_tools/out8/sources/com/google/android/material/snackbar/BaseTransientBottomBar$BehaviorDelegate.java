package com.google.android.material.snackbar;

import android.view.MotionEvent;
import android.view.View;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.behavior.SwipeDismissBehavior;
import com.google.android.material.snackbar.SnackbarManager;

/* loaded from: classes.dex */
public class BaseTransientBottomBar$BehaviorDelegate {
    private SnackbarManager.Callback managerCallback;

    public BaseTransientBottomBar$BehaviorDelegate(SwipeDismissBehavior<?> behavior) {
        behavior.setStartAlphaSwipeDistance(0.1f);
        behavior.setEndAlphaSwipeDistance(0.6f);
        behavior.setSwipeDirection(0);
    }

    public boolean canSwipeDismissView(View child) {
        return child instanceof BaseTransientBottomBar$SnackbarBaseLayout;
    }

    public void onInterceptTouchEvent(CoordinatorLayout parent, View child, MotionEvent event) {
        switch (event.getActionMasked()) {
            case 0:
                if (parent.isPointInChildBounds(child, (int) event.getX(), (int) event.getY())) {
                    SnackbarManager.getInstance().pauseTimeout(this.managerCallback);
                    return;
                }
                return;
            case 1:
            case 3:
                SnackbarManager.getInstance().restoreTimeoutIfPaused(this.managerCallback);
                return;
            case 2:
            default:
                return;
        }
    }
}
