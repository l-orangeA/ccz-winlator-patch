package org.apache.commons.compress.utils;

import java.io.IOException;
import java.io.InputStream;

/* loaded from: classes.dex */
public final class IOUtils {
    private static final byte[] SKIP_BUF = new byte[4096];

    public static long skip(InputStream input, long numToSkip) throws IOException {
        int read;
        while (numToSkip > 0) {
            long skipped = input.skip(numToSkip);
            if (skipped == 0) {
                break;
            }
            numToSkip -= skipped;
        }
        while (numToSkip > 0 && (read = readFully(input, SKIP_BUF, 0, (int) Math.min(numToSkip, 4096L))) >= 1) {
            numToSkip -= read;
        }
        return numToSkip - numToSkip;
    }

    public static int readFully(InputStream input, byte[] array) throws IOException {
        return readFully(input, array, 0, array.length);
    }

    public static int readFully(InputStream input, byte[] array, int offset, int len) throws IOException {
        if (len < 0 || offset < 0 || len + offset > array.length) {
            throw new IndexOutOfBoundsException();
        }
        int count = 0;
        while (count != len) {
            int x = input.read(array, offset + count, len - count);
            if (x == -1) {
                break;
            }
            count += x;
        }
        return count;
    }
}
