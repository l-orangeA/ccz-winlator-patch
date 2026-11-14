package com.winlator.alsaserver;

import com.winlator.alsaserver.ALSAClient;
import com.winlator.sysvshm.SysVSharedMemory;
import com.winlator.xconnector.Client;
import com.winlator.xconnector.RequestHandler;
import com.winlator.xconnector.XConnectorEpoll;
import com.winlator.xconnector.XInputStream;
import com.winlator.xconnector.XOutputStream;
import com.winlator.xconnector.XStreamLock;
import java.io.IOException;
import java.nio.ByteBuffer;

/* loaded from: classes.dex */
public class ALSARequestHandler implements RequestHandler {
    private int maxSHMemoryId = 0;

    @Override // com.winlator.xconnector.RequestHandler
    public boolean handleRequest(Client client) throws IOException {
        XStreamLock lock;
        ALSAClient alsaClient = (ALSAClient) client.getTag();
        XInputStream inputStream = client.getInputStream();
        XOutputStream outputStream = client.getOutputStream();
        if (inputStream.available() < 5) {
            return false;
        }
        byte requestCode = inputStream.readByte();
        int requestLength = inputStream.readInt();
        switch (requestCode) {
            case 0:
                alsaClient.release();
                return true;
            case 1:
                alsaClient.start();
                return true;
            case 2:
                alsaClient.stop();
                return true;
            case 3:
                alsaClient.pause();
                return true;
            case 4:
                if (inputStream.available() < requestLength) {
                    return false;
                }
                alsaClient.setChannels(inputStream.readByte());
                alsaClient.setDataType(ALSAClient.DataType.values()[inputStream.readByte()]);
                alsaClient.setSampleRate(inputStream.readInt());
                alsaClient.setBufferSize(inputStream.readInt());
                alsaClient.prepare();
                createSharedMemory(alsaClient, outputStream);
                return true;
            case 5:
                ByteBuffer sharedBuffer = alsaClient.getSharedBuffer();
                if (sharedBuffer != null) {
                    sharedBuffer.limit(requestLength);
                    alsaClient.writeDataToTrack(sharedBuffer);
                    return true;
                }
                if (inputStream.available() < requestLength) {
                    return false;
                }
                alsaClient.writeDataToTrack(inputStream.readByteBuffer(requestLength));
                return true;
            case 6:
                alsaClient.drain();
                return true;
            case 7:
                lock = outputStream.lock();
                try {
                    outputStream.writeInt(alsaClient.pointer());
                    if (lock != null) {
                        lock.close();
                        return true;
                    }
                    return true;
                } finally {
                }
            case 8:
                byte channels = inputStream.readByte();
                ALSAClient.DataType dataType = ALSAClient.DataType.values()[inputStream.readByte()];
                int sampleRate = inputStream.readInt();
                int minBufferSize = ALSAClient.latencyMillisToBufferSize(alsaClient.options.latencyMillis, channels, dataType, sampleRate);
                lock = outputStream.lock();
                try {
                    outputStream.writeInt(minBufferSize);
                    if (lock != null) {
                        lock.close();
                        return true;
                    }
                    return true;
                } finally {
                }
            default:
                return true;
        }
    }

    private void createSharedMemory(ALSAClient alsaClient, XOutputStream outputStream) throws IOException {
        ByteBuffer buffer;
        int size = alsaClient.getBufferSizeInBytes();
        StringBuilder sb = new StringBuilder();
        sb.append("alsa-shm");
        int i = this.maxSHMemoryId + 1;
        this.maxSHMemoryId = i;
        sb.append(i);
        int fd = SysVSharedMemory.createMemoryFd(sb.toString(), size);
        if (fd >= 0 && (buffer = SysVSharedMemory.mapSHMSegment(fd, size, 0, true)) != null) {
            alsaClient.setSharedBuffer(buffer);
        }
        try {
            XStreamLock lock = outputStream.lock();
            try {
                outputStream.writeByte((byte) 0);
                outputStream.setAncillaryFd(fd);
                if (lock != null) {
                    lock.close();
                }
            } finally {
            }
        } finally {
            if (fd >= 0) {
                XConnectorEpoll.closeFd(fd);
            }
        }
    }
}
