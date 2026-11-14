package com.winlator.renderer;

import android.opengl.GLES20;
import java.nio.Buffer;
import java.nio.FloatBuffer;

/* loaded from: classes.dex */
public class VertexAttribute {
    private Buffer buffer;
    private final byte itemSize;
    private final String name;
    private int bufferId = 0;
    private int location = -1;
    private boolean needsUpdate = true;

    public VertexAttribute(String name, int itemSize) {
        this.name = name;
        this.itemSize = (byte) itemSize;
    }

    public void put(float[] array) {
        this.buffer = FloatBuffer.wrap(array);
        this.needsUpdate = true;
    }

    public void update() {
        if (!this.needsUpdate || this.buffer == null) {
            return;
        }
        if (this.bufferId == 0) {
            int[] bufferIds = new int[1];
            GLES20.glGenBuffers(1, bufferIds, 0);
            this.bufferId = bufferIds[0];
        }
        int size = this.buffer.limit() * 4;
        GLES20.glBindBuffer(34962, this.bufferId);
        GLES20.glBufferData(34962, size, this.buffer, 35044);
        GLES20.glBindBuffer(34962, 0);
        this.needsUpdate = false;
    }

    public void bind(int programId) {
        update();
        if (this.location == -1) {
            this.location = GLES20.glGetAttribLocation(programId, this.name);
        }
        GLES20.glBindBuffer(34962, this.bufferId);
        GLES20.glEnableVertexAttribArray(this.location);
        GLES20.glVertexAttribPointer(this.location, (int) this.itemSize, 5126, false, 0, 0);
    }

    public void disable() {
        int i = this.location;
        if (i == -1) {
            return;
        }
        GLES20.glDisableVertexAttribArray(i);
    }

    public int count() {
        Buffer buffer = this.buffer;
        if (buffer != null) {
            return buffer.limit() / this.itemSize;
        }
        return 0;
    }
}
