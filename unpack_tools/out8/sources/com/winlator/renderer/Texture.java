package com.winlator.renderer;

import android.opengl.GLES20;
import com.winlator.xserver.Drawable;
import java.nio.ByteBuffer;

/* loaded from: classes.dex */
public class Texture {
    protected int textureId = 0;
    protected int wrapS = 33071;
    protected int wrapT = 33071;
    protected int magFilter = 9729;
    protected int minFilter = 9729;
    protected int format = 32993;
    protected byte unpackAlignment = 4;
    protected boolean needsUpdate = true;

    /* JADX INFO: Access modifiers changed from: protected */
    public void generateTextureId() {
        int[] textureIds = new int[1];
        GLES20.glGenTextures(1, textureIds, 0);
        this.textureId = textureIds[0];
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void setTextureParameters() {
        GLES20.glTexParameteri(3553, 10242, this.wrapS);
        GLES20.glTexParameteri(3553, 10243, this.wrapT);
        GLES20.glTexParameteri(3553, 10240, this.magFilter);
        GLES20.glTexParameteri(3553, 10241, this.minFilter);
    }

    public void allocateTexture(short width, short height, ByteBuffer data) {
        generateTextureId();
        GLES20.glActiveTexture(33984);
        GLES20.glPixelStorei(3317, this.unpackAlignment);
        GLES20.glBindTexture(3553, this.textureId);
        if (data != null) {
            int i = this.format;
            GLES20.glTexImage2D(3553, 0, i, width, height, 0, i, 5121, data);
        }
        setTextureParameters();
        GLES20.glBindTexture(3553, 0);
    }

    public void setNeedsUpdate(boolean needsUpdate) {
        this.needsUpdate = needsUpdate;
    }

    public void updateFromDrawable(Drawable drawable) {
        ByteBuffer data = drawable.getData();
        if (data == null) {
            return;
        }
        if (!isAllocated()) {
            allocateTexture(drawable.width, drawable.height, data);
        } else if (this.needsUpdate) {
            GLES20.glBindTexture(3553, this.textureId);
            GLES20.glTexSubImage2D(3553, 0, 0, 0, drawable.width, drawable.height, this.format, 5121, data);
            GLES20.glBindTexture(3553, 0);
            this.needsUpdate = false;
        }
    }

    public boolean isAllocated() {
        return this.textureId > 0;
    }

    public int getTextureId() {
        return this.textureId;
    }

    public void copyFromFramebuffer(int framebuffer, short width, short height) {
        if (!isAllocated()) {
            allocateTexture(width, height, null);
        }
        GLES20.glBindFramebuffer(36160, framebuffer);
        GLES20.glActiveTexture(33984);
        GLES20.glBindTexture(3553, this.textureId);
        GLES20.glCopyTexImage2D(3553, 0, 6408, 0, 0, width, height, 0);
        GLES20.glBindTexture(3553, 0);
        GLES20.glBindFramebuffer(36160, 0);
    }

    public void destroy() {
        int i = this.textureId;
        if (i > 0) {
            int[] textureIds = {i};
            GLES20.glDeleteTextures(textureIds.length, textureIds, 0);
            this.textureId = 0;
        }
    }
}
