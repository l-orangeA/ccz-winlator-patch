package org.apache.commons.compress.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;

/* loaded from: classes.dex */
public class FixedLengthBlockOutputStream extends OutputStream implements WritableByteChannel {
    private final int blockSize;
    private final ByteBuffer buffer;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final WritableByteChannel out;

    public FixedLengthBlockOutputStream(OutputStream os, int blockSize) {
        if (os instanceof FileOutputStream) {
            FileOutputStream fileOutputStream = (FileOutputStream) os;
            this.out = fileOutputStream.getChannel();
            this.buffer = ByteBuffer.allocateDirect(blockSize);
        } else {
            this.out = new BufferAtATimeOutputChannel(os);
            this.buffer = ByteBuffer.allocate(blockSize);
        }
        this.blockSize = blockSize;
    }

    private void maybeFlush() throws IOException {
        if (!this.buffer.hasRemaining()) {
            writeBlock();
        }
    }

    private void writeBlock() throws IOException {
        this.buffer.flip();
        int i = this.out.write(this.buffer);
        boolean hasRemaining = this.buffer.hasRemaining();
        int i2 = this.blockSize;
        if (i != i2 || hasRemaining) {
            String msg = String.format("Failed to write %,d bytes atomically. Only wrote  %,d", Integer.valueOf(i2), Integer.valueOf(i));
            throw new IOException(msg);
        }
        this.buffer.clear();
    }

    @Override // java.io.OutputStream
    public void write(int b) throws IOException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
        this.buffer.put((byte) b);
        maybeFlush();
    }

    @Override // java.io.OutputStream
    public void write(byte[] b, int offset, int length) throws IOException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
        int off = offset;
        int len = length;
        while (len > 0) {
            int n = Math.min(len, this.buffer.remaining());
            this.buffer.put(b, off, n);
            maybeFlush();
            len -= n;
            off += n;
        }
    }

    @Override // java.nio.channels.WritableByteChannel
    public int write(ByteBuffer src) throws IOException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
        int srcRemaining = src.remaining();
        if (srcRemaining < this.buffer.remaining()) {
            this.buffer.put(src);
        } else {
            int srcLeft = srcRemaining;
            int savedLimit = src.limit();
            if (this.buffer.position() != 0) {
                int n = this.buffer.remaining();
                src.limit(src.position() + n);
                this.buffer.put(src);
                writeBlock();
                srcLeft -= n;
            }
            while (srcLeft >= this.blockSize) {
                src.limit(src.position() + this.blockSize);
                this.out.write(src);
                srcLeft -= this.blockSize;
            }
            src.limit(savedLimit);
            this.buffer.put(src);
        }
        return srcRemaining;
    }

    @Override // java.nio.channels.Channel
    public boolean isOpen() {
        if (!this.out.isOpen()) {
            this.closed.set(true);
        }
        return !this.closed.get();
    }

    public void flushBlock() throws IOException {
        if (this.buffer.position() != 0) {
            padBlock();
            writeBlock();
        }
    }

    @Override // java.io.OutputStream, java.io.Closeable, java.lang.AutoCloseable, java.nio.channels.Channel
    public void close() throws IOException {
        if (this.closed.compareAndSet(false, true)) {
            try {
                flushBlock();
            } finally {
                this.out.close();
            }
        }
    }

    private void padBlock() {
        this.buffer.order(ByteOrder.nativeOrder());
        int bytesToWrite = this.buffer.remaining();
        if (bytesToWrite > 8) {
            int align = this.buffer.position() & 7;
            if (align != 0) {
                int limit = 8 - align;
                for (int i = 0; i < limit; i++) {
                    this.buffer.put((byte) 0);
                }
                bytesToWrite -= limit;
            }
            while (bytesToWrite >= 8) {
                this.buffer.putLong(0L);
                bytesToWrite -= 8;
            }
        }
        while (this.buffer.hasRemaining()) {
            this.buffer.put((byte) 0);
        }
    }

    /* loaded from: classes.dex */
    private static class BufferAtATimeOutputChannel implements WritableByteChannel {
        private final AtomicBoolean closed;
        private final OutputStream out;

        private BufferAtATimeOutputChannel(OutputStream out) {
            this.closed = new AtomicBoolean(false);
            this.out = out;
        }

        @Override // java.nio.channels.WritableByteChannel
        public int write(ByteBuffer buffer) throws IOException {
            if (!isOpen()) {
                throw new ClosedChannelException();
            }
            if (!buffer.hasArray()) {
                throw new IllegalArgumentException("Direct buffer somehow written to BufferAtATimeOutputChannel");
            }
            try {
                int pos = buffer.position();
                int len = buffer.limit() - pos;
                this.out.write(buffer.array(), buffer.arrayOffset() + pos, len);
                buffer.position(buffer.limit());
                return len;
            } catch (IOException e) {
                try {
                    close();
                } catch (IOException e2) {
                }
                throw e;
            }
        }

        @Override // java.nio.channels.Channel
        public boolean isOpen() {
            return !this.closed.get();
        }

        @Override // java.nio.channels.Channel, java.io.Closeable, java.lang.AutoCloseable
        public void close() throws IOException {
            if (this.closed.compareAndSet(false, true)) {
                this.out.close();
            }
        }
    }
}
