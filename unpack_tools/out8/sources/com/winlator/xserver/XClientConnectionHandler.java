package com.winlator.xserver;

import com.winlator.xconnector.Client;
import com.winlator.xconnector.ConnectionHandler;

/* loaded from: classes.dex */
public class XClientConnectionHandler implements ConnectionHandler {
    private final XServer xServer;

    public XClientConnectionHandler(XServer xServer) {
        this.xServer = xServer;
    }

    @Override // com.winlator.xconnector.ConnectionHandler
    public void handleNewConnection(Client client) {
        client.createIOStreams();
        client.setTag(new XClient(this.xServer, client.getInputStream(), client.getOutputStream()));
    }

    @Override // com.winlator.xconnector.ConnectionHandler
    public void handleConnectionShutdown(Client client) {
        ((XClient) client.getTag()).freeResources();
    }
}
