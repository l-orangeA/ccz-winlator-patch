package com.winlator.core;

import android.content.Context;
import android.net.Uri;
import com.winlator.container.Container;
import com.winlator.container.Drive;
import com.winlator.core.TarCompressorUtils;
import com.winlator.win32.MSLogFont;
import com.winlator.win32.WinVersions;
import com.winlator.xenvironment.ImageFs;
import com.winlator.xenvironment.XEnvironment;
import com.winlator.xenvironment.components.GuestProgramLauncherComponent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public abstract class WineUtils {
    public static void createDosdevicesSymlinks(Container container) {
        String dosdevicesPath = new File(container.getRootDir(), ".wine/dosdevices").getPath();
        File[] files = new File(dosdevicesPath).listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().matches("[a-z]:")) {
                    file.delete();
                }
            }
        }
        FileUtils.symlink("../drive_c", dosdevicesPath + "/c:");
        FileUtils.symlink("../../../../", dosdevicesPath + "/z:");
        for (Drive drive : container.drivesIterator()) {
            File linkTarget = new File(drive.path);
            String path = linkTarget.getAbsolutePath();
            if (!linkTarget.isDirectory() && path.startsWith("/data/data/com.winlator/storage")) {
                linkTarget.mkdirs();
                FileUtils.chmod(linkTarget, 505);
            }
            FileUtils.symlink(path, dosdevicesPath + "/" + drive.letter.toLowerCase(Locale.ENGLISH) + ":");
        }
    }

    public static void extractWineFileForInstallAsync(final Context context, final Uri uri, final Callback<File> callback) {
        Executors.newSingleThreadExecutor().execute(new Runnable() { // from class: com.winlator.core.WineUtils$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                WineUtils.lambda$extractWineFileForInstallAsync$0(context, uri, callback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$extractWineFileForInstallAsync$0(Context context, Uri uri, Callback callback) {
        File destination = new File(ImageFs.find(context).getInstalledWineDir(), "/preinstall/wine");
        FileUtils.delete(destination);
        destination.mkdirs();
        boolean success = TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, context, uri, destination);
        if (!success) {
            FileUtils.delete(destination);
        }
        if (callback != null) {
            callback.call(success ? destination : null);
        }
    }

    public static void findWineVersionAsync(Context context, File wineDir, final Callback<WineInfo> callback) {
        File wineDir2;
        if (wineDir == null || !wineDir.isDirectory()) {
            callback.call(null);
            return;
        }
        File[] files = wineDir.listFiles();
        if (files == null || files.length == 0) {
            callback.call(null);
            return;
        }
        boolean is64Bit = true;
        if (files.length != 1) {
            wineDir2 = wineDir;
        } else {
            if (!files[0].isDirectory()) {
                callback.call(null);
                return;
            }
            wineDir2 = files[0];
            files = wineDir2.listFiles();
            if (files == null || files.length == 0) {
                callback.call(null);
                return;
            }
        }
        File binDir = null;
        int length = files.length;
        int i = 0;
        while (true) {
            if (i >= length) {
                break;
            }
            File file = files[i];
            if (!file.isDirectory() || !file.getName().equals("bin")) {
                i++;
            } else {
                binDir = file;
                break;
            }
        }
        if (binDir == null) {
            callback.call(null);
            return;
        }
        File wineBin = new File(binDir, "wine");
        File wineBin64 = new File(binDir, "wine64");
        if (!wineBin.isFile()) {
            callback.call(null);
            return;
        }
        if ((!wineBin64.isFile() || !ElfHelper.is64Bit(wineBin64)) && !ElfHelper.is64Bit(wineBin)) {
            is64Bit = false;
        }
        if (!is64Bit) {
            callback.call(null);
            return;
        }
        ImageFs imageFs = ImageFs.find(context);
        File rootDir = imageFs.getRootDir();
        String wineBinAbsPath = wineBin64.isFile() ? wineBin64.getPath() : wineBin.getPath();
        String wineBinRelPath = FileUtils.toRelativePath(rootDir.getPath(), wineBinAbsPath);
        final String winePath = wineDir2.getPath();
        final AtomicReference<WineInfo> wineInfoRef = new AtomicReference<>();
        final Callback<String> debugCallback = new Callback() { // from class: com.winlator.core.WineUtils$$ExternalSyntheticLambda2
            @Override // com.winlator.core.Callback
            public final void call(Object obj) {
                WineUtils.lambda$findWineVersionAsync$1(wineInfoRef, winePath, (String) obj);
            }
        };
        ProcessHelper.addDebugCallback(debugCallback);
        File linkFile = new File(rootDir, "/home/xuser");
        linkFile.delete();
        FileUtils.symlink(wineDir2, linkFile);
        XEnvironment environment = new XEnvironment(context, imageFs);
        GuestProgramLauncherComponent guestProgramLauncherComponent = new GuestProgramLauncherComponent();
        guestProgramLauncherComponent.setGuestExecutable(wineBinRelPath + " --version");
        guestProgramLauncherComponent.setTerminationCallback(new Callback() { // from class: com.winlator.core.WineUtils$$ExternalSyntheticLambda0
            @Override // com.winlator.core.Callback
            public final void call(Object obj) {
                WineUtils.lambda$findWineVersionAsync$2(Callback.this, wineInfoRef, debugCallback, (Integer) obj);
            }
        });
        environment.addComponent(guestProgramLauncherComponent);
        environment.startEnvironmentComponents();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$findWineVersionAsync$1(AtomicReference wineInfoRef, String winePath, String line) {
        Pattern pattern = Pattern.compile("^wine\\-([0-9\\.]+)\\-?([0-9\\.]+)?", 2);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String version = matcher.group(1);
            String subversion = matcher.groupCount() >= 2 ? matcher.group(2) : null;
            wineInfoRef.set(new WineInfo(version, subversion, winePath));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$findWineVersionAsync$2(Callback callback, AtomicReference wineInfoRef, Callback debugCallback, Integer status) {
        callback.call((WineInfo) wineInfoRef.get());
        ProcessHelper.removeDebugCallback(debugCallback);
    }

    public static ArrayList<WineInfo> getInstalledWineInfos(Context context) {
        ArrayList<WineInfo> wineInfos = new ArrayList<>();
        wineInfos.add(WineInfo.MAIN_WINE_VERSION);
        File installedWineDir = ImageFs.find(context).getInstalledWineDir();
        File[] files = installedWineDir.listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                if (name.startsWith("wine")) {
                    wineInfos.add(WineInfo.fromIdentifier(context, name));
                }
            }
        }
        return wineInfos;
    }

    private static void setWindowMetrics(WineRegistryEditor registryEditor) {
        byte[] fontNormalData = new MSLogFont().toByteArray();
        byte[] fontBoldData = new MSLogFont().setWeight(700).toByteArray();
        registryEditor.setHexValue("Control Panel\\Desktop\\WindowMetrics", "CaptionFont", fontBoldData);
        registryEditor.setHexValue("Control Panel\\Desktop\\WindowMetrics", "IconFont", fontNormalData);
        registryEditor.setHexValue("Control Panel\\Desktop\\WindowMetrics", "MenuFont", fontNormalData);
        registryEditor.setHexValue("Control Panel\\Desktop\\WindowMetrics", "MessageFont", fontNormalData);
        registryEditor.setHexValue("Control Panel\\Desktop\\WindowMetrics", "SmCaptionFont", fontNormalData);
        registryEditor.setHexValue("Control Panel\\Desktop\\WindowMetrics", "StatusFont", fontNormalData);
    }

    public static void applySystemTweaks(Context context, WineInfo wineInfo) {
        Throwable th;
        int length;
        int i;
        String str;
        int i2;
        File userRegFile;
        String[] direct3dLibs;
        String name;
        File rootDir = ImageFs.find(context).getRootDir();
        File systemRegFile = new File(rootDir, "/home/xuser/.wine/system.reg");
        File userRegFile2 = new File(rootDir, "/home/xuser/.wine/user.reg");
        WineRegistryEditor registryEditor = new WineRegistryEditor(systemRegFile);
        try {
            registryEditor.setStringValue("Software\\Classes\\.reg", null, "REGfile");
            registryEditor.setStringValue("Software\\Classes\\.reg", "Content Type", "application/reg");
            registryEditor.setStringValue("Software\\Classes\\REGfile\\Shell\\Open\\command", null, "C:\\windows\\regedit.exe /C \"%1\"");
            registryEditor.setStringValue("Software\\Classes\\dllfile\\DefaultIcon", null, "shell32.dll,-154");
            registryEditor.setStringValue("Software\\Classes\\lnkfile\\DefaultIcon", null, "shell32.dll,-30");
            registryEditor.setStringValue("Software\\Classes\\inifile\\DefaultIcon", null, "shell32.dll,-151");
            registryEditor.close();
            String[] direct3dLibs2 = {"d3d8", "d3d9", "d3d10", "d3d10_1", "d3d10core", "d3d11", "d3d12", "d3d12core", "ddraw", "dxgi", "wined3d"};
            String[] inputLibs = {"dinput", "dinput8", "xinput1_1", "xinput1_2", "xinput1_3", "xinput1_4", "xinput9_1_0", "xinputuap"};
            boolean isMainWineVersion = WineInfo.isMainWineVersion(wineInfo.identifier());
            File wineSystem32Dir = new File(rootDir, "/opt/wine/lib/wine/x86_64-windows");
            File wineSysWoW64Dir = new File(rootDir, "/opt/wine/lib/wine/i386-windows");
            File containerSystem32Dir = new File(rootDir, "/home/xuser/.wine/drive_c/windows/system32");
            File containerSysWoW64Dir = new File(rootDir, "/home/xuser/.wine/drive_c/windows/syswow64");
            WineRegistryEditor registryEditor2 = new WineRegistryEditor(userRegFile2);
            try {
                length = direct3dLibs2.length;
                i = 0;
            } catch (Throwable th2) {
                th = th2;
            }
            while (true) {
                File rootDir2 = rootDir;
                str = "native,builtin";
                File systemRegFile2 = systemRegFile;
                if (i < length) {
                    try {
                        int i3 = length;
                        registryEditor2.setStringValue("Software\\Wine\\DllOverrides", direct3dLibs2[i], "native,builtin");
                        i++;
                        rootDir = rootDir2;
                        systemRegFile = systemRegFile2;
                        length = i3;
                    } catch (Throwable th3) {
                        th = th3;
                    }
                } else {
                    try {
                        break;
                    } catch (Throwable th4) {
                        th = th4;
                    }
                }
                try {
                    registryEditor2.close();
                    throw th;
                } catch (Throwable th5) {
                    th.addSuppressed(th5);
                    throw th;
                }
            }
            int length2 = inputLibs.length;
            int i4 = 0;
            while (i4 < length2) {
                String name2 = inputLibs[i4];
                if (isMainWineVersion) {
                    i2 = length2;
                    userRegFile = userRegFile2;
                    direct3dLibs = direct3dLibs2;
                    name = str;
                    registryEditor2.setStringValue("Software\\Wine\\DllOverrides", name2, "builtin,native");
                } else {
                    i2 = length2;
                    registryEditor2.setStringValue("Software\\Wine\\DllOverrides", name2, str);
                    name = str;
                    userRegFile = userRegFile2;
                    try {
                        direct3dLibs = direct3dLibs2;
                        try {
                            FileUtils.copy(new File(wineSysWoW64Dir, name2 + ".dll"), new File(containerSysWoW64Dir, name2 + ".dll"));
                            FileUtils.copy(new File(wineSystem32Dir, name2 + ".dll"), new File(containerSystem32Dir, name2 + ".dll"));
                        } catch (Throwable th6) {
                            th = th6;
                        }
                    } catch (Throwable th7) {
                        th = th7;
                    }
                }
                i4++;
                length2 = i2;
                str = name;
                userRegFile2 = userRegFile;
                direct3dLibs2 = direct3dLibs;
            }
            registryEditor2.removeKey("Software\\Winlator\\WFM\\ContextMenu\\7-Zip");
            registryEditor2.setStringValue("Software\\Winlator\\WFM\\ContextMenu\\7-Zip", "Open Archive", "Z:\\opt\\apps\\7-Zip\\7zFM.exe \"%FILE%\"");
            registryEditor2.setStringValue("Software\\Winlator\\WFM\\ContextMenu\\7-Zip", "Extract Here", "Z:\\opt\\apps\\7-Zip\\7zG.exe x \"%FILE%\" -r -o\"%DIR%\" -y");
            registryEditor2.setStringValue("Software\\Winlator\\WFM\\ContextMenu\\7-Zip", "Extract to Folder", "Z:\\opt\\apps\\7-Zip\\7zG.exe x \"%FILE%\" -r -o\"%DIR%\\%BASENAME%\" -y");
            setWindowMetrics(registryEditor2);
            registryEditor2.close();
        } finally {
        }
    }

    public static void overrideWinComponentDlls(Context context, Container container, String wincomponents) {
        Throwable th;
        JSONObject wincomponentsJSONObject;
        File userRegFile = new File(container.getRootDir(), ".wine/user.reg");
        Iterator<String[]> oldWinComponentsIter = new KeyValueSet(container.getExtra("wincomponents", "direct3d=0,directsound=0,directmusic=0,directshow=0,directplay=0,xaudio=0,vcrun2010=0,wmdecoder=0")).iterator();
        try {
            WineRegistryEditor registryEditor = new WineRegistryEditor(userRegFile);
            try {
                try {
                    try {
                        JSONObject wincomponentsJSONObject2 = new JSONObject(FileUtils.readString(context, "wincomponents/wincomponents.json"));
                        try {
                            Iterator<String[]> it = new KeyValueSet(wincomponents).iterator();
                            while (it.hasNext()) {
                                String[] wincomponent = it.next();
                                if (!wincomponent[1].equals(oldWinComponentsIter.next()[1])) {
                                    String identifier = wincomponent[0];
                                    boolean useNative = wincomponent[1].equals("1");
                                    JSONArray dlnames = wincomponentsJSONObject2.getJSONArray(identifier);
                                    int i = 0;
                                    while (i < dlnames.length()) {
                                        String dlname = dlnames.getString(i);
                                        if (useNative) {
                                            wincomponentsJSONObject = wincomponentsJSONObject2;
                                            registryEditor.setStringValue("Software\\Wine\\DllOverrides", dlname, "native,builtin");
                                        } else {
                                            wincomponentsJSONObject = wincomponentsJSONObject2;
                                            registryEditor.removeValue("Software\\Wine\\DllOverrides", dlname);
                                        }
                                        i++;
                                        wincomponentsJSONObject2 = wincomponentsJSONObject;
                                    }
                                }
                            }
                            registryEditor.close();
                        } catch (Throwable th2) {
                            th = th2;
                            th = th;
                            try {
                                registryEditor.close();
                                throw th;
                            } catch (Throwable th3) {
                                th.addSuppressed(th3);
                                throw th;
                            }
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        th = th;
                        registryEditor.close();
                        throw th;
                    }
                } catch (JSONException e) {
                }
            } catch (Throwable th5) {
                th = th5;
            }
        } catch (JSONException e2) {
        }
    }

    public static void setWinComponentRegistryKeys(File systemRegFile, String identifier, boolean useNative) {
        WineRegistryEditor registryEditor;
        if (identifier.equals("directsound")) {
            registryEditor = new WineRegistryEditor(systemRegFile);
            try {
                if (useNative) {
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{083863F1-70DE-11D0-BD40-00A0C911CE86}\\Instance\\{E30629D1-27E5-11CE-875D-00608CB78066}", "CLSID", "{E30629D1-27E5-11CE-875D-00608CB78066}");
                    registryEditor.setHexValue("Software\\Classes\\Wow6432Node\\CLSID\\{083863F1-70DE-11D0-BD40-00A0C911CE86}\\Instance\\{E30629D1-27E5-11CE-875D-00608CB78066}", "FilterData", "02000000000080000100000000000000307069330200000000000000010000000000000000000000307479330000000038000000480000006175647300001000800000aa00389b710100000000001000800000aa00389b71");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{083863F1-70DE-11D0-BD40-00A0C911CE86}\\Instance\\{E30629D1-27E5-11CE-875D-00608CB78066}", "FriendlyName", "Wave Audio Renderer");
                    registryEditor.setStringValue("Software\\Classes\\CLSID\\{083863F1-70DE-11D0-BD40-00A0C911CE86}\\Instance\\{E30629D1-27E5-11CE-875D-00608CB78066}", "CLSID", "{E30629D1-27E5-11CE-875D-00608CB78066}");
                    registryEditor.setHexValue("Software\\Classes\\CLSID\\{083863F1-70DE-11D0-BD40-00A0C911CE86}\\Instance\\{E30629D1-27E5-11CE-875D-00608CB78066}", "FilterData", "02000000000080000100000000000000307069330200000000000000010000000000000000000000307479330000000038000000480000006175647300001000800000aa00389b710100000000001000800000aa00389b71");
                    registryEditor.setStringValue("Software\\Classes\\CLSID\\{083863F1-70DE-11D0-BD40-00A0C911CE86}\\Instance\\{E30629D1-27E5-11CE-875D-00608CB78066}", "FriendlyName", "Wave Audio Renderer");
                } else {
                    registryEditor.removeKey("Software\\Classes\\Wow6432Node\\CLSID\\{083863F1-70DE-11D0-BD40-00A0C911CE86}\\Instance\\{E30629D1-27E5-11CE-875D-00608CB78066}");
                    registryEditor.removeKey("Software\\Classes\\CLSID\\{083863F1-70DE-11D0-BD40-00A0C911CE86}\\Instance\\{E30629D1-27E5-11CE-875D-00608CB78066}");
                }
                registryEditor.close();
                return;
            } finally {
                try {
                    registryEditor.close();
                } catch (Throwable th) {
                    th.addSuppressed(th);
                }
            }
        }
        if (identifier.equals("wmdecoder")) {
            registryEditor = new WineRegistryEditor(systemRegFile);
            try {
                if (useNative) {
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{2EEB4ADF-4578-4D10-BCA7-BB955F56320A}\\InprocServer32", null, "C:\\windows\\system32\\wmadmod.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{82D353DF-90BD-4382-8BC2-3F6192B76E34}\\InprocServer32", null, "C:\\windows\\system32\\wmvdecod.dll");
                } else {
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{2EEB4ADF-4578-4D10-BCA7-BB955F56320A}\\InprocServer32", null, "C:\\windows\\system32\\winegstreamer.dll");
                    registryEditor.setStringValue("Software\\Classes\\Wow6432Node\\CLSID\\{82D353DF-90BD-4382-8BC2-3F6192B76E34}\\InprocServer32", null, "C:\\windows\\system32\\winegstreamer.dll");
                }
                registryEditor.close();
            } catch (Throwable th2) {
                throw th2;
            }
        }
    }

    public static void updateWineprefix(Context context, final Callback<Integer> terminationCallback) {
        ImageFs imageFs = ImageFs.find(context);
        final File rootDir = imageFs.getRootDir();
        File tmpDir = imageFs.getTmpDir();
        if (!tmpDir.isDirectory()) {
            tmpDir.mkdir();
        }
        FileUtils.writeString(new File(rootDir, "/home/xuser/.wine/.update-timestamp"), "0\n");
        EnvVars envVars = new EnvVars();
        envVars.put("WINEPREFIX", "/home/xuser/.wine");
        envVars.put("WINEDLLOVERRIDES", "mscoree,mshtml=d");
        XEnvironment environment = new XEnvironment(context, imageFs);
        GuestProgramLauncherComponent guestProgramLauncherComponent = new GuestProgramLauncherComponent();
        guestProgramLauncherComponent.setEnvVars(envVars);
        guestProgramLauncherComponent.setGuestExecutable(WineInfo.MAIN_WINE_VERSION.getExecutable() + " wineboot -u");
        guestProgramLauncherComponent.setTerminationCallback(new Callback() { // from class: com.winlator.core.WineUtils$$ExternalSyntheticLambda1
            @Override // com.winlator.core.Callback
            public final void call(Object obj) {
                WineUtils.lambda$updateWineprefix$3(rootDir, terminationCallback, (Integer) obj);
            }
        });
        environment.addComponent(guestProgramLauncherComponent);
        environment.startEnvironmentComponents();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$updateWineprefix$3(File rootDir, Callback terminationCallback, Integer status) {
        FileUtils.writeString(new File(rootDir, "/home/xuser/.wine/.update-timestamp"), "disable\n");
        if (terminationCallback != null) {
            terminationCallback.call(status);
        }
    }

    public static boolean isWineprefixWasUpdated(Container container) {
        File file = new File(container.getRootDir(), "/.wine/.update-timestamp");
        String content = FileUtils.readString(file);
        if (!content.startsWith("disable")) {
            try {
                int updateTimestamp = Integer.parseInt(content.replaceAll("[\r\n]+", ""));
                if (updateTimestamp != 0) {
                    return FileUtils.writeString(file, "disable\n");
                }
                return false;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    public static void changeServicesStatus(Container container, boolean onlyEssential) {
        String[] services = {"BITS:3", "Eventlog:2", "HTTP:3", "LanmanServer:3", "NDIS:2", "PlugPlay:2", "RpcSs:3", "scardsvr:3", "Schedule:3", "Spooler:3", "StiSvc:3", "TermService:3", "winebus:3", "winehid:3", "Winmgmt:3", "wuauserv:3"};
        File systemRegFile = new File(container.getRootDir(), ".wine/system.reg");
        WineRegistryEditor registryEditor = new WineRegistryEditor(systemRegFile);
        try {
            registryEditor.setCreateKeyIfNotExist(false);
            for (String service : services) {
                String name = service.substring(0, service.indexOf(":"));
                int value = onlyEssential ? 4 : Character.getNumericValue(service.charAt(service.length() - 1));
                registryEditor.setDwordValue("System\\CurrentControlSet\\Services\\" + name, "Start", value);
            }
            registryEditor.close();
        } finally {
        }
    }

    public static String unixToDOSPath(String unixPath, Container container) {
        String dosPath = "";
        String driveLetter = "";
        int index = unixPath.indexOf("/.wine/drive_c");
        if (index != -1) {
            driveLetter = "C:";
            dosPath = unixPath.substring(index + 14).replace("/", "\\");
        } else {
            Iterator<Drive> it = container.drivesIterator().iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                Drive drive = it.next();
                if (unixPath.startsWith(drive.path)) {
                    driveLetter = drive.letter + ":";
                    dosPath = unixPath.substring(drive.path.length()).replace("/", "\\");
                    break;
                }
            }
        }
        if (!dosPath.startsWith("\\")) {
            dosPath = dosPath + "\\";
        }
        String dosPath2 = driveLetter + StringUtils.removeEndSlash(dosPath);
        if (dosPath2.equals(driveLetter)) {
            return dosPath2 + "\\";
        }
        return dosPath2;
    }

    public static String dosToUnixPath(String dosPath, Container container) {
        int index = dosPath.indexOf(":");
        if (index == -1) {
            return "";
        }
        String driveLetter = dosPath.substring(0, index).toUpperCase(Locale.ENGLISH);
        String relativePath = StringUtils.removeStartSlash(dosPath.substring(index + 1).replace("\\", "/"));
        if (driveLetter.equals("C")) {
            String unixPath = container.getRootDir() + "/.wine/drive_c/" + relativePath;
            return unixPath;
        }
        if (driveLetter.equals("Z")) {
            File rootDir = new File(container.getRootDir(), "../../");
            try {
                String unixPath2 = rootDir.getCanonicalPath() + "/" + relativePath;
                return unixPath2;
            } catch (IOException e) {
                return "";
            }
        }
        for (Drive drive : container.drivesIterator()) {
            if (drive.letter.equals(driveLetter)) {
                String unixPath3 = drive.path + "/" + relativePath;
                return unixPath3;
            }
        }
        return "";
    }

    public static void setWinVersion(Container container, int winVersionIdx) {
        Throwable th;
        WinVersions.WinVersion winVersion = WinVersions.getWinVersions()[winVersionIdx];
        String currentBuild = String.valueOf(winVersion.buildNumber);
        String currentVersion = winVersion.currentVersion;
        if (currentVersion == null) {
            currentVersion = ((int) winVersion.majorVersion) + "." + ((int) winVersion.minorVersion);
        }
        File systemRegFile = new File(container.getRootDir(), ".wine/system.reg");
        WineRegistryEditor registryEditor = new WineRegistryEditor(systemRegFile);
        try {
            registryEditor.setStringValue("Software\\Wow6432Node\\Microsoft\\Windows NT\\CurrentVersion", "CurrentVersion", currentVersion);
            registryEditor.setDwordValue("Software\\Wow6432Node\\Microsoft\\Windows NT\\CurrentVersion", "CurrentMajorVersionNumber", winVersion.majorVersion);
            registryEditor.setDwordValue("Software\\Wow6432Node\\Microsoft\\Windows NT\\CurrentVersion", "CurrentMinorVersionNumber", winVersion.minorVersion);
            registryEditor.setStringValue("Software\\Wow6432Node\\Microsoft\\Windows NT\\CurrentVersion", "CSDVersion", winVersion.csdVersion);
            registryEditor.setStringValue("Software\\Wow6432Node\\Microsoft\\Windows NT\\CurrentVersion", "CurrentBuild", currentBuild);
            registryEditor.setStringValue("Software\\Wow6432Node\\Microsoft\\Windows NT\\CurrentVersion", "CurrentBuildNumber", currentBuild);
            StringBuilder sb = new StringBuilder();
            sb.append("Microsoft ");
            try {
                sb.append(winVersion.description);
                registryEditor.setStringValue("Software\\Wow6432Node\\Microsoft\\Windows NT\\CurrentVersion", "ProductName", sb.toString());
                registryEditor.setStringValue("Software\\Microsoft\\Windows NT\\CurrentVersion", "CurrentVersion", currentVersion);
                registryEditor.setDwordValue("Software\\Microsoft\\Windows NT\\CurrentVersion", "CurrentMajorVersionNumber", winVersion.majorVersion);
                registryEditor.setDwordValue("Software\\Microsoft\\Windows NT\\CurrentVersion", "CurrentMinorVersionNumber", winVersion.minorVersion);
                registryEditor.setStringValue("Software\\Microsoft\\Windows NT\\CurrentVersion", "CSDVersion", winVersion.csdVersion);
                registryEditor.setStringValue("Software\\Microsoft\\Windows NT\\CurrentVersion", "CurrentBuild", currentBuild);
                registryEditor.setStringValue("Software\\Microsoft\\Windows NT\\CurrentVersion", "CurrentBuildNumber", currentBuild);
                registryEditor.setStringValue("Software\\Microsoft\\Windows NT\\CurrentVersion", "ProductName", "Microsoft " + winVersion.description);
                registryEditor.close();
            } catch (Throwable th2) {
                th = th2;
                try {
                    registryEditor.close();
                    throw th;
                } catch (Throwable th3) {
                    th.addSuppressed(th3);
                    throw th;
                }
            }
        } catch (Throwable th4) {
            th = th4;
        }
    }
}
