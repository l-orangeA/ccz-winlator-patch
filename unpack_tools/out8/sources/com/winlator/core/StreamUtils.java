package com.winlator.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/* loaded from: classes.dex */
public class StreamUtils {
    public static int skip(InputStream inStream, int bytesToSkip) {
        try {
            int bytesSkipped = (int) inStream.skip(bytesToSkip);
            if (bytesSkipped > 0 && bytesSkipped != bytesToSkip) {
                byte[] skipBuffer = new byte[1024];
                while (bytesSkipped != bytesToSkip) {
                    int bytesRead = inStream.read(skipBuffer, 0, Math.min(skipBuffer.length, bytesToSkip - bytesSkipped));
                    if (bytesRead == -1) {
                        break;
                    }
                    bytesSkipped += bytesRead;
                }
            }
            return bytesSkipped;
        } catch (IOException e) {
            return 0;
        }
    }

    public static byte[] copyToByteArray(InputStream inStream) {
        if (inStream == null) {
            return new byte[0];
        }
        ByteArrayOutputStream outStream = new ByteArrayOutputStream(65536);
        copy(inStream, outStream);
        return outStream.toByteArray();
    }

    public static boolean copy(InputStream inStream, OutputStream outStream) {
        try {
            byte[] buffer = new byte[65536];
            while (true) {
                int amountRead = inStream.read(buffer);
                if (amountRead != -1) {
                    outStream.write(buffer, 0, amountRead);
                } else {
                    outStream.flush();
                    return true;
                }
            }
        } catch (IOException e) {
            return false;
        }
    }
}
