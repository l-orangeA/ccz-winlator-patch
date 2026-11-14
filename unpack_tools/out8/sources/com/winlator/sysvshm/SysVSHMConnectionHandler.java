package com.winlator.sysvshm;

import com.winlator.xconnector.Client;
import com.winlator.xconnector.ConnectionHandler;

/* loaded from: classes.dex */
public class SysVSHMConnectionHandler implements ConnectionHandler {
    private final SysVSharedMemory sysVSharedMemory;

    public SysVSHMConnectionHandler(SysVSharedMemory sysVSharedMemory) {
        this.sysVSharedMemory = sysVSharedMemory;
    }

    @Override // com.winlator.xconnector.ConnectionHandler
    public void handleNewConnection(Client client) {
        client.createIOStreams();
        client.setTag(this.sysVSharedMemory);
    }

    @Override // com.winlator.xconnector.ConnectionHandler
    public void handleConnectionShutdown(Client client) {
    }
}
