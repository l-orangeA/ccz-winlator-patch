package com.google.android.material.internal;

import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristic;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.text.TextUtils;

/* loaded from: classes.dex */
final class StaticLayoutBuilderCompat {
    static final int DEFAULT_HYPHENATION_FREQUENCY = 1;
    private int end;
    private boolean isRtl;
    private final TextPaint paint;
    private CharSequence source;
    private final int width;
    private int start = 0;
    private Layout.Alignment alignment = Layout.Alignment.ALIGN_NORMAL;
    private int maxLines = Integer.MAX_VALUE;
    private float lineSpacingAdd = 0.0f;
    private float lineSpacingMultiplier = 1.0f;
    private int hyphenationFrequency = DEFAULT_HYPHENATION_FREQUENCY;
    private boolean includePad = true;
    private TextUtils.TruncateAt ellipsize = null;

    /* loaded from: classes.dex */
    static class StaticLayoutBuilderCompatException extends Exception {
    }

    private StaticLayoutBuilderCompat(CharSequence source, TextPaint paint, int width) {
        this.source = source;
        this.paint = paint;
        this.width = width;
        this.end = source.length();
    }

    public static StaticLayoutBuilderCompat obtain(CharSequence source, TextPaint paint, int width) {
        return new StaticLayoutBuilderCompat(source, paint, width);
    }

    public StaticLayoutBuilderCompat setAlignment(Layout.Alignment alignment) {
        this.alignment = alignment;
        return this;
    }

    public StaticLayoutBuilderCompat setIncludePad(boolean includePad) {
        this.includePad = includePad;
        return this;
    }

    public StaticLayoutBuilderCompat setMaxLines(int maxLines) {
        this.maxLines = maxLines;
        return this;
    }

    public StaticLayoutBuilderCompat setLineSpacing(float spacingAdd, float lineSpacingMultiplier) {
        this.lineSpacingAdd = spacingAdd;
        this.lineSpacingMultiplier = lineSpacingMultiplier;
        return this;
    }

    public StaticLayoutBuilderCompat setHyphenationFrequency(int hyphenationFrequency) {
        this.hyphenationFrequency = hyphenationFrequency;
        return this;
    }

    public StaticLayoutBuilderCompat setEllipsize(TextUtils.TruncateAt ellipsize) {
        this.ellipsize = ellipsize;
        return this;
    }

    public StaticLayout build() throws StaticLayoutBuilderCompatException {
        if (this.source == null) {
            this.source = "";
        }
        int availableWidth = Math.max(0, this.width);
        CharSequence textToDraw = this.source;
        if (this.maxLines == 1) {
            textToDraw = TextUtils.ellipsize(this.source, this.paint, availableWidth, this.ellipsize);
        }
        int min = Math.min(textToDraw.length(), this.end);
        this.end = min;
        if (this.isRtl && this.maxLines == 1) {
            this.alignment = Layout.Alignment.ALIGN_OPPOSITE;
        }
        StaticLayout.Builder builder = StaticLayout.Builder.obtain(textToDraw, this.start, min, this.paint, availableWidth);
        builder.setAlignment(this.alignment);
        builder.setIncludePad(this.includePad);
        TextDirectionHeuristic textDirectionHeuristic = this.isRtl ? TextDirectionHeuristics.RTL : TextDirectionHeuristics.LTR;
        builder.setTextDirection(textDirectionHeuristic);
        TextUtils.TruncateAt truncateAt = this.ellipsize;
        if (truncateAt != null) {
            builder.setEllipsize(truncateAt);
        }
        builder.setMaxLines(this.maxLines);
        float f = this.lineSpacingAdd;
        if (f != 0.0f || this.lineSpacingMultiplier != 1.0f) {
            builder.setLineSpacing(f, this.lineSpacingMultiplier);
        }
        if (this.maxLines > 1) {
            builder.setHyphenationFrequency(this.hyphenationFrequency);
        }
        return builder.build();
    }

    public StaticLayoutBuilderCompat setIsRtl(boolean isRtl) {
        this.isRtl = isRtl;
        return this;
    }
}
