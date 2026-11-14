package com.winlator.alsaserver;

import com.winlator.alsaserver.ALSAClient;
import com.winlator.xconnector.Client;
import com.winlator.xconnector.ConnectionHandler;

/* loaded from: classes.dex */
public class ALSAClientConnectionHandler implements ConnectionHandler {
    private final ALSAClient.Options options;

    public ALSAClientConnectionHandler(ALSAClient.Options options) {
        this.options = options;
    }

    @Override // com.winlator.xconnector.ConnectionHandler
    public void handleNewConnection(Client client) {
        client.createIOStreams();
        client.setTag(new ALSAClient(this.options));
    }

    @Override // com.winlator.xconnector.ConnectionHandler
    public void handleConnectionShutdown(Client client) {
        ((ALSAClient) client.getTag()).release();
    }
}
