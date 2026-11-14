package com.winlator.container;

import android.content.Context;
import android.os.Handler;
import com.winlatcn.R;
import com.winlator.container.FileInfo;
import com.winlator.core.Callback;
import com.winlator.core.FileUtils;
import com.winlator.core.OnExtractFileListener;
import com.winlator.core.TarCompressorUtils;
import com.winlator.core.WineInfo;
import com.winlator.xenvironment.ImageFs;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class ContainerManager {
    private final Context context;
    private final File homeDir;
    private final ArrayList<Container> containers = new ArrayList<>();
    private int maxContainerId = 0;

    public ContainerManager(Context context) {
        this.context = context;
        File rootDir = ImageFs.find(context).getRootDir();
        this.homeDir = new File(rootDir, "home");
        loadContainers();
    }

    public Context getContext() {
        return this.context;
    }

    public ArrayList<Container> getContainers() {
        return this.containers;
    }

    private void loadContainers() {
        this.containers.clear();
        this.maxContainerId = 0;
        try {
            File[] files = this.homeDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory() && file.getName().startsWith("xuser-")) {
                        Container container = new Container(Integer.parseInt(file.getName().replace("xuser-", "")));
                        container.setRootDir(new File(this.homeDir, "xuser-" + container.id));
                        JSONObject data = new JSONObject(FileUtils.readString(container.getConfigFile()));
                        container.loadData(data);
                        this.containers.add(container);
                        this.maxContainerId = Math.max(this.maxContainerId, container.id);
                    }
                }
            }
        } catch (JSONException e) {
        }
    }

    public void activateContainer(Container container) {
        container.setRootDir(new File(this.homeDir, "xuser-" + container.id));
        File file = new File(this.homeDir, "xuser");
        file.delete();
        FileUtils.symlink("./xuser-" + container.id, file.getPath());
    }

    public void createContainerAsync(final JSONObject data, final Callback<Container> callback) {
        final Handler handler = new Handler();
        Executors.newSingleThreadExecutor().execute(new Runnable() { // from class: com.winlator.container.ContainerManager$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                ContainerManager.this.lambda$createContainerAsync$1(data, handler, callback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$createContainerAsync$1(JSONObject data, Handler handler, final Callback callback) {
        final Container container = createContainer(data);
        handler.post(new Runnable() { // from class: com.winlator.container.ContainerManager$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                Callback.this.call(container);
            }
        });
    }

    public void duplicateContainerAsync(final Container container, final Runnable callback) {
        final Handler handler = new Handler();
        Executors.newSingleThreadExecutor().execute(new Runnable() { // from class: com.winlator.container.ContainerManager$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                ContainerManager.this.lambda$duplicateContainerAsync$2(container, handler, callback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$duplicateContainerAsync$2(Container container, Handler handler, Runnable callback) {
        duplicateContainer(container);
        handler.post(callback);
    }

    public void removeContainerAsync(final Container container, final Runnable callback) {
        final Handler handler = new Handler();
        Executors.newSingleThreadExecutor().execute(new Runnable() { // from class: com.winlator.container.ContainerManager$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                ContainerManager.this.lambda$removeContainerAsync$3(container, handler, callback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$removeContainerAsync$3(Container container, Handler handler, Runnable callback) {
        removeContainer(container);
        handler.post(callback);
    }

    /* JADX WARN: Removed duplicated region for block: B:14:0x004c A[Catch: JSONException -> 0x006f, TryCatch #0 {JSONException -> 0x006f, blocks: (B:3:0x0003, B:7:0x002b, B:9:0x003c, B:14:0x004c, B:15:0x0053, B:17:0x005d, B:19:0x0061), top: B:2:0x0003 }] */
    /* JADX WARN: Removed duplicated region for block: B:17:0x005d A[Catch: JSONException -> 0x006f, TryCatch #0 {JSONException -> 0x006f, blocks: (B:3:0x0003, B:7:0x002b, B:9:0x003c, B:14:0x004c, B:15:0x0053, B:17:0x005d, B:19:0x0061), top: B:2:0x0003 }] */
    /* JADX WARN: Removed duplicated region for block: B:19:0x0061 A[Catch: JSONException -> 0x006f, TRY_LEAVE, TryCatch #0 {JSONException -> 0x006f, blocks: (B:3:0x0003, B:7:0x002b, B:9:0x003c, B:14:0x004c, B:15:0x0053, B:17:0x005d, B:19:0x0061), top: B:2:0x0003 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private com.winlator.container.Container createContainer(org.json.JSONObject r9) {
        /*
            r8 = this;
            java.lang.String r0 = "wineVersion"
            r1 = 0
            int r2 = r8.maxContainerId     // Catch: org.json.JSONException -> L6f
            r3 = 1
            int r2 = r2 + r3
            java.lang.String r4 = "id"
            r9.put(r4, r2)     // Catch: org.json.JSONException -> L6f
            java.io.File r4 = new java.io.File     // Catch: org.json.JSONException -> L6f
            java.io.File r5 = r8.homeDir     // Catch: org.json.JSONException -> L6f
            java.lang.StringBuilder r6 = new java.lang.StringBuilder     // Catch: org.json.JSONException -> L6f
            r6.<init>()     // Catch: org.json.JSONException -> L6f
            java.lang.String r7 = "xuser-"
            r6.append(r7)     // Catch: org.json.JSONException -> L6f
            r6.append(r2)     // Catch: org.json.JSONException -> L6f
            java.lang.String r6 = r6.toString()     // Catch: org.json.JSONException -> L6f
            r4.<init>(r5, r6)     // Catch: org.json.JSONException -> L6f
            boolean r5 = r4.mkdirs()     // Catch: org.json.JSONException -> L6f
            if (r5 != 0) goto L2b
            return r1
        L2b:
            com.winlator.container.Container r5 = new com.winlator.container.Container     // Catch: org.json.JSONException -> L6f
            r5.<init>(r2)     // Catch: org.json.JSONException -> L6f
            r5.setRootDir(r4)     // Catch: org.json.JSONException -> L6f
            r5.loadData(r9)     // Catch: org.json.JSONException -> L6f
            boolean r6 = r9.has(r0)     // Catch: org.json.JSONException -> L6f
            if (r6 == 0) goto L49
            java.lang.String r6 = r9.getString(r0)     // Catch: org.json.JSONException -> L6f
            boolean r6 = com.winlator.core.WineInfo.isMainWineVersion(r6)     // Catch: org.json.JSONException -> L6f
            if (r6 == 0) goto L47
            goto L49
        L47:
            r6 = 0
            goto L4a
        L49:
            r6 = r3
        L4a:
            if (r6 != 0) goto L53
            java.lang.String r0 = r9.getString(r0)     // Catch: org.json.JSONException -> L6f
            r5.setWineVersion(r0)     // Catch: org.json.JSONException -> L6f
        L53:
            java.lang.String r0 = r5.getWineVersion()     // Catch: org.json.JSONException -> L6f
            boolean r0 = r8.extractContainerPatternFile(r0, r4, r1)     // Catch: org.json.JSONException -> L6f
            if (r0 != 0) goto L61
            com.winlator.core.FileUtils.delete(r4)     // Catch: org.json.JSONException -> L6f
            return r1
        L61:
            r5.saveData()     // Catch: org.json.JSONException -> L6f
            int r0 = r8.maxContainerId     // Catch: org.json.JSONException -> L6f
            int r0 = r0 + r3
            r8.maxContainerId = r0     // Catch: org.json.JSONException -> L6f
            java.util.ArrayList<com.winlator.container.Container> r0 = r8.containers     // Catch: org.json.JSONException -> L6f
            r0.add(r5)     // Catch: org.json.JSONException -> L6f
            return r5
        L6f:
            r0 = move-exception
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.winlator.container.ContainerManager.createContainer(org.json.JSONObject):com.winlator.container.Container");
    }

    private void duplicateContainer(Container srcContainer) {
        int id = this.maxContainerId + 1;
        File dstDir = new File(this.homeDir, "xuser-" + id);
        if (dstDir.mkdirs()) {
            if (!FileUtils.copy(srcContainer.getRootDir(), dstDir, new Callback() { // from class: com.winlator.container.ContainerManager$$ExternalSyntheticLambda0
                @Override // com.winlator.core.Callback
                public final void call(Object obj) {
                    FileUtils.chmod((File) obj, 505);
                }
            })) {
                FileUtils.delete(dstDir);
                return;
            }
            Container dstContainer = new Container(id);
            dstContainer.setRootDir(dstDir);
            dstContainer.setName(srcContainer.getName() + " (" + this.context.getString(R.string.copy) + ")");
            dstContainer.setScreenSize(srcContainer.getScreenSize());
            dstContainer.setEnvVars(srcContainer.getEnvVars());
            dstContainer.setCPUList(srcContainer.getCPUList());
            dstContainer.setCPUListWoW64(srcContainer.getCPUListWoW64());
            dstContainer.setGraphicsDriver(srcContainer.getGraphicsDriver());
            dstContainer.setGraphicsDriverConfig(srcContainer.getGraphicsDriverConfig());
            dstContainer.setDXWrapper(srcContainer.getDXWrapper());
            dstContainer.setDXWrapperConfig(srcContainer.getDXWrapperConfig());
            dstContainer.setAudioDriver(srcContainer.getAudioDriver());
            dstContainer.setAudioDriverConfig(srcContainer.getAudioDriverConfig());
            dstContainer.setWinComponents(srcContainer.getWinComponents());
            dstContainer.setDrives(srcContainer.getDrives());
            dstContainer.setShowFPS(srcContainer.isShowFPS());
            dstContainer.setStartupSelection(srcContainer.getStartupSelection());
            dstContainer.setBox64Preset(srcContainer.getBox64Preset());
            dstContainer.setDesktopTheme(srcContainer.getDesktopTheme());
            dstContainer.saveData();
            this.maxContainerId++;
            this.containers.add(dstContainer);
        }
    }

    private void removeContainer(Container container) {
        if (FileUtils.delete(container.getRootDir())) {
            this.containers.remove(container);
        }
    }

    public ArrayList<Shortcut> loadShortcuts(Shortcut selectedFolder) {
        ArrayList<Shortcut> shortcuts = new ArrayList<>();
        if (selectedFolder != null) {
            File[] files = selectedFolder.file.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".desktop") || file.isDirectory()) {
                        shortcuts.add(new Shortcut(selectedFolder.container, file));
                    }
                }
            }
        } else {
            String desktopDirname = this.context.getString(R.string.desktop);
            Iterator<Container> it = this.containers.iterator();
            while (it.hasNext()) {
                Container container = it.next();
                File desktopDir = new File(container.getUserDir(), desktopDirname);
                File[] files2 = desktopDir.listFiles();
                if (files2 != null) {
                    for (File file2 : files2) {
                        if (file2.getName().endsWith(".desktop") || file2.isDirectory()) {
                            shortcuts.add(new Shortcut(container, file2));
                        }
                    }
                }
            }
        }
        shortcuts.sort(new Comparator() { // from class: com.winlator.container.ContainerManager$$ExternalSyntheticLambda5
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                int lambda$loadShortcuts$5;
                lambda$loadShortcuts$5 = ContainerManager.lambda$loadShortcuts$5((Shortcut) obj, (Shortcut) obj2);
                return lambda$loadShortcuts$5;
            }
        });
        return shortcuts;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ int lambda$loadShortcuts$5(Shortcut a, Shortcut b) {
        int value = Boolean.compare(b.file.isDirectory(), a.file.isDirectory());
        return value == 0 ? a.name.compareTo(b.name) : value;
    }

    public ArrayList<FileInfo> loadFiles(Container container, FileInfo parent) {
        ArrayList<FileInfo> fileInfos = new ArrayList<>();
        if (parent != null) {
            return parent.list();
        }
        String rootPath = container.getRootDir().getPath();
        fileInfos.add(new FileInfo(container, "C:", rootPath + "/.wine/drive_c", FileInfo.Type.DRIVE));
        for (Drive drive : container.drivesIterator()) {
            fileInfos.add(new FileInfo(container, drive.letter + ":", drive.path, FileInfo.Type.DRIVE));
        }
        File userDir = container.getUserDir();
        File documentsDir = new File(userDir, this.context.getString(R.string.documents));
        File favoritesDir = new File(userDir, this.context.getString(R.string.favorites));
        String name = documentsDir.getName();
        String path = documentsDir.getPath();
        FileInfo.Type type = FileInfo.Type.DIRECTORY;
        fileInfos.add(new FileInfo(container, name, path, type));
        fileInfos.add(new FileInfo(container, favoritesDir.getName(), favoritesDir.getPath(), type));
        Collections.sort(fileInfos);
        return fileInfos;
    }

    public int getNextContainerId() {
        return this.maxContainerId + 1;
    }

    public Container getContainerById(int id) {
        Iterator<Container> it = this.containers.iterator();
        while (it.hasNext()) {
            Container container = it.next();
            if (container.id == id) {
                return container;
            }
        }
        return null;
    }

    private void extractCommonDlls(String srcName, String dstName, JSONObject commonDlls, File containerDir, OnExtractFileListener onExtractFileListener) throws JSONException {
        File srcDir = new File(ImageFs.find(this.context).getRootDir(), "/opt/wine/lib/wine/" + srcName);
        JSONArray dlnames = commonDlls.getJSONArray(dstName);
        for (int i = 0; i < dlnames.length(); i++) {
            String dlname = dlnames.getString(i);
            File dstFile = new File(containerDir, ".wine/drive_c/windows/" + dstName + "/" + dlname);
            if (onExtractFileListener == null || (dstFile = onExtractFileListener.onExtractFile(dstFile, 0L)) != null) {
                FileUtils.copy(new File(srcDir, dlname), dstFile);
            }
        }
    }

    public boolean extractContainerPatternFile(String wineVersion, File containerDir, OnExtractFileListener onExtractFileListener) {
        if (WineInfo.isMainWineVersion(wineVersion)) {
            boolean result = TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this.context, "container_pattern.tzst", containerDir, onExtractFileListener);
            if (result) {
                try {
                    JSONObject commonDlls = new JSONObject(FileUtils.readString(this.context, "common_dlls.json"));
                    extractCommonDlls("x86_64-windows", "system32", commonDlls, containerDir, onExtractFileListener);
                    extractCommonDlls("i386-windows", "syswow64", commonDlls, containerDir, onExtractFileListener);
                } catch (JSONException e) {
                    return false;
                }
            }
            return result;
        }
        File installedWineDir = ImageFs.find(this.context).getInstalledWineDir();
        WineInfo wineInfo = WineInfo.fromIdentifier(this.context, wineVersion);
        File file = new File(installedWineDir, "container-pattern-" + wineInfo.fullVersion() + ".tzst");
        return TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, file, containerDir, onExtractFileListener);
    }
}
