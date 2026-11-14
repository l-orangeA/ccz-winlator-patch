package com.winlator.xserver;

import android.graphics.Bitmap;
import com.winlator.core.Callback;
import com.winlator.math.Mathf;
import com.winlator.renderer.GPUImage;
import com.winlator.renderer.Texture;
import com.winlator.xserver.GraphicsContext;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/* loaded from: classes.dex */
public class Drawable extends XResource {
    private boolean blank;
    private ByteBuffer data;
    public final short height;
    private Callback<Drawable> onDestroyListener;
    private Runnable onDrawListener;
    public final Object renderLock;
    private Texture texture;
    public final Visual visual;
    public final short width;

    private static native void copyArea(short s, short s2, short s3, short s4, short s5, short s6, short s7, short s8, ByteBuffer byteBuffer, ByteBuffer byteBuffer2);

    private static native void copyAreaOp(short s, short s2, short s3, short s4, short s5, short s6, short s7, short s8, ByteBuffer byteBuffer, ByteBuffer byteBuffer2, int i);

    private static native void drawAlphaMaskedBitmap(byte b, byte b2, byte b3, byte b4, byte b5, byte b6, ByteBuffer byteBuffer, ByteBuffer byteBuffer2, ByteBuffer byteBuffer3);

    private static native void drawBitmap(short s, short s2, ByteBuffer byteBuffer, ByteBuffer byteBuffer2);

    private static native void drawLine(short s, short s2, short s3, short s4, int i, short s5, short s6, ByteBuffer byteBuffer);

    private static native void fillRect(short s, short s2, short s3, short s4, int i, short s5, ByteBuffer byteBuffer);

    private static native void fromBitmap(Bitmap bitmap, ByteBuffer byteBuffer);

    static {
        System.loadLibrary("winlator");
    }

    public Drawable(int id, int width, int height, Visual visual) {
        super(id);
        this.texture = new Texture();
        this.blank = true;
        this.renderLock = new Object();
        this.width = (short) width;
        this.height = (short) height;
        this.visual = visual;
        this.data = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.LITTLE_ENDIAN);
    }

    public static Drawable fromBitmap(Bitmap bitmap) {
        Drawable drawable = new Drawable(0, bitmap.getWidth(), bitmap.getHeight(), null);
        fromBitmap(bitmap, drawable.data);
        drawable.blank = false;
        return drawable;
    }

    public boolean isBlank() {
        return this.blank;
    }

    public Texture getTexture() {
        return this.texture;
    }

    public void setTexture(Texture texture) {
        if (texture instanceof GPUImage) {
            this.data = ((GPUImage) texture).getVirtualData();
        }
        this.texture = texture;
    }

    public ByteBuffer getData() {
        return this.data;
    }

    public void setData(ByteBuffer data) {
        this.data = data;
        this.blank = false;
    }

    private short getStride() {
        Texture texture = this.texture;
        return texture instanceof GPUImage ? ((GPUImage) texture).getStride() : this.width;
    }

    public Runnable getOnDrawListener() {
        return this.onDrawListener;
    }

    public void setOnDrawListener(Runnable onDrawListener) {
        this.onDrawListener = onDrawListener;
    }

    public Callback<Drawable> getOnDestroyListener() {
        return this.onDestroyListener;
    }

    public void setOnDestroyListener(Callback<Drawable> onDestroyListener) {
        this.onDestroyListener = onDestroyListener;
    }

    /* JADX WARN: Removed duplicated region for block: B:10:? A[RETURN, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:7:0x0078  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void drawImage(short r20, short r21, short r22, short r23, short r24, short r25, byte r26, java.nio.ByteBuffer r27, short r28, short r29) {
        /*
            r19 = this;
            r0 = r19
            r1 = r24
            r2 = r25
            r3 = r26
            r4 = 0
            r5 = 1
            if (r3 != r5) goto L14
            java.nio.ByteBuffer r6 = r0.data
            r15 = r27
            drawBitmap(r1, r2, r15, r6)
            goto L1f
        L14:
            r15 = r27
            r6 = 24
            if (r3 == r6) goto L24
            r6 = 32
            if (r3 != r6) goto L1f
            goto L24
        L1f:
            r6 = r22
            r18 = r23
            goto L65
        L24:
            short r6 = r0.width
            int r6 = r6 - r5
            r7 = r22
            int r6 = com.winlator.math.Mathf.clamp(r7, r4, r6)
            short r6 = (short) r6
            short r7 = r0.height
            int r7 = r7 - r5
            r8 = r23
            int r7 = com.winlator.math.Mathf.clamp(r8, r4, r7)
            short r14 = (short) r7
            int r7 = r6 + r1
            short r8 = r0.width
            if (r7 <= r8) goto L40
            int r8 = r8 - r6
            short r1 = (short) r8
        L40:
            int r7 = r14 + r2
            short r8 = r0.height
            if (r7 <= r8) goto L48
            int r8 = r8 - r14
            short r2 = (short) r8
        L48:
            short r16 = r19.getStride()
            java.nio.ByteBuffer r13 = r0.data
            r7 = r20
            r8 = r21
            r9 = r6
            r10 = r14
            r11 = r1
            r12 = r2
            r17 = r13
            r13 = r28
            r18 = r14
            r14 = r16
            r15 = r27
            r16 = r17
            copyArea(r7, r8, r9, r10, r11, r12, r13, r14, r15, r16)
        L65:
            java.nio.ByteBuffer r7 = r0.data
            r7.rewind()
            r27.rewind()
            com.winlator.renderer.Texture r7 = r0.texture
            r7.setNeedsUpdate(r5)
            r0.blank = r4
            java.lang.Runnable r4 = r0.onDrawListener
            if (r4 == 0) goto L7b
            r4.run()
        L7b:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.winlator.xserver.Drawable.drawImage(short, short, short, short, short, short, byte, java.nio.ByteBuffer, short, short):void");
    }

    public ByteBuffer getImage(short x, short y, short width, short height) {
        ByteBuffer dstData = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.LITTLE_ENDIAN);
        short x2 = (short) Mathf.clamp((int) x, 0, this.width - 1);
        short y2 = (short) Mathf.clamp((int) y, 0, this.height - 1);
        int i = x2 + width;
        short s = this.width;
        short width2 = i > s ? (short) (s - x2) : width;
        int i2 = y2 + height;
        short s2 = this.height;
        copyArea(x2, y2, (short) 0, (short) 0, width2, i2 > s2 ? (short) (s2 - y2) : height, getStride(), width2, this.data, dstData);
        this.data.rewind();
        dstData.rewind();
        return dstData;
    }

    public void copyArea(short srcX, short srcY, short dstX, short dstY, short width, short height, Drawable drawable) {
        copyArea(srcX, srcY, dstX, dstY, width, height, drawable, GraphicsContext.Function.COPY);
    }

    public void copyArea(short srcX, short srcY, short dstX, short dstY, short width, short height, Drawable drawable, GraphicsContext.Function gcFunction) {
        short dstX2 = (short) Mathf.clamp((int) dstX, 0, this.width - 1);
        short dstY2 = (short) Mathf.clamp((int) dstY, 0, this.height - 1);
        int i = dstX2 + width;
        short s = this.width;
        short width2 = i > s ? (short) (s - dstX2) : width;
        int i2 = dstY2 + height;
        short s2 = this.height;
        short height2 = i2 > s2 ? (short) (s2 - dstY2) : height;
        if (gcFunction == GraphicsContext.Function.COPY) {
            copyArea(srcX, srcY, dstX2, dstY2, width2, height2, drawable.getStride(), getStride(), drawable.data, this.data);
        } else {
            copyAreaOp(srcX, srcY, dstX2, dstY2, width2, height2, drawable.getStride(), getStride(), drawable.data, this.data, gcFunction.ordinal());
        }
        this.data.rewind();
        drawable.data.rewind();
        this.texture.setNeedsUpdate(true);
        this.blank = false;
        Runnable runnable = this.onDrawListener;
        if (runnable != null) {
            runnable.run();
        }
    }

    public void fillColor(int color) {
        fillRect(0, 0, this.width, this.height, color);
    }

    public void fillRect(int x, int y, int width, int height, int color) {
        int x2 = (short) Mathf.clamp(x, 0, this.width - 1);
        int y2 = (short) Mathf.clamp(y, 0, this.height - 1);
        int i = x2 + width;
        int i2 = this.width;
        if (i > i2) {
            width = (short) (i2 - x2);
        }
        int i3 = y2 + height;
        int i4 = this.height;
        if (i3 > i4) {
            height = (short) (i4 - y2);
        }
        fillRect((short) x2, (short) y2, (short) width, (short) height, color, getStride(), this.data);
        this.data.rewind();
        this.texture.setNeedsUpdate(true);
        this.blank = false;
        Runnable runnable = this.onDrawListener;
        if (runnable != null) {
            runnable.run();
        }
    }

    public void drawLines(int color, int lineWidth, short... points) {
        for (int i = 2; i < points.length; i += 2) {
            drawLine(points[i - 2], points[i - 1], points[i + 0], points[i + 1], color, (short) lineWidth);
        }
    }

    public void drawLine(int x0, int y0, int x1, int y1, int color, int lineWidth) {
        drawLine((short) Mathf.clamp(x0, 0, this.width - lineWidth), (short) Mathf.clamp(y0, 0, this.height - lineWidth), (short) Mathf.clamp(x1, 0, this.width - lineWidth), (short) Mathf.clamp(y1, 0, this.height - lineWidth), color, (short) lineWidth, getStride(), this.data);
        this.data.rewind();
        this.texture.setNeedsUpdate(true);
        this.blank = false;
        Runnable runnable = this.onDrawListener;
        if (runnable != null) {
            runnable.run();
        }
    }

    public void drawAlphaMaskedBitmap(byte foreRed, byte foreGreen, byte foreBlue, byte backRed, byte backGreen, byte backBlue, Drawable srcDrawable, Drawable maskDrawable) {
        drawAlphaMaskedBitmap(foreRed, foreGreen, foreBlue, backRed, backGreen, backBlue, srcDrawable.data, maskDrawable.data, this.data);
        this.data.rewind();
        this.texture.setNeedsUpdate(true);
        this.blank = false;
        Runnable runnable = this.onDrawListener;
        if (runnable != null) {
            runnable.run();
        }
    }
}
