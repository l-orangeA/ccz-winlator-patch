package com.winlator.math;

/* loaded from: classes.dex */
public abstract class Mathf {
    public static float clamp(float x, float min, float max) {
        return x < min ? min : x > max ? max : x;
    }

    public static int clamp(int x, int min, int max) {
        return x < min ? min : x > max ? max : x;
    }

    public static float roundTo(float x, float step) {
        return roundTo(x, step, true);
    }

    public static float roundTo(float x, float step, boolean roundHalfDown) {
        return (float) ((roundHalfDown ? Math.floor(x / step) : Math.round(r0)) * step);
    }

    public static int roundPoint(float x) {
        return (int) (x <= 0.0f ? Math.floor(x) : Math.ceil(x));
    }

    public static byte sign(float x) {
        return (byte) (x < 0.0f ? -1 : x > 0.0f ? 1 : 0);
    }

    public static float lengthSq(float x, float y) {
        return (x * x) + (y * y);
    }

    public static float fract(float x) {
        return x - ((int) Math.floor(x));
    }
}
