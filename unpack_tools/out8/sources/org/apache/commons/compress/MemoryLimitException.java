package org.apache.commons.compress;

import java.io.IOException;

/* loaded from: classes.dex */
public class MemoryLimitException extends IOException {
    private final int memoryLimitInKb;
    private final long memoryNeededInKb;

    public MemoryLimitException(long memoryNeededInKb, int memoryLimitInKb, Exception e) {
        super(buildMessage(memoryNeededInKb, memoryLimitInKb), e);
        this.memoryNeededInKb = memoryNeededInKb;
        this.memoryLimitInKb = memoryLimitInKb;
    }

    private static String buildMessage(long memoryNeededInKb, int memoryLimitInKb) {
        return memoryNeededInKb + " kb of memory would be needed; limit was " + memoryLimitInKb + " kb. If the file is not corrupt, consider increasing the memory limit.";
    }
}
