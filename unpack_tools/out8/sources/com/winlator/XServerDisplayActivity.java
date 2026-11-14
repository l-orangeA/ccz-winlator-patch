package com.winlator;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;
import com.google.android.material.navigation.NavigationView;
import com.winlatcn.R;
import com.winlator.alsaserver.ALSAClient;
import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.container.Shortcut;
import com.winlator.contentdialog.ContentDialog;
import com.winlator.contentdialog.DXVKConfigDialog;
import com.winlator.contentdialog.DebugDialog;
import com.winlator.contentdialog.ScreenEffectDialog;
import com.winlator.core.AppUtils;
import com.winlator.core.Callback;
import com.winlator.core.EnvVars;
import com.winlator.core.FileUtils;
import com.winlator.core.GPUInformation;
import com.winlator.core.KeyValueSet;
import com.winlator.core.OnExtractFileListener;
import com.winlator.core.PreloaderDialog;
import com.winlator.core.ProcessHelper;
import com.winlator.core.StringUtils;
import com.winlator.core.TarCompressorUtils;
import com.winlator.core.Win32AppWorkarounds;
import com.winlator.core.WineInfo;
import com.winlator.core.WineRegistryEditor;
import com.winlator.core.WineStartMenuCreator;
import com.winlator.core.WineThemeManager;
import com.winlator.core.WineUtils;
import com.winlator.inputcontrols.ControlsProfile;
import com.winlator.inputcontrols.ExternalController;
import com.winlator.inputcontrols.InputControlsManager;
import com.winlator.math.Mathf;
import com.winlator.renderer.GLRenderer;
import com.winlator.widget.FrameRating;
import com.winlator.widget.InputControlsView;
import com.winlator.widget.MagnifierView;
import com.winlator.widget.TouchpadView;
import com.winlator.widget.XServerView;
import com.winlator.winhandler.TaskManagerDialog;
import com.winlator.winhandler.WinHandler;
import com.winlator.xconnector.UnixSocketConfig;
import com.winlator.xenvironment.ImageFs;
import com.winlator.xenvironment.XEnvironment;
import com.winlator.xenvironment.components.ALSAServerComponent;
import com.winlator.xenvironment.components.GuestProgramLauncherComponent;
import com.winlator.xenvironment.components.NetworkInfoUpdateComponent;
import com.winlator.xenvironment.components.PulseAudioComponent;
import com.winlator.xenvironment.components.SysVSharedMemoryComponent;
import com.winlator.xenvironment.components.VirGLRendererComponent;
import com.winlator.xenvironment.components.XServerComponent;
import com.winlator.xserver.Property;
import com.winlator.xserver.ScreenInfo;
import com.winlator.xserver.Window;
import com.winlator.xserver.WindowManager;
import com.winlator.xserver.XServer;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class XServerDisplayActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private KeyValueSet audioDriverConfig;
    private Container container;
    private ContainerManager containerManager;
    private DebugDialog debugDialog;
    private DrawerLayout drawerLayout;
    private KeyValueSet dxwrapperConfig;
    private Runnable editInputControlsCallback;
    private XEnvironment environment;
    private FrameRating frameRating;
    private KeyValueSet graphicsDriverConfig;
    private ImageFs imageFs;
    private InputControlsManager inputControlsManager;
    private InputControlsView inputControlsView;
    private MagnifierView magnifierView;
    private EnvVars overrideEnvVars;
    private SharedPreferences preferences;
    private String screenEffectProfile;
    private Shortcut shortcut;
    private TouchpadView touchpadView;
    private Win32AppWorkarounds win32AppWorkarounds;
    private String wincomponents;
    private WineInfo wineInfo;
    private XServer xServer;
    private XServerView xServerView;
    private String graphicsDriver = "turnip";
    private String audioDriver = "alsa";
    private String dxwrapper = "dxvk";
    private ScreenInfo screenInfo = new ScreenInfo("1280x720");
    private final EnvVars envVars = new EnvVars();
    private boolean firstTimeBoot = false;
    private boolean killServices = false;
    private final WinHandler winHandler = new WinHandler(this);
    private float globalCursorSpeed = 1.0f;
    private int frameRatingWindowId = -1;

    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppUtils.hideSystemUI(this);
        AppUtils.keepScreenOn(this);
        setContentView(R.layout.xserver_display_activity);
        final PreloaderDialog preloaderDialog = new PreloaderDialog(this);
        this.preferences = PreferenceManager.getDefaultSharedPreferences(this);
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.DrawerLayout);
        this.drawerLayout = drawerLayout;
        drawerLayout.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() { // from class: com.winlator.XServerDisplayActivity$$ExternalSyntheticLambda0
            @Override // android.view.View.OnApplyWindowInsetsListener
            public final WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                WindowInsets lambda$onCreate$0;
                lambda$onCreate$0 = XServerDisplayActivity.lambda$onCreate$0(view, windowInsets);
                return lambda$onCreate$0;
            }
        });
        this.drawerLayout.setDrawerLockMode(1);
        NavigationView navigationView = (NavigationView) findViewById(R.id.NavigationView);
        ProcessHelper.removeAllDebugCallbacks();
        boolean enableLogs = this.preferences.getBoolean("enable_wine_debug", false) || this.preferences.getBoolean("enable_box64_logs", false);
        if (enableLogs) {
            DebugDialog debugDialog = new DebugDialog(this);
            this.debugDialog = debugDialog;
            ProcessHelper.addDebugCallback(debugDialog);
        }
        Menu menu = navigationView.getMenu();
        menu.findItem(R.id.menu_item_logs).setVisible(enableLogs);
        navigationView.setNavigationItemSelectedListener(this);
        this.imageFs = ImageFs.find(this);
        if (!isGenerateWineprefix()) {
            ContainerManager containerManager = new ContainerManager(this);
            this.containerManager = containerManager;
            Container containerById = containerManager.getContainerById(getIntent().getIntExtra("container_id", 0));
            this.container = containerById;
            this.containerManager.activateContainer(containerById);
            boolean wineprefixNeedsUpdate = this.container.getExtra("wineprefixNeedsUpdate").equals("t");
            if (!wineprefixNeedsUpdate) {
                this.win32AppWorkarounds = new Win32AppWorkarounds(this);
                this.firstTimeBoot = this.container.getExtra("appVersion").isEmpty();
                String wineVersion = this.container.getWineVersion();
                WineInfo fromIdentifier = WineInfo.fromIdentifier(this, wineVersion);
                this.wineInfo = fromIdentifier;
                if (fromIdentifier != WineInfo.MAIN_WINE_VERSION) {
                    this.imageFs.setWinePath(fromIdentifier.path);
                }
                String shortcutPath = getIntent().getStringExtra("shortcut_path");
                if (shortcutPath != null && !shortcutPath.isEmpty()) {
                    this.shortcut = new Shortcut(this.container, new File(shortcutPath));
                }
                this.graphicsDriver = this.container.getGraphicsDriver();
                this.audioDriver = this.container.getAudioDriver();
                this.dxwrapper = this.container.getDXWrapper();
                this.wincomponents = this.container.getWinComponents();
                String dxwrapperConfig = this.container.getDXWrapperConfig();
                String graphicsDriverConfig = this.container.getGraphicsDriverConfig();
                String audioDriverConfig = this.container.getAudioDriverConfig();
                this.screenInfo = new ScreenInfo(this.container.getScreenSize());
                this.killServices = this.container.getStartupSelection() == 2;
                Intent intent = getIntent();
                Shortcut shortcut = this.shortcut;
                if (shortcut != null) {
                    this.graphicsDriver = shortcut.getExtra("graphicsDriver", this.container.getGraphicsDriver());
                    this.audioDriver = this.shortcut.getExtra("audioDriver", this.container.getAudioDriver());
                    this.dxwrapper = this.shortcut.getExtra("dxwrapper", this.container.getDXWrapper());
                    this.wincomponents = this.shortcut.getExtra("wincomponents", this.container.getWinComponents());
                    dxwrapperConfig = this.shortcut.getExtra("dxwrapperConfig", this.container.getDXWrapperConfig());
                    graphicsDriverConfig = this.shortcut.getExtra("graphicsDriverConfig", this.container.getGraphicsDriverConfig());
                    audioDriverConfig = this.shortcut.getExtra("audioDriverConfig", this.container.getAudioDriverConfig());
                    this.screenInfo = new ScreenInfo(this.shortcut.getExtra("screenSize", this.container.getScreenSize()));
                    String dinputMapperType = this.shortcut.getExtra("dinputMapperType");
                    if (!dinputMapperType.isEmpty()) {
                        this.winHandler.setDInputMapperType(Byte.parseByte(dinputMapperType));
                    }
                    this.win32AppWorkarounds.applyStartupWorkarounds(this.shortcut.wmClass);
                } else if (intent.hasExtra("exec_path")) {
                    this.win32AppWorkarounds.applyStartupWorkarounds(FileUtils.getName(intent.getStringExtra("exec_path")));
                }
                if (this.dxwrapper.equals("dxvk")) {
                    this.dxwrapperConfig = new KeyValueSet(dxwrapperConfig);
                }
                if (this.graphicsDriver.equals("turnip") || this.graphicsDriver.equals("virgl")) {
                    this.graphicsDriverConfig = new KeyValueSet(graphicsDriverConfig);
                }
                this.audioDriverConfig = new KeyValueSet(audioDriverConfig);
                int preferredInputApi = this.preferences.getInt("preferred_input_api", WinHandler.PreferredInputApi.BOTH.ordinal());
                this.winHandler.setPreferredInputApi(WinHandler.PreferredInputApi.values()[preferredInputApi]);
            } else {
                preloaderDialog.lambda$showOnUiThread$0(R.string.updating_system_files);
                WineUtils.updateWineprefix(this, new Callback() { // from class: com.winlator.XServerDisplayActivity$$ExternalSyntheticLambda2
                    @Override // com.winlator.core.Callback
                    public final void call(Object obj) {
                        XServerDisplayActivity.this.lambda$onCreate$1((Integer) obj);
                    }
                });
                return;
            }
        }
        preloaderDialog.lambda$showOnUiThread$0(R.string.starting_up);
        this.inputControlsManager = new InputControlsManager(this);
        XServer xServer = new XServer(this.screenInfo);
        this.xServer = xServer;
        xServer.setWinHandler(this.winHandler);
        final boolean[] winStarted = {false};
        this.xServer.windowManager.addOnWindowModificationListener(new WindowManager.OnWindowModificationListener() { // from class: com.winlator.XServerDisplayActivity.1
            @Override // com.winlator.xserver.WindowManager.OnWindowModificationListener
            public void onUpdateWindowContent(Window window) {
                if (!winStarted[0] && !window.getClassName().isEmpty() && window.getHeight() > 1) {
                    XServerDisplayActivity.this.xServerView.getRenderer().setCursorVisible(true);
                    preloaderDialog.closeOnUiThread();
                    winStarted[0] = true;
                }
                if (window.id == XServerDisplayActivity.this.frameRatingWindowId) {
                    XServerDisplayActivity.this.frameRating.update();
                }
            }

            @Override // com.winlator.xserver.WindowManager.OnWindowModificationListener
            public void onModifyWindowProperty(Window window, Property property) {
                XServerDisplayActivity.this.changeFrameRatingVisibility(window, property);
            }

            @Override // com.winlator.xserver.WindowManager.OnWindowModificationListener
            public void onMapWindow(Window window) {
                if (XServerDisplayActivity.this.win32AppWorkarounds != null) {
                    XServerDisplayActivity.this.win32AppWorkarounds.assignTaskAffinity(window);
                }
            }

            @Override // com.winlator.xserver.WindowManager.OnWindowModificationListener
            public void onUnmapWindow(Window window) {
                XServerDisplayActivity.this.changeFrameRatingVisibility(window, null);
            }
        });
        setupUI();
        Executors.newSingleThreadExecutor().execute(new Runnable() { // from class: com.winlator.XServerDisplayActivity$$ExternalSyntheticLambda10
            @Override // java.lang.Runnable
            public final void run() {
                XServerDisplayActivity.this.lambda$onCreate$2();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ WindowInsets lambda$onCreate$0(View view, WindowInsets windowInsets) {
        return windowInsets.replaceSystemWindowInsets(0, 0, 0, 0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$1(Integer status) {
        if (status.intValue() == 0) {
            this.container.putExtra("wineprefixNeedsUpdate", null);
            this.container.putExtra("wincomponents", null);
            this.container.saveData();
            AppUtils.restartActivity(this);
            return;
        }
        finish();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$2() {
        if (!isGenerateWineprefix()) {
            setupWineSystemFiles();
            extractGraphicsDriverFiles();
            changeWineAudioDriver();
        }
        setupXEnvironment();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Runnable runnable;
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 3 && resultCode == -1 && (runnable = this.editInputControlsCallback) != null) {
            runnable.run();
            this.editInputControlsCallback = null;
        }
    }

    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onResume() {
        super.onResume();
        if (this.environment != null) {
            this.xServerView.onResume();
            this.environment.onResume();
        }
    }

    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onPause() {
        super.onPause();
        XEnvironment xEnvironment = this.environment;
        if (xEnvironment != null) {
            xEnvironment.onPause();
            this.xServerView.onPause();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onDestroy() {
        this.winHandler.stop();
        XEnvironment xEnvironment = this.environment;
        if (xEnvironment != null) {
            xEnvironment.stopEnvironmentComponents();
        }
        super.onDestroy();
    }

    @Override // androidx.activity.ComponentActivity, android.app.Activity
    public void onBackPressed() {
        if (this.environment != null) {
            if (!this.drawerLayout.isDrawerOpen(8388611)) {
                this.drawerLayout.openDrawer(8388611);
            } else {
                this.drawerLayout.closeDrawers();
            }
        }
    }

    @Override // com.google.android.material.navigation.NavigationView.OnNavigationItemSelectedListener
    public boolean onNavigationItemSelected(MenuItem item) {
        final GLRenderer renderer = this.xServerView.getRenderer();
        switch (item.getItemId()) {
            case R.id.menu_item_exit /* 2131296684 */:
                exit();
                return true;
            case R.id.menu_item_input_controls /* 2131296688 */:
                showInputControlsDialog();
                this.drawerLayout.closeDrawers();
                return true;
            case R.id.menu_item_keyboard /* 2131296690 */:
                AppUtils.showKeyboard(this);
                this.drawerLayout.closeDrawers();
                return true;
            case R.id.menu_item_logs /* 2131296692 */:
                this.debugDialog.show();
                this.drawerLayout.closeDrawers();
                return true;
            case R.id.menu_item_magnifier /* 2131296693 */:
                if (this.magnifierView == null) {
                    final FrameLayout container = (FrameLayout) findViewById(R.id.FLXServerDisplay);
                    MagnifierView magnifierView = new MagnifierView(this);
                    this.magnifierView = magnifierView;
                    magnifierView.setZoomButtonCallback(new Callback() { // from class: com.winlator.XServerDisplayActivity$$ExternalSyntheticLambda4
                        @Override // com.winlator.core.Callback
                        public final void call(Object obj) {
                            XServerDisplayActivity.this.lambda$onNavigationItemSelected$3(renderer, (Float) obj);
                        }
                    });
                    this.magnifierView.setZoomValue(renderer.getMagnifierZoom());
                    this.magnifierView.setHideButtonCallback(new Runnable() { // from class: com.winlator.XServerDisplayActivity$$ExternalSyntheticLambda12
                        @Override // java.lang.Runnable
                        public final void run() {
                            XServerDisplayActivity.this.lambda$onNavigationItemSelected$4(container);
                        }
                    });
                    container.addView(this.magnifierView);
                }
                this.drawerLayout.closeDrawers();
                return true;
            case R.id.menu_item_screen_effect /* 2131296700 */:
                new ScreenEffectDialog(this).show();
                this.drawerLayout.closeDrawers();
                return true;
            case R.id.menu_item_task_manager /* 2131296703 */:
                new TaskManagerDialog(this).show();
                this.drawerLayout.closeDrawers();
                return true;
            case R.id.menu_item_toggle_fullscreen /* 2131296704 */:
                renderer.toggleFullscreen();
                this.drawerLayout.closeDrawers();
                return true;
            case R.id.menu_item_touchpad_help /* 2131296705 */:
                showTouchpadHelpDialog();
                return true;
            default:
                return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onNavigationItemSelected$3(GLRenderer renderer, Float value) {
        renderer.setMagnifierZoom(Mathf.clamp(renderer.getMagnifierZoom() + value.floatValue(), 1.0f, 3.0f));
        this.magnifierView.setZoomValue(renderer.getMagnifierZoom());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onNavigationItemSelected$4(FrameLayout container) {
        container.removeView(this.magnifierView);
        this.magnifierView = null;
    }

    private void exit() {
        this.winHandler.stop();
        XEnvironment xEnvironment = this.environment;
        if (xEnvironment != null) {
            xEnvironment.stopEnvironmentComponents();
        }
        Intent intent = getIntent();
        if (intent.hasExtra("exec_path")) {
            AppUtils.RestartApplicationOptions options = new AppUtils.RestartApplicationOptions();
            options.containerId = this.container.id;
            options.startPath = FileUtils.getDirname(intent.getStringExtra("exec_path"));
            AppUtils.restartApplication(this, options);
            return;
        }
        AppUtils.restartApplication(this);
    }

    private void setupWineSystemFiles() {
        String appVersion = String.valueOf(AppUtils.getVersionCode(this));
        String imgVersion = String.valueOf(this.imageFs.getVersion());
        boolean containerDataChanged = false;
        boolean wineprefixWasUpdated = WineUtils.isWineprefixWasUpdated(this.container);
        if (!this.container.getExtra("appVersion").equals(appVersion) || !this.container.getExtra("imgVersion").equals(imgVersion) || wineprefixWasUpdated) {
            applyGeneralPatches(this.container);
            this.container.putExtra("appVersion", appVersion);
            this.container.putExtra("imgVersion", imgVersion);
            containerDataChanged = true;
        }
        String dxwrapper = this.dxwrapper;
        if (dxwrapper.equals("dxvk")) {
            dxwrapper = "dxvk-" + this.dxwrapperConfig.get("version", "2.4");
        }
        if (!dxwrapper.equals(this.container.getExtra("dxwrapper"))) {
            extractDXWrapperFiles(dxwrapper);
            this.container.putExtra("dxwrapper", dxwrapper);
            containerDataChanged = true;
        }
        if (dxwrapper.equals("cnc-ddraw")) {
            this.envVars.put("CNC_DDRAW_CONFIG_FILE", "C:\\ProgramData\\cnc-ddraw\\ddraw.ini");
        }
        if (!this.wincomponents.equals(this.container.getExtra("wincomponents"))) {
            extractWinComponentFiles();
            this.container.putExtra("wincomponents", this.wincomponents);
            containerDataChanged = true;
        }
        String desktopTheme = this.container.getDesktopTheme();
        if (!(desktopTheme + "," + this.xServer.screenInfo).equals(this.container.getExtra("desktopTheme"))) {
            WineThemeManager.apply(this, new WineThemeManager.ThemeInfo(desktopTheme), this.xServer.screenInfo);
            this.container.putExtra("desktopTheme", desktopTheme + "," + this.xServer.screenInfo);
            containerDataChanged = true;
        }
        WineStartMenuCreator.create(this, this.container);
        WineUtils.createDosdevicesSymlinks(this.container);
        String startupSelection = String.valueOf((int) this.container.getStartupSelection());
        if (!startupSelection.equals(this.container.getExtra("startupSelection")) || wineprefixWasUpdated) {
            Container container = this.container;
            WineUtils.changeServicesStatus(container, container.getStartupSelection() != 0);
            this.container.putExtra("startupSelection", startupSelection);
            containerDataChanged = true;
        }
        if (containerDataChanged) {
            this.container.saveData();
        }
    }

    private void setupXEnvironment() {
        String str;
        this.envVars.put("MESA_DEBUG", "silent");
        this.envVars.put("MESA_NO_ERROR", "1");
        this.envVars.put("WINEPREFIX", "/home/xuser/.wine");
        boolean enableWineDebug = this.preferences.getBoolean("enable_wine_debug", false);
        String wineDebugChannels = this.preferences.getString("wine_debug_channels", "warn,err,fixme");
        EnvVars envVars = this.envVars;
        if (!enableWineDebug || wineDebugChannels.isEmpty()) {
            str = "-all";
        } else {
            str = "+" + wineDebugChannels.replace(",", ",+");
        }
        envVars.put("WINEDEBUG", str);
        String rootPath = this.imageFs.getRootDir().getPath();
        FileUtils.clear(this.imageFs.getTmpDir());
        GuestProgramLauncherComponent guestProgramLauncherComponent = new GuestProgramLauncherComponent();
        if (this.container != null) {
            if (this.killServices) {
                this.winHandler.killProcess("services.exe");
            }
            String desktopName = (this.shortcut != null || getIntent().hasExtra("exec_path")) ? "nogui" : "shell";
            String guestExecutable = this.wineInfo.getExecutable() + " explorer /desktop=" + desktopName + "," + this.xServer.screenInfo + " " + getWineStartCommand();
            guestProgramLauncherComponent.setGuestExecutable(guestExecutable);
            this.envVars.putAll(this.container.getEnvVars());
            Shortcut shortcut = this.shortcut;
            if (shortcut != null) {
                this.envVars.putAll(shortcut.getExtra("envVars"));
            }
            if (!this.envVars.has("WINEESYNC")) {
                this.envVars.put("WINEESYNC", "1");
            }
            Shortcut shortcut2 = this.shortcut;
            guestProgramLauncherComponent.setBox64Preset(shortcut2 != null ? shortcut2.getExtra("box64Preset", this.container.getBox64Preset()) : this.container.getBox64Preset());
        }
        XEnvironment xEnvironment = new XEnvironment(this, this.imageFs);
        this.environment = xEnvironment;
        xEnvironment.addComponent(new SysVSharedMemoryComponent(this.xServer, UnixSocketConfig.createSocket(rootPath, "/tmp/.sysvshm/SM0")));
        this.environment.addComponent(new XServerComponent(this.xServer, UnixSocketConfig.createSocket(rootPath, "/tmp/.X11-unix/X0")));
        this.environment.addComponent(new NetworkInfoUpdateComponent());
        if (this.audioDriver.equals("alsa")) {
            this.envVars.put("ANDROID_ALSA_SERVER", "/tmp/.sound/AS0");
            this.envVars.put("ANDROID_ASERVER_USE_SHM", "true");
            ALSAClient.Options options = ALSAClient.Options.fromKeyValueSet(this.audioDriverConfig);
            this.environment.addComponent(new ALSAServerComponent(UnixSocketConfig.createSocket(rootPath, "/tmp/.sound/AS0"), options));
        } else if (this.audioDriver.equals("pulseaudio")) {
            PulseAudioComponent pulseAudioComponent = new PulseAudioComponent(UnixSocketConfig.createSocket(rootPath, "/tmp/.sound/PS0"));
            this.envVars.put("PULSE_SERVER", "/tmp/.sound/PS0");
            KeyValueSet keyValueSet = this.audioDriverConfig;
            if (keyValueSet != null) {
                this.envVars.put("PULSE_LATENCY_MSEC", Integer.valueOf(keyValueSet.getInt("latencyMillis", 40)));
                pulseAudioComponent.setVolume(this.audioDriverConfig.getFloat("volume", 1.0f));
                pulseAudioComponent.setPerformanceMode(this.audioDriverConfig.getInt("performanceMode", 1));
                this.environment.addComponent(pulseAudioComponent);
            }
        }
        if (this.graphicsDriver.equals("virgl")) {
            this.environment.addComponent(new VirGLRendererComponent(this.xServer, UnixSocketConfig.createSocket(rootPath, "/tmp/.virgl/V0")));
        }
        guestProgramLauncherComponent.setEnvVars(this.envVars);
        guestProgramLauncherComponent.setTerminationCallback(new Callback() { // from class: com.winlator.XServerDisplayActivity$$ExternalSyntheticLambda3
            @Override // com.winlator.core.Callback
            public final void call(Object obj) {
                XServerDisplayActivity.this.lambda$setupXEnvironment$5((Integer) obj);
            }
        });
        this.environment.addComponent(guestProgramLauncherComponent);
        if (isGenerateWineprefix()) {
            generateWineprefix();
        }
        EnvVars envVars2 = this.overrideEnvVars;
        if (envVars2 != null) {
            this.envVars.putAll(envVars2);
            this.overrideEnvVars = null;
        }
        this.environment.startEnvironmentComponents();
        this.winHandler.start();
        this.envVars.clear();
        this.dxwrapperConfig = null;
        this.graphicsDriverConfig = null;
        this.audioDriverConfig = null;
        this.wincomponents = null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupXEnvironment$5(Integer status) {
        exit();
    }

    private void setupUI() {
        ControlsProfile profile;
        FrameLayout rootView = (FrameLayout) findViewById(R.id.FLXServerDisplay);
        XServerView xServerView = new XServerView(this, this.xServer);
        this.xServerView = xServerView;
        final GLRenderer renderer = xServerView.getRenderer();
        renderer.setCursorVisible(false);
        renderer.setCursorColor(this.preferences.getInt("cursor_color", 16777215));
        renderer.setCursorScale(this.preferences.getFloat("cursor_scale", 1.0f));
        Shortcut shortcut = this.shortcut;
        if (shortcut != null && shortcut.getExtra("forceFullscreen", "0").equals("1")) {
            renderer.setForceFullscreenWMClass(this.shortcut.wmClass);
        }
        if (this.shortcut != null || getIntent().hasExtra("exec_path")) {
            renderer.setUnviewableWMClasses("explorer.exe");
        }
        this.xServer.setRenderer(renderer);
        rootView.addView(this.xServerView);
        this.globalCursorSpeed = this.preferences.getFloat("cursor_speed", 1.0f);
        TouchpadView touchpadView = new TouchpadView(this, this.xServer);
        this.touchpadView = touchpadView;
        touchpadView.setSensitivity(this.globalCursorSpeed);
        this.touchpadView.setMoveCursorToTouchpoint(this.preferences.getBoolean("move_cursor_to_touchpoint", false));
        this.touchpadView.setFourFingersTapCallback(new Runnable() { // from class: com.winlator.XServerDisplayActivity$$ExternalSyntheticLambda9
            @Override // java.lang.Runnable
            public final void run() {
                XServerDisplayActivity.this.lambda$setupUI$6();
            }
        });
        rootView.addView(this.touchpadView);
        InputControlsView inputControlsView = new InputControlsView(this);
        this.inputControlsView = inputControlsView;
        inputControlsView.setOverlayOpacity(this.preferences.getFloat("overlay_opacity", 0.4f));
        this.inputControlsView.setTouchpadView(this.touchpadView);
        this.inputControlsView.setXServer(this.xServer);
        this.inputControlsView.setVisibility(8);
        rootView.addView(this.inputControlsView);
        Container container = this.container;
        if (container != null && container.isShowFPS()) {
            FrameRating frameRating = new FrameRating(this);
            this.frameRating = frameRating;
            frameRating.setVisibility(8);
            rootView.addView(this.frameRating);
        }
        Shortcut shortcut2 = this.shortcut;
        if (shortcut2 != null) {
            String controlsProfile = shortcut2.getExtra("controlsProfile");
            if (!controlsProfile.isEmpty() && (profile = this.inputControlsManager.getProfile(Integer.parseInt(controlsProfile))) != null) {
                showInputControls(profile);
            }
        }
        DrawerLayout drawerLayout = this.drawerLayout;
        Objects.requireNonNull(renderer);
        AppUtils.observeSoftKeyboardVisibility(drawerLayout, new Callback() { // from class: com.winlator.XServerDisplayActivity$$ExternalSyntheticLambda6
            @Override // com.winlator.core.Callback
            public final void call(Object obj) {
                GLRenderer.this.setScreenOffsetYRelativeToCursor(((Boolean) obj).booleanValue());
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$setupUI$6() {
        if (!this.drawerLayout.isDrawerOpen(8388611)) {
            this.drawerLayout.openDrawer(8388611);
        }
    }

    private void showInputControlsDialog() {
        ContentDialog dialog = new ContentDialog(this, R.layout.input_controls_dialog);
        dialog.setTitle(R.string.input_controls);
        dialog.setIcon(R.drawable.icon_input_controls);
        final Spinner sProfile = (Spinner) dialog.findViewById(R.id.SProfile);
        final Runnable loadProfileSpinner = new Runnable() { // from class: com.winlator.XServerDisplayActivity$$ExternalSyntheticLambda13
            @Override // java.lang.Runnable
            public final void run() {
                XServerDisplayActivity.this.lambda$showInputControlsDialog$7(sProfile);
            }
        };
        loadProfileSpinner.run();
        final CheckBox cbRelativeMouseMovement = (CheckBox) dialog.findViewById(R.id.CBRelativeMouseMovement);
        cbRelativeMouseMovement.setChecked(this.xServer.isRelativeMouseMovement());
        final CheckBox cbShowTouchscreenControls = (CheckBox) dialog.findViewById(R.id.CBShowTouchscreenControls);
        cbShowTouchscreenControls.setChecked(this.inputControlsView.isShowTouchscreenControls());
        dialog.findViewById(R.id.BTSettings).setOnClickListener(new View.OnClickListener() { // from class: com.winlator.XServerDisplayActivity$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                XServerDisplayActivity.this.lambda$showInputControlsDialog$9(sProfile, loadProfileSpinner, view);
            }
        });
        dialog.setOnConfirmCallback(new Runnable() { // from class: com.winlator.XServerDisplayActivity$$ExternalSyntheticLambda11
            @Override // java.lang.Runnable
            public final void run() {
                XServerDisplayActivity.this.lambda$showInputControlsDialog$10(cbRelativeMouseMovement, cbShowTouchscreenControls, sProfile);
            }
        });
        dialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showInputControlsDialog$7(Spinner sProfile) {
        ArrayList<ControlsProfile> profiles = this.inputControlsManager.getProfiles(true);
        ArrayList<String> profileItems = new ArrayList<>();
        int selectedPosition = 0;
        profileItems.add("-- " + getString(R.string.disabled) + " --");
        for (int i = 0; i < profiles.size(); i++) {
            ControlsProfile profile = profiles.get(i);
            if (profile == this.inputControlsView.getProfile()) {
                selectedPosition = i + 1;
            }
            profileItems.add(profile.getName());
        }
        sProfile.setAdapter((SpinnerAdapter) new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, profileItems));
        sProfile.setSelection(selectedPosition);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showInputControlsDialog$9(Spinner sProfile, final Runnable loadProfileSpinner, View v) {
        int position = sProfile.getSelectedItemPosition();
        Intent intent = new Intent(this, (Class<?>) MainActivity.class);
        intent.putExtra("edit_input_controls", true);
        intent.putExtra("selected_profile_id", position > 0 ? this.inputControlsManager.getProfiles().get(position - 1).id : 0);
        this.editInputControlsCallback = new Runnable() { // from class: com.winlator.XServerDisplayActivity$$ExternalSyntheticLambda15
            @Override // java.lang.Runnable
            public final void run() {
                XServerDisplayActivity.this.lambda$showInputControlsDialog$8(loadProfileSpinner);
            }
        };
        startActivityForResult(intent, 3);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showInputControlsDialog$8(Runnable loadProfileSpinner) {
        hideInputControls();
        this.inputControlsManager.loadProfiles(true);
        loadProfileSpinner.run();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showInputControlsDialog$10(CheckBox cbRelativeMouseMovement, CheckBox cbShowTouchscreenControls, Spinner sProfile) {
        this.xServer.setRelativeMouseMovement(cbRelativeMouseMovement.isChecked());
        this.inputControlsView.setShowTouchscreenControls(cbShowTouchscreenControls.isChecked());
        int position = sProfile.getSelectedItemPosition();
        if (position > 0) {
            showInputControls(this.inputControlsManager.getProfiles().get(position - 1));
        } else {
            hideInputControls();
        }
    }

    private void showInputControls(ControlsProfile profile) {
        this.inputControlsView.setVisibility(0);
        this.inputControlsView.requestFocus();
        this.inputControlsView.setProfile(profile);
        this.touchpadView.setSensitivity(profile.getCursorSpeed() * this.globalCursorSpeed);
        this.touchpadView.setPointerButtonRightEnabled(false);
        GLRenderer renderer = this.xServerView.getRenderer();
        if (profile.isDisableMouseInput()) {
            renderer.setCursorVisible(false);
            this.touchpadView.setEnabled(false);
        } else {
            renderer.setCursorVisible(true);
            this.touchpadView.setEnabled(true);
        }
        this.inputControlsView.invalidate();
    }

    private void hideInputControls() {
        this.inputControlsView.setShowTouchscreenControls(true);
        this.inputControlsView.setVisibility(8);
        this.inputControlsView.setProfile(null);
        this.touchpadView.setSensitivity(this.globalCursorSpeed);
        this.touchpadView.setPointerButtonLeftEnabled(true);
        this.touchpadView.setPointerButtonRightEnabled(true);
        if (!this.touchpadView.isEnabled()) {
            this.touchpadView.setEnabled(true);
            this.xServerView.getRenderer().setCursorVisible(true);
        }
        this.inputControlsView.invalidate();
    }

    private void extractGraphicsDriverFiles() {
        String str;
        String cacheId = this.graphicsDriver;
        if (this.graphicsDriver.equals("turnip")) {
            cacheId = cacheId + "-" + this.graphicsDriverConfig.get("version", "24.1.0") + "-22.2.5";
        } else if (this.graphicsDriver.equals("virgl")) {
            cacheId = cacheId + "-23.1.9";
        }
        boolean changed = !cacheId.equals(this.container.getExtra("graphicsDriver"));
        File rootDir = this.imageFs.getRootDir();
        if (changed) {
            File libDir = this.imageFs.getLibDir();
            FileUtils.delete(new File(libDir, "libvulkan_freedreno.so"));
            FileUtils.delete(new File(libDir, "libGL.so.1.7.0"));
            this.container.putExtra("graphicsDriver", cacheId);
            this.container.saveData();
        }
        if (this.graphicsDriver.equals("turnip")) {
            String version = this.graphicsDriverConfig.get("version", "24.1.0");
            if (this.dxwrapper.equals("dxvk")) {
                DXVKConfigDialog.setEnvVars(this, this.dxwrapperConfig, this.envVars);
            } else if (this.dxwrapper.equals("vkd3d")) {
                this.envVars.put("VKD3D_FEATURE_LEVEL", "12_2");
                this.envVars.put("VKD3D_SHADER_CACHE_PATH", "/home/xuser/.cache");
            }
            this.envVars.put("WINEVKUSEPLACEDADDR", "1");
            this.envVars.put("GALLIUM_DRIVER", "zink");
            this.envVars.put("TU_OVERRIDE_HEAP_SIZE", "4096");
            this.envVars.put("MESA_VK_WSI_PRESENT_MODE", "mailbox");
            if (this.graphicsDriverConfig.getBoolean("forceWaitForFences")) {
                this.envVars.put("MESA_VK_WSI_FORCE_WAIT_FOR_FENCES", "1");
            }
            this.envVars.put("vblank_mode", "0");
            if (!GPUInformation.isAdreno6xx(this)) {
                EnvVars userEnvVars = new EnvVars(this.container.getEnvVars());
                String tuDebug = userEnvVars.get("TU_DEBUG");
                if (!tuDebug.contains("sysmem")) {
                    StringBuilder sb = new StringBuilder();
                    if (tuDebug.isEmpty()) {
                        str = "";
                    } else {
                        str = tuDebug + ",";
                    }
                    sb.append(str);
                    sb.append("sysmem");
                    userEnvVars.put("TU_DEBUG", sb.toString());
                }
                this.container.setEnvVars(userEnvVars.toString());
            }
            if (changed) {
                TarCompressorUtils.Type type = TarCompressorUtils.Type.ZSTD;
                TarCompressorUtils.extract(type, this, "graphics_driver/turnip-" + version + ".tzst", rootDir);
                TarCompressorUtils.extract(type, this, "graphics_driver/zink-22.2.5.tzst", rootDir);
                return;
            }
            return;
        }
        if (this.graphicsDriver.equals("virgl")) {
            this.envVars.put("GALLIUM_DRIVER", "virpipe");
            this.envVars.put("VIRGL_NO_READBACK", "true");
            this.envVars.put("VIRGL_SERVER_PATH", "/tmp/.virgl/V0");
            if (this.graphicsDriverConfig.getBoolean("disableVertexArrayBGRA", true)) {
                this.envVars.put("MESA_EXTENSION_OVERRIDE", "-GL_EXT_vertex_array_bgra");
            }
            this.envVars.put("MESA_GL_VERSION_OVERRIDE", this.graphicsDriverConfig.get("glVersion", "3.1"));
            this.envVars.put("vblank_mode", "0");
            if (changed) {
                TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "graphics_driver/virgl-23.1.9.tzst", rootDir);
            }
        }
    }

    private void showTouchpadHelpDialog() {
        ContentDialog dialog = new ContentDialog(this, R.layout.touchpad_help_dialog);
        dialog.setTitle(R.string.touchpad_help);
        dialog.setIcon(R.drawable.icon_help);
        dialog.findViewById(R.id.BTCancel).setVisibility(8);
        dialog.show();
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        return (this.winHandler.onGenericMotionEvent(event) || this.touchpadView.onExternalMouseEvent(event) || !super.dispatchGenericMotionEvent(event)) ? false : true;
    }

    @Override // androidx.appcompat.app.AppCompatActivity, androidx.core.app.ComponentActivity, android.app.Activity, android.view.Window.Callback
    public boolean dispatchKeyEvent(KeyEvent event) {
        return !(this.inputControlsView.onKeyEvent(event) || this.winHandler.onKeyEvent(event) || !this.xServer.keyboard.onKeyEvent(event)) || (!ExternalController.isGameController(event.getDevice()) && super.dispatchKeyEvent(event));
    }

    public InputControlsView getInputControlsView() {
        return this.inputControlsView;
    }

    private void generateWineprefix() {
        Intent intent = getIntent();
        final File rootDir = this.imageFs.getRootDir();
        final File installedWineDir = this.imageFs.getInstalledWineDir();
        this.wineInfo = (WineInfo) intent.getParcelableExtra("wine_info");
        this.envVars.put("WINEARCH", "win64");
        this.imageFs.setWinePath(this.wineInfo.path);
        File containerPatternDir = new File(installedWineDir, "/preinstall/container-pattern");
        if (containerPatternDir.isDirectory()) {
            FileUtils.delete(containerPatternDir);
        }
        containerPatternDir.mkdirs();
        File linkFile = new File(rootDir, "/home/xuser");
        linkFile.delete();
        FileUtils.symlink(".." + FileUtils.toRelativePath(rootDir.getPath(), containerPatternDir.getPath()), linkFile.getPath());
        GuestProgramLauncherComponent guestProgramLauncherComponent = (GuestProgramLauncherComponent) this.environment.getComponent(GuestProgramLauncherComponent.class);
        guestProgramLauncherComponent.setBox64Preset("STABILITY");
        guestProgramLauncherComponent.setGuestExecutable(this.wineInfo.getExecutable() + " explorer /desktop=shell,1280x720 winecfg");
        final PreloaderDialog preloaderDialog = new PreloaderDialog(this);
        guestProgramLauncherComponent.setTerminationCallback(new Callback() { // from class: com.winlator.XServerDisplayActivity$$ExternalSyntheticLambda5
            @Override // com.winlator.core.Callback
            public final void call(Object obj) {
                XServerDisplayActivity.this.lambda$generateWineprefix$12(installedWineDir, preloaderDialog, rootDir, (Integer) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$generateWineprefix$12(final File installedWineDir, final PreloaderDialog preloaderDialog, final File rootDir, final Integer status) {
        Executors.newSingleThreadExecutor().execute(new Runnable() { // from class: com.winlator.XServerDisplayActivity$$ExternalSyntheticLambda14
            @Override // java.lang.Runnable
            public final void run() {
                XServerDisplayActivity.this.lambda$generateWineprefix$11(status, installedWineDir, preloaderDialog, rootDir);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$generateWineprefix$11(Integer status, File installedWineDir, PreloaderDialog preloaderDialog, File rootDir) {
        if (status.intValue() > 0) {
            AppUtils.showToast(this, R.string.unable_to_install_wine);
            FileUtils.delete(new File(installedWineDir, "/preinstall"));
            AppUtils.restartApplication(this);
            return;
        }
        preloaderDialog.showOnUiThread(R.string.finishing_installation);
        FileUtils.writeString(new File(rootDir, "/home/xuser/.wine/.update-timestamp"), "disable\n");
        File userDir = new File(rootDir, "/home/xuser/.wine/drive_c/users/xuser");
        File[] userFiles = userDir.listFiles();
        if (userFiles != null) {
            for (File userFile : userFiles) {
                if (FileUtils.isSymlink(userFile)) {
                    String path = userFile.getPath();
                    userFile.delete();
                    new File(path).mkdirs();
                }
            }
        }
        File containerPatternFile = new File(installedWineDir, "/preinstall/container-pattern-" + this.wineInfo.fullVersion() + ".tzst");
        TarCompressorUtils.compress(TarCompressorUtils.Type.ZSTD, new File(rootDir, "/home/xuser/.wine"), containerPatternFile, 9);
        if (!containerPatternFile.renameTo(new File(installedWineDir, containerPatternFile.getName())) || !new File(this.wineInfo.path).renameTo(new File(installedWineDir, this.wineInfo.identifier()))) {
            containerPatternFile.delete();
        }
        FileUtils.delete(new File(installedWineDir, "/preinstall"));
        preloaderDialog.closeOnUiThread();
        AppUtils.RestartApplicationOptions options = new AppUtils.RestartApplicationOptions();
        options.selectedMenuItemId = R.id.menu_item_settings;
        AppUtils.restartApplication(this, options);
    }

    private void extractDXWrapperFiles(String dxwrapper) {
        String[] dlls = {"d3d10.dll", "d3d10_1.dll", "d3d10core.dll", "d3d11.dll", "d3d12.dll", "d3d12core.dll", "d3d8.dll", "d3d9.dll", "dxgi.dll", "ddraw.dll"};
        if (this.firstTimeBoot && !dxwrapper.equals("vkd3d")) {
            cloneOriginalDllFiles(dlls);
        }
        File rootDir = this.imageFs.getRootDir();
        File windowsDir = new File(rootDir, "/home/xuser/.wine/drive_c/windows");
        char c = 65535;
        switch (dxwrapper.hashCode()) {
            case -1061449997:
                if (dxwrapper.equals("cnc-ddraw")) {
                    c = 1;
                    break;
                }
                break;
            case 112260896:
                if (dxwrapper.equals("vkd3d")) {
                    c = 2;
                    break;
                }
                break;
            case 1349510476:
                if (dxwrapper.equals("wined3d")) {
                    c = 0;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                restoreOriginalDllFiles(dlls);
                return;
            case 1:
                restoreOriginalDllFiles(dlls);
                File configFile = new File(rootDir, "/home/xuser/.wine/drive_c/ProgramData/cnc-ddraw/ddraw.ini");
                if (!configFile.isFile()) {
                    FileUtils.copy(this, "dxwrapper/cnc-ddraw-6.6/ddraw.ini", configFile);
                }
                File shadersDir = new File(rootDir, "/home/xuser/.wine/drive_c/ProgramData/cnc-ddraw/Shaders");
                FileUtils.delete(shadersDir);
                FileUtils.copy(this, "dxwrapper/cnc-ddraw-6.6/Shaders", shadersDir);
                TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "dxwrapper/cnc-ddraw-6.6/ddraw.tzst", windowsDir);
                return;
            case 2:
                String[] dxvkVersions = getResources().getStringArray(R.array.dxvk_version_entries);
                TarCompressorUtils.Type type = TarCompressorUtils.Type.ZSTD;
                TarCompressorUtils.extract(type, this, "dxwrapper/dxvk-" + dxvkVersions[dxvkVersions.length - 1] + ".tzst", windowsDir);
                TarCompressorUtils.extract(type, this, "dxwrapper/vkd3d-2.12.tzst", windowsDir);
                return;
            default:
                restoreOriginalDllFiles("d3d12.dll", "d3d12core.dll", "ddraw.dll");
                TarCompressorUtils.Type type2 = TarCompressorUtils.Type.ZSTD;
                TarCompressorUtils.extract(type2, this, "dxwrapper/" + dxwrapper + ".tzst", windowsDir);
                TarCompressorUtils.extract(type2, this, "dxwrapper/d8vk-1.0.tzst", windowsDir);
                return;
        }
    }

    private void extractWinComponentFiles() {
        JSONObject wincomponentsJSONObject;
        JSONObject wincomponentsJSONObject2;
        String str;
        File rootDir = this.imageFs.getRootDir();
        File windowsDir = new File(rootDir, "/home/xuser/.wine/drive_c/windows");
        File systemRegFile = new File(rootDir, "/home/xuser/.wine/system.reg");
        try {
            JSONObject wincomponentsJSONObject3 = new JSONObject(FileUtils.readString(this, "wincomponents/wincomponents.json"));
            ArrayList<String> dlls = new ArrayList<>();
            char c = 0;
            if (this.firstTimeBoot) {
                Iterator<String[]> it = new KeyValueSet(this.wincomponents).iterator();
                while (it.hasNext()) {
                    JSONArray dlnames = wincomponentsJSONObject3.getJSONArray(it.next()[0]);
                    for (int i = 0; i < dlnames.length(); i++) {
                        String dlname = dlnames.getString(i);
                        dlls.add(dlname.endsWith(".exe") ? dlname : dlname + ".dll");
                    }
                }
                cloneOriginalDllFiles((String[]) dlls.toArray(new String[0]));
                dlls.clear();
            }
            Iterator<String[]> oldWinComponentsIter = new KeyValueSet(this.container.getExtra("wincomponents", "direct3d=0,directsound=0,directmusic=0,directshow=0,directplay=0,xaudio=0,vcrun2010=0,wmdecoder=0")).iterator();
            Iterator<String[]> it2 = new KeyValueSet(this.wincomponents).iterator();
            while (it2.hasNext()) {
                String[] wincomponent = it2.next();
                if (!wincomponent[1].equals(oldWinComponentsIter.next()[1])) {
                    String identifier = wincomponent[c];
                    boolean useNative = wincomponent[1].equals("1");
                    if (useNative) {
                        TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this, "wincomponents/" + identifier + ".tzst", windowsDir);
                        wincomponentsJSONObject = wincomponentsJSONObject3;
                    } else {
                        JSONArray dlnames2 = wincomponentsJSONObject3.getJSONArray(identifier);
                        int i2 = 0;
                        while (i2 < dlnames2.length()) {
                            String dlname2 = dlnames2.getString(i2);
                            if (dlname2.endsWith(".exe")) {
                                wincomponentsJSONObject2 = wincomponentsJSONObject3;
                                str = dlname2;
                            } else {
                                wincomponentsJSONObject2 = wincomponentsJSONObject3;
                                str = dlname2 + ".dll";
                            }
                            dlls.add(str);
                            i2++;
                            wincomponentsJSONObject3 = wincomponentsJSONObject2;
                        }
                        wincomponentsJSONObject = wincomponentsJSONObject3;
                    }
                    WineUtils.setWinComponentRegistryKeys(systemRegFile, identifier, useNative);
                    wincomponentsJSONObject3 = wincomponentsJSONObject;
                    c = 0;
                }
            }
            if (!dlls.isEmpty()) {
                restoreOriginalDllFiles((String[]) dlls.toArray(new String[0]));
            }
            WineUtils.overrideWinComponentDlls(this, this.container, this.wincomponents);
        } catch (JSONException e) {
        }
    }

    private void restoreOriginalDllFiles(final String... dlls) {
        File rootDir = this.imageFs.getRootDir();
        File cacheDir = new File(rootDir, "/home/xuser/.cache/original_dlls");
        if (cacheDir.isDirectory()) {
            File windowsDir = new File(rootDir, "/home/xuser/.wine/drive_c/windows");
            String[] dirnames = cacheDir.list();
            int filesCopied = 0;
            int length = dlls.length;
            int i = 0;
            while (i < length) {
                String dll = dlls[i];
                boolean success = false;
                int length2 = dirnames.length;
                int i2 = 0;
                while (i2 < length2) {
                    String dirname = dirnames[i2];
                    StringBuilder sb = new StringBuilder();
                    sb.append(dirname);
                    File rootDir2 = rootDir;
                    sb.append("/");
                    sb.append(dll);
                    File srcFile = new File(cacheDir, sb.toString());
                    File cacheDir2 = cacheDir;
                    File dstFile = new File(windowsDir, dirname + "/" + dll);
                    if (FileUtils.copy(srcFile, dstFile)) {
                        success = true;
                    }
                    i2++;
                    rootDir = rootDir2;
                    cacheDir = cacheDir2;
                }
                File rootDir3 = rootDir;
                File cacheDir3 = cacheDir;
                if (success) {
                    filesCopied++;
                }
                i++;
                rootDir = rootDir3;
                cacheDir = cacheDir3;
            }
            if (filesCopied == dlls.length) {
                return;
            }
        }
        this.containerManager.extractContainerPatternFile(this.container.getWineVersion(), this.container.getRootDir(), new OnExtractFileListener() { // from class: com.winlator.XServerDisplayActivity$$ExternalSyntheticLambda7
            @Override // com.winlator.core.OnExtractFileListener
            public final File onExtractFile(File file, long j) {
                File lambda$restoreOriginalDllFiles$13;
                lambda$restoreOriginalDllFiles$13 = XServerDisplayActivity.lambda$restoreOriginalDllFiles$13(dlls, file, j);
                return lambda$restoreOriginalDllFiles$13;
            }
        });
        cloneOriginalDllFiles(dlls);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ File lambda$restoreOriginalDllFiles$13(String[] dlls, File file, long size) {
        String path = file.getPath();
        if (path.contains("system32/") || path.contains("syswow64/")) {
            for (String dll : dlls) {
                if (!path.endsWith("system32/" + dll)) {
                    if (!path.endsWith("syswow64/" + dll)) {
                    }
                }
                return file;
            }
            return null;
        }
        return null;
    }

    private void cloneOriginalDllFiles(String... dlls) {
        File rootDir = this.imageFs.getRootDir();
        File cacheDir = new File(rootDir, "/home/xuser/.cache/original_dlls");
        if (!cacheDir.isDirectory()) {
            cacheDir.mkdirs();
        }
        File windowsDir = new File(rootDir, "/home/xuser/.wine/drive_c/windows");
        String[] dirnames = {"system32", "syswow64"};
        for (String dll : dlls) {
            for (String dirname : dirnames) {
                File dllFile = new File(windowsDir, dirname + "/" + dll);
                if (dllFile.isFile()) {
                    FileUtils.copy(dllFile, new File(cacheDir, dirname + "/" + dll));
                }
            }
        }
    }

    private boolean isGenerateWineprefix() {
        return getIntent().getBooleanExtra("generate_wineprefix", false);
    }

    private String getWineStartCommand() {
        int spaceIndex;
        String str;
        String cmdArgs = "";
        String execPath = null;
        String execArgs = "";
        Shortcut shortcut = this.shortcut;
        if (shortcut != null) {
            String execArgs2 = shortcut.getExtra("execArgs");
            if (execArgs2.isEmpty()) {
                str = "";
            } else {
                str = " " + execArgs2;
            }
            execArgs = str;
            if (this.shortcut.path.endsWith(".lnk")) {
                cmdArgs = "\"" + this.shortcut.path + "\"" + execArgs;
            } else {
                execPath = this.shortcut.path;
            }
        } else {
            Intent intent = getIntent();
            if (intent.hasExtra("exec_path")) {
                execPath = WineUtils.unixToDOSPath(intent.getStringExtra("exec_path"), this.container);
                if (execPath.endsWith(".lnk")) {
                    cmdArgs = "\"" + execPath + "\"";
                    execPath = null;
                }
            }
        }
        if (execPath != null) {
            String execDir = FileUtils.getDirname(execPath);
            String filename = FileUtils.getName(execPath);
            int dotIndex = filename.lastIndexOf(".");
            if (dotIndex != -1 && (spaceIndex = filename.indexOf(" ", dotIndex)) != -1) {
                execArgs = filename.substring(spaceIndex + 1) + execArgs;
                filename = filename.substring(0, spaceIndex);
            }
            cmdArgs = "/dir " + StringUtils.escapeDOSPath(execDir) + " \"" + filename + "\"" + execArgs;
        }
        if (cmdArgs.isEmpty()) {
            cmdArgs = "\"wfm.exe\"";
        }
        EnvVars envVars = this.overrideEnvVars;
        if (envVars != null && envVars.has("EXTRA_EXEC_ARGS")) {
            cmdArgs = cmdArgs + " " + this.overrideEnvVars.get("EXTRA_EXEC_ARGS");
            this.overrideEnvVars.remove("EXTRA_EXEC_ARGS");
        }
        return "winhandler.exe " + cmdArgs;
    }

    public XServer getXServer() {
        return this.xServer;
    }

    public WinHandler getWinHandler() {
        return this.winHandler;
    }

    public XServerView getXServerView() {
        return this.xServerView;
    }

    public Container getContainer() {
        return this.container;
    }

    public EnvVars getOverrideEnvVars() {
        if (this.overrideEnvVars == null) {
            this.overrideEnvVars = new EnvVars();
        }
        return this.overrideEnvVars;
    }

    public void setGraphicsDriver(String graphicsDriver) {
        this.graphicsDriver = graphicsDriver;
    }

    public String getDXWrapper() {
        return this.dxwrapper;
    }

    public void setDXWrapper(String dxwrapper) {
        this.dxwrapper = dxwrapper;
    }

    public ScreenInfo getScreenInfo() {
        return this.screenInfo;
    }

    public void setScreenInfo(ScreenInfo screenInfo) {
        this.screenInfo = screenInfo;
    }

    public void setWinComponents(String wincomponents) {
        this.wincomponents = wincomponents;
    }

    public void setKillServices(boolean killServices) {
        this.killServices = killServices;
    }

    public String getScreenEffectProfile() {
        return this.screenEffectProfile;
    }

    public void setScreenEffectProfile(String screenEffectProfile) {
        this.screenEffectProfile = screenEffectProfile;
    }

    private void changeWineAudioDriver() {
        if (!this.audioDriver.equals(this.container.getExtra("audioDriver"))) {
            File rootDir = this.imageFs.getRootDir();
            File userRegFile = new File(rootDir, "/home/xuser/.wine/user.reg");
            WineRegistryEditor registryEditor = new WineRegistryEditor(userRegFile);
            try {
                if (this.audioDriver.equals("alsa")) {
                    registryEditor.setStringValue("Software\\Wine\\Drivers", "Audio", "alsa");
                } else if (this.audioDriver.equals("pulseaudio")) {
                    registryEditor.setStringValue("Software\\Wine\\Drivers", "Audio", "pulse");
                }
                registryEditor.close();
                this.container.putExtra("audioDriver", this.audioDriver);
                this.container.saveData();
            } catch (Throwable th) {
                try {
                    registryEditor.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        }
    }

    private void applyGeneralPatches(Container container) {
        File rootDir = this.imageFs.getRootDir();
        FileUtils.delete(new File(rootDir, "/opt/apps"));
        TarCompressorUtils.Type type = TarCompressorUtils.Type.ZSTD;
        TarCompressorUtils.extract(type, this, "imagefs_patches.tzst", rootDir);
        TarCompressorUtils.extract(type, this, "pulseaudio.tzst", new File(getFilesDir(), "pulseaudio"));
        WineUtils.applySystemTweaks(this, this.wineInfo);
        container.putExtra("graphicsDriver", null);
        container.putExtra("desktopTheme", null);
        SettingsFragment.resetBox64Version(this);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void changeFrameRatingVisibility(Window window, Property property) {
        if (this.frameRating == null) {
            return;
        }
        if (property != null) {
            if (this.frameRatingWindowId == -1 && window.attributes.isMapped() && property.nameAsString().equals("_MESA_DRV")) {
                this.frameRatingWindowId = window.id;
                return;
            }
            return;
        }
        if (window.id == this.frameRatingWindowId) {
            this.frameRatingWindowId = -1;
            runOnUiThread(new Runnable() { // from class: com.winlator.XServerDisplayActivity$$ExternalSyntheticLambda8
                @Override // java.lang.Runnable
                public final void run() {
                    XServerDisplayActivity.this.lambda$changeFrameRatingVisibility$14();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$changeFrameRatingVisibility$14() {
        this.frameRating.setVisibility(8);
    }
}
