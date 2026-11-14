package com.winlator.xconnector;

/* loaded from: classes.dex */
public interface ConnectionHandler {
    void handleConnectionShutdown(Client client);

    void handleNewConnection(Client client);
}
