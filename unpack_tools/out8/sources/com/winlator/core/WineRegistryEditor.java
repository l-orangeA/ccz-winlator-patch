package com.winlator.core;

import com.winlator.math.Mathf;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

/* loaded from: classes.dex */
public class WineRegistryEditor implements Closeable {
    private final File cloneFile;
    private final File file;
    private boolean modified = false;
    private boolean createKeyIfNotExist = true;
    private int lastParentKeyPosition = 0;
    private String lastParentKey = "";

    /* loaded from: classes.dex */
    public static class Location {
        public final int end;
        public final int offset;
        public final int start;

        public Location(int offset, int start, int end) {
            this.offset = offset;
            this.start = start;
            this.end = end;
        }

        public int length() {
            return this.end - this.start;
        }
    }

    public WineRegistryEditor(File file) {
        this.file = file;
        File createTempFile = FileUtils.createTempFile(file.getParentFile(), FileUtils.getBasename(file.getPath()));
        this.cloneFile = createTempFile;
        if (!file.isFile()) {
            try {
                createTempFile.createNewFile();
            } catch (IOException e) {
            }
        } else {
            FileUtils.copy(file, createTempFile);
        }
    }

    private static String escape(String str) {
        return str.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String unescape(String str) {
        return str.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static boolean lineHasName(String line) {
        int index;
        int index2 = line.indexOf(34);
        return (index2 == -1 || (index = line.indexOf(34, index2)) == -1 || line.indexOf(61, index) == -1) ? false : true;
    }

    @Override // java.io.Closeable, java.lang.AutoCloseable
    public void close() {
        if (this.modified && this.cloneFile.exists()) {
            this.cloneFile.renameTo(this.file);
        } else {
            this.cloneFile.delete();
        }
    }

    private void resetLastParentKeyPositionIfNeed(String newKey) {
        int lastIndex = newKey.lastIndexOf("\\");
        if (lastIndex == -1) {
            this.lastParentKeyPosition = 0;
            this.lastParentKey = "";
        } else {
            String parentKey = newKey.substring(0, lastIndex);
            if (!parentKey.equals(this.lastParentKey)) {
                this.lastParentKeyPosition = 0;
            }
            this.lastParentKey = parentKey;
        }
    }

    public void setCreateKeyIfNotExist(boolean createKeyIfNotExist) {
        this.createKeyIfNotExist = createKeyIfNotExist;
    }

    private Location createKey(String key) {
        Throwable th;
        int end;
        Throwable th2;
        this.lastParentKeyPosition = 0;
        Location location = getParentKeyLocation(key);
        boolean success = false;
        int offset = 0;
        int totalLength = 0;
        char[] buffer = new char[65536];
        File tempFile = FileUtils.createTempFile(this.file.getParentFile(), FileUtils.getBasename(this.file.getPath()));
        try {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(this.cloneFile), 65536);
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile), 65536);
                    int i = 0;
                    try {
                        if (location != null) {
                            try {
                                end = location.end + 1;
                            } catch (Throwable th3) {
                                th2 = th3;
                                try {
                                    writer.close();
                                    throw th2;
                                } catch (Throwable th4) {
                                    th2.addSuppressed(th4);
                                    throw th2;
                                }
                            }
                        } else {
                            try {
                                end = (int) this.cloneFile.length();
                            } catch (Throwable th5) {
                                th2 = th5;
                                writer.close();
                                throw th2;
                            }
                        }
                        while (i < end) {
                            int length = Math.min(buffer.length, end - i);
                            reader.read(buffer, 0, length);
                            writer.write(buffer, 0, length);
                            totalLength += length;
                            i += length;
                        }
                        offset = totalLength;
                        long currentTime = System.currentTimeMillis() + 116444736000000000L;
                        StringBuilder sb = new StringBuilder();
                        sb.append("\n[");
                        sb.append(escape(key));
                        sb.append("] ");
                        sb.append((currentTime - 116444736000000000L) / 1000);
                        Locale locale = Locale.ENGLISH;
                        Object[] objArr = new Object[2];
                        objArr[0] = Long.valueOf(currentTime >> 32);
                        try {
                            objArr[1] = Integer.valueOf((int) currentTime);
                            sb.append(String.format(locale, "\n#time=%x%08x", objArr));
                            sb.append("\n");
                            String content = sb.toString();
                            writer.write(content);
                            totalLength += content.length() - 1;
                            while (true) {
                                int length2 = reader.read(buffer);
                                if (length2 == -1) {
                                    break;
                                }
                                writer.write(buffer, 0, length2);
                            }
                            success = true;
                            writer.close();
                            reader.close();
                        } catch (Throwable th6) {
                            th2 = th6;
                            writer.close();
                            throw th2;
                        }
                    } catch (Throwable th7) {
                        th = th7;
                        try {
                            reader.close();
                            throw th;
                        } catch (Throwable th8) {
                            th.addSuppressed(th8);
                            throw th;
                        }
                    }
                } catch (Throwable th9) {
                    th = th9;
                }
            } catch (IOException e) {
            }
        } catch (IOException e2) {
        }
        if (success) {
            this.modified = true;
            tempFile.renameTo(this.cloneFile);
            return new Location(offset, totalLength, totalLength);
        }
        tempFile.delete();
        return null;
    }

    public String getStringValue(String key, String name, String fallback) {
        String value = getRawValue(key, name);
        return value != null ? value.substring(1, value.length() - 1) : fallback;
    }

    public void setStringValue(String key, String name, String value) {
        String str;
        if (value != null) {
            str = "\"" + escape(value) + "\"";
        } else {
            str = "\"\"";
        }
        setRawValue(key, name, str);
    }

    public Integer getDwordValue(String key, String name, Integer fallback) {
        String value = getRawValue(key, name);
        if (value == null) {
            return fallback;
        }
        return Integer.decode("0x" + value.substring(6));
    }

    public void setDwordValue(String key, String name, int value) {
        setRawValue(key, name, "dword:" + String.format("%08x", Integer.valueOf(value)));
    }

    public void setHexValue(String key, String name, String value) {
        int start = ((int) Mathf.roundTo(name.length(), 2.0f)) + 7;
        StringBuilder lines = new StringBuilder();
        int j = start;
        for (int i = 0; i < value.length(); i++) {
            if (i > 0 && i % 2 == 0) {
                lines.append(",");
            }
            int j2 = j + 1;
            if (j <= 56) {
                j = j2;
            } else {
                lines.append("\\\n  ");
                j = 8;
            }
            lines.append(value.charAt(i));
        }
        setRawValue(key, name, "hex:" + ((Object) lines));
    }

    public void setHexValue(String key, String name, byte[] bytes) {
        StringBuilder data = new StringBuilder();
        for (byte b : bytes) {
            data.append(String.format(Locale.ENGLISH, "%02x", Integer.valueOf(Byte.toUnsignedInt(b))));
        }
        setHexValue(key, name, data.toString());
    }

    private String getRawValue(String key, String name) {
        Location valueLocation;
        this.lastParentKeyPosition = 0;
        Location keyLocation = getKeyLocation(key);
        if (keyLocation == null || (valueLocation = getValueLocation(keyLocation, name)) == null) {
            return null;
        }
        boolean success = false;
        char[] buffer = new char[valueLocation.length()];
        try {
            BufferedReader reader = new BufferedReader(new FileReader(this.cloneFile), 65536);
            try {
                reader.skip(valueLocation.start);
                success = reader.read(buffer) == buffer.length;
                reader.close();
            } finally {
            }
        } catch (IOException e) {
        }
        if (success) {
            return unescape(new String(buffer));
        }
        return null;
    }

    private void setRawValue(String key, String name, String value) {
        BufferedReader reader;
        String str;
        resetLastParentKeyPositionIfNeed(key);
        Location keyLocation = getKeyLocation(key);
        if (keyLocation == null) {
            if (this.createKeyIfNotExist) {
                keyLocation = createKey(key);
            } else {
                return;
            }
        }
        Location valueLocation = getValueLocation(keyLocation, name);
        char[] buffer = new char[65536];
        boolean success = false;
        File tempFile = FileUtils.createTempFile(this.file.getParentFile(), FileUtils.getBasename(this.file.getPath()));
        try {
            reader = new BufferedReader(new FileReader(this.cloneFile), 65536);
        } catch (IOException e) {
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile), 65536);
            int i = 0;
            try {
                int end = valueLocation != null ? valueLocation.start : keyLocation.end;
                while (i < end) {
                    int length = Math.min(buffer.length, end - i);
                    reader.read(buffer, 0, length);
                    writer.write(buffer, 0, length);
                    i += length;
                }
                if (valueLocation == null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("\n");
                    if (name != null) {
                        str = "\"" + escape(name) + "\"";
                    } else {
                        str = "@";
                    }
                    sb.append(str);
                    sb.append("=");
                    sb.append(value);
                    writer.write(sb.toString());
                } else {
                    writer.write(value);
                    reader.skip(valueLocation.length());
                }
                while (true) {
                    int length2 = reader.read(buffer);
                    if (length2 == -1) {
                        break;
                    } else {
                        writer.write(buffer, 0, length2);
                    }
                }
                success = true;
                writer.close();
                reader.close();
                if (success) {
                    this.modified = true;
                    tempFile.renameTo(this.cloneFile);
                } else {
                    tempFile.delete();
                }
            } finally {
            }
        } finally {
        }
    }

    public void removeValue(String key, String name) {
        Location valueLocation;
        this.lastParentKeyPosition = 0;
        Location keyLocation = getKeyLocation(key);
        if (keyLocation == null || (valueLocation = getValueLocation(keyLocation, name)) == null) {
            return;
        }
        removeRegion(valueLocation);
    }

    public boolean removeKey(String key) {
        return removeKey(key, false);
    }

    public boolean removeKey(String key, boolean removeTree) {
        this.lastParentKeyPosition = 0;
        boolean removed = false;
        if (!removeTree) {
            Location location = getKeyLocation(key, false);
            return location != null && removeRegion(location);
        }
        while (true) {
            Location location2 = getKeyLocation(key, true);
            if (location2 != null) {
                if (removeRegion(location2)) {
                    removed = true;
                }
            } else {
                return removed;
            }
        }
    }

    private boolean removeRegion(Location location) {
        int i;
        char[] buffer = new char[65536];
        boolean success = false;
        File tempFile = FileUtils.createTempFile(this.file.getParentFile(), FileUtils.getBasename(this.file.getPath()));
        try {
            BufferedReader reader = new BufferedReader(new FileReader(this.cloneFile), 65536);
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile), 65536);
                int length = 0;
                int i2 = 0;
                while (true) {
                    try {
                        i = location.offset;
                        if (i2 >= i) {
                            break;
                        }
                        length = Math.min(buffer.length, i - i2);
                        reader.read(buffer, 0, length);
                        writer.write(buffer, 0, length);
                        i2 += length;
                    } finally {
                    }
                }
                boolean skipLine = length > 1 && buffer[length + (-1)] == '\n';
                reader.skip((location.end - i) + (skipLine ? 1 : 0));
                while (true) {
                    int length2 = reader.read(buffer);
                    if (length2 == -1) {
                        break;
                    }
                    writer.write(buffer, 0, length2);
                }
                success = true;
                writer.close();
                reader.close();
            } finally {
            }
        } catch (IOException e) {
        }
        if (success) {
            this.modified = true;
            tempFile.renameTo(this.cloneFile);
        } else {
            tempFile.delete();
        }
        return success;
    }

    private Location getKeyLocation(String key) {
        return getKeyLocation(key, false);
    }

    private Location getKeyLocation(String key, boolean keyAsPrefix) {
        Throwable th;
        String parentKey;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(this.cloneFile), 65536);
            try {
                try {
                    int lastIndex = key.lastIndexOf("\\");
                    if (this.lastParentKeyPosition != 0 || lastIndex == -1) {
                        parentKey = null;
                    } else {
                        parentKey = "[" + escape(key.substring(0, lastIndex));
                    }
                    int i = this.lastParentKeyPosition;
                    if (i > 0) {
                        reader.skip(i);
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("[");
                    sb.append(escape(key));
                    sb.append(!keyAsPrefix ? "]" : "");
                    String key2 = sb.toString();
                    try {
                        int totalLength = this.lastParentKeyPosition;
                        int start = -1;
                        int end = -1;
                        int emptyLines = 0;
                        int offset = 0;
                        while (true) {
                            String line = reader.readLine();
                            if (line == null) {
                                break;
                            }
                            if (start == -1) {
                                if (parentKey != null && line.startsWith(parentKey)) {
                                    this.lastParentKeyPosition = totalLength;
                                    parentKey = null;
                                }
                                if (parentKey == null && line.startsWith(key2)) {
                                    offset = totalLength - 1;
                                    start = line.length() + totalLength + 1;
                                }
                            } else {
                                if (line.startsWith("[")) {
                                    end = Math.max(-1, (totalLength - emptyLines) - 1);
                                    break;
                                }
                                emptyLines = line.isEmpty() ? emptyLines + 1 : 0;
                            }
                            totalLength += line.length() + 1;
                        }
                        if (end == -1) {
                            end = totalLength - 1;
                        }
                        Location location = start != -1 ? new Location(offset, start, end) : null;
                        reader.close();
                        return location;
                    } catch (Throwable th2) {
                        th = th2;
                        try {
                            reader.close();
                            throw th;
                        } catch (Throwable th3) {
                            th.addSuppressed(th3);
                            throw th;
                        }
                    }
                } catch (IOException e) {
                    return null;
                }
            } catch (Throwable th4) {
                th = th4;
            }
        } catch (IOException e2) {
        }
    }

    private Location getParentKeyLocation(String key) {
        String[] parts = key.split("\\\\");
        ArrayList<String> stack = new ArrayList<>(Arrays.asList(parts).subList(0, parts.length - 1));
        while (!stack.isEmpty()) {
            String currentKey = String.join("\\", stack);
            Location location = getKeyLocation(currentKey, true);
            if (location != null) {
                return location;
            }
            stack.remove(stack.size() - 1);
        }
        return null;
    }

    /* JADX WARN: Code restructure failed: missing block: B:29:0x0074, code lost:
    
        r4 = r1 - 1;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private com.winlator.core.WineRegistryEditor.Location getValueLocation(com.winlator.core.WineRegistryEditor.Location r12, java.lang.String r13) {
        /*
            r11 = this;
            int r0 = r12.start
            int r1 = r12.end
            r2 = 0
            if (r0 != r1) goto L8
            return r2
        L8:
            java.io.BufferedReader r0 = new java.io.BufferedReader     // Catch: java.io.IOException -> L98
            java.io.FileReader r1 = new java.io.FileReader     // Catch: java.io.IOException -> L98
            java.io.File r3 = r11.cloneFile     // Catch: java.io.IOException -> L98
            r1.<init>(r3)     // Catch: java.io.IOException -> L98
            r3 = 65536(0x10000, float:9.1835E-41)
            r0.<init>(r1, r3)     // Catch: java.io.IOException -> L98
            int r1 = r12.start     // Catch: java.lang.Throwable -> L8e
            long r3 = (long) r1     // Catch: java.lang.Throwable -> L8e
            r0.skip(r3)     // Catch: java.lang.Throwable -> L8e
            if (r13 == 0) goto L39
            java.lang.StringBuilder r1 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L8e
            r1.<init>()     // Catch: java.lang.Throwable -> L8e
            java.lang.String r3 = "\""
            r1.append(r3)     // Catch: java.lang.Throwable -> L8e
            java.lang.String r3 = escape(r13)     // Catch: java.lang.Throwable -> L8e
            r1.append(r3)     // Catch: java.lang.Throwable -> L8e
            java.lang.String r3 = "\"="
            r1.append(r3)     // Catch: java.lang.Throwable -> L8e
            java.lang.String r1 = r1.toString()     // Catch: java.lang.Throwable -> L8e
            goto L3b
        L39:
            java.lang.String r1 = "@="
        L3b:
            r13 = r1
            r1 = 0
            r3 = -1
            r4 = -1
            r5 = 0
        L40:
            java.lang.String r6 = r0.readLine()     // Catch: java.lang.Throwable -> L8e
            r7 = r6
            r8 = -1
            if (r6 == 0) goto L76
            int r6 = r12.length()     // Catch: java.lang.Throwable -> L8e
            if (r1 >= r6) goto L76
            if (r3 != r8) goto L5f
            boolean r6 = r7.startsWith(r13)     // Catch: java.lang.Throwable -> L8e
            if (r6 == 0) goto L6c
            int r5 = r1 + (-1)
            int r6 = r13.length()     // Catch: java.lang.Throwable -> L8e
            int r6 = r6 + r1
            r3 = r6
            goto L6c
        L5f:
            boolean r6 = r7.isEmpty()     // Catch: java.lang.Throwable -> L8e
            if (r6 != 0) goto L74
            boolean r6 = lineHasName(r7)     // Catch: java.lang.Throwable -> L8e
            if (r6 == 0) goto L6c
            goto L74
        L6c:
            int r6 = r7.length()     // Catch: java.lang.Throwable -> L8e
            int r6 = r6 + 1
            int r1 = r1 + r6
            goto L40
        L74:
            int r4 = r1 + (-1)
        L76:
            if (r4 != r8) goto L7a
            int r4 = r1 + (-1)
        L7a:
            if (r3 == r8) goto L89
            com.winlator.core.WineRegistryEditor$Location r6 = new com.winlator.core.WineRegistryEditor$Location     // Catch: java.lang.Throwable -> L8e
            int r8 = r12.start     // Catch: java.lang.Throwable -> L8e
            int r9 = r8 + r5
            int r10 = r8 + r3
            int r8 = r8 + r4
            r6.<init>(r9, r10, r8)     // Catch: java.lang.Throwable -> L8e
            goto L8a
        L89:
            r6 = r2
        L8a:
            r0.close()     // Catch: java.io.IOException -> L98
            return r6
        L8e:
            r1 = move-exception
            r0.close()     // Catch: java.lang.Throwable -> L93
            goto L97
        L93:
            r3 = move-exception
            r1.addSuppressed(r3)     // Catch: java.io.IOException -> L98
        L97:
            throw r1     // Catch: java.io.IOException -> L98
        L98:
            r0 = move-exception
            return r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.winlator.core.WineRegistryEditor.getValueLocation(com.winlator.core.WineRegistryEditor$Location, java.lang.String):com.winlator.core.WineRegistryEditor$Location");
    }
}
