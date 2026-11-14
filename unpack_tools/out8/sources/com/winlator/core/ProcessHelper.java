package com.winlator.core;

import android.os.Process;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Executors;

/* loaded from: classes.dex */
public abstract class ProcessHelper {
    private static final ArrayList<Callback<String>> debugCallbacks = new ArrayList<>();

    public static void suspendProcess(int pid) {
        Process.sendSignal(pid, 19);
    }

    public static void resumeProcess(int pid) {
        Process.sendSignal(pid, 18);
    }

    public static int exec(String command, String[] envp, File workingDir) {
        return exec(command, envp, workingDir, null);
    }

    public static int exec(String command, String[] envp, File workingDir, Callback<Integer> terminationCallback) {
        int pid = -1;
        try {
            Process process = Runtime.getRuntime().exec(splitCommand(command), envp, workingDir);
            Field pidField = process.getClass().getDeclaredField("pid");
            pidField.setAccessible(true);
            pid = pidField.getInt(process);
            pidField.setAccessible(false);
            if (!debugCallbacks.isEmpty()) {
                createDebugThread(process.getInputStream());
                createDebugThread(process.getErrorStream());
            }
            if (terminationCallback != null) {
                createWaitForThread(process, terminationCallback);
            }
        } catch (Exception e) {
        }
        return pid;
    }

    private static void createDebugThread(final InputStream inputStream) {
        Executors.newSingleThreadExecutor().execute(new Runnable() { // from class: com.winlator.core.ProcessHelper$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ProcessHelper.lambda$createDebugThread$0(inputStream);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$createDebugThread$0(InputStream inputStream) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            while (true) {
                try {
                    String line = reader.readLine();
                    if (line != null) {
                        ArrayList<Callback<String>> arrayList = debugCallbacks;
                        synchronized (arrayList) {
                            if (!arrayList.isEmpty()) {
                                Iterator<Callback<String>> it = arrayList.iterator();
                                while (it.hasNext()) {
                                    Callback<String> callback = it.next();
                                    callback.call(line);
                                }
                            }
                        }
                    } else {
                        reader.close();
                        return;
                    }
                } finally {
                }
            }
        } catch (IOException e) {
        }
    }

    private static void createWaitForThread(final Process process, final Callback<Integer> terminationCallback) {
        Executors.newSingleThreadExecutor().execute(new Runnable() { // from class: com.winlator.core.ProcessHelper$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                ProcessHelper.lambda$createWaitForThread$1(process, terminationCallback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$createWaitForThread$1(Process process, Callback terminationCallback) {
        try {
            int status = process.waitFor();
            terminationCallback.call(Integer.valueOf(status));
        } catch (InterruptedException e) {
        }
    }

    public static void removeAllDebugCallbacks() {
        ArrayList<Callback<String>> arrayList = debugCallbacks;
        synchronized (arrayList) {
            arrayList.clear();
        }
    }

    public static void addDebugCallback(Callback<String> callback) {
        ArrayList<Callback<String>> arrayList = debugCallbacks;
        synchronized (arrayList) {
            if (!arrayList.contains(callback)) {
                arrayList.add(callback);
            }
        }
    }

    public static void removeDebugCallback(Callback<String> callback) {
        ArrayList<Callback<String>> arrayList = debugCallbacks;
        synchronized (arrayList) {
            arrayList.remove(callback);
        }
    }

    public static String[] splitCommand(String command) {
        ArrayList<String> result = new ArrayList<>();
        boolean startedQuotes = false;
        String value = "";
        int i = 0;
        int count = command.length();
        while (true) {
            if (i < count) {
                char currChar = command.charAt(i);
                if (startedQuotes) {
                    if (currChar == '\"') {
                        startedQuotes = false;
                        if (!value.isEmpty()) {
                            result.add(value + '\"');
                            value = "";
                        }
                    } else {
                        value = value + currChar;
                    }
                } else if (currChar == '\"') {
                    startedQuotes = true;
                    value = value + '\"';
                } else {
                    char nextChar = i < count + (-1) ? command.charAt(i + 1) : (char) 0;
                    if (currChar == ' ' || (currChar == '\\' && nextChar == ' ')) {
                        if (currChar == '\\') {
                            value = value + ' ';
                            i++;
                        } else if (!value.isEmpty()) {
                            result.add(value);
                            value = "";
                        }
                    } else {
                        value = value + currChar;
                        if (i == count - 1) {
                            result.add(value);
                            value = "";
                        }
                    }
                }
                i++;
            } else {
                return (String[]) result.toArray(new String[0]);
            }
        }
    }

    public static int getAffinityMask(String cpuList) {
        if (cpuList == null || cpuList.isEmpty()) {
            return 0;
        }
        String[] values = cpuList.split(",");
        int affinityMask = 0;
        for (String value : values) {
            byte index = Byte.parseByte(value);
            affinityMask |= (int) Math.pow(2.0d, index);
        }
        return affinityMask;
    }

    public static int getAffinityMask(boolean[] cpuList) {
        int affinityMask = 0;
        for (int i = 0; i < cpuList.length; i++) {
            if (cpuList[i]) {
                affinityMask |= (int) Math.pow(2.0d, i);
            }
        }
        return affinityMask;
    }

    public static int getLastCoreAffinityMask() {
        int numProcessors = Runtime.getRuntime().availableProcessors();
        return (int) Math.pow(2.0d, numProcessors - 1);
    }
}
