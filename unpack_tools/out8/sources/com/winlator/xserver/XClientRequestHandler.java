package com.winlator.xserver;

import androidx.constraintlayout.widget.R$styleable;
import com.winlator.xconnector.Client;
import com.winlator.xconnector.RequestHandler;
import com.winlator.xconnector.XInputStream;
import com.winlator.xconnector.XOutputStream;
import com.winlator.xconnector.XStreamLock;
import com.winlator.xserver.XServer;
import com.winlator.xserver.errors.XRequestError;
import com.winlator.xserver.extensions.Extension;
import com.winlator.xserver.requests.AtomRequests;
import com.winlator.xserver.requests.CursorRequests;
import com.winlator.xserver.requests.DrawRequests;
import com.winlator.xserver.requests.ExtensionRequests;
import com.winlator.xserver.requests.FontRequests;
import com.winlator.xserver.requests.GrabRequests;
import com.winlator.xserver.requests.GraphicsContextRequests;
import com.winlator.xserver.requests.KeyboardRequests;
import com.winlator.xserver.requests.PixmapRequests;
import com.winlator.xserver.requests.SelectionRequests;
import com.winlator.xserver.requests.WindowRequests;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Objects;

/* loaded from: classes.dex */
public class XClientRequestHandler implements RequestHandler {
    @Override // com.winlator.xconnector.RequestHandler
    public boolean handleRequest(Client client) throws IOException {
        XClient xClient = (XClient) client.getTag();
        XInputStream inputStream = client.getInputStream();
        XOutputStream outputStream = client.getOutputStream();
        if (xClient.isAuthenticated()) {
            return handleNormalRequest(xClient, inputStream, outputStream);
        }
        return handleAuthRequest(xClient, inputStream, outputStream);
    }

    private void sendServerInformation(XClient client, XOutputStream outputStream) throws IOException {
        int i;
        short vendorNameLength = (short) "Elbrus Technologies, LLC".length();
        PixmapManager pixmapManager = client.xServer.pixmapManager;
        byte pixmapFormatCount = (byte) pixmapManager.supportedPixmapFormats.length;
        short additionalDataLength = (short) ((pixmapFormatCount * 2) + 8 + ((vendorNameLength + 3) / 4) + (((((pixmapManager.supportedVisuals.length * 8) + 40) + 24) + 3) / 4));
        XStreamLock lock = outputStream.lock();
        try {
            outputStream.writeByte((byte) 1);
            outputStream.writeByte((byte) 0);
            outputStream.writeShort((short) 11);
            outputStream.writeShort((short) 0);
            outputStream.writeShort(additionalDataLength);
            outputStream.writeInt(1);
            outputStream.writeInt(client.resourceIDBase.intValue());
            outputStream.writeInt(client.xServer.resourceIDs.idMask);
            outputStream.writeInt(256);
            outputStream.writeShort(vendorNameLength);
            outputStream.writeShort((short) -1);
            outputStream.writeByte((byte) 1);
            outputStream.writeByte(pixmapFormatCount);
            outputStream.writeByte((byte) 0);
            outputStream.writeByte((byte) 0);
            outputStream.writeByte((byte) 32);
            outputStream.writeByte((byte) 32);
            outputStream.writeByte((byte) 8);
            outputStream.writeByte((byte) -1);
            outputStream.writeInt(0);
            outputStream.writeString8("Elbrus Technologies, LLC");
            for (PixmapFormat pixmapFormat : client.xServer.pixmapManager.supportedPixmapFormats) {
                outputStream.writeByte(pixmapFormat.depth);
                outputStream.writeByte(pixmapFormat.bitsPerPixel);
                outputStream.writeByte(pixmapFormat.scanlinePad);
                outputStream.writePad(5);
            }
            Visual rootVisual = client.xServer.windowManager.rootWindow.getContent().visual;
            outputStream.writeInt(client.xServer.windowManager.rootWindow.id);
            outputStream.writeInt(0);
            outputStream.writeInt(16777215);
            outputStream.writeInt(0);
            outputStream.writeInt(client.xServer.windowManager.rootWindow.getAllEventMasks().getBits());
            outputStream.writeShort(client.xServer.screenInfo.width);
            outputStream.writeShort(client.xServer.screenInfo.height);
            outputStream.writeShort(client.xServer.screenInfo.getWidthInMillimeters());
            outputStream.writeShort(client.xServer.screenInfo.getHeightInMillimeters());
            outputStream.writeShort((short) 1);
            outputStream.writeShort((short) 1);
            outputStream.writeInt(rootVisual.id);
            outputStream.writeByte((byte) 0);
            outputStream.writeByte((byte) 0);
            outputStream.writeByte(rootVisual.depth);
            outputStream.writeByte((byte) client.xServer.pixmapManager.supportedVisuals.length);
            for (Visual visual : client.xServer.pixmapManager.supportedVisuals) {
                outputStream.writeByte(visual.depth);
                outputStream.writeByte((byte) 0);
                if (visual.displayable) {
                    i = 1;
                } else {
                    i = 0;
                }
                outputStream.writeShort((short) i);
                outputStream.writeInt(0);
                if (visual.displayable) {
                    outputStream.writeInt(visual.id);
                    outputStream.writeByte((byte) visual.type.ordinal());
                    outputStream.writeByte(visual.bitsPerRGBValue);
                    Objects.requireNonNull(visual);
                    outputStream.writeShort((short) 256);
                    outputStream.writeInt(visual.redMask);
                    outputStream.writeInt(visual.greenMask);
                    outputStream.writeInt(visual.blueMask);
                    outputStream.writeInt(0);
                }
            }
            if (lock != null) {
                lock.close();
            }
        } catch (Throwable th) {
            if (lock != null) {
                try {
                    lock.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    private boolean handleAuthRequest(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        if (inputStream.available() < 12) {
            return false;
        }
        byte byteOrder = inputStream.readByte();
        if (byteOrder == 66) {
            ByteOrder byteOrder2 = ByteOrder.BIG_ENDIAN;
            inputStream.setByteOrder(byteOrder2);
            outputStream.setByteOrder(byteOrder2);
        } else if (byteOrder == 108) {
            ByteOrder byteOrder3 = ByteOrder.LITTLE_ENDIAN;
            inputStream.setByteOrder(byteOrder3);
            outputStream.setByteOrder(byteOrder3);
        }
        inputStream.skip(1);
        short majorVersion = inputStream.readShort();
        if (majorVersion != 11) {
            throw new UnsupportedOperationException("Unsupported major X protocol version " + ((int) majorVersion) + ".");
        }
        inputStream.skip(2);
        int nameLength = inputStream.readShort();
        int dataLength = inputStream.readShort();
        inputStream.skip(2);
        if (nameLength > 0) {
            inputStream.readString8(nameLength);
        }
        if (dataLength > 0) {
            inputStream.readString8(dataLength);
        }
        XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER);
        try {
            sendServerInformation(client, outputStream);
            if (lock != null) {
                lock.close();
            }
            client.setAuthenticated(true);
            return true;
        } catch (Throwable th) {
            if (lock != null) {
                try {
                    lock.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    /* JADX WARN: Failed to find 'out' block for switch in B:13:0x003c. Please report as an issue. */
    private boolean handleNormalRequest(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        int requestLength;
        XLock lock;
        XLock lock2;
        if (inputStream.available() < 4) {
            return false;
        }
        byte opcode = inputStream.readByte();
        byte requestData = inputStream.readByte();
        int requestLength2 = inputStream.readUnsignedShort();
        if (requestLength2 != 0) {
            requestLength = (requestLength2 * 4) - 4;
        } else {
            if (inputStream.available() < 4) {
                return false;
            }
            requestLength = (inputStream.readInt() * 4) - 8;
        }
        if (inputStream.available() < requestLength) {
            return false;
        }
        client.generateSequenceNumber();
        client.setRequestData(requestData);
        client.setRequestLength(requestLength);
        try {
        } catch (XRequestError e) {
            client.skipRequest();
            e.sendError(client, opcode);
        }
        switch (opcode) {
            case 1:
                XLock lock3 = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.DRAWABLE_MANAGER, XServer.Lockable.INPUT_DEVICE, XServer.Lockable.CURSOR_MANAGER);
                try {
                    WindowRequests.createWindow(client, inputStream, outputStream);
                    if (lock3 != null) {
                        lock3.close();
                    }
                    return true;
                } finally {
                    if (lock3 != null) {
                        try {
                            lock3.close();
                        } catch (Throwable th) {
                            th.addSuppressed(th);
                        }
                    }
                }
            case 2:
                XLock lock4 = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.CURSOR_MANAGER);
                try {
                    WindowRequests.changeWindowAttributes(client, inputStream, outputStream);
                    if (lock4 != null) {
                        lock4.close();
                    }
                    return true;
                } finally {
                    if (lock4 != null) {
                        try {
                            lock4.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                    }
                }
            case 3:
                XLock lock5 = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER);
                try {
                    WindowRequests.getWindowAttributes(client, inputStream, outputStream);
                    if (lock5 != null) {
                        lock5.close();
                    }
                    return true;
                } finally {
                    if (lock5 != null) {
                        try {
                            lock5.close();
                        } catch (Throwable th3) {
                            th.addSuppressed(th3);
                        }
                    }
                }
            case 4:
                XLock lock6 = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.DRAWABLE_MANAGER, XServer.Lockable.INPUT_DEVICE);
                try {
                    WindowRequests.destroyWindow(client, inputStream, outputStream);
                    if (lock6 != null) {
                        lock6.close();
                    }
                    return true;
                } finally {
                    if (lock6 != null) {
                        try {
                            lock6.close();
                        } catch (Throwable th4) {
                            th.addSuppressed(th4);
                        }
                    }
                }
            case 5:
                XLock lock7 = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.DRAWABLE_MANAGER, XServer.Lockable.INPUT_DEVICE);
                try {
                    WindowRequests.destroySubWindows(client, inputStream, outputStream);
                    if (lock7 != null) {
                        lock7.close();
                    }
                    return true;
                } finally {
                    if (lock7 != null) {
                        try {
                            lock7.close();
                        } catch (Throwable th5) {
                            th.addSuppressed(th5);
                        }
                    }
                }
            case 7:
                XLock lock8 = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER);
                try {
                    WindowRequests.reparentWindow(client, inputStream, outputStream);
                    if (lock8 != null) {
                        lock8.close();
                    }
                    return true;
                } finally {
                    if (lock8 != null) {
                        try {
                            lock8.close();
                        } catch (Throwable th6) {
                            th.addSuppressed(th6);
                        }
                    }
                }
            case 8:
                XLock lock9 = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.INPUT_DEVICE);
                try {
                    WindowRequests.mapWindow(client, inputStream, outputStream);
                    if (lock9 != null) {
                        lock9.close();
                    }
                    return true;
                } finally {
                    if (lock9 != null) {
                        try {
                            lock9.close();
                        } catch (Throwable th7) {
                            th.addSuppressed(th7);
                        }
                    }
                }
            case 9:
                XLock lock10 = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.INPUT_DEVICE);
                try {
                    WindowRequests.mapSubWindows(client, inputStream, outputStream);
                    if (lock10 != null) {
                        lock10.close();
                    }
                    return true;
                } finally {
                    if (lock10 != null) {
                        try {
                            lock10.close();
                        } catch (Throwable th8) {
                            th.addSuppressed(th8);
                        }
                    }
                }
            case 10:
                XLock lock11 = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.INPUT_DEVICE);
                try {
                    WindowRequests.unmapWindow(client, inputStream, outputStream);
                    if (lock11 != null) {
                        lock11.close();
                    }
                    return true;
                } finally {
                    if (lock11 != null) {
                        try {
                            lock11.close();
                        } catch (Throwable th9) {
                            th.addSuppressed(th9);
                        }
                    }
                }
            case 12:
                XLock lock12 = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.INPUT_DEVICE);
                try {
                    WindowRequests.configureWindow(client, inputStream, outputStream);
                    if (lock12 != null) {
                        lock12.close();
                    }
                    return true;
                } finally {
                    if (lock12 != null) {
                        try {
                            lock12.close();
                        } catch (Throwable th10) {
                            th.addSuppressed(th10);
                        }
                    }
                }
            case 14:
                XLock lock13 = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.DRAWABLE_MANAGER);
                try {
                    WindowRequests.getGeometry(client, inputStream, outputStream);
                    if (lock13 != null) {
                        lock13.close();
                    }
                    return true;
                } finally {
                    if (lock13 != null) {
                        try {
                            lock13.close();
                        } catch (Throwable th11) {
                            th.addSuppressed(th11);
                        }
                    }
                }
            case 15:
                XLock lock14 = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER);
                try {
                    WindowRequests.queryTree(client, inputStream, outputStream);
                    if (lock14 != null) {
                        lock14.close();
                    }
                    return true;
                } finally {
                    if (lock14 != null) {
                        try {
                            lock14.close();
                        } catch (Throwable th12) {
                            th.addSuppressed(th12);
                        }
                    }
                }
            case 16:
                AtomRequests.internAtom(client, inputStream, outputStream);
                return true;
            case 18:
                lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER);
                try {
                    WindowRequests.changeProperty(client, inputStream, outputStream);
                    if (lock != null) {
                        lock.close();
                    }
                    return true;
                } finally {
                    if (lock != null) {
                        try {
                            lock.close();
                        } catch (Throwable th13) {
                            th.addSuppressed(th13);
                        }
                    }
                }
            case 19:
                XLock lock15 = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER);
                try {
                    WindowRequests.deleteProperty(client, inputStream, outputStream);
                    if (lock15 != null) {
                        lock15.close();
                    }
                    return true;
                } finally {
                    if (lock15 != null) {
                        try {
                            lock15.close();
                        } catch (Throwable th14) {
                            th.addSuppressed(th14);
                        }
                    }
                }
            case 20:
                XLock lock16 = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER);
                try {
                    WindowRequests.getProperty(client, inputStream, outputStream);
                    if (lock16 != null) {
                        lock16.close();
                    }
                    return true;
                } finally {
                    if (lock16 != null) {
                        try {
                            lock16.close();
                        } catch (Throwable th15) {
                            th.addSuppressed(th15);
                        }
                    }
                }
            case 22:
                XLock lock17 = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER);
                try {
                    SelectionRequests.setSelectionOwner(client, inputStream, outputStream);
                    if (lock17 != null) {
                        lock17.close();
                    }
                    return true;
                } finally {
                    if (lock17 != null) {
                        try {
                            lock17.close();
                        } catch (Throwable th16) {
                            th.addSuppressed(th16);
                        }
                    }
                }
            case 23:
                XLock lock18 = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER);
                try {
                    SelectionRequests.getSelectionOwner(client, inputStream, outputStream);
                    if (lock18 != null) {
                        lock18.close();
                    }
                    return true;
                } finally {
                    if (lock18 != null) {
                        try {
                            lock18.close();
                        } catch (Throwable th17) {
                            th.addSuppressed(th17);
                        }
                    }
                }
            case 25:
                XLock lock19 = client.xServer.lockAll();
                try {
                    WindowRequests.sendEvent(client, inputStream, outputStream);
                    if (lock19 != null) {
                        lock19.close();
                    }
                    return true;
                } finally {
                    if (lock19 != null) {
                        try {
                            lock19.close();
                        } catch (Throwable th18) {
                            th.addSuppressed(th18);
                        }
                    }
                }
            case 26:
                XLock lock20 = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.INPUT_DEVICE, XServer.Lockable.CURSOR_MANAGER);
                try {
                    GrabRequests.grabPointer(client, inputStream, outputStream);
                    if (lock20 != null) {
                        lock20.close();
                    }
                    return true;
                } finally {
                    if (lock20 != null) {
                        try {
                            lock20.close();
                        } catch (Throwable th19) {
                            th.addSuppressed(th19);
                        }
                    }
                }
            case 27:
                XLock lock21 = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.INPUT_DEVICE);
                try {
                    GrabRequests.ungrabPointer(client, inputStream, outputStream);
                    if (lock21 != null) {
                        lock21.close();
                    }
                    return true;
                } finally {
                    if (lock21 != null) {
                        try {
                            lock21.close();
                        } catch (Throwable th20) {
                            th.addSuppressed(th20);
                        }
                    }
                }
            case 38:
                XLock lock22 = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.INPUT_DEVICE);
                try {
                    WindowRequests.queryPointer(client, inputStream, outputStream);
                    if (lock22 != null) {
                        lock22.close();
                    }
                    return true;
                } finally {
                    if (lock22 != null) {
                        try {
                            lock22.close();
                        } catch (Throwable th21) {
                            th.addSuppressed(th21);
                        }
                    }
                }
            case 40:
                XLock lock23 = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER);
                try {
                    WindowRequests.translateCoordinates(client, inputStream, outputStream);
                    if (lock23 != null) {
                        lock23.close();
                    }
                    return true;
                } finally {
                    if (lock23 != null) {
                        try {
                            lock23.close();
                        } catch (Throwable th22) {
                            th.addSuppressed(th22);
                        }
                    }
                }
            case 41:
                XLock lock24 = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.INPUT_DEVICE);
                try {
                    WindowRequests.warpPointer(client, inputStream, outputStream);
                    if (lock24 != null) {
                        lock24.close();
                    }
                    return true;
                } finally {
                    if (lock24 != null) {
                        try {
                            lock24.close();
                        } catch (Throwable th23) {
                            th.addSuppressed(th23);
                        }
                    }
                }
            case 42:
                XLock lock25 = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER);
                try {
                    WindowRequests.setInputFocus(client, inputStream, outputStream);
                    if (lock25 != null) {
                        lock25.close();
                    }
                    return true;
                } finally {
                    if (lock25 != null) {
                        try {
                            lock25.close();
                        } catch (Throwable th24) {
                            th.addSuppressed(th24);
                        }
                    }
                }
            case 43:
                XLock lock26 = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER);
                try {
                    WindowRequests.getInputFocus(client, inputStream, outputStream);
                    if (lock26 != null) {
                        lock26.close();
                    }
                    return true;
                } finally {
                    if (lock26 != null) {
                        try {
                            lock26.close();
                        } catch (Throwable th25) {
                            th.addSuppressed(th25);
                        }
                    }
                }
            case 45:
                FontRequests.openFont(client, inputStream, outputStream);
                return true;
            case 49:
                FontRequests.listFonts(client, inputStream, outputStream);
                return true;
            case 53:
                XLock lock27 = client.xServer.lock(XServer.Lockable.PIXMAP_MANAGER, XServer.Lockable.DRAWABLE_MANAGER);
                try {
                    PixmapRequests.createPixmap(client, inputStream, outputStream);
                    if (lock27 != null) {
                        lock27.close();
                    }
                    return true;
                } finally {
                    if (lock27 != null) {
                        try {
                            lock27.close();
                        } catch (Throwable th26) {
                            th.addSuppressed(th26);
                        }
                    }
                }
            case 54:
                XLock lock28 = client.xServer.lock(XServer.Lockable.PIXMAP_MANAGER, XServer.Lockable.DRAWABLE_MANAGER);
                try {
                    PixmapRequests.freePixmap(client, inputStream, outputStream);
                    if (lock28 != null) {
                        lock28.close();
                    }
                    return true;
                } finally {
                    if (lock28 != null) {
                        try {
                            lock28.close();
                        } catch (Throwable th27) {
                            th.addSuppressed(th27);
                        }
                    }
                }
            case 55:
                XLock lock29 = client.xServer.lock(XServer.Lockable.PIXMAP_MANAGER, XServer.Lockable.DRAWABLE_MANAGER, XServer.Lockable.GRAPHIC_CONTEXT_MANAGER);
                try {
                    GraphicsContextRequests.createGC(client, inputStream, outputStream);
                    if (lock29 != null) {
                        lock29.close();
                    }
                    return true;
                } finally {
                    if (lock29 != null) {
                        try {
                            lock29.close();
                        } catch (Throwable th28) {
                            th.addSuppressed(th28);
                        }
                    }
                }
            case 56:
                lock2 = client.xServer.lock(XServer.Lockable.PIXMAP_MANAGER, XServer.Lockable.DRAWABLE_MANAGER, XServer.Lockable.GRAPHIC_CONTEXT_MANAGER);
                try {
                    GraphicsContextRequests.changeGC(client, inputStream, outputStream);
                    if (lock2 != null) {
                        lock2.close();
                    }
                    return true;
                } finally {
                    if (lock2 != null) {
                        try {
                            lock2.close();
                        } catch (Throwable th29) {
                            th.addSuppressed(th29);
                        }
                    }
                }
            case 59:
                client.skipRequest();
                return true;
            case 60:
                XLock lock30 = client.xServer.lock(XServer.Lockable.GRAPHIC_CONTEXT_MANAGER);
                try {
                    GraphicsContextRequests.freeGC(client, inputStream, outputStream);
                    if (lock30 != null) {
                        lock30.close();
                    }
                    return true;
                } finally {
                    if (lock30 != null) {
                        try {
                            lock30.close();
                        } catch (Throwable th30) {
                            th.addSuppressed(th30);
                        }
                    }
                }
            case 62:
                XLock lock31 = client.xServer.lock(XServer.Lockable.DRAWABLE_MANAGER, XServer.Lockable.GRAPHIC_CONTEXT_MANAGER);
                try {
                    DrawRequests.copyArea(client, inputStream, outputStream);
                    if (lock31 != null) {
                        lock31.close();
                    }
                    return true;
                } finally {
                    if (lock31 != null) {
                        try {
                            lock31.close();
                        } catch (Throwable th31) {
                            th.addSuppressed(th31);
                        }
                    }
                }
            case 65:
                XLock lock32 = client.xServer.lock(XServer.Lockable.DRAWABLE_MANAGER, XServer.Lockable.GRAPHIC_CONTEXT_MANAGER);
                try {
                    DrawRequests.polyLine(client, inputStream, outputStream);
                    if (lock32 != null) {
                        lock32.close();
                    }
                    return true;
                } finally {
                    if (lock32 != null) {
                        try {
                            lock32.close();
                        } catch (Throwable th32) {
                            th.addSuppressed(th32);
                        }
                    }
                }
            case 66:
                client.skipRequest();
                return true;
            case 67:
                client.skipRequest();
                return true;
            case 70:
                XLock lock33 = client.xServer.lock(XServer.Lockable.DRAWABLE_MANAGER, XServer.Lockable.GRAPHIC_CONTEXT_MANAGER);
                try {
                    DrawRequests.polyFillRectangle(client, inputStream, outputStream);
                    if (lock33 != null) {
                        lock33.close();
                    }
                    return true;
                } finally {
                    if (lock33 != null) {
                        try {
                            lock33.close();
                        } catch (Throwable th33) {
                            th.addSuppressed(th33);
                        }
                    }
                }
            case 72:
                XLock lock34 = client.xServer.lock(XServer.Lockable.DRAWABLE_MANAGER, XServer.Lockable.GRAPHIC_CONTEXT_MANAGER);
                try {
                    DrawRequests.putImage(client, inputStream, outputStream);
                    if (lock34 != null) {
                        lock34.close();
                    }
                    return true;
                } finally {
                    if (lock34 != null) {
                        try {
                            lock34.close();
                        } catch (Throwable th34) {
                            th.addSuppressed(th34);
                        }
                    }
                }
            case 73:
                XLock lock35 = client.xServer.lock(XServer.Lockable.PIXMAP_MANAGER, XServer.Lockable.DRAWABLE_MANAGER);
                try {
                    DrawRequests.getImage(client, inputStream, outputStream);
                    if (lock35 != null) {
                        lock35.close();
                    }
                    return true;
                } finally {
                    if (lock35 != null) {
                        try {
                            lock35.close();
                        } catch (Throwable th35) {
                            th.addSuppressed(th35);
                        }
                    }
                }
            case 78:
                client.skipRequest();
                return true;
            case 79:
                client.skipRequest();
                return true;
            case R$styleable.Constraint_layout_constraintWidth_percent /* 93 */:
                XLock lock36 = client.xServer.lock(XServer.Lockable.PIXMAP_MANAGER, XServer.Lockable.DRAWABLE_MANAGER, XServer.Lockable.CURSOR_MANAGER);
                try {
                    CursorRequests.createCursor(client, inputStream, outputStream);
                    if (lock36 != null) {
                        lock36.close();
                    }
                    return true;
                } finally {
                    if (lock36 != null) {
                        try {
                            lock36.close();
                        } catch (Throwable th36) {
                            th.addSuppressed(th36);
                        }
                    }
                }
            case R$styleable.Constraint_layout_editor_absoluteX /* 94 */:
                client.skipRequest();
                return true;
            case R$styleable.Constraint_layout_editor_absoluteY /* 95 */:
                lock2 = client.xServer.lock(XServer.Lockable.PIXMAP_MANAGER, XServer.Lockable.DRAWABLE_MANAGER, XServer.Lockable.CURSOR_MANAGER);
                try {
                    CursorRequests.freeCursor(client, inputStream, outputStream);
                    if (lock2 != null) {
                        lock2.close();
                    }
                    return true;
                } finally {
                }
            case R$styleable.Constraint_layout_goneMarginLeft /* 98 */:
                ExtensionRequests.queryExtension(client, inputStream, outputStream);
                return true;
            case R$styleable.Constraint_layout_goneMarginTop /* 101 */:
                lock = client.xServer.lock(XServer.Lockable.INPUT_DEVICE);
                try {
                    KeyboardRequests.getKeyboardMapping(client, inputStream, outputStream);
                    if (lock != null) {
                        lock.close();
                    }
                    return true;
                } finally {
                }
            case R$styleable.Constraint_pathMotionArc /* 104 */:
                client.skipRequest();
                return true;
            case R$styleable.Constraint_transitionPathRotate /* 107 */:
                client.skipRequest();
                return true;
            case R$styleable.Constraint_visibilityMode /* 108 */:
                WindowRequests.getScreenSaver(client, inputStream, outputStream);
                return true;
            case androidx.appcompat.R$styleable.AppCompatTheme_windowActionBar /* 115 */:
                client.skipRequest();
                return true;
            case androidx.appcompat.R$styleable.AppCompatTheme_windowActionModeOverlay /* 117 */:
                CursorRequests.getPointerMapping(client, inputStream, outputStream);
                return true;
            case androidx.appcompat.R$styleable.AppCompatTheme_windowFixedHeightMinor /* 119 */:
                KeyboardRequests.getModifierMapping(client, inputStream, outputStream);
                return true;
            case Byte.MAX_VALUE:
                client.skipRequest();
                return true;
            default:
                if (opcode < 0) {
                    Extension extension = client.xServer.extensions.get(opcode);
                    if (extension != null) {
                        extension.handleRequest(client, inputStream, outputStream);
                    }
                    return true;
                }
                throw new UnsupportedOperationException("Unsupported opcode " + ((int) opcode) + ".");
        }
    }
}
