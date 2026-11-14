package org.apache.commons.compress.utils;

import java.io.UnsupportedEncodingException;

/* loaded from: classes.dex */
public class ArchiveUtils {
    public static boolean matchAsciiBuffer(String expected, byte[] buffer, int offset, int length) {
        try {
            byte[] buffer1 = expected.getBytes("US-ASCII");
            return isEqual(buffer1, 0, buffer1.length, buffer, offset, length, false);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isEqual(byte[] buffer1, int offset1, int length1, byte[] buffer2, int offset2, int length2, boolean ignoreTrailingNulls) {
        int minLen = length1 < length2 ? length1 : length2;
        for (int i = 0; i < minLen; i++) {
            if (buffer1[offset1 + i] != buffer2[offset2 + i]) {
                return false;
            }
        }
        if (length1 == length2) {
            return true;
        }
        if (!ignoreTrailingNulls) {
            return false;
        }
        if (length1 > length2) {
            for (int i2 = length2; i2 < length1; i2++) {
                if (buffer1[offset1 + i2] != 0) {
                    return false;
                }
            }
        } else {
            for (int i3 = length1; i3 < length2; i3++) {
                if (buffer2[offset2 + i3] != 0) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isArrayZero(byte[] a, int size) {
        for (int i = 0; i < size; i++) {
            if (a[i] != 0) {
                return false;
            }
        }
        return true;
    }
}
