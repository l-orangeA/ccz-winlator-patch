package com.winlator.xenvironment.components;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Process;
import androidx.preference.PreferenceManager;
import com.winlator.box64.Box64PresetManager;
import com.winlator.core.Callback;
import com.winlator.core.EnvVars;
import com.winlator.core.ProcessHelper;
import com.winlator.core.TarCompressorUtils;
import com.winlator.xenvironment.EnvironmentComponent;
import com.winlator.xenvironment.ImageFs;
import java.io.File;

/* loaded from: classes.dex */
public class GuestProgramLauncherComponent extends EnvironmentComponent {
    private String box64Preset = "COMPATIBILITY";
    private EnvVars envVars;
    private String guestExecutable;
    private Callback<Integer> terminationCallback;
    private static int pid = -1;
    private static final Object lock = new Object();

    @Override // com.winlator.xenvironment.EnvironmentComponent
    public void start() {
        synchronized (lock) {
            stop();
            extractBox64File();
            pid = execGuestProgram();
        }
    }

    @Override // com.winlator.xenvironment.EnvironmentComponent
    public void stop() {
        synchronized (lock) {
            int i = pid;
            if (i != -1) {
                Process.killProcess(i);
                pid = -1;
            }
        }
    }

    public void setTerminationCallback(Callback<Integer> terminationCallback) {
        this.terminationCallback = terminationCallback;
    }

    public void setGuestExecutable(String guestExecutable) {
        this.guestExecutable = guestExecutable;
    }

    public void setEnvVars(EnvVars envVars) {
        this.envVars = envVars;
    }

    public void setBox64Preset(String box64Preset) {
        this.box64Preset = box64Preset;
    }

    private int execGuestProgram() {
        Context context = this.environment.getContext();
        ImageFs imageFs = this.environment.getImageFs();
        File rootDir = imageFs.getRootDir();
        String nativeLibraryDir = context.getApplicationInfo().nativeLibraryDir;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean enableBox64Logs = preferences.getBoolean("enable_box64_logs", false);
        EnvVars envVars = new EnvVars();
        addBox64EnvVars(envVars, enableBox64Logs);
        envVars.put("HOME", "/home/xuser");
        envVars.put("USER", "xuser");
        envVars.put("TMPDIR", "/tmp");
        envVars.put("LC_ALL", "en_US.utf8");
        envVars.put("DISPLAY", ":0");
        envVars.put("PATH", imageFs.getWinePath() + "/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin");
        envVars.put("LD_LIBRARY_PATH", "/usr/lib/aarch64-linux-gnu");
        envVars.put("ANDROID_SYSVSHM_SERVER", "/tmp/.sysvshm/SM0");
        if (new File(imageFs.getLibDir(), "libandroid-sysvshm.so").exists()) {
            envVars.put("LD_PRELOAD", "libandroid-sysvshm.so");
        }
        EnvVars envVars2 = this.envVars;
        if (envVars2 != null) {
            envVars.putAll(envVars2);
        }
        File shmDir = new File(rootDir, "/tmp/shm");
        if (!shmDir.isDirectory()) {
            shmDir.mkdirs();
        }
        String command = nativeLibraryDir + "/libtinyproot.so";
        String command2 = ((command + " --rootfs=" + rootDir) + " --cwd=/home/xuser") + " /usr/bin/env " + envVars.toEscapedString() + " box64 " + this.guestExecutable;
        envVars.clear();
        envVars.put("TINYPROOT_LOADER", nativeLibraryDir + "/libtinyproot-loader.so");
        return ProcessHelper.exec(command2, envVars.toStringArray(), rootDir, new Callback() { // from class: com.winlator.xenvironment.components.GuestProgramLauncherComponent$$ExternalSyntheticLambda0
            @Override // com.winlator.core.Callback
            public final void call(Object obj) {
                GuestProgramLauncherComponent.this.lambda$execGuestProgram$0((Integer) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$execGuestProgram$0(Integer status) {
        synchronized (lock) {
            pid = -1;
        }
        Callback<Integer> callback = this.terminationCallback;
        if (callback != null) {
            callback.call(status);
        }
    }

    private void extractBox64File() {
        ImageFs imageFs = this.environment.getImageFs();
        Context context = this.environment.getContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String box64Version = preferences.getString("box64_version", "0.3.0");
        String currentBox64Version = preferences.getString("current_box64_version", "");
        File rootDir = imageFs.getRootDir();
        if (!box64Version.equals(currentBox64Version)) {
            TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context, "box64/box64-" + box64Version + ".tzst", rootDir);
            preferences.edit().putString("current_box64_version", box64Version).apply();
        }
    }

    private void addBox64EnvVars(EnvVars envVars, boolean enableLogs) {
        envVars.put("BOX64_NOBANNER", "1");
        envVars.put("BOX64_DYNAREC", "1");
        if (enableLogs) {
            envVars.put("BOX64_LOG", "1");
            envVars.put("BOX64_DYNAREC_MISSING", "1");
        }
        envVars.putAll(Box64PresetManager.getEnvVars(this.environment.getContext(), this.box64Preset));
        envVars.put("BOX64_X11GLX", "1");
        envVars.put("BOX64_NORCFILES", "1");
    }

    public void suspendProcess() {
        synchronized (lock) {
            int i = pid;
            if (i != -1) {
                ProcessHelper.suspendProcess(i);
            }
        }
    }

    public void resumeProcess() {
        synchronized (lock) {
            int i = pid;
            if (i != -1) {
                ProcessHelper.resumeProcess(i);
            }
        }
    }
}
