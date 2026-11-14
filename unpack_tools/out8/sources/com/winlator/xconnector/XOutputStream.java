package com.winlator.xconnector;

import com.winlator.xserver.XServer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.locks.ReentrantLock;

/* loaded from: classes.dex */
public class XOutputStream {
    private static final byte[] ZERO = new byte[64];
    private ByteBuffer buffer;
    public final ClientSocket clientSocket;
    private final ReentrantLock lock = new ReentrantLock();
    private int ancillaryFd = -1;

    public XOutputStream(ClientSocket clientSocket, int initialCapacity) {
        this.clientSocket = clientSocket;
        this.buffer = ByteBuffer.allocateDirect(initialCapacity);
    }

    public void setByteOrder(ByteOrder byteOrder) {
        this.buffer.order(byteOrder);
    }

    public void setAncillaryFd(int ancillaryFd) {
        this.ancillaryFd = ancillaryFd;
    }

    public void writeByte(byte value) {
        ensureSpaceIsAvailable(1);
        this.buffer.put(value);
    }

    public void writeShort(short value) {
        ensureSpaceIsAvailable(2);
        this.buffer.putShort(value);
    }

    public void writeInt(int value) {
        ensureSpaceIsAvailable(4);
        this.buffer.putInt(value);
    }

    public void writeLong(long value) {
        ensureSpaceIsAvailable(8);
        this.buffer.putLong(value);
    }

    public void writeString8(String str) {
        byte[] bytes = str.getBytes(XServer.LATIN1_CHARSET);
        int length = (-str.length()) & 3;
        ensureSpaceIsAvailable(bytes.length + length);
        this.buffer.put(bytes);
        if (length > 0) {
            writePad(length);
        }
    }

    public void write(byte[] data) {
        write(data, 0, data.length);
    }

    public void write(byte[] data, int offset, int length) {
        ensureSpaceIsAvailable(length);
        this.buffer.put(data, offset, length);
    }

    public void write(ByteBuffer data) {
        ensureSpaceIsAvailable(data.remaining());
        this.buffer.put(data);
    }

    public void writePad(int length) {
        write(ZERO, 0, length);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void flush() throws IOException {
        if (this.buffer.position() != 0) {
            this.buffer.flip();
            int i = this.ancillaryFd;
            if (i != -1) {
                this.clientSocket.sendAncillaryMsg(this.buffer, i);
                this.ancillaryFd = -1;
            } else {
                this.clientSocket.write(this.buffer);
            }
            this.buffer.clear();
        }
    }

    public XStreamLock lock() {
        return new OutputStreamLock();
    }

    private void ensureSpaceIsAvailable(int length) {
        int position = this.buffer.position();
        if (this.buffer.capacity() - position >= length) {
            return;
        }
        ByteBuffer newBuffer = ByteBuffer.allocateDirect(this.buffer.capacity() + length).order(this.buffer.order());
        this.buffer.rewind();
        newBuffer.put(this.buffer).position(position);
        this.buffer = newBuffer;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class OutputStreamLock implements XStreamLock {
        public OutputStreamLock() {
            XOutputStream.this.lock.lock();
        }

        @Override // com.winlator.xconnector.XStreamLock, java.lang.AutoCloseable
        public void close() throws IOException {
            try {
                XOutputStream.this.flush();
            } finally {
                XOutputStream.this.lock.unlock();
            }
        }
    }
}
