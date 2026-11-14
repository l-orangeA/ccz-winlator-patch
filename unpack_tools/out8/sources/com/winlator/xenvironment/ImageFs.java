package com.winlator.xenvironment;

import android.content.Context;
import com.winlator.core.FileUtils;
import java.io.File;
import java.io.IOException;

/* loaded from: classes.dex */
public class ImageFs {
    private final File rootDir;
    private String winePath = "/opt/wine";

    private ImageFs(File rootDir) {
        this.rootDir = rootDir;
    }

    public static ImageFs find(Context context) {
        return new ImageFs(new File(context.getFilesDir(), "imagefs"));
    }

    public File getRootDir() {
        return this.rootDir;
    }

    public boolean isValid() {
        return this.rootDir.isDirectory() && getImgVersionFile().exists();
    }

    public int getVersion() {
        File imgVersionFile = getImgVersionFile();
        if (imgVersionFile.exists()) {
            return Integer.parseInt(FileUtils.readLines(imgVersionFile).get(0));
        }
        return 0;
    }

    public void createImgVersionFile(int version) {
        getConfigDir().mkdirs();
        File file = getImgVersionFile();
        try {
            file.createNewFile();
            FileUtils.writeString(file, String.valueOf(version));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getWinePath() {
        return this.winePath;
    }

    public void setWinePath(String winePath) {
        this.winePath = FileUtils.toRelativePath(this.rootDir.getPath(), winePath);
    }

    public File getConfigDir() {
        return new File(this.rootDir, ".winlator");
    }

    public File getImgVersionFile() {
        return new File(getConfigDir(), ".img_version");
    }

    public File getInstalledWineDir() {
        return new File(this.rootDir, "/opt/installed-wine");
    }

    public File getTmpDir() {
        return new File(this.rootDir, "/tmp");
    }

    public File getLibDir() {
        return new File(this.rootDir, "/usr/lib/aarch64-linux-gnu");
    }

    public String toString() {
        return this.rootDir.getPath();
    }
}
