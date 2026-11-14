package com.winlator.xconnector;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/* loaded from: classes.dex */
public class Client {
    public final ClientSocket clientSocket;
    protected boolean connected;
    private final XConnectorEpoll connector;
    private XInputStream inputStream;
    private XOutputStream outputStream;
    protected Thread pollThread;
    protected int shutdownFd;
    private Object tag;

    public Client(XConnectorEpoll connector, ClientSocket clientSocket) {
        this.connector = connector;
        this.clientSocket = clientSocket;
    }

    public void createIOStreams() {
        if (this.inputStream != null || this.outputStream != null) {
            return;
        }
        this.inputStream = new XInputStream(this.clientSocket, this.connector.getInitialInputBufferCapacity());
        this.outputStream = new XOutputStream(this.clientSocket, this.connector.getInitialOutputBufferCapacity());
        XInputStream xInputStream = this.inputStream;
        ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;
        xInputStream.setByteOrder(byteOrder);
        this.outputStream.setByteOrder(byteOrder);
    }

    public XInputStream getInputStream() {
        return this.inputStream;
    }

    public XOutputStream getOutputStream() {
        return this.outputStream;
    }

    public Object getTag() {
        return this.tag;
    }

    public void setTag(Object tag) {
        this.tag = tag;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void requestShutdown() {
        try {
            ByteBuffer data = ByteBuffer.allocateDirect(8);
            data.asLongBuffer().put(1L);
            new ClientSocket(this.shutdownFd).write(data);
        } catch (IOException e) {
        }
    }
}
