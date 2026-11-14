package com.winlator.winhandler;

import android.view.KeyEvent;
import android.view.MotionEvent;
import com.winlator.XServerDisplayActivity;
import com.winlator.core.StringUtils;
import com.winlator.inputcontrols.ControlsProfile;
import com.winlator.inputcontrols.ExternalController;
import com.winlator.xserver.XServer;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

/* loaded from: classes.dex */
public class WinHandler {
    private final ArrayDeque<Runnable> actions;
    private final XServerDisplayActivity activity;
    private ExternalController currentController;
    private byte dinputMapperType;
    private final List<Integer> gamepadClients;
    private boolean initReceived;
    private InetAddress localhost;
    private OnGetProcessInfoListener onGetProcessInfoListener;
    private PreferredInputApi preferredInputApi;
    private final ByteBuffer receiveData;
    private final DatagramPacket receivePacket;
    private boolean running;
    private final ByteBuffer sendData;
    private final DatagramPacket sendPacket;
    private DatagramSocket socket;
    private final ArrayList<Integer> xinputProcesses;

    /* loaded from: classes.dex */
    public enum PreferredInputApi {
        AUTO,
        DINPUT,
        XINPUT,
        BOTH
    }

    public WinHandler(XServerDisplayActivity activity) {
        ByteBuffer allocate = ByteBuffer.allocate(64);
        ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;
        ByteBuffer order = allocate.order(byteOrder);
        this.sendData = order;
        ByteBuffer order2 = ByteBuffer.allocate(64).order(byteOrder);
        this.receiveData = order2;
        this.sendPacket = new DatagramPacket(order.array(), 64);
        this.receivePacket = new DatagramPacket(order2.array(), 64);
        this.actions = new ArrayDeque<>();
        this.initReceived = false;
        this.running = false;
        this.dinputMapperType = (byte) 1;
        this.preferredInputApi = PreferredInputApi.BOTH;
        this.gamepadClients = new CopyOnWriteArrayList();
        this.xinputProcesses = new ArrayList<>();
        this.activity = activity;
    }

    private boolean sendPacket(int port) {
        try {
            int size = this.sendData.position();
            if (size == 0) {
                return false;
            }
            this.sendPacket.setAddress(this.localhost);
            this.sendPacket.setPort(port);
            this.socket.send(this.sendPacket);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void exec(String command) {
        String command2 = command.trim();
        if (command2.isEmpty()) {
            return;
        }
        String[] cmdList = command2.split(" ", 2);
        final String filename = cmdList[0];
        final String parameters = cmdList.length > 1 ? cmdList[1] : "";
        addAction(new Runnable() { // from class: com.winlator.winhandler.WinHandler$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                WinHandler.this.lambda$exec$0(filename, parameters);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$exec$0(String filename, String parameters) {
        byte[] filenameBytes = filename.getBytes();
        byte[] parametersBytes = parameters.getBytes();
        this.sendData.rewind();
        this.sendData.put((byte) 2);
        this.sendData.putInt(filenameBytes.length + parametersBytes.length + 8);
        this.sendData.putInt(filenameBytes.length);
        this.sendData.putInt(parametersBytes.length);
        this.sendData.put(filenameBytes);
        this.sendData.put(parametersBytes);
        sendPacket(7946);
    }

    public void killProcess(final String processName) {
        addAction(new Runnable() { // from class: com.winlator.winhandler.WinHandler$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                WinHandler.this.lambda$killProcess$1(processName);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$killProcess$1(String processName) {
        this.sendData.rewind();
        this.sendData.put((byte) 3);
        byte[] bytes = processName.getBytes();
        this.sendData.putInt(bytes.length);
        this.sendData.put(bytes);
        sendPacket(7946);
    }

    public void listProcesses() {
        addAction(new Runnable() { // from class: com.winlator.winhandler.WinHandler$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                WinHandler.this.lambda$listProcesses$2();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$listProcesses$2() {
        OnGetProcessInfoListener onGetProcessInfoListener;
        this.sendData.rewind();
        this.sendData.put((byte) 4);
        this.sendData.putInt(0);
        if (!sendPacket(7946) && (onGetProcessInfoListener = this.onGetProcessInfoListener) != null) {
            onGetProcessInfoListener.onGetProcessInfo(0, 0, null);
        }
    }

    public void setProcessAffinity(final String processName, final int affinityMask) {
        addAction(new Runnable() { // from class: com.winlator.winhandler.WinHandler$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                WinHandler.this.lambda$setProcessAffinity$3(processName, affinityMask);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setProcessAffinity$3(String processName, int affinityMask) {
        byte[] bytes = processName.getBytes();
        this.sendData.rewind();
        this.sendData.put((byte) 6);
        this.sendData.putInt(bytes.length + 9);
        this.sendData.putInt(0);
        this.sendData.putInt(affinityMask);
        this.sendData.put((byte) bytes.length);
        this.sendData.put(bytes);
        sendPacket(7946);
    }

    public void setProcessAffinity(final int pid, final int affinityMask) {
        addAction(new Runnable() { // from class: com.winlator.winhandler.WinHandler$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                WinHandler.this.lambda$setProcessAffinity$4(pid, affinityMask);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setProcessAffinity$4(int pid, int affinityMask) {
        this.sendData.rewind();
        this.sendData.put((byte) 6);
        this.sendData.putInt(9);
        this.sendData.putInt(pid);
        this.sendData.putInt(affinityMask);
        this.sendData.put((byte) 0);
        sendPacket(7946);
    }

    public void mouseEvent(final int flags, final int dx, final int dy, final int wheelDelta) {
        if (this.initReceived) {
            addAction(new Runnable() { // from class: com.winlator.winhandler.WinHandler$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    WinHandler.this.lambda$mouseEvent$5(flags, dx, dy, wheelDelta);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$mouseEvent$5(int flags, int dx, int dy, int wheelDelta) {
        this.sendData.rewind();
        this.sendData.put((byte) 7);
        this.sendData.putInt(10);
        this.sendData.putInt(flags);
        this.sendData.putShort((short) dx);
        this.sendData.putShort((short) dy);
        this.sendData.putShort((short) wheelDelta);
        this.sendData.put((byte) ((flags & 1) != 0 ? 1 : 0));
        sendPacket(7946);
    }

    public void bringToFront(String processName) {
        bringToFront(processName, 0L);
    }

    public void bringToFront(final String processName, final long handle) {
        addAction(new Runnable() { // from class: com.winlator.winhandler.WinHandler$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                WinHandler.this.lambda$bringToFront$7(processName, handle);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$bringToFront$7(String processName, long handle) {
        this.sendData.rewind();
        this.sendData.put((byte) 12);
        byte[] bytes = processName.getBytes();
        this.sendData.putInt(bytes.length);
        this.sendData.put(bytes);
        this.sendData.putLong(handle);
        sendPacket(7946);
    }

    private void addAction(Runnable action) {
        synchronized (this.actions) {
            this.actions.add(action);
            this.actions.notify();
        }
    }

    public void setOnGetProcessInfoListener(OnGetProcessInfoListener onGetProcessInfoListener) {
        synchronized (this.actions) {
            this.onGetProcessInfoListener = onGetProcessInfoListener;
        }
    }

    private void startSendThread() {
        Executors.newSingleThreadExecutor().execute(new Runnable() { // from class: com.winlator.winhandler.WinHandler$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                WinHandler.this.lambda$startSendThread$8();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$startSendThread$8() {
        while (this.running) {
            synchronized (this.actions) {
                while (this.initReceived && !this.actions.isEmpty()) {
                    this.actions.poll().run();
                }
                try {
                    this.actions.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public void stop() {
        this.running = false;
        DatagramSocket datagramSocket = this.socket;
        if (datagramSocket != null) {
            datagramSocket.close();
            this.socket = null;
        }
        synchronized (this.actions) {
            this.actions.notify();
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private void handleRequest(byte requestCode, final int port) {
        boolean enabled;
        ExternalController externalController;
        switch (requestCode) {
            case 1:
                this.initReceived = true;
                synchronized (this.actions) {
                    this.actions.notify();
                }
                return;
            case 5:
                if (this.onGetProcessInfoListener == null) {
                    return;
                }
                ByteBuffer byteBuffer = this.receiveData;
                byteBuffer.position(byteBuffer.position() + 4);
                int numProcesses = this.receiveData.getShort();
                int index = this.receiveData.getShort();
                int pid = this.receiveData.getInt();
                long memoryUsage = this.receiveData.getLong();
                int affinityMask = this.receiveData.getInt();
                boolean wow64Process = this.receiveData.get() == 1;
                byte[] bytes = new byte[32];
                this.receiveData.get(bytes);
                String name = StringUtils.fromANSIString(bytes);
                this.onGetProcessInfoListener.onGetProcessInfo(index, numProcesses, new ProcessInfo(pid, name, memoryUsage, affinityMask, wow64Process));
                return;
            case 8:
                boolean isXInput = this.receiveData.get() == 1;
                boolean notify = this.receiveData.get() == 1;
                final ControlsProfile profile = this.activity.getInputControlsView().getProfile();
                final boolean useVirtualGamepad = profile != null && profile.isVirtualGamepad();
                int processId = this.receiveData.getInt();
                if (!useVirtualGamepad && ((externalController = this.currentController) == null || !externalController.isConnected())) {
                    this.currentController = ExternalController.getController(0);
                }
                boolean enabled2 = this.currentController != null || useVirtualGamepad;
                if (enabled2) {
                    switch (AnonymousClass1.$SwitchMap$com$winlator$winhandler$WinHandler$PreferredInputApi[this.preferredInputApi.ordinal()]) {
                        case 1:
                            boolean hasXInputProcess = this.xinputProcesses.contains(Integer.valueOf(processId));
                            if (isXInput) {
                                if (!hasXInputProcess) {
                                    this.xinputProcesses.add(Integer.valueOf(processId));
                                    break;
                                }
                            } else if (hasXInputProcess) {
                                enabled = false;
                                break;
                            }
                            break;
                        case 2:
                            if (isXInput) {
                                enabled = false;
                                break;
                            }
                            break;
                        case 3:
                            if (!isXInput) {
                                enabled = false;
                                break;
                            }
                            break;
                    }
                    if (!enabled && notify) {
                        if (!this.gamepadClients.contains(Integer.valueOf(port))) {
                            this.gamepadClients.add(Integer.valueOf(port));
                        }
                    } else {
                        this.gamepadClients.remove(Integer.valueOf(port));
                    }
                    final boolean finalEnabled = enabled;
                    addAction(new Runnable() { // from class: com.winlator.winhandler.WinHandler$$ExternalSyntheticLambda10
                        @Override // java.lang.Runnable
                        public final void run() {
                            WinHandler.this.lambda$handleRequest$9(finalEnabled, useVirtualGamepad, profile, port);
                        }
                    });
                    return;
                }
                enabled = enabled2;
                if (!enabled) {
                }
                this.gamepadClients.remove(Integer.valueOf(port));
                final boolean finalEnabled2 = enabled;
                addAction(new Runnable() { // from class: com.winlator.winhandler.WinHandler$$ExternalSyntheticLambda10
                    @Override // java.lang.Runnable
                    public final void run() {
                        WinHandler.this.lambda$handleRequest$9(finalEnabled2, useVirtualGamepad, profile, port);
                    }
                });
                return;
            case 9:
                final int gamepadId = this.receiveData.getInt();
                final ControlsProfile profile2 = this.activity.getInputControlsView().getProfile();
                final boolean useVirtualGamepad2 = profile2 != null && profile2.isVirtualGamepad();
                ExternalController externalController2 = this.currentController;
                final boolean enabled3 = externalController2 != null || useVirtualGamepad2;
                if (externalController2 != null && externalController2.getDeviceId() != gamepadId) {
                    this.currentController = null;
                }
                addAction(new Runnable() { // from class: com.winlator.winhandler.WinHandler$$ExternalSyntheticLambda9
                    @Override // java.lang.Runnable
                    public final void run() {
                        WinHandler.this.lambda$handleRequest$10(enabled3, gamepadId, useVirtualGamepad2, profile2, port);
                    }
                });
                return;
            case 10:
                this.currentController = null;
                this.gamepadClients.clear();
                this.xinputProcesses.clear();
                return;
            case 13:
                short x = this.receiveData.getShort();
                short y = this.receiveData.getShort();
                XServer xServer = this.activity.getXServer();
                xServer.pointer.setX(x);
                xServer.pointer.setY(y);
                this.activity.getXServerView().requestRender();
                return;
            default:
                return;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.winlator.winhandler.WinHandler$1, reason: invalid class name */
    /* loaded from: classes.dex */
    public static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$winlator$winhandler$WinHandler$PreferredInputApi;

        static {
            int[] iArr = new int[PreferredInputApi.values().length];
            $SwitchMap$com$winlator$winhandler$WinHandler$PreferredInputApi = iArr;
            try {
                iArr[PreferredInputApi.AUTO.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$winlator$winhandler$WinHandler$PreferredInputApi[PreferredInputApi.DINPUT.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$winlator$winhandler$WinHandler$PreferredInputApi[PreferredInputApi.XINPUT.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$handleRequest$9(boolean finalEnabled, boolean useVirtualGamepad, ControlsProfile profile, int port) {
        this.sendData.rewind();
        this.sendData.put((byte) 8);
        if (finalEnabled) {
            this.sendData.putInt(!useVirtualGamepad ? this.currentController.getDeviceId() : profile.id);
            this.sendData.put(this.dinputMapperType);
            byte[] bytes = (useVirtualGamepad ? profile.getName() : this.currentController.getName()).getBytes();
            this.sendData.putInt(bytes.length);
            this.sendData.put(bytes);
        } else {
            this.sendData.putInt(0);
            this.sendData.put((byte) 0);
            this.sendData.putInt(0);
        }
        sendPacket(port);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$handleRequest$10(boolean z, int i, boolean z2, ControlsProfile controlsProfile, int i2) {
        this.sendData.rewind();
        this.sendData.put((byte) 9);
        this.sendData.put(z ? (byte) 1 : (byte) 0);
        if (z) {
            this.sendData.putInt(i);
            if (z2) {
                controlsProfile.getGamepadState().writeTo(this.sendData);
            } else {
                this.currentController.state.writeTo(this.sendData);
            }
        }
        sendPacket(i2);
    }

    public void start() {
        try {
            this.localhost = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            try {
                this.localhost = InetAddress.getByName("127.0.0.1");
            } catch (UnknownHostException e2) {
            }
        }
        this.running = true;
        startSendThread();
        Executors.newSingleThreadExecutor().execute(new Runnable() { // from class: com.winlator.winhandler.WinHandler$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                WinHandler.this.lambda$start$11();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$start$11() {
        try {
            DatagramSocket datagramSocket = new DatagramSocket((SocketAddress) null);
            this.socket = datagramSocket;
            datagramSocket.setReuseAddress(true);
            this.socket.bind(new InetSocketAddress((InetAddress) null, 7947));
            while (this.running) {
                this.socket.receive(this.receivePacket);
                synchronized (this.actions) {
                    this.receiveData.rewind();
                    byte requestCode = this.receiveData.get();
                    handleRequest(requestCode, this.receivePacket.getPort());
                }
            }
        } catch (IOException e) {
        }
    }

    public void sendGamepadState() {
        if (!this.initReceived || this.gamepadClients.isEmpty()) {
            return;
        }
        final ControlsProfile profile = this.activity.getInputControlsView().getProfile();
        final boolean useVirtualGamepad = profile != null && profile.isVirtualGamepad();
        final boolean enabled = this.currentController != null || useVirtualGamepad;
        Iterator<Integer> it = this.gamepadClients.iterator();
        while (it.hasNext()) {
            final int port = it.next().intValue();
            addAction(new Runnable() { // from class: com.winlator.winhandler.WinHandler$$ExternalSyntheticLambda11
                @Override // java.lang.Runnable
                public final void run() {
                    WinHandler.this.lambda$sendGamepadState$12(enabled, useVirtualGamepad, profile, port);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$sendGamepadState$12(boolean z, boolean z2, ControlsProfile controlsProfile, int i) {
        this.sendData.rewind();
        this.sendData.put((byte) 9);
        this.sendData.put(z ? (byte) 1 : (byte) 0);
        if (z) {
            this.sendData.putInt(!z2 ? this.currentController.getDeviceId() : controlsProfile.id);
            if (z2) {
                controlsProfile.getGamepadState().writeTo(this.sendData);
            } else {
                this.currentController.state.writeTo(this.sendData);
            }
        }
        sendPacket(i);
    }

    public boolean onGenericMotionEvent(MotionEvent event) {
        boolean handled = false;
        ExternalController externalController = this.currentController;
        if (externalController != null && externalController.getDeviceId() == event.getDeviceId() && (handled = this.currentController.updateStateFromMotionEvent(event))) {
            sendGamepadState();
        }
        return handled;
    }

    public boolean onKeyEvent(KeyEvent event) {
        boolean handled = false;
        ExternalController externalController = this.currentController;
        if (externalController != null && externalController.getDeviceId() == event.getDeviceId() && event.getRepeatCount() == 0) {
            int action = event.getAction();
            if (action == 0) {
                handled = this.currentController.updateStateFromKeyEvent(event);
            } else if (action == 1) {
                handled = this.currentController.updateStateFromKeyEvent(event);
            }
            if (handled) {
                sendGamepadState();
            }
        }
        return handled;
    }

    public void setDInputMapperType(byte dinputMapperType) {
        this.dinputMapperType = dinputMapperType;
    }

    public void setPreferredInputApi(PreferredInputApi preferredInputApi) {
        this.preferredInputApi = preferredInputApi;
    }

    public ExternalController getCurrentController() {
        return this.currentController;
    }
}
