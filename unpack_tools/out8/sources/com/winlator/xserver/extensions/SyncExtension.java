package com.winlator.xserver.extensions;

import android.util.SparseBooleanArray;
import com.winlator.xconnector.XInputStream;
import com.winlator.xconnector.XOutputStream;
import com.winlator.xserver.XClient;
import com.winlator.xserver.errors.BadFence;
import com.winlator.xserver.errors.BadIdChoice;
import com.winlator.xserver.errors.BadImplementation;
import com.winlator.xserver.errors.BadMatch;
import com.winlator.xserver.errors.XRequestError;
import java.io.IOException;

/* loaded from: classes.dex */
public class SyncExtension implements Extension {
    private final SparseBooleanArray fences = new SparseBooleanArray();

    @Override // com.winlator.xserver.extensions.Extension
    public String getName() {
        return "SYNC";
    }

    @Override // com.winlator.xserver.extensions.Extension
    public byte getMajorOpcode() {
        return (byte) -104;
    }

    @Override // com.winlator.xserver.extensions.Extension
    public byte getFirstErrorId() {
        return Byte.MIN_VALUE;
    }

    @Override // com.winlator.xserver.extensions.Extension
    public byte getFirstEventId() {
        return (byte) 0;
    }

    public void setTriggered(int id) {
        synchronized (this.fences) {
            if (this.fences.indexOfKey(id) >= 0) {
                this.fences.put(id, true);
            }
        }
    }

    private void createFence(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        synchronized (this.fences) {
            inputStream.skip(4);
            int id = inputStream.readInt();
            if (this.fences.indexOfKey(id) >= 0) {
                throw new BadIdChoice(id);
            }
            boolean z = true;
            if (inputStream.readByte() != 1) {
                z = false;
            }
            boolean initiallyTriggered = z;
            inputStream.skip(3);
            this.fences.put(id, initiallyTriggered);
        }
    }

    private void triggerFence(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        synchronized (this.fences) {
            int id = inputStream.readInt();
            if (this.fences.indexOfKey(id) < 0) {
                throw new BadFence(id);
            }
            this.fences.put(id, true);
        }
    }

    private void resetFence(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        synchronized (this.fences) {
            int id = inputStream.readInt();
            if (this.fences.indexOfKey(id) < 0) {
                throw new BadFence(id);
            }
            boolean triggered = this.fences.get(id);
            if (!triggered) {
                throw new BadMatch();
            }
            this.fences.put(id, false);
        }
    }

    private void destroyFence(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        synchronized (this.fences) {
            int id = inputStream.readInt();
            if (this.fences.indexOfKey(id) < 0) {
                throw new BadFence(id);
            }
            this.fences.delete(id);
        }
    }

    private void awaitFence(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        synchronized (this.fences) {
            int length = client.getRemainingRequestLength();
            int[] ids = new int[length / 4];
            int i = 0;
            while (length != 0) {
                ids[i] = inputStream.readInt();
                length -= 4;
                i++;
            }
            boolean anyTriggered = false;
            do {
                for (int id : ids) {
                    if (this.fences.indexOfKey(id) < 0) {
                        throw new BadFence(id);
                    }
                    anyTriggered = this.fences.get(id);
                    if (anyTriggered) {
                        break;
                    }
                }
            } while (!anyTriggered);
        }
    }

    @Override // com.winlator.xserver.extensions.Extension
    public void handleRequest(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int opcode = client.getRequestData();
        switch (opcode) {
            case 14:
                createFence(client, inputStream, outputStream);
                return;
            case 15:
                triggerFence(client, inputStream, outputStream);
                return;
            case 16:
                resetFence(client, inputStream, outputStream);
                return;
            case 17:
                destroyFence(client, inputStream, outputStream);
                return;
            case 18:
            default:
                throw new BadImplementation();
            case 19:
                awaitFence(client, inputStream, outputStream);
                return;
        }
    }
}
