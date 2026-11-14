package com.winlator.xconnector;

import java.io.IOException;

/* loaded from: classes.dex */
public interface RequestHandler {
    boolean handleRequest(Client client) throws IOException;
}
