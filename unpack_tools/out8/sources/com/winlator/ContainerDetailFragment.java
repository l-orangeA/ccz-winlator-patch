package com.winlator;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import com.winlatcn.R;
import com.winlator.ContainerDetailFragment;
import com.winlator.box64.Box64PresetManager;
import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.container.Drive;
import com.winlator.contentdialog.AddEnvVarDialog;
import com.winlator.contentdialog.AudioDriverConfigDialog;
import com.winlator.contentdialog.DXVKConfigDialog;
import com.winlator.contentdialog.TurnipConfigDialog;
import com.winlator.contentdialog.VirGLConfigDialog;
import com.winlator.core.AppUtils;
import com.winlator.core.Callback;
import com.winlator.core.EnvVars;
import com.winlator.core.FileUtils;
import com.winlator.core.KeyValueSet;
import com.winlator.core.PreloaderDialog;
import com.winlator.core.StringUtils;
import com.winlator.core.WineInfo;
import com.winlator.core.WineRegistryEditor;
import com.winlator.core.WineThemeManager;
import com.winlator.core.WineUtils;
import com.winlator.widget.CPUListView;
import com.winlator.widget.ColorPickerView;
import com.winlator.widget.EnvVarsView;
import com.winlator.widget.GPUCardAdapter;
import com.winlator.widget.ImagePickerView;
import com.winlator.widget.SeekBar;
import com.winlator.win32.WinVersions;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class ContainerDetailFragment extends Fragment {
    private Container container;
    private final int containerId;
    private ContainerManager manager;
    private Callback<String> openDirectoryCallback;
    private PreloaderDialog preloaderDialog;

    public ContainerDetailFragment() {
        this(0);
    }

    public ContainerDetailFragment(int containerId) {
        this.containerId = containerId;
    }

    @Override // androidx.fragment.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
        this.preloaderDialog = new PreloaderDialog(getActivity());
    }

    @Override // androidx.fragment.app.Fragment
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        String path;
        Callback<String> callback;
        if (requestCode == 4 && resultCode == -1) {
            if (data != null && (path = FileUtils.getFilePathFromUri(data.getData())) != null && (callback = this.openDirectoryCallback) != null) {
                callback.call(path);
            }
            this.openDirectoryCallback = null;
        }
    }

    @Override // androidx.fragment.app.Fragment
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(isEditMode() ? R.string.edit_container : R.string.new_container);
    }

    public boolean isEditMode() {
        return this.container != null;
    }

    /* JADX WARN: Removed duplicated region for block: B:38:0x0174  */
    /* JADX WARN: Removed duplicated region for block: B:41:0x0181  */
    /* JADX WARN: Removed duplicated region for block: B:44:0x01aa  */
    /* JADX WARN: Removed duplicated region for block: B:47:0x01dd  */
    /* JADX WARN: Removed duplicated region for block: B:50:0x01fa  */
    /* JADX WARN: Removed duplicated region for block: B:53:0x0216  */
    /* JADX WARN: Removed duplicated region for block: B:57:0x021d  */
    /* JADX WARN: Removed duplicated region for block: B:58:0x0202  */
    /* JADX WARN: Removed duplicated region for block: B:59:0x01e9  */
    /* JADX WARN: Removed duplicated region for block: B:60:0x01b5  */
    /* JADX WARN: Removed duplicated region for block: B:61:0x0183  */
    /* JADX WARN: Removed duplicated region for block: B:62:0x017b  */
    @Override // androidx.fragment.app.Fragment
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public android.view.View onCreateView(android.view.LayoutInflater r41, android.view.ViewGroup r42, android.os.Bundle r43) {
        /*
            Method dump skipped, instructions count: 662
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.winlator.ContainerDetailFragment.onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle):android.view.View");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$onCreateView$1(View v) {
        new AudioDriverConfigDialog(v).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreateView$2(Spinner sWinVersion, Integer tabResId) {
        if (tabResId.intValue() != R.id.LLTabAdvanced || ((Byte) sWinVersion.getTag()).byteValue() != -1) {
            return;
        }
        WinVersions.loadSpinner(this.container, sWinVersion);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r19v1, types: [org.json.JSONObject] */
    /* JADX WARN: Type inference failed for: r22v0, types: [com.winlator.ContainerDetailFragment, androidx.fragment.app.Fragment] */
    /* JADX WARN: Type inference failed for: r2v0, types: [android.view.View] */
    /* JADX WARN: Type inference failed for: r2v1 */
    /* JADX WARN: Type inference failed for: r2v2 */
    /* JADX WARN: Type inference failed for: r2v24, types: [int] */
    /* JADX WARN: Type inference failed for: r2v29, types: [java.lang.Object, java.lang.String] */
    /* JADX WARN: Type inference failed for: r2v3 */
    /* JADX WARN: Type inference failed for: r2v31, types: [com.winlator.container.ContainerManager] */
    /* JADX WARN: Type inference failed for: r2v32 */
    /* JADX WARN: Type inference failed for: r2v4 */
    /* JADX WARN: Type inference failed for: r2v5, types: [java.lang.String] */
    /* JADX WARN: Type inference failed for: r2v51, types: [android.view.View] */
    /* JADX WARN: Type inference failed for: r2v6 */
    /* JADX WARN: Type inference failed for: r2v7 */
    public /* synthetic */ void lambda$onCreateView$4(EditText etName, final View view, EnvVarsView envVarsView, Spinner sGraphicsDriver, Spinner sDXWrapper, View vDXWrapperConfig, View vGraphicsDriverConfig, View vAudioDriverConfig, Spinner sAudioDriver, CheckBox cbShowFPS, CPUListView cpuListView, CPUListView cpuListViewWoW64, Spinner sStartupSelection, Spinner sBox64Preset, ArrayList wineInfos, Spinner sWineVersion, View v) {
        ?? r2 = view;
        try {
            String name = etName.getText().toString();
            String screenSize = getScreenSize(view);
            String envVars = envVarsView.getEnvVars();
            String graphicsDriver = StringUtils.parseIdentifier(sGraphicsDriver.getSelectedItem());
            String dxwrapper = StringUtils.parseIdentifier(sDXWrapper.getSelectedItem());
            String dxwrapperConfig = vDXWrapperConfig.getTag().toString();
            String graphicsDriverConfig = vGraphicsDriverConfig.getTag().toString();
            String audioDriverConfig = vAudioDriverConfig.getTag().toString();
            String audioDriver = StringUtils.parseIdentifier(sAudioDriver.getSelectedItem());
            String wincomponents = getWinComponents(view);
            String drives = getDrives(r2);
            boolean showFPS = cbShowFPS.isChecked();
            String cpuList = cpuListView.getCheckedCPUListAsString();
            String cpuListWoW64 = cpuListViewWoW64.getCheckedCPUListAsString();
            byte startupSelection = (byte) sStartupSelection.getSelectedItemPosition();
            String box64Preset = Box64PresetManager.getSpinnerSelectedId(sBox64Preset);
            String desktopTheme = getDesktopTheme(r2);
            try {
                if (isEditMode()) {
                    this.container.setName(name);
                    this.container.setScreenSize(screenSize);
                    this.container.setEnvVars(envVars);
                    this.container.setCPUList(cpuList);
                    this.container.setCPUListWoW64(cpuListWoW64);
                    this.container.setGraphicsDriver(graphicsDriver);
                    this.container.setDXWrapper(dxwrapper);
                    this.container.setDXWrapperConfig(dxwrapperConfig);
                    this.container.setGraphicsDriverConfig(graphicsDriverConfig);
                    this.container.setAudioDriver(audioDriver);
                    this.container.setAudioDriverConfig(audioDriverConfig);
                    this.container.setWinComponents(wincomponents);
                    this.container.setDrives(drives);
                    this.container.setShowFPS(showFPS);
                    this.container.setStartupSelection(startupSelection);
                    this.container.setBox64Preset(box64Preset);
                    this.container.setDesktopTheme(desktopTheme);
                    this.container.saveData();
                    r2 = view;
                    saveWineRegistryKeys(r2);
                    getActivity().onBackPressed();
                } else {
                    ?? jSONObject = new JSONObject();
                    r2 = "name";
                    try {
                        jSONObject.put("name", name);
                        jSONObject.put("screenSize", screenSize);
                        jSONObject.put("envVars", envVars);
                        jSONObject.put("cpuList", cpuList);
                        jSONObject.put("cpuListWoW64", cpuListWoW64);
                        jSONObject.put("graphicsDriver", graphicsDriver);
                        jSONObject.put("dxwrapper", dxwrapper);
                        jSONObject.put("dxwrapperConfig", dxwrapperConfig);
                        jSONObject.put("graphicsDriverConfig", graphicsDriverConfig);
                        jSONObject.put("audioDriver", audioDriver);
                        jSONObject.put("audioDriverConfig", audioDriverConfig);
                        jSONObject.put("wincomponents", wincomponents);
                        jSONObject.put("drives", drives);
                        jSONObject.put("showFPS", showFPS);
                        jSONObject.put("startupSelection", startupSelection);
                        jSONObject.put("box64Preset", box64Preset);
                        jSONObject.put("desktopTheme", desktopTheme);
                        r2 = wineInfos.size();
                        if (r2 > 1) {
                            try {
                                r2 = ((WineInfo) wineInfos.get(sWineVersion.getSelectedItemPosition())).identifier();
                                jSONObject.put("wineVersion", r2);
                            } catch (JSONException e) {
                            }
                        }
                        this.preloaderDialog.lambda$showOnUiThread$0(R.string.creating_container);
                        r2 = this.manager;
                        try {
                            r2.createContainerAsync(jSONObject, new Callback() { // from class: com.winlator.ContainerDetailFragment$$ExternalSyntheticLambda9
                                @Override // com.winlator.core.Callback
                                public final void call(Object obj) {
                                    ContainerDetailFragment.this.lambda$onCreateView$3(view, (Container) obj);
                                }
                            });
                        } catch (JSONException e2) {
                        }
                    } catch (JSONException e3) {
                    }
                }
            } catch (JSONException e4) {
            }
        } catch (JSONException e5) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreateView$3(View view, Container container) {
        if (container != null) {
            this.container = container;
            saveWineRegistryKeys(view);
        }
        this.preloaderDialog.close();
        getActivity().onBackPressed();
    }

    private void saveWineRegistryKeys(View view) {
        Throwable th;
        int newPosition;
        File userRegFile = new File(this.container.getRootDir(), ".wine/user.reg");
        WineRegistryEditor registryEditor = new WineRegistryEditor(userRegFile);
        try {
            CheckBox cbCSMT = (CheckBox) view.findViewById(R.id.CBCSMT);
            registryEditor.setDwordValue("Software\\Wine\\Direct3D", "csmt", cbCSMT.isChecked() ? 3 : 0);
            Spinner sGPUName = (Spinner) view.findViewById(R.id.SGPUName);
            GPUCardAdapter.GPUCard gpuCard = (GPUCardAdapter.GPUCard) sGPUName.getSelectedItem();
            registryEditor.setDwordValue("Software\\Wine\\Direct3D", "VideoPciDeviceID", gpuCard.deviceId);
            registryEditor.setDwordValue("Software\\Wine\\Direct3D", "VideoPciVendorID", gpuCard.vendorId);
            Spinner sOffscreenRenderingMode = (Spinner) view.findViewById(R.id.SOffscreenRenderingMode);
            String obj = sOffscreenRenderingMode.getSelectedItem().toString();
            Locale locale = Locale.ENGLISH;
            registryEditor.setStringValue("Software\\Wine\\Direct3D", "OffScreenRenderingMode", obj.toLowerCase(locale));
            CheckBox cbStrictShaderMath = (CheckBox) view.findViewById(R.id.CBStrictShaderMath);
            registryEditor.setDwordValue("Software\\Wine\\Direct3D", "strict_shader_math", cbStrictShaderMath.isChecked() ? 1 : 0);
            Spinner sVideoMemorySize = (Spinner) view.findViewById(R.id.SVideoMemorySize);
            registryEditor.setStringValue("Software\\Wine\\Direct3D", "VideoMemorySize", StringUtils.parseNumber(sVideoMemorySize.getSelectedItem()));
            SeekBar sbLogPixels = (SeekBar) view.findViewById(R.id.SBLogPixels);
            registryEditor.setDwordValue("Control Panel\\Desktop", "LogPixels", (int) sbLogPixels.getValue());
            Spinner sMouseWarpOverride = (Spinner) view.findViewById(R.id.SMouseWarpOverride);
            try {
                registryEditor.setStringValue("Software\\Wine\\DirectInput", "MouseWarpOverride", sMouseWarpOverride.getSelectedItem().toString().toLowerCase(locale));
                registryEditor.setStringValue("Software\\Wine\\Direct3D", "shader_backend", "glsl");
                registryEditor.setStringValue("Software\\Wine\\Direct3D", "UseGLSL", "enabled");
                registryEditor.close();
                Spinner sWinVersion = (Spinner) view.findViewById(R.id.SWinVersion);
                int oldPosition = ((Byte) sWinVersion.getTag()).byteValue();
                if (oldPosition != -1 && oldPosition != (newPosition = sWinVersion.getSelectedItemPosition())) {
                    WineUtils.setWinVersion(this.container, newPosition);
                }
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

    private void createWineConfigurationTab(View view) {
        Throwable th;
        List<String> offscreenRenderingModeList;
        Spinner sOffscreenRenderingMode;
        Context context = getContext();
        WineThemeManager.ThemeInfo desktopTheme = new WineThemeManager.ThemeInfo(isEditMode() ? this.container.getDesktopTheme() : WineThemeManager.DEFAULT_DESKTOP_THEME);
        Spinner sDesktopTheme = (Spinner) view.findViewById(R.id.SDesktopTheme);
        sDesktopTheme.setSelection(desktopTheme.theme.ordinal());
        final ImagePickerView ipvDesktopBackgroundImage = (ImagePickerView) view.findViewById(R.id.IPVDesktopBackgroundImage);
        final ColorPickerView cpvDesktopBackgroundColor = (ColorPickerView) view.findViewById(R.id.CPVDesktopBackgroundColor);
        cpvDesktopBackgroundColor.setColor(desktopTheme.backgroundColor);
        Spinner sDesktopBackgroundType = (Spinner) view.findViewById(R.id.SDesktopBackgroundType);
        sDesktopBackgroundType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // from class: com.winlator.ContainerDetailFragment.1
            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onItemSelected(AdapterView<?> parent, View view2, int position, long id) {
                WineThemeManager.BackgroundType type = WineThemeManager.BackgroundType.values()[position];
                ipvDesktopBackgroundImage.setVisibility(8);
                cpvDesktopBackgroundColor.setVisibility(8);
                if (type == WineThemeManager.BackgroundType.IMAGE) {
                    ipvDesktopBackgroundImage.setVisibility(0);
                } else if (type == WineThemeManager.BackgroundType.COLOR) {
                    cpvDesktopBackgroundColor.setVisibility(0);
                }
            }

            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        sDesktopBackgroundType.setSelection(desktopTheme.backgroundType.ordinal());
        File containerDir = isEditMode() ? this.container.getRootDir() : null;
        File userRegFile = new File(containerDir, ".wine/user.reg");
        WineRegistryEditor registryEditor = new WineRegistryEditor(userRegFile);
        try {
            CheckBox cbCSMT = (CheckBox) view.findViewById(R.id.CBCSMT);
            cbCSMT.setChecked(registryEditor.getDwordValue("Software\\Wine\\Direct3D", "csmt", 3).intValue() != 0);
            GPUCardAdapter adapter = new GPUCardAdapter(context, android.R.layout.simple_spinner_dropdown_item);
            Spinner sGPUName = (Spinner) view.findViewById(R.id.SGPUName);
            sGPUName.setAdapter((SpinnerAdapter) adapter);
            sGPUName.setSelection(adapter.getPositionByDeviceId(registryEditor.getDwordValue("Software\\Wine\\Direct3D", "VideoPciDeviceID", 1728).intValue()));
            offscreenRenderingModeList = Arrays.asList("Backbuffer", "FBO");
            sOffscreenRenderingMode = (Spinner) view.findViewById(R.id.SOffscreenRenderingMode);
            try {
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (Throwable th3) {
            th = th3;
        }
        try {
            sOffscreenRenderingMode.setAdapter((SpinnerAdapter) new ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, offscreenRenderingModeList));
            AppUtils.setSpinnerSelectionFromValue(sOffscreenRenderingMode, registryEditor.getStringValue("Software\\Wine\\Direct3D", "OffScreenRenderingMode", "fbo"));
            CheckBox cbStrictShaderMath = (CheckBox) view.findViewById(R.id.CBStrictShaderMath);
            cbStrictShaderMath.setChecked(registryEditor.getDwordValue("Software\\Wine\\Direct3D", "strict_shader_math", 1).intValue() != 0);
            Spinner sVideoMemorySize = (Spinner) view.findViewById(R.id.SVideoMemorySize);
            String videoMemorySize = registryEditor.getStringValue("Software\\Wine\\Direct3D", "VideoMemorySize", "2048");
            AppUtils.setSpinnerSelectionFromNumber(sVideoMemorySize, videoMemorySize);
            SeekBar sbLogPixels = (SeekBar) view.findViewById(R.id.SBLogPixels);
            sbLogPixels.setValue(registryEditor.getDwordValue("Control Panel\\Desktop", "LogPixels", 96).intValue());
            List<String> mouseWarpOverrideList = Arrays.asList(context.getString(R.string.disable), context.getString(R.string.enable), context.getString(R.string.force));
            Spinner sMouseWarpOverride = (Spinner) view.findViewById(R.id.SMouseWarpOverride);
            sMouseWarpOverride.setAdapter((SpinnerAdapter) new ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, mouseWarpOverrideList));
            AppUtils.setSpinnerSelectionFromValue(sMouseWarpOverride, registryEditor.getStringValue("Software\\Wine\\DirectInput", "MouseWarpOverride", "disable"));
            registryEditor.close();
        } catch (Throwable th4) {
            th = th4;
            try {
                registryEditor.close();
                throw th;
            } catch (Throwable th5) {
                th.addSuppressed(th5);
                throw th;
            }
        }
    }

    public static String getScreenSize(View view) {
        Spinner sScreenSize = (Spinner) view.findViewById(R.id.SScreenSize);
        String value = sScreenSize.getSelectedItem().toString();
        if (value.equalsIgnoreCase("custom")) {
            value = "1280x720";
            String strWidth = ((EditText) view.findViewById(R.id.ETScreenWidth)).getText().toString().trim();
            String strHeight = ((EditText) view.findViewById(R.id.ETScreenHeight)).getText().toString().trim();
            if (strWidth.matches("[0-9]+") && strHeight.matches("[0-9]+")) {
                int width = Integer.parseInt(strWidth);
                int height = Integer.parseInt(strHeight);
                if (width % 2 == 0 && height % 2 == 0) {
                    return width + "x" + height;
                }
            }
        }
        return StringUtils.parseIdentifier(value);
    }

    private String getDesktopTheme(View view) {
        Spinner sDesktopBackgroundType = (Spinner) view.findViewById(R.id.SDesktopBackgroundType);
        WineThemeManager.BackgroundType type = WineThemeManager.BackgroundType.values()[sDesktopBackgroundType.getSelectedItemPosition()];
        Spinner sDesktopTheme = (Spinner) view.findViewById(R.id.SDesktopTheme);
        ColorPickerView cpvDesktopBackground = (ColorPickerView) view.findViewById(R.id.CPVDesktopBackgroundColor);
        WineThemeManager.Theme theme = WineThemeManager.Theme.values()[sDesktopTheme.getSelectedItemPosition()];
        String desktopTheme = theme + "," + type + "," + cpvDesktopBackground.getColorAsString();
        if (type == WineThemeManager.BackgroundType.IMAGE) {
            File userWallpaperFile = WineThemeManager.getUserWallpaperFile(getContext());
            StringBuilder sb = new StringBuilder();
            sb.append(desktopTheme);
            sb.append(",");
            sb.append(userWallpaperFile.isFile() ? Long.valueOf(userWallpaperFile.lastModified()) : "0");
            return sb.toString();
        }
        return desktopTheme;
    }

    public static void loadScreenSizeSpinner(View view, String selectedValue) {
        final Spinner sScreenSize = (Spinner) view.findViewById(R.id.SScreenSize);
        final LinearLayout llCustomScreenSize = (LinearLayout) view.findViewById(R.id.LLCustomScreenSize);
        sScreenSize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // from class: com.winlator.ContainerDetailFragment.2
            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onItemSelected(AdapterView<?> parent, View view2, int position, long id) {
                String value = sScreenSize.getItemAtPosition(position).toString();
                llCustomScreenSize.setVisibility(value.equalsIgnoreCase("custom") ? 0 : 8);
            }

            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        boolean found = AppUtils.setSpinnerSelectionFromIdentifier(sScreenSize, selectedValue);
        if (!found) {
            AppUtils.setSpinnerSelectionFromValue(sScreenSize, "custom");
            String[] screenSize = selectedValue.split("x");
            ((EditText) view.findViewById(R.id.ETScreenWidth)).setText(screenSize[0]);
            ((EditText) view.findViewById(R.id.ETScreenHeight)).setText(screenSize[1]);
        }
    }

    public static void loadGraphicsDriverSpinner(final Spinner sGraphicsDriver, final Spinner sDXWrapper, View vGraphicsDriverConfig, String selectedGraphicsDriver, final String selectedDXWrapper) {
        final Context context = sGraphicsDriver.getContext();
        final String[] dxwrapperEntries = context.getResources().getStringArray(R.array.dxwrapper_entries);
        Runnable update = new Runnable() { // from class: com.winlator.ContainerDetailFragment$$ExternalSyntheticLambda12
            @Override // java.lang.Runnable
            public final void run() {
                ContainerDetailFragment.lambda$loadGraphicsDriverSpinner$5(sGraphicsDriver, dxwrapperEntries, sDXWrapper, context, selectedDXWrapper);
            }
        };
        sGraphicsDriver.setOnItemSelectedListener(new AnonymousClass3(sGraphicsDriver, vGraphicsDriverConfig, update));
        AppUtils.setSpinnerSelectionFromIdentifier(sGraphicsDriver, selectedGraphicsDriver);
        update.run();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$loadGraphicsDriverSpinner$5(Spinner sGraphicsDriver, String[] dxwrapperEntries, Spinner sDXWrapper, Context context, String selectedDXWrapper) {
        String graphicsDriver = StringUtils.parseIdentifier(sGraphicsDriver.getSelectedItem());
        boolean addAll = graphicsDriver.equals("turnip");
        ArrayList<String> items = new ArrayList<>();
        for (String value : dxwrapperEntries) {
            if (addAll || (!value.equalsIgnoreCase("dxvk") && !value.equalsIgnoreCase("vkd3d"))) {
                items.add(value);
            }
        }
        sDXWrapper.setAdapter((SpinnerAdapter) new ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, (String[]) items.toArray(new String[0])));
        AppUtils.setSpinnerSelectionFromIdentifier(sDXWrapper, selectedDXWrapper);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.winlator.ContainerDetailFragment$3, reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass3 implements AdapterView.OnItemSelectedListener {
        final /* synthetic */ Spinner val$sGraphicsDriver;
        final /* synthetic */ Runnable val$update;
        final /* synthetic */ View val$vGraphicsDriverConfig;

        AnonymousClass3(Spinner spinner, View view, Runnable runnable) {
            this.val$sGraphicsDriver = spinner;
            this.val$vGraphicsDriverConfig = view;
            this.val$update = runnable;
        }

        @Override // android.widget.AdapterView.OnItemSelectedListener
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            String graphicsDriver = StringUtils.parseIdentifier(this.val$sGraphicsDriver.getSelectedItem());
            this.val$vGraphicsDriverConfig.setVisibility(8);
            if (graphicsDriver.equals("turnip")) {
                final View view2 = this.val$vGraphicsDriverConfig;
                view2.setOnClickListener(new View.OnClickListener() { // from class: com.winlator.ContainerDetailFragment$3$$ExternalSyntheticLambda0
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view3) {
                        ContainerDetailFragment.AnonymousClass3.lambda$onItemSelected$0(view2, view3);
                    }
                });
                this.val$vGraphicsDriverConfig.setVisibility(0);
            } else if (graphicsDriver.equals("virgl")) {
                final View view3 = this.val$vGraphicsDriverConfig;
                view3.setOnClickListener(new View.OnClickListener() { // from class: com.winlator.ContainerDetailFragment$3$$ExternalSyntheticLambda1
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view4) {
                        ContainerDetailFragment.AnonymousClass3.lambda$onItemSelected$1(view3, view4);
                    }
                });
                this.val$vGraphicsDriverConfig.setVisibility(0);
            }
            this.val$update.run();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static /* synthetic */ void lambda$onItemSelected$0(View vGraphicsDriverConfig, View v) {
            new TurnipConfigDialog(vGraphicsDriverConfig).show();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static /* synthetic */ void lambda$onItemSelected$1(View vGraphicsDriverConfig, View v) {
            new VirGLConfigDialog(vGraphicsDriverConfig).show();
        }

        @Override // android.widget.AdapterView.OnItemSelectedListener
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.winlator.ContainerDetailFragment$4, reason: invalid class name */
    /* loaded from: classes.dex */
    public class AnonymousClass4 implements AdapterView.OnItemSelectedListener {
        final /* synthetic */ Spinner val$sDXWrapper;
        final /* synthetic */ View val$vDXWrapperConfig;

        AnonymousClass4(Spinner spinner, View view) {
            this.val$sDXWrapper = spinner;
            this.val$vDXWrapperConfig = view;
        }

        @Override // android.widget.AdapterView.OnItemSelectedListener
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            String dxwrapper = StringUtils.parseIdentifier(this.val$sDXWrapper.getSelectedItem());
            if (dxwrapper.equals("dxvk")) {
                final View view2 = this.val$vDXWrapperConfig;
                view2.setOnClickListener(new View.OnClickListener() { // from class: com.winlator.ContainerDetailFragment$4$$ExternalSyntheticLambda0
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view3) {
                        ContainerDetailFragment.AnonymousClass4.lambda$onItemSelected$0(view2, view3);
                    }
                });
                this.val$vDXWrapperConfig.setVisibility(0);
                return;
            }
            this.val$vDXWrapperConfig.setVisibility(8);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static /* synthetic */ void lambda$onItemSelected$0(View vDXWrapperConfig, View v) {
            new DXVKConfigDialog(vDXWrapperConfig).show();
        }

        @Override // android.widget.AdapterView.OnItemSelectedListener
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    public static void setupDXWrapperSpinner(Spinner sDXWrapper, View vDXWrapperConfig) {
        sDXWrapper.setOnItemSelectedListener(new AnonymousClass4(sDXWrapper, vDXWrapperConfig));
    }

    public static String getWinComponents(View view) {
        ViewGroup parent = (ViewGroup) view.findViewById(R.id.LLTabWinComponents);
        ArrayList<View> views = new ArrayList<>();
        AppUtils.findViewsWithClass(parent, Spinner.class, views);
        String[] wincomponents = new String[views.size()];
        for (int i = 0; i < views.size(); i++) {
            Spinner spinner = (Spinner) views.get(i);
            wincomponents[i] = spinner.getTag() + "=" + spinner.getSelectedItemPosition();
        }
        return String.join(",", wincomponents);
    }

    public static void createWinComponentsTab(View view, String wincomponents) {
        Context context = view.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        ViewGroup tabView = (ViewGroup) view.findViewById(R.id.LLTabWinComponents);
        ViewGroup directxSectionView = (ViewGroup) tabView.findViewById(R.id.LLWinComponentsDirectX);
        ViewGroup generalSectionView = (ViewGroup) tabView.findViewById(R.id.LLWinComponentsGeneral);
        Iterator<String[]> it = new KeyValueSet(wincomponents).iterator();
        while (it.hasNext()) {
            String[] wincomponent = it.next();
            String name = wincomponent[0];
            ViewGroup parent = (name.startsWith("direct") || name.startsWith("x")) ? directxSectionView : generalSectionView;
            View itemView = inflater.inflate(R.layout.wincomponent_list_item, parent, false);
            ((TextView) itemView.findViewById(R.id.TextView)).setText(StringUtils.getString(context, name));
            Spinner spinner = (Spinner) itemView.findViewById(R.id.Spinner);
            spinner.setSelection(Integer.parseInt(wincomponent[1]), false);
            spinner.setTag(name);
            parent.addView(itemView);
        }
    }

    private EnvVarsView createEnvVarsTab(View view) {
        final Context context = view.getContext();
        final EnvVarsView envVarsView = (EnvVarsView) view.findViewById(R.id.EnvVarsView);
        envVarsView.setEnvVars(new EnvVars(isEditMode() ? this.container.getEnvVars() : "ZINK_DESCRIPTORS=lazy ZINK_DEBUG=compact MESA_SHADER_CACHE_DISABLE=false MESA_SHADER_CACHE_MAX_SIZE=512MB MESA_VK_WSI_PRESENT_MODE=mailbox MESA_VK_WSI_DEBUG=(sw mesa_glthread=true WINEESYNC=1 DXVK_HUD=fps,version,api,gpuload TU_DEBUG=noconform LC_ALL=zh_CN.utf8"));
        view.findViewById(R.id.BTAddEnvVar).setOnClickListener(new View.OnClickListener() { // from class: com.winlator.ContainerDetailFragment$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                ContainerDetailFragment.lambda$createEnvVarsTab$6(context, envVarsView, view2);
            }
        });
        return envVarsView;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$createEnvVarsTab$6(Context context, EnvVarsView envVarsView, View v) {
        new AddEnvVarDialog(context, envVarsView).show();
    }

    private String getDrives(View view) {
        LinearLayout parent = (LinearLayout) view.findViewById(R.id.LLDrives);
        String drives = "";
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            Spinner spinner = (Spinner) child.findViewById(R.id.Spinner);
            EditText editText = (EditText) child.findViewById(R.id.EditText);
            String path = editText.getText().toString().replace(":", "").trim();
            if (!path.isEmpty()) {
                drives = drives + spinner.getSelectedItem() + path;
            }
        }
        return drives;
    }

    private void createDrivesTab(View view) {
        final Context context = getContext();
        final LinearLayout parent = (LinearLayout) view.findViewById(R.id.LLDrives);
        final View emptyTextView = view.findViewById(R.id.TVDrivesEmptyText);
        final LayoutInflater inflater = LayoutInflater.from(context);
        String drives = isEditMode() ? this.container.getDrives() : Container.DEFAULT_DRIVES;
        final String[] driveLetters = new String[8];
        for (int i = 0; i < driveLetters.length; i++) {
            driveLetters[i] = ((char) (i + 68)) + ":";
        }
        final Callback<Drive> addItem = new Callback() { // from class: com.winlator.ContainerDetailFragment$$ExternalSyntheticLambda8
            @Override // com.winlator.core.Callback
            public final void call(Object obj) {
                ContainerDetailFragment.this.lambda$createDrivesTab$9(inflater, parent, context, driveLetters, emptyTextView, (Drive) obj);
            }
        };
        for (Drive drive : Container.drivesIterator(drives)) {
            addItem.call(drive);
        }
        view.findViewById(R.id.BTAddDrive).setOnClickListener(new View.OnClickListener() { // from class: com.winlator.ContainerDetailFragment$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                ContainerDetailFragment.lambda$createDrivesTab$10(parent, driveLetters, addItem, view2);
            }
        });
        if (drives.isEmpty()) {
            emptyTextView.setVisibility(0);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$createDrivesTab$9(LayoutInflater inflater, final LinearLayout parent, Context context, String[] driveLetters, final View emptyTextView, final Drive drive) {
        final View itemView = inflater.inflate(R.layout.drive_list_item, (ViewGroup) parent, false);
        Spinner spinner = (Spinner) itemView.findViewById(R.id.Spinner);
        spinner.setAdapter((SpinnerAdapter) new ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, driveLetters));
        AppUtils.setSpinnerSelectionFromValue(spinner, drive.letter + ":");
        final EditText editText = (EditText) itemView.findViewById(R.id.EditText);
        editText.setText(drive.path);
        itemView.findViewById(R.id.BTSearch).setOnClickListener(new View.OnClickListener() { // from class: com.winlator.ContainerDetailFragment$$ExternalSyntheticLambda5
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ContainerDetailFragment.this.lambda$createDrivesTab$7(drive, editText, view);
            }
        });
        itemView.findViewById(R.id.BTRemove).setOnClickListener(new View.OnClickListener() { // from class: com.winlator.ContainerDetailFragment$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ContainerDetailFragment.lambda$createDrivesTab$8(parent, itemView, emptyTextView, view);
            }
        });
        parent.addView(itemView);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$createDrivesTab$8(LinearLayout parent, View itemView, View emptyTextView, View v) {
        parent.removeView(itemView);
        if (parent.getChildCount() == 0) {
            emptyTextView.setVisibility(0);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$createDrivesTab$10(LinearLayout parent, String[] driveLetters, Callback addItem, View v) {
        if (parent.getChildCount() >= 8) {
            return;
        }
        String nextDriveLetter = String.valueOf(driveLetters[parent.getChildCount()].charAt(0));
        addItem.call(new Drive(nextDriveLetter, ""));
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: showDriveSearchPopupMenu, reason: merged with bridge method [inline-methods] */
    public void lambda$createDrivesTab$7(View anchorView, final Drive drive, final EditText editText) {
        final FragmentActivity activity = getActivity();
        PopupMenu popupMenu = new PopupMenu(activity, anchorView);
        if (Build.VERSION.SDK_INT >= 29) {
            popupMenu.setForceShowIcon(true);
        }
        popupMenu.inflate(R.menu.drive_search_popup_menu);
        Menu menu = popupMenu.getMenu();
        SubMenu subMenu = menu.findItem(R.id.menu_item_locations).getSubMenu();
        ArrayList<Container> containers = this.manager.getContainers();
        for (int i = 0; i < containers.size(); i++) {
            Container container = containers.get(i);
            subMenu.add(0, 0, container.id, container.getName() + " (Drive C:)");
        }
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() { // from class: com.winlator.ContainerDetailFragment$$ExternalSyntheticLambda7
            @Override // android.widget.PopupMenu.OnMenuItemClickListener
            public final boolean onMenuItemClick(MenuItem menuItem) {
                boolean lambda$showDriveSearchPopupMenu$12;
                lambda$showDriveSearchPopupMenu$12 = ContainerDetailFragment.this.lambda$showDriveSearchPopupMenu$12(drive, editText, activity, this, menuItem);
                return lambda$showDriveSearchPopupMenu$12;
            }
        });
        popupMenu.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean lambda$showDriveSearchPopupMenu$12(final Drive drive, final EditText editText, FragmentActivity activity, Fragment $this, MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        switch (itemId) {
            case R.id.menu_item_downloads /* 2131296680 */:
                String str = AppUtils.DIRECTORY_DOWNLOADS;
                drive.path = str;
                editText.setText(str);
                return true;
            case R.id.menu_item_internal_storage /* 2131296689 */:
                drive.path = "/data/data/com.winlator/storage";
                editText.setText("/data/data/com.winlator/storage");
                return true;
            case R.id.menu_item_open_directory /* 2131296695 */:
                this.openDirectoryCallback = new Callback() { // from class: com.winlator.ContainerDetailFragment$$ExternalSyntheticLambda11
                    @Override // com.winlator.core.Callback
                    public final void call(Object obj) {
                        ContainerDetailFragment.lambda$showDriveSearchPopupMenu$11(Drive.this, editText, (String) obj);
                    }
                };
                Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT_TREE");
                intent.putExtra("android.provider.extra.INITIAL_URI", Uri.fromFile(Environment.getExternalStorageDirectory()));
                activity.startActivityFromFragment($this, intent, 4);
                return true;
            default:
                Container container = this.manager.getContainerById(menuItem.getOrder());
                if (container != null) {
                    String path = container.getRootDir() + "/.wine/drive_c";
                    drive.path = path;
                    editText.setText(path);
                    return true;
                }
                return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$showDriveSearchPopupMenu$11(Drive drive, EditText editText, String path) {
        drive.path = path;
        editText.setText(path);
    }

    private void loadWineVersionSpinner(View view, Spinner sWineVersion, ArrayList<WineInfo> wineInfos) {
        Context context = getContext();
        sWineVersion.setEnabled(!isEditMode());
        view.findViewById(R.id.LLWineVersion).setVisibility(0);
        sWineVersion.setAdapter((SpinnerAdapter) new ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, wineInfos));
        if (isEditMode()) {
            AppUtils.setSpinnerSelectionFromValue(sWineVersion, WineInfo.fromIdentifier(context, this.container.getWineVersion()).toString());
        }
    }
}
