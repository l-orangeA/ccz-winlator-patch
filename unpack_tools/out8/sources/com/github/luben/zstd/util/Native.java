package com.github.luben.zstd.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/* loaded from: classes.dex */
public enum Native {
    ;

    private static final String errorMsg = "Unsupported OS/arch, cannot find " + resourceName() + " or load zstd-jni-1.5.2-3 from system libraries. Please try building from source the jar or providing libzstd-jni-1.5.2-3 in your system.";
    private static boolean loaded = false;

    private static String osName() {
        String os = System.getProperty("os.name").toLowerCase().replace(' ', '_');
        if (os.startsWith("win")) {
            return "win";
        }
        if (os.startsWith("mac")) {
            return "darwin";
        }
        return os;
    }

    private static String osArch() {
        return System.getProperty("os.arch");
    }

    private static String libExtension() {
        if (osName().contains("os_x") || osName().contains("darwin")) {
            return "dylib";
        }
        if (osName().contains("win")) {
            return "dll";
        }
        return "so";
    }

    private static String resourceName() {
        return "/" + osName() + "/" + osArch() + "/libzstd-jni-1.5.2-3." + libExtension();
    }

    public static synchronized void load() {
        synchronized (Native.class) {
            load(null);
        }
    }

    public static synchronized void load(File tempFolder) {
        synchronized (Native.class) {
            if (loaded) {
                return;
            }
            String resourceName = resourceName();
            String overridePath = System.getProperty("ZstdNativePath");
            if (overridePath != null) {
                System.load(overridePath);
                loaded = true;
                return;
            }
            try {
                Class.forName("org.osgi.framework.BundleEvent");
                System.loadLibrary("libzstd-jni-1.5.2-3");
                loaded = true;
            } catch (Throwable th) {
                InputStream is = Native.class.getResourceAsStream(resourceName);
                if (is == null) {
                    try {
                        System.loadLibrary("zstd-jni-1.5.2-3");
                        loaded = true;
                        return;
                    } catch (UnsatisfiedLinkError e) {
                        UnsatisfiedLinkError err = new UnsatisfiedLinkError(e.getMessage() + "\n" + errorMsg);
                        err.setStackTrace(e.getStackTrace());
                        throw err;
                    }
                }
                File tempLib = null;
                FileOutputStream out = null;
                try {
                    try {
                        File tempLib2 = File.createTempFile("libzstd-jni-1.5.2-3", "." + libExtension(), tempFolder);
                        tempLib2.deleteOnExit();
                        FileOutputStream out2 = new FileOutputStream(tempLib2);
                        byte[] buf = new byte[4096];
                        while (true) {
                            int read = is.read(buf);
                            if (read == -1) {
                                try {
                                    break;
                                } catch (IOException e2) {
                                }
                            } else {
                                out2.write(buf, 0, read);
                            }
                        }
                        out2.flush();
                        out2.close();
                        out2 = null;
                        try {
                            System.load(tempLib2.getAbsolutePath());
                        } catch (UnsatisfiedLinkError e3) {
                            try {
                                System.loadLibrary("zstd-jni-1.5.2-3");
                            } catch (UnsatisfiedLinkError e1) {
                                UnsatisfiedLinkError err2 = new UnsatisfiedLinkError(e3.getMessage() + "\n" + e1.getMessage() + "\n" + errorMsg);
                                err2.setStackTrace(e1.getStackTrace());
                                throw err2;
                            }
                        }
                        loaded = true;
                        try {
                            is.close();
                            if (out2 != null) {
                                out2.close();
                            }
                            if (tempLib2.exists()) {
                                tempLib2.delete();
                            }
                        } catch (IOException e4) {
                        }
                    } catch (Throwable th2) {
                        try {
                            is.close();
                            if (0 != 0) {
                                out.close();
                            }
                            if (0 != 0 && tempLib.exists()) {
                                tempLib.delete();
                            }
                        } catch (IOException e5) {
                        }
                        throw th2;
                    }
                } catch (IOException e6) {
                    ExceptionInInitializerError err3 = new ExceptionInInitializerError("Cannot unpack libzstd-jni-1.5.2-3: " + e6.getMessage());
                    err3.setStackTrace(e6.getStackTrace());
                    throw err3;
                }
            }
        }
    }
}
