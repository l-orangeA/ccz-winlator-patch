package com.winlator.xserver;

/* loaded from: classes.dex */
public class GraphicsContext extends XResource {
    private int background;
    public final Drawable drawable;
    private int foreground;
    private Function function;
    private int lineWidth;
    private int planeMask;
    private SubwindowMode subwindowMode;

    /* loaded from: classes.dex */
    public enum Function {
        CLEAR,
        AND,
        AND_REVERSE,
        COPY,
        AND_INVERTED,
        NO_OP,
        XOR,
        OR,
        NOR,
        EQUIV,
        INVERT,
        OR_REVERSE,
        COPY_INVERTED,
        OR_INVERTED,
        NAND,
        SET
    }

    /* loaded from: classes.dex */
    public enum SubwindowMode {
        CLIP_BY_CHILDREN,
        INCLUDE_INFERIORS
    }

    public GraphicsContext(int id, Drawable drawable) {
        super(id);
        this.function = Function.COPY;
        this.background = 16777215;
        this.foreground = 0;
        this.lineWidth = 1;
        this.planeMask = -1;
        this.subwindowMode = SubwindowMode.CLIP_BY_CHILDREN;
        this.drawable = drawable;
    }

    public int getForeground() {
        return this.foreground;
    }

    public void setForeground(int foreground) {
        this.foreground = foreground;
    }

    public int getBackground() {
        return this.background;
    }

    public void setBackground(int background) {
        this.background = background;
    }

    public int getLineWidth() {
        return this.lineWidth;
    }

    public void setLineWidth(int lineWidth) {
        this.lineWidth = lineWidth;
    }

    public void setPlaneMask(int planeMask) {
        this.planeMask = planeMask;
    }

    public Function getFunction() {
        return this.function;
    }

    public void setFunction(Function function) {
        this.function = function;
    }

    public void setSubwindowMode(SubwindowMode subwindowMode) {
        this.subwindowMode = subwindowMode;
    }
}
