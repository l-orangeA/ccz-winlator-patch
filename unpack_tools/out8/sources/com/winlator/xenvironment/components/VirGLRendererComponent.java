package com.winlator.xenvironment.components;

import androidx.annotation.Keep;
import com.winlator.renderer.GLRenderer;
import com.winlator.renderer.Texture;
import com.winlator.xconnector.Client;
import com.winlator.xconnector.ConnectionHandler;
import com.winlator.xconnector.RequestHandler;
import com.winlator.xconnector.UnixSocketConfig;
import com.winlator.xconnector.XConnectorEpoll;
import com.winlator.xenvironment.EnvironmentComponent;
import com.winlator.xserver.Drawable;
import com.winlator.xserver.XServer;
import java.io.IOException;

/* loaded from: classes.dex */
public class VirGLRendererComponent extends EnvironmentComponent implements ConnectionHandler, RequestHandler {
    private XConnectorEpoll connector;
    private long sharedEGLContextPtr;
    private final UnixSocketConfig socketConfig;
    private final XServer xServer;

    private native void destroyClient(long j);

    private native long getCurrentEGLContextPtr();

    private native long handleNewConnection(int i);

    private native void handleRequest(long j);

    static {
        System.loadLibrary("virglrenderer");
    }

    public VirGLRendererComponent(XServer xServer, UnixSocketConfig socketConfig) {
        this.xServer = xServer;
        this.socketConfig = socketConfig;
    }

    @Override // com.winlator.xenvironment.EnvironmentComponent
    public void start() {
        if (this.connector != null) {
            return;
        }
        XConnectorEpoll xConnectorEpoll = new XConnectorEpoll(this.socketConfig, this, this);
        this.connector = xConnectorEpoll;
        xConnectorEpoll.start();
    }

    @Override // com.winlator.xenvironment.EnvironmentComponent
    public void stop() {
        XConnectorEpoll xConnectorEpoll = this.connector;
        if (xConnectorEpoll != null) {
            xConnectorEpoll.stop();
            this.connector = null;
        }
    }

    @Keep
    private void killConnection(int fd) {
        XConnectorEpoll xConnectorEpoll = this.connector;
        xConnectorEpoll.killConnection(xConnectorEpoll.getClient(fd));
    }

    @Keep
    private long getSharedEGLContext() {
        long j = this.sharedEGLContextPtr;
        if (j != 0) {
            return j;
        }
        final Thread thread = Thread.currentThread();
        try {
            GLRenderer renderer = this.xServer.getRenderer();
            renderer.xServerView.queueEvent(new Runnable() { // from class: com.winlator.xenvironment.components.VirGLRendererComponent$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    VirGLRendererComponent.this.lambda$getSharedEGLContext$0(thread);
                }
            });
            synchronized (thread) {
                thread.wait();
            }
            return this.sharedEGLContextPtr;
        } catch (Exception e) {
            return 0L;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$getSharedEGLContext$0(Thread thread) {
        this.sharedEGLContextPtr = getCurrentEGLContextPtr();
        synchronized (thread) {
            thread.notify();
        }
    }

    @Override // com.winlator.xconnector.ConnectionHandler
    public void handleConnectionShutdown(Client client) {
        long clientPtr = ((Long) client.getTag()).longValue();
        destroyClient(clientPtr);
    }

    @Override // com.winlator.xconnector.ConnectionHandler
    public void handleNewConnection(Client client) {
        getSharedEGLContext();
        long clientPtr = handleNewConnection(client.clientSocket.fd);
        client.setTag(Long.valueOf(clientPtr));
    }

    @Override // com.winlator.xconnector.RequestHandler
    public boolean handleRequest(Client client) throws IOException {
        long clientPtr = ((Long) client.getTag()).longValue();
        handleRequest(clientPtr);
        return true;
    }

    @Keep
    private void flushFrontbuffer(int drawableId, int framebuffer) {
        Drawable drawable = this.xServer.drawableManager.getDrawable(drawableId);
        if (drawable == null) {
            return;
        }
        synchronized (drawable.renderLock) {
            drawable.setData(null);
            Texture texture = drawable.getTexture();
            texture.copyFromFramebuffer(framebuffer, drawable.width, drawable.height);
        }
        Runnable onDrawListener = drawable.getOnDrawListener();
        if (onDrawListener != null) {
            onDrawListener.run();
        }
    }
}
