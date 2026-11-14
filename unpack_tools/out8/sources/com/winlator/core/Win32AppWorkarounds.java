package com.winlator.core;

import androidx.collection.ArrayMap;
import com.winlator.XServerDisplayActivity;
import com.winlator.container.Container;
import com.winlator.winhandler.WinHandler;
import com.winlator.xserver.ScreenInfo;
import com.winlator.xserver.Window;
import java.util.Locale;

/* loaded from: classes.dex */
public class Win32AppWorkarounds {
    private final XServerDisplayActivity activity;
    private final short taskAffinityMask;
    private final short taskAffinityMaskWoW64;
    private ArrayMap<String, Workaround> workarounds;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public interface DXWrapperWorkaround extends Workaround {
        String getValue();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public interface EnvVarsWorkaround extends Workaround {
        void apply(EnvVars envVars);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public interface GraphicsDriverWorkaround extends Workaround {
        String getValue();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public interface KillServicesWorkaround extends Workaround {
        boolean getValue();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public interface ScreenSizeWorkaround extends Workaround {
        String getValue();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public interface TaskAffinityWorkaround extends Workaround {
        int getAffinityMask(Window window);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public interface WinComponentsWorkaround extends Workaround {
        String getValue();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public interface Workaround {
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class MultiWorkaround implements Workaround {
        private final Workaround[] list;

        public MultiWorkaround(Workaround... list) {
            this.list = list;
        }
    }

    public Win32AppWorkarounds(XServerDisplayActivity activity) {
        this.activity = activity;
        Container container = activity.getContainer();
        this.taskAffinityMask = (short) ProcessHelper.getAffinityMask(container.getCPUList(true));
        this.taskAffinityMaskWoW64 = (short) ProcessHelper.getAffinityMask(container.getCPUListWoW64(true));
    }

    private void applyWorkaround(Workaround workaround) {
        if (workaround instanceof EnvVarsWorkaround) {
            ((EnvVarsWorkaround) workaround).apply(this.activity.getOverrideEnvVars());
            return;
        }
        if (workaround instanceof ScreenSizeWorkaround) {
            this.activity.setScreenInfo(new ScreenInfo(((ScreenSizeWorkaround) workaround).getValue()));
            return;
        }
        if (workaround instanceof DXWrapperWorkaround) {
            this.activity.setDXWrapper(((DXWrapperWorkaround) workaround).getValue());
            return;
        }
        if (workaround instanceof GraphicsDriverWorkaround) {
            this.activity.setGraphicsDriver(((GraphicsDriverWorkaround) workaround).getValue());
        } else if (workaround instanceof WinComponentsWorkaround) {
            this.activity.setWinComponents(((WinComponentsWorkaround) workaround).getValue());
        } else if (workaround instanceof KillServicesWorkaround) {
            this.activity.setKillServices(((KillServicesWorkaround) workaround).getValue());
        }
    }

    public void applyStartupWorkarounds(String className) {
        initWorkarounds();
        Workaround workaround = this.workarounds.get(className.toLowerCase(Locale.ENGLISH));
        if (workaround == null) {
            return;
        }
        if (workaround instanceof MultiWorkaround) {
            for (Workaround workaround2 : ((MultiWorkaround) workaround).list) {
                applyWorkaround(workaround2);
            }
            return;
        }
        applyWorkaround(workaround);
    }

    private void setProcessAffinity(Window window, int processAffinity) {
        int processId = window.getProcessId();
        String className = window.getClassName();
        WinHandler winHandler = this.activity.getWinHandler();
        if (processId > 0) {
            winHandler.setProcessAffinity(processId, processAffinity);
        } else if (!className.isEmpty()) {
            winHandler.setProcessAffinity(window.getClassName(), processAffinity);
        }
    }

    public void assignTaskAffinity(Window window) {
        if (this.taskAffinityMask == 0 || !window.isApplicationWindow()) {
            return;
        }
        initWorkarounds();
        int processAffinity = window.isWoW64() ? this.taskAffinityMaskWoW64 : this.taskAffinityMask;
        Workaround workaround = this.workarounds.get(window.getClassName());
        if (workaround instanceof TaskAffinityWorkaround) {
            processAffinity = ((TaskAffinityWorkaround) workaround).getAffinityMask(window);
        } else if (workaround instanceof MultiWorkaround) {
            Workaround[] workaroundArr = ((MultiWorkaround) workaround).list;
            int length = workaroundArr.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                Workaround workaround2 = workaroundArr[i];
                if (!(workaround2 instanceof TaskAffinityWorkaround)) {
                    i++;
                } else {
                    processAffinity = ((TaskAffinityWorkaround) workaround2).getAffinityMask(window);
                    break;
                }
            }
        }
        setProcessAffinity(window, processAffinity);
    }

    private void initWorkarounds() {
        if (this.workarounds != null) {
            return;
        }
        ArrayMap<String, Workaround> workarounds = new ArrayMap<>();
        final ScreenInfo screenInfo = this.activity.getScreenInfo();
        Workaround oldGameWorkaround = new MultiWorkaround(new EnvVarsWorkaround() { // from class: com.winlator.core.Win32AppWorkarounds$$ExternalSyntheticLambda4
            @Override // com.winlator.core.Win32AppWorkarounds.EnvVarsWorkaround
            public final void apply(EnvVars envVars) {
                envVars.put("MESA_EXTENSION_MAX_YEAR", "2002");
            }
        }, new GraphicsDriverWorkaround() { // from class: com.winlator.core.Win32AppWorkarounds$$ExternalSyntheticLambda9
            @Override // com.winlator.core.Win32AppWorkarounds.GraphicsDriverWorkaround
            public final String getValue() {
                String lambda$initWorkarounds$1;
                lambda$initWorkarounds$1 = Win32AppWorkarounds.lambda$initWorkarounds$1();
                return lambda$initWorkarounds$1;
            }
        }, new DXWrapperWorkaround() { // from class: com.winlator.core.Win32AppWorkarounds$$ExternalSyntheticLambda0
            @Override // com.winlator.core.Win32AppWorkarounds.DXWrapperWorkaround
            public final String getValue() {
                String lambda$initWorkarounds$2;
                lambda$initWorkarounds$2 = Win32AppWorkarounds.lambda$initWorkarounds$2();
                return lambda$initWorkarounds$2;
            }
        });
        workarounds.put("wolfsp.exe", oldGameWorkaround);
        workarounds.put("mohaa.exe", oldGameWorkaround);
        workarounds.put("moh_breakthrough.exe", oldGameWorkaround);
        workarounds.put("moh_spearhead.exe", oldGameWorkaround);
        workarounds.put("mgsgroundzeroes.exe", new EnvVarsWorkaround() { // from class: com.winlator.core.Win32AppWorkarounds$$ExternalSyntheticLambda2
            @Override // com.winlator.core.Win32AppWorkarounds.EnvVarsWorkaround
            public final void apply(EnvVars envVars) {
                envVars.put("WINEFORCENTCONTINUE", "1");
            }
        });
        workarounds.put("sonicgenerations.exe", new EnvVarsWorkaround() { // from class: com.winlator.core.Win32AppWorkarounds$$ExternalSyntheticLambda5
            @Override // com.winlator.core.Win32AppWorkarounds.EnvVarsWorkaround
            public final void apply(EnvVars envVars) {
                envVars.put("WINEESYNC", "0");
            }
        });
        workarounds.put("blacklist_game.exe", new EnvVarsWorkaround() { // from class: com.winlator.core.Win32AppWorkarounds$$ExternalSyntheticLambda1
            @Override // com.winlator.core.Win32AppWorkarounds.EnvVarsWorkaround
            public final void apply(EnvVars envVars) {
                Win32AppWorkarounds.this.lambda$initWorkarounds$5(envVars);
            }
        });
        workarounds.put("fate.exe", new ScreenSizeWorkaround() { // from class: com.winlator.core.Win32AppWorkarounds$$ExternalSyntheticLambda12
            @Override // com.winlator.core.Win32AppWorkarounds.ScreenSizeWorkaround
            public final String getValue() {
                String lambda$initWorkarounds$6;
                lambda$initWorkarounds$6 = Win32AppWorkarounds.lambda$initWorkarounds$6();
                return lambda$initWorkarounds$6;
            }
        });
        workarounds.put("elex.exe", new TaskAffinityWorkaround() { // from class: com.winlator.core.Win32AppWorkarounds$$ExternalSyntheticLambda14
            @Override // com.winlator.core.Win32AppWorkarounds.TaskAffinityWorkaround
            public final int getAffinityMask(Window window) {
                return Win32AppWorkarounds.lambda$initWorkarounds$7(window);
            }
        });
        workarounds.put("ffxii_tza.exe", new ScreenSizeWorkaround() { // from class: com.winlator.core.Win32AppWorkarounds$$ExternalSyntheticLambda11
            @Override // com.winlator.core.Win32AppWorkarounds.ScreenSizeWorkaround
            public final String getValue() {
                String lambda$initWorkarounds$8;
                lambda$initWorkarounds$8 = Win32AppWorkarounds.lambda$initWorkarounds$8(ScreenInfo.this);
                return lambda$initWorkarounds$8;
            }
        });
        workarounds.put("ffviii.exe", new EnvVarsWorkaround() { // from class: com.winlator.core.Win32AppWorkarounds$$ExternalSyntheticLambda7
            @Override // com.winlator.core.Win32AppWorkarounds.EnvVarsWorkaround
            public final void apply(EnvVars envVars) {
                envVars.put("ZINK_CONTEXT_THREADED", "1");
            }
        });
        workarounds.put("farcry4.exe", new MultiWorkaround(new EnvVarsWorkaround() { // from class: com.winlator.core.Win32AppWorkarounds$$ExternalSyntheticLambda6
            @Override // com.winlator.core.Win32AppWorkarounds.EnvVarsWorkaround
            public final void apply(EnvVars envVars) {
                envVars.put("WINVERSION", "winxp");
            }
        }, new KillServicesWorkaround() { // from class: com.winlator.core.Win32AppWorkarounds$$ExternalSyntheticLambda10
            @Override // com.winlator.core.Win32AppWorkarounds.KillServicesWorkaround
            public final boolean getValue() {
                boolean lambda$initWorkarounds$11;
                lambda$initWorkarounds$11 = Win32AppWorkarounds.lambda$initWorkarounds$11();
                return lambda$initWorkarounds$11;
            }
        }));
        workarounds.put("drakensang.exe", new EnvVarsWorkaround() { // from class: com.winlator.core.Win32AppWorkarounds$$ExternalSyntheticLambda8
            @Override // com.winlator.core.Win32AppWorkarounds.EnvVarsWorkaround
            public final void apply(EnvVars envVars) {
                envVars.put("EXTRA_EXEC_ARGS", "-novideo");
            }
        });
        String dxwrapper = this.activity.getDXWrapper();
        if (dxwrapper.equals("dxvk")) {
            workarounds.put("dxmd.exe", new MultiWorkaround(new EnvVarsWorkaround() { // from class: com.winlator.core.Win32AppWorkarounds$$ExternalSyntheticLambda3
                @Override // com.winlator.core.Win32AppWorkarounds.EnvVarsWorkaround
                public final void apply(EnvVars envVars) {
                    envVars.put("WINEVMEMMAXSIZE", "16384");
                }
            }, new WinComponentsWorkaround() { // from class: com.winlator.core.Win32AppWorkarounds$$ExternalSyntheticLambda15
                @Override // com.winlator.core.Win32AppWorkarounds.WinComponentsWorkaround
                public final String getValue() {
                    String lambda$initWorkarounds$14;
                    lambda$initWorkarounds$14 = Win32AppWorkarounds.lambda$initWorkarounds$14();
                    return lambda$initWorkarounds$14;
                }
            }, new TaskAffinityWorkaround() { // from class: com.winlator.core.Win32AppWorkarounds$$ExternalSyntheticLambda13
                @Override // com.winlator.core.Win32AppWorkarounds.TaskAffinityWorkaround
                public final int getAffinityMask(Window window) {
                    int lambda$initWorkarounds$16;
                    lambda$initWorkarounds$16 = Win32AppWorkarounds.this.lambda$initWorkarounds$16(window);
                    return lambda$initWorkarounds$16;
                }
            }));
        }
        this.workarounds = workarounds;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ String lambda$initWorkarounds$1() {
        return "virgl";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ String lambda$initWorkarounds$2() {
        return "wined3d";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$initWorkarounds$5(EnvVars envVars) {
        envVars.put("WINEOVERRIDEAFFINITYMASK", Short.valueOf(this.taskAffinityMaskWoW64));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ String lambda$initWorkarounds$6() {
        return "1024x768";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ int lambda$initWorkarounds$7(Window window) {
        return ProcessHelper.getLastCoreAffinityMask();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ String lambda$initWorkarounds$8(ScreenInfo screenInfo) {
        return (screenInfo.width + 4) + "x" + (screenInfo.height + 4);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ boolean lambda$initWorkarounds$11() {
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ String lambda$initWorkarounds$14() {
        return "direct3d=0,directsound=0,directmusic=0,directshow=0,directplay=0,xaudio=0,vcrun2010=0,wmdecoder=0";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ int lambda$initWorkarounds$16(final Window window) {
        if (!window.getName().equals("Deus Ex: Mankind Divided")) {
            AppUtils.runDelayed(new Runnable() { // from class: com.winlator.core.Win32AppWorkarounds$$ExternalSyntheticLambda16
                @Override // java.lang.Runnable
                public final void run() {
                    Win32AppWorkarounds.this.lambda$initWorkarounds$15(window);
                }
            }, 40000L);
        }
        return ProcessHelper.getLastCoreAffinityMask();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$initWorkarounds$15(Window window) {
        if (window.attributes.isMapped()) {
            setProcessAffinity(window, this.taskAffinityMask);
        }
    }
}
