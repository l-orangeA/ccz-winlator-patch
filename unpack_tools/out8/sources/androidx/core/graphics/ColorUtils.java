package androidx.core.graphics;

import android.graphics.Color;

/* loaded from: classes.dex */
public final class ColorUtils {
    private static final ThreadLocal<double[]> TEMP_ARRAY = new ThreadLocal<>();

    public static int compositeColors(int foreground, int background) {
        int bgAlpha = Color.alpha(background);
        int fgAlpha = Color.alpha(foreground);
        int a = compositeAlpha(fgAlpha, bgAlpha);
        int r = compositeComponent(Color.red(foreground), fgAlpha, Color.red(background), bgAlpha, a);
        int g = compositeComponent(Color.green(foreground), fgAlpha, Color.green(background), bgAlpha, a);
        int b = compositeComponent(Color.blue(foreground), fgAlpha, Color.blue(background), bgAlpha, a);
        return Color.argb(a, r, g, b);
    }

    private static int compositeAlpha(int foregroundAlpha, int backgroundAlpha) {
        return 255 - (((255 - backgroundAlpha) * (255 - foregroundAlpha)) / 255);
    }

    private static int compositeComponent(int fgC, int fgA, int bgC, int bgA, int a) {
        if (a == 0) {
            return 0;
        }
        return (((fgC * 255) * fgA) + ((bgC * bgA) * (255 - fgA))) / (a * 255);
    }

    public static double calculateLuminance(int color) {
        double[] result = getTempDouble3Array();
        colorToXYZ(color, result);
        return result[1] / 100.0d;
    }

    public static int setAlphaComponent(int color, int alpha) {
        if (alpha < 0 || alpha > 255) {
            throw new IllegalArgumentException("alpha must be between 0 and 255.");
        }
        return (16777215 & color) | (alpha << 24);
    }

    public static void colorToXYZ(int color, double[] outXyz) {
        RGBToXYZ(Color.red(color), Color.green(color), Color.blue(color), outXyz);
    }

    public static void RGBToXYZ(int r, int g, int b, double[] outXyz) {
        if (outXyz.length != 3) {
            throw new IllegalArgumentException("outXyz must have a length of 3.");
        }
        double sr = r / 255.0d;
        double sr2 = sr < 0.04045d ? sr / 12.92d : Math.pow((sr + 0.055d) / 1.055d, 2.4d);
        double sg = g / 255.0d;
        double sg2 = sg < 0.04045d ? sg / 12.92d : Math.pow((sg + 0.055d) / 1.055d, 2.4d);
        double sb = b / 255.0d;
        double sb2 = sb < 0.04045d ? sb / 12.92d : Math.pow((0.055d + sb) / 1.055d, 2.4d);
        outXyz[0] = ((0.4124d * sr2) + (0.3576d * sg2) + (0.1805d * sb2)) * 100.0d;
        outXyz[1] = ((0.2126d * sr2) + (0.7152d * sg2) + (0.0722d * sb2)) * 100.0d;
        outXyz[2] = ((0.0193d * sr2) + (0.1192d * sg2) + (0.9505d * sb2)) * 100.0d;
    }

    private static double[] getTempDouble3Array() {
        ThreadLocal<double[]> threadLocal = TEMP_ARRAY;
        double[] result = threadLocal.get();
        if (result == null) {
            double[] result2 = new double[3];
            threadLocal.set(result2);
            return result2;
        }
        return result;
    }
}
