package org.tukaani.xz;

/* loaded from: classes.dex */
public class CorruptedInputException extends XZIOException {
    public CorruptedInputException() {
        super("Compressed data is corrupt");
    }

    public CorruptedInputException(String str) {
        super(str);
    }
}
