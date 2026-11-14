package com.winlator.renderer;

import androidx.annotation.Keep;
import com.winlator.xserver.Drawable;
import java.nio.ByteBuffer;

/* loaded from: classes.dex */
public class GPUImage extends Texture {
    private static boolean supported = false;
    private long hardwareBufferPtr;
    private long imageKHRPtr;
    private short stride;
    private ByteBuffer virtualData;

    private native long createHardwareBuffer(short s, short s2);

    private native long createImageKHR(long j, int i);

    private native void destroyHardwareBuffer(long j);

    private native void destroyImageKHR(long j);

    private native ByteBuffer lockHardwareBuffer(long j);

    static {
        System.loadLibrary("winlator");
    }

    public GPUImage(short width, short height) {
        long createHardwareBuffer = createHardwareBuffer(width, height);
        this.hardwareBufferPtr = createHardwareBuffer;
        if (createHardwareBuffer != 0) {
            this.virtualData = lockHardwareBuffer(createHardwareBuffer);
        }
    }

    @Override // com.winlator.renderer.Texture
    public void allocateTexture(short width, short height, ByteBuffer data) {
        if (isAllocated()) {
            return;
        }
        super.allocateTexture(width, height, null);
        this.imageKHRPtr = createImageKHR(this.hardwareBufferPtr, this.textureId);
    }

    @Override // com.winlator.renderer.Texture
    public void updateFromDrawable(Drawable drawable) {
        if (!isAllocated()) {
            allocateTexture(drawable.width, drawable.height, null);
        }
        this.needsUpdate = false;
    }

    public short getStride() {
        return this.stride;
    }

    @Keep
    private void setStride(short stride) {
        this.stride = stride;
    }

    public ByteBuffer getVirtualData() {
        return this.virtualData;
    }

    @Override // com.winlator.renderer.Texture
    public void destroy() {
        destroyImageKHR(this.imageKHRPtr);
        destroyHardwareBuffer(this.hardwareBufferPtr);
        this.virtualData = null;
        this.imageKHRPtr = 0L;
        this.hardwareBufferPtr = 0L;
        super.destroy();
    }

    public static boolean isSupported() {
        return supported;
    }

    public static void checkIsSupported() {
        GPUImage gpuImage = new GPUImage((short) 8, (short) 8);
        gpuImage.allocateTexture((short) 8, (short) 8, null);
        supported = (gpuImage.hardwareBufferPtr == 0 || gpuImage.imageKHRPtr == 0 || gpuImage.virtualData == null) ? false : true;
        gpuImage.destroy();
    }
}
