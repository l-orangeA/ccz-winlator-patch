package com.winlator.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import com.winlatcn.R;
import com.winlator.R$styleable;
import com.winlator.core.StringUtils;
import com.winlator.core.UnitUtils;
import com.winlator.math.Mathf;
import java.text.DecimalFormat;

/* loaded from: classes.dex */
public class SeekBar extends AppCompatImageView {
    private final float barHeight;
    private final int colorPrimary;
    private final int colorSecondary;
    private DecimalFormat decimalFormat;
    private LinearGradient glossyEffectGradient;
    private float maxValue;
    private float minValue;
    private float normalizedValue;
    private OnValueChangeListener onValueChangeListener;
    private float padding;
    private final Paint paint;
    private final RectF rect;
    private float step;
    private String suffix;
    private final int textColor;
    private float textSize;
    private final float thumbRadius;
    private final float thumbSize;

    /* loaded from: classes.dex */
    public interface OnValueChangeListener {
        void onValueChangeListener(SeekBar seekBar, float f);
    }

    public SeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.paint = new Paint(1);
        this.minValue = 0.0f;
        this.maxValue = 100.0f;
        this.step = 1.0f;
        this.normalizedValue = 0.0f;
        this.textSize = UnitUtils.dpToPx(16.0f);
        this.textColor = -9211021;
        this.barHeight = UnitUtils.dpToPx(6.0f);
        float dpToPx = UnitUtils.dpToPx(20.0f);
        this.thumbSize = dpToPx;
        this.rect = new RectF();
        this.colorPrimary = -2631721;
        this.colorSecondary = ContextCompat.getColor(context, R.color.colorAccent);
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R$styleable.SeekBar, 0, 0);
            this.minValue = ta.getFloat(1, this.minValue);
            this.maxValue = ta.getFloat(0, this.maxValue);
            this.suffix = ta.getString(3);
            this.textSize = ta.getDimension(4, this.textSize);
            setStep(ta.getFloat(2, this.step));
            setValue(ta.getFloat(5, 0.0f));
            ta.recycle();
        }
        this.thumbRadius = dpToPx / 2.0f;
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    public String getSuffix() {
        return this.suffix;
    }

    public synchronized void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public OnValueChangeListener getOnValueChangeListener() {
        return this.onValueChangeListener;
    }

    public void setOnValueChangeListener(OnValueChangeListener onValueChangeListener) {
        this.onValueChangeListener = onValueChangeListener;
    }

    public float getValue() {
        float f = this.minValue;
        return f + (this.normalizedValue * (this.maxValue - f));
    }

    public synchronized void setValue(float value) {
        float roundTo = Mathf.roundTo(value, this.step);
        float f = this.minValue;
        this.normalizedValue = Mathf.clamp((roundTo - f) / (this.maxValue - f), 0.0f, 1.0f);
        postInvalidate();
    }

    public float getStep() {
        return this.step;
    }

    public synchronized void setStep(float step) {
        this.step = step;
        String[] parts = String.valueOf(Mathf.fract(step)).split("\\.");
        int decimalPlaces = parts[parts.length - 1].length();
        StringBuilder sb = new StringBuilder();
        sb.append("0.");
        sb.append(StringUtils.repeat(decimalPlaces > 1 ? '0' : '#', decimalPlaces));
        String format = sb.toString();
        this.decimalFormat = new DecimalFormat(format);
    }

    public float getMinValue() {
        return this.minValue;
    }

    public synchronized void setMinValue(float minValue) {
        this.minValue = minValue;
    }

    public float getMaxValue() {
        return this.maxValue;
    }

    public synchronized void setMaxValue(float maxValue) {
        this.maxValue = maxValue;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        switch (event.getAction()) {
            case 0:
                setPressed(isInThumbRange(event.getX()));
                if (!isPressed()) {
                    return super.onTouchEvent(event);
                }
                setNormalizedValue(event.getX());
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                invalidate();
                return true;
            case 1:
                if (isPressed()) {
                    setNormalizedValue(event.getX());
                    setPressed(false);
                }
                invalidate();
                OnValueChangeListener onValueChangeListener = this.onValueChangeListener;
                if (onValueChangeListener != null) {
                    onValueChangeListener.onValueChangeListener(this, getValue());
                }
                return true;
            case 2:
                if (isPressed()) {
                    setNormalizedValue(event.getX());
                    invalidate();
                }
                return true;
            case 3:
                if (isPressed()) {
                    setPressed(false);
                }
                invalidate();
                return true;
            default:
                return true;
        }
    }

    @Override // android.widget.ImageView, android.view.View
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int margin = (int) UnitUtils.dpToPx(2.0f);
        int width = (int) UnitUtils.dpToPx(220.0f);
        int height = (int) (this.thumbSize + margin);
        setMeasuredDimension(ImageView.resolveSizeAndState(width + margin, widthMeasureSpec, 0), ImageView.resolveSizeAndState(height, heightMeasureSpec, 0));
    }

    /* JADX WARN: Removed duplicated region for block: B:19:0x0073 A[Catch: all -> 0x0143, TryCatch #0 {, blocks: (B:4:0x0005, B:8:0x0016, B:11:0x0050, B:13:0x0064, B:17:0x006e, B:19:0x0073, B:20:0x0079, B:22:0x00e9, B:23:0x0113), top: B:3:0x0005 }] */
    /* JADX WARN: Removed duplicated region for block: B:22:0x00e9 A[Catch: all -> 0x0143, TryCatch #0 {, blocks: (B:4:0x0005, B:8:0x0016, B:11:0x0050, B:13:0x0064, B:17:0x006e, B:19:0x0073, B:20:0x0079, B:22:0x00e9, B:23:0x0113), top: B:3:0x0005 }] */
    /* JADX WARN: Removed duplicated region for block: B:28:0x0078  */
    @Override // android.widget.ImageView, android.view.View
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    protected synchronized void onDraw(android.graphics.Canvas r26) {
        /*
            Method dump skipped, instructions count: 326
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.winlator.widget.SeekBar.onDraw(android.graphics.Canvas):void");
    }

    private int getThumbHoleColor() {
        int r = Mathf.clamp(Color.red(this.colorSecondary) - 30, 0, 255);
        int g = Mathf.clamp(Color.green(this.colorSecondary) - 30, 0, 255);
        int b = Mathf.clamp(Color.blue(this.colorSecondary) - 30, 0, 255);
        return Color.rgb(r, g, b);
    }

    @Override // android.view.View
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("SUPER", super.onSaveInstanceState());
        bundle.putFloat("normalizedValue", this.normalizedValue);
        return bundle;
    }

    @Override // android.view.View
    protected void onRestoreInstanceState(Parcelable parcel) {
        Bundle bundle = (Bundle) parcel;
        super.onRestoreInstanceState(bundle.getParcelable("SUPER"));
        this.normalizedValue = bundle.getFloat("normalizedValue");
    }

    private boolean isInThumbRange(float touchX) {
        return Math.abs(touchX - getScreenCoord()) <= this.thumbRadius;
    }

    private float getScreenCoord() {
        return this.thumbRadius + (this.normalizedValue * (getWidth() - this.padding));
    }

    private void setNormalizedValue(float touchX) {
        int width = getWidth();
        float f = this.padding;
        if (width - f <= 0.0f) {
            return;
        }
        float normalizedStep = this.step / (this.maxValue - this.minValue);
        this.normalizedValue = Mathf.clamp(Mathf.roundTo((touchX - this.thumbRadius) / (width - f), normalizedStep), 0.0f, 1.0f);
    }
}
