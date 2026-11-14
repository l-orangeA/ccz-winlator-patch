package com.winlator.xserver.extensions;

import com.winlator.xconnector.XInputStream;
import com.winlator.xconnector.XOutputStream;
import com.winlator.xserver.XClient;
import com.winlator.xserver.errors.XRequestError;
import java.io.IOException;

/* loaded from: classes.dex */
public interface Extension {
    byte getFirstErrorId();

    byte getFirstEventId();

    byte getMajorOpcode();

    String getName();

    void handleRequest(XClient xClient, XInputStream xInputStream, XOutputStream xOutputStream) throws IOException, XRequestError;
}
