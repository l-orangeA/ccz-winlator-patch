package com.google.android.material.internal;

import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;

/* loaded from: classes.dex */
public class ToolbarUtils {
    public static TextView getTitleTextView(Toolbar toolbar) {
        return getTextView(toolbar, toolbar.getTitle());
    }

    public static TextView getSubtitleTextView(Toolbar toolbar) {
        return getTextView(toolbar, toolbar.getSubtitle());
    }

    private static TextView getTextView(Toolbar toolbar, CharSequence text) {
        for (int i = 0; i < toolbar.getChildCount(); i++) {
            View child = toolbar.getChildAt(i);
            if (child instanceof TextView) {
                TextView textView = (TextView) child;
                if (TextUtils.equals(textView.getText(), text)) {
                    return textView;
                }
            }
        }
        return null;
    }
}
