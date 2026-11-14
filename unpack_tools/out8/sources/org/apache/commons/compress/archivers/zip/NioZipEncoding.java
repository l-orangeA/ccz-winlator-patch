package org.apache.commons.compress.archivers.zip;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

/* loaded from: classes.dex */
class NioZipEncoding implements ZipEncoding {
    private final Charset charset;
    private final boolean useReplacement;
    private static final byte[] REPLACEMENT_BYTES = {63};
    private static final String REPLACEMENT_STRING = String.valueOf('?');
    private static final char[] HEX_CHARS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    /* JADX INFO: Access modifiers changed from: package-private */
    public NioZipEncoding(Charset charset, boolean useReplacement) {
        this.charset = charset;
        this.useReplacement = useReplacement;
    }

    @Override // org.apache.commons.compress.archivers.zip.ZipEncoding
    public boolean canEncode(String name) {
        CharsetEncoder enc = newEncoder();
        return enc.canEncode(name);
    }

    @Override // org.apache.commons.compress.archivers.zip.ZipEncoding
    public ByteBuffer encode(String name) {
        CharsetEncoder enc = newEncoder();
        CharBuffer cb = CharBuffer.wrap(name);
        CharBuffer tmp = null;
        ByteBuffer out = ByteBuffer.allocate(estimateInitialBufferSize(enc, cb.remaining()));
        while (cb.hasRemaining()) {
            CoderResult res = enc.encode(cb, out, false);
            if (res.isUnmappable() || res.isMalformed()) {
                int spaceForSurrogate = estimateIncrementalEncodingSize(enc, res.length() * 6);
                if (spaceForSurrogate > out.remaining()) {
                    int charCount = 0;
                    for (int i = cb.position(); i < cb.limit(); i++) {
                        charCount += !enc.canEncode(cb.get(i)) ? 6 : 1;
                    }
                    int totalExtraSpace = estimateIncrementalEncodingSize(enc, charCount);
                    out = ZipEncodingHelper.growBufferBy(out, totalExtraSpace - out.remaining());
                }
                if (tmp == null) {
                    tmp = CharBuffer.allocate(6);
                }
                for (int i2 = 0; i2 < res.length(); i2++) {
                    out = encodeFully(enc, encodeSurrogate(tmp, cb.get()), out);
                }
            } else if (!res.isOverflow()) {
                if (res.isUnderflow() || res.isError()) {
                    break;
                }
            } else {
                int increment = estimateIncrementalEncodingSize(enc, cb.remaining());
                out = ZipEncodingHelper.growBufferBy(out, increment);
            }
        }
        enc.encode(cb, out, true);
        out.limit(out.position());
        out.rewind();
        return out;
    }

    @Override // org.apache.commons.compress.archivers.zip.ZipEncoding
    public String decode(byte[] data) throws IOException {
        return newDecoder().decode(ByteBuffer.wrap(data)).toString();
    }

    private static ByteBuffer encodeFully(CharsetEncoder enc, CharBuffer cb, ByteBuffer out) {
        ByteBuffer o = out;
        while (cb.hasRemaining()) {
            CoderResult result = enc.encode(cb, o, false);
            if (result.isOverflow()) {
                int increment = estimateIncrementalEncodingSize(enc, cb.remaining());
                o = ZipEncodingHelper.growBufferBy(o, increment);
            }
        }
        return o;
    }

    private static CharBuffer encodeSurrogate(CharBuffer cb, char c) {
        cb.position(0).limit(6);
        cb.put('%');
        cb.put('U');
        char[] cArr = HEX_CHARS;
        cb.put(cArr[(c >> '\f') & 15]);
        cb.put(cArr[(c >> '\b') & 15]);
        cb.put(cArr[(c >> 4) & 15]);
        cb.put(cArr[c & 15]);
        cb.flip();
        return cb;
    }

    private CharsetEncoder newEncoder() {
        if (this.useReplacement) {
            return this.charset.newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE).replaceWith(REPLACEMENT_BYTES);
        }
        return this.charset.newEncoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
    }

    private CharsetDecoder newDecoder() {
        if (!this.useReplacement) {
            return this.charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        }
        return this.charset.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE).replaceWith(REPLACEMENT_STRING);
    }

    private static int estimateInitialBufferSize(CharsetEncoder enc, int charChount) {
        float first = enc.maxBytesPerChar();
        float rest = (charChount - 1) * enc.averageBytesPerChar();
        return (int) Math.ceil(first + rest);
    }

    private static int estimateIncrementalEncodingSize(CharsetEncoder enc, int charCount) {
        return (int) Math.ceil(charCount * enc.averageBytesPerChar());
    }
}
