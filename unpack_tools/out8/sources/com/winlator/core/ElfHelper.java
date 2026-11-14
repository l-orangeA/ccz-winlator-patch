package com.winlator.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/* loaded from: classes.dex */
public abstract class ElfHelper {
    private static int getEIClass(File binFile) {
        InputStream inStream;
        byte[] header;
        try {
            inStream = new FileInputStream(binFile);
            try {
                header = new byte[52];
                inStream.read(header);
            } finally {
            }
        } catch (IOException e) {
        }
        if (header[0] == Byte.MAX_VALUE && header[1] == 69 && header[2] == 76 && header[3] == 70) {
            byte b = header[4];
            inStream.close();
            return b;
        }
        inStream.close();
        return 0;
    }

    public static boolean is64Bit(File binFile) {
        return getEIClass(binFile) == 2;
    }
}
