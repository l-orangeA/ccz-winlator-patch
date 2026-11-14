package com.winlator.xenvironment.components;

import android.content.Context;
import android.os.Process;
import com.winlator.core.FileUtils;
import com.winlator.core.ProcessHelper;
import com.winlator.xconnector.UnixSocketConfig;
import com.winlator.xenvironment.EnvironmentComponent;
import java.io.File;
import java.util.ArrayList;

/* loaded from: classes.dex */
public class PulseAudioComponent extends EnvironmentComponent {
    private final UnixSocketConfig socketConfig;
    private static int pid = -1;
    private static final Object lock = new Object();
    private float volume = 1.0f;
    private byte performanceMode = 1;

    public PulseAudioComponent(UnixSocketConfig socketConfig) {
        this.socketConfig = socketConfig;
    }

    @Override // com.winlator.xenvironment.EnvironmentComponent
    public void start() {
        synchronized (lock) {
            stop();
            pid = execPulseAudio();
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

    public void setVolume(float volume) {
        this.volume = volume;
    }

    public void setPerformanceMode(int performanceMode) {
        this.performanceMode = (byte) performanceMode;
    }

    private int execPulseAudio() {
        Context context = this.environment.getContext();
        String nativeLibraryDir = context.getApplicationInfo().nativeLibraryDir;
        File workingDir = new File(context.getFilesDir(), "/pulseaudio");
        if (!workingDir.isDirectory()) {
            workingDir.mkdirs();
            FileUtils.chmod(workingDir, 505);
        }
        File configFile = new File(workingDir, "default.pa");
        FileUtils.writeString(configFile, String.join("\n", "load-module module-native-protocol-unix auth-anonymous=1 auth-cookie-enabled=0 socket=\"" + this.socketConfig.path + "\"", "load-module module-aaudio-sink volume=" + this.volume + " performance_mode=" + ((int) this.performanceMode), "set-default-sink AAudioSink"));
        File modulesDir = new File(workingDir, "modules");
        ArrayList<String> envVars = new ArrayList<>();
        envVars.add("LD_LIBRARY_PATH=/system/lib64:" + nativeLibraryDir + ":" + modulesDir);
        envVars.add("HOME=" + workingDir);
        envVars.add("TMPDIR=" + this.environment.getTmpDir());
        String command = nativeLibraryDir + "/libpulseaudio.so";
        return ProcessHelper.exec(((((((command + " --system=false") + " --disable-shm=true") + " --fail=false") + " -n --file=default.pa") + " --daemonize=false") + " --use-pid-file=false") + " --exit-idle-time=-1", (String[]) envVars.toArray(new String[0]), workingDir);
    }
}
