package com.winlator.widget;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.winlatcn.R;
import java.util.Locale;

/* loaded from: classes.dex */
public class FrameRating extends FrameLayout implements Runnable {
    private int frameCount;
    private float lastFPS;
    private long lastTime;
    private final TextView textView;

    public FrameRating(Context context) {
        this(context, null);
    }

    public FrameRating(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FrameRating(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.lastTime = 0L;
        this.frameCount = 0;
        this.lastFPS = 0.0f;
        View view = LayoutInflater.from(context).inflate(R.layout.frame_rating, (ViewGroup) this, false);
        this.textView = (TextView) view.findViewById(R.id.TVFPS);
        addView(view);
    }

    public void update() {
        if (this.lastTime == 0) {
            this.lastTime = SystemClock.elapsedRealtime();
        }
        long time = SystemClock.elapsedRealtime();
        long j = this.lastTime;
        if (time >= 500 + j) {
            this.lastFPS = (this.frameCount * 1000) / ((float) (time - j));
            post(this);
            this.lastTime = time;
            this.frameCount = 0;
        }
        this.frameCount++;
    }

    @Override // java.lang.Runnable
    public void run() {
        if (getVisibility() == 8) {
            setVisibility(0);
        }
        this.textView.setText(String.format(Locale.ENGLISH, "%.1f", Float.valueOf(this.lastFPS)));
    }
}
