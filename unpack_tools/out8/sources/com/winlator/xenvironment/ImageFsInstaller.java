package com.winlator.xenvironment;

import android.content.Context;
import com.winlatcn.R;
import com.winlator.MainActivity;
import com.winlator.SettingsFragment;
import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.core.AppUtils;
import com.winlator.core.DownloadProgressDialog;
import com.winlator.core.FileUtils;
import com.winlator.core.OnExtractFileListener;
import com.winlator.core.TarCompressorUtils;
import com.winlator.core.WineInfo;
import java.io.File;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/* loaded from: classes.dex */
public abstract class ImageFsInstaller {
    private static void resetContainerImgVersions(Context context) {
        ContainerManager manager = new ContainerManager(context);
        Iterator<Container> it = manager.getContainers().iterator();
        while (it.hasNext()) {
            Container container = it.next();
            String imgVersion = container.getExtra("imgVersion");
            String wineVersion = container.getWineVersion();
            if (!imgVersion.isEmpty() && WineInfo.isMainWineVersion(wineVersion) && Short.parseShort(imgVersion) <= 5) {
                container.putExtra("wineprefixNeedsUpdate", "t");
            }
            container.putExtra("imgVersion", null);
            container.saveData();
        }
    }

    public static void install(final MainActivity activity) {
        AppUtils.keepScreenOn(activity);
        final ImageFs imageFs = ImageFs.find(activity);
        final File rootDir = imageFs.getRootDir();
        SettingsFragment.resetBox64Version(activity);
        final DownloadProgressDialog dialog = new DownloadProgressDialog(activity);
        dialog.show(R.string.installing_system_files);
        Executors.newSingleThreadExecutor().execute(new Runnable() { // from class: com.winlator.xenvironment.ImageFsInstaller$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                ImageFsInstaller.lambda$install$2(rootDir, activity, dialog, imageFs);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$install$2(File rootDir, final MainActivity activity, final DownloadProgressDialog dialog, ImageFs imageFs) {
        clearRootDir(rootDir);
        final long contentLength = ((float) FileUtils.getSize(activity, "imagefs.txz")) * 5.5555553f;
        final AtomicLong totalSizeRef = new AtomicLong();
        boolean success = TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, activity, "imagefs.txz", rootDir, new OnExtractFileListener() { // from class: com.winlator.xenvironment.ImageFsInstaller$$ExternalSyntheticLambda0
            @Override // com.winlator.core.OnExtractFileListener
            public final File onExtractFile(File file, long j) {
                File lambda$install$1;
                lambda$install$1 = ImageFsInstaller.lambda$install$1(totalSizeRef, contentLength, activity, dialog, file, j);
                return lambda$install$1;
            }
        });
        if (success) {
            imageFs.createImgVersionFile(9);
            resetContainerImgVersions(activity);
        } else {
            AppUtils.showToast(activity, R.string.unable_to_install_system_files);
        }
        dialog.closeOnUiThread();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ File lambda$install$1(AtomicLong totalSizeRef, long contentLength, MainActivity activity, final DownloadProgressDialog dialog, File file, long size) {
        if (size > 0) {
            long totalSize = totalSizeRef.addAndGet(size);
            final int progress = (int) ((((float) totalSize) / ((float) contentLength)) * 100.0f);
            activity.runOnUiThread(new Runnable() { // from class: com.winlator.xenvironment.ImageFsInstaller$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    DownloadProgressDialog.this.setProgress(progress);
                }
            });
        }
        return file;
    }

    public static void installIfNeeded(MainActivity activity) {
        ImageFs imageFs = ImageFs.find(activity);
        if (!imageFs.isValid() || imageFs.getVersion() < 9) {
            install(activity);
        }
    }

    private static void clearOptDir(File optDir) {
        File[] files = optDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.getName().equals("installed-wine")) {
                    FileUtils.delete(file);
                }
            }
        }
    }

    private static void clearRootDir(File rootDir) {
        if (rootDir.isDirectory()) {
            File[] files = rootDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        String name = file.getName();
                        if (name.equals("home") || name.equals("opt")) {
                            if (name.equals("opt")) {
                                clearOptDir(file);
                            }
                        }
                    }
                    FileUtils.delete(file);
                }
                return;
            }
            return;
        }
        rootDir.mkdirs();
    }
}
