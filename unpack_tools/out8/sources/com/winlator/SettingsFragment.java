package com.winlator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;
import com.google.android.material.navigation.NavigationView;
import com.winlatcn.R;
import com.winlator.box64.Box64EditPresetDialog;
import com.winlator.box64.Box64PresetManager;
import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.contentdialog.ContentDialog;
import com.winlator.core.AppUtils;
import com.winlator.core.ArrayUtils;
import com.winlator.core.Callback;
import com.winlator.core.FileUtils;
import com.winlator.core.PreloaderDialog;
import com.winlator.core.StringUtils;
import com.winlator.core.WineInfo;
import com.winlator.core.WineUtils;
import com.winlator.widget.ColorPickerView;
import com.winlator.widget.LogView;
import com.winlator.widget.SeekBar;
import com.winlator.winhandler.WinHandler;
import com.winlator.xenvironment.ImageFs;
import com.winlator.xenvironment.ImageFsInstaller;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import org.json.JSONArray;
import org.json.JSONException;

/* loaded from: classes.dex */
public class SettingsFragment extends Fragment {
    private SharedPreferences preferences;
    private PreloaderDialog preloaderDialog;
    private Callback<Uri> selectWineFileCallback;

    @Override // androidx.fragment.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
        this.preloaderDialog = new PreloaderDialog(getActivity());
    }

    @Override // androidx.fragment.app.Fragment
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.settings);
    }

    @Override // androidx.fragment.app.Fragment
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 2 && resultCode == -1) {
            try {
                Callback<Uri> callback = this.selectWineFileCallback;
                if (callback != null && data != null) {
                    callback.call(data.getData());
                }
            } catch (Exception e) {
                AppUtils.showToast(getContext(), R.string.unable_to_import_profile);
            }
            this.selectWineFileCallback = null;
        }
    }

    @Override // androidx.fragment.app.Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.settings_fragment, container, false);
        final Context context = getContext();
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
        final Spinner sBox64Version = (Spinner) view.findViewById(R.id.SBox64Version);
        String box64Version = this.preferences.getString("box64_version", "0.3.0");
        if (!AppUtils.setSpinnerSelectionFromIdentifier(sBox64Version, box64Version)) {
            AppUtils.setSpinnerSelectionFromIdentifier(sBox64Version, "0.3.0");
        }
        final Spinner sBox64Preset = (Spinner) view.findViewById(R.id.SBox64Preset);
        loadBox64PresetSpinner(view, sBox64Preset);
        final CheckBox cbMoveCursorToTouchpoint = (CheckBox) view.findViewById(R.id.CBMoveCursorToTouchpoint);
        cbMoveCursorToTouchpoint.setChecked(this.preferences.getBoolean("move_cursor_to_touchpoint", false));
        final CheckBox cbEnableWineDebug = (CheckBox) view.findViewById(R.id.CBEnableWineDebug);
        cbEnableWineDebug.setChecked(this.preferences.getBoolean("enable_wine_debug", false));
        final ArrayList<String> wineDebugChannels = new ArrayList<>(Arrays.asList(this.preferences.getString("wine_debug_channels", "warn,err,fixme").split(",")));
        loadWineDebugChannels(view, wineDebugChannels);
        final CheckBox cbEnableBox64Logs = (CheckBox) view.findViewById(R.id.CBEnableBox64Logs);
        cbEnableBox64Logs.setChecked(this.preferences.getBoolean("enable_box64_logs", false));
        final CheckBox cbSaveLogsToFile = (CheckBox) view.findViewById(R.id.CBSaveLogsToFile);
        cbSaveLogsToFile.setChecked(this.preferences.getBoolean("save_logs_to_file", false));
        final EditText etLogFile = (EditText) view.findViewById(R.id.ETLogFile);
        final String defaultLogPath = LogView.getLogFile().getPath();
        etLogFile.setText(this.preferences.getString("log_file", defaultLogPath));
        etLogFile.setVisibility(cbSaveLogsToFile.isChecked() ? 0 : 8);
        cbSaveLogsToFile.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: com.winlator.SettingsFragment$$ExternalSyntheticLambda11
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                SettingsFragment.lambda$onCreateView$0(etLogFile, compoundButton, z);
            }
        });
        final SeekBar sbCursorSpeed = (SeekBar) view.findViewById(R.id.SBCursorSpeed);
        sbCursorSpeed.setValue(this.preferences.getFloat("cursor_speed", 1.0f) * 100.0f);
        final SeekBar sbCursorSize = (SeekBar) view.findViewById(R.id.SBCursorSize);
        sbCursorSize.setValue(this.preferences.getFloat("cursor_scale", 1.0f) * 100.0f);
        final ColorPickerView cpvCursorColor = (ColorPickerView) view.findViewById(R.id.CPVCursorColor);
        cpvCursorColor.setPalette(16777215, 0, 6627327, 16771584, 16748800, 16056407, 45311, 1960374);
        cpvCursorColor.setColor(this.preferences.getInt("cursor_color", 16777215));
        final Spinner sPreferredInputApi = (Spinner) view.findViewById(R.id.SDPreferredInputApi);
        sPreferredInputApi.setSelection(this.preferences.getInt("preferred_input_api", WinHandler.PreferredInputApi.BOTH.ordinal()));
        Spinner sWineVersion = (Spinner) view.findViewById(R.id.SWineVersion);
        lambda$loadWineVersionSpinner$12(view, sWineVersion);
        view.findViewById(R.id.BTReinstallSystemFiles).setOnClickListener(new View.OnClickListener() { // from class: com.winlator.SettingsFragment$$ExternalSyntheticLambda5
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                SettingsFragment.this.lambda$onCreateView$2(context, view2);
            }
        });
        view.findViewById(R.id.BTConfirm).setOnClickListener(new View.OnClickListener() { // from class: com.winlator.SettingsFragment$$ExternalSyntheticLambda7
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                SettingsFragment.this.lambda$onCreateView$3(sBox64Version, sBox64Preset, cbMoveCursorToTouchpoint, sbCursorSpeed, sbCursorSize, cpvCursorColor, cbEnableWineDebug, cbEnableBox64Logs, cbSaveLogsToFile, sPreferredInputApi, etLogFile, defaultLogPath, wineDebugChannels, view2);
            }
        });
        return view;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$onCreateView$0(EditText etLogFile, CompoundButton buttonView, boolean isChecked) {
        etLogFile.setVisibility(isChecked ? 0 : 8);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreateView$1() {
        ImageFsInstaller.install((MainActivity) getActivity());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreateView$2(Context context, View v) {
        ContentDialog.confirm(context, R.string.do_you_want_to_reinstall_system_files, new Runnable() { // from class: com.winlator.SettingsFragment$$ExternalSyntheticLambda20
            @Override // java.lang.Runnable
            public final void run() {
                SettingsFragment.this.lambda$onCreateView$1();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreateView$3(Spinner sBox64Version, Spinner sBox64Preset, CheckBox cbMoveCursorToTouchpoint, SeekBar sbCursorSpeed, SeekBar sbCursorSize, ColorPickerView cpvCursorColor, CheckBox cbEnableWineDebug, CheckBox cbEnableBox64Logs, CheckBox cbSaveLogsToFile, Spinner sPreferredInputApi, EditText etLogFile, String defaultLogPath, ArrayList wineDebugChannels, View v) {
        SharedPreferences.Editor editor = this.preferences.edit();
        editor.putString("box64_version", StringUtils.parseIdentifier(sBox64Version.getSelectedItem()));
        editor.putString("box64_preset", Box64PresetManager.getSpinnerSelectedId(sBox64Preset));
        editor.putBoolean("move_cursor_to_touchpoint", cbMoveCursorToTouchpoint.isChecked());
        editor.putFloat("cursor_speed", sbCursorSpeed.getValue() / 100.0f);
        editor.putFloat("cursor_scale", sbCursorSize.getValue() / 100.0f);
        editor.putInt("cursor_color", cpvCursorColor.getColor());
        editor.putBoolean("enable_wine_debug", cbEnableWineDebug.isChecked());
        editor.putBoolean("enable_box64_logs", cbEnableBox64Logs.isChecked());
        editor.putBoolean("save_logs_to_file", cbSaveLogsToFile.isChecked());
        editor.putInt("preferred_input_api", sPreferredInputApi.getSelectedItemPosition());
        String logPath = etLogFile.getText().toString().trim();
        if (!logPath.equals(defaultLogPath) && !logPath.isEmpty()) {
            editor.putString("log_file", logPath);
        } else {
            editor.remove("log_file");
        }
        if (wineDebugChannels.isEmpty()) {
            if (this.preferences.contains("wine_debug_channels")) {
                editor.remove("wine_debug_channels");
            }
        } else {
            editor.putString("wine_debug_channels", String.join(",", wineDebugChannels));
        }
        if (editor.commit()) {
            NavigationView navigationView = (NavigationView) getActivity().findViewById(R.id.NavigationView);
            navigationView.setCheckedItem(R.id.menu_item_containers);
            FragmentManager fragmentManager = getParentFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.FLFragmentContainer, new ContainersFragment()).commit();
        }
    }

    private void loadBox64PresetSpinner(View view, final Spinner sBox64Preset) {
        final Context context = getContext();
        final Runnable updateSpinner = new Runnable() { // from class: com.winlator.SettingsFragment$$ExternalSyntheticLambda22
            @Override // java.lang.Runnable
            public final void run() {
                SettingsFragment.this.lambda$loadBox64PresetSpinner$4(sBox64Preset);
            }
        };
        updateSpinner.run();
        view.findViewById(R.id.BTAddBox64Preset).setOnClickListener(new View.OnClickListener() { // from class: com.winlator.SettingsFragment$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                SettingsFragment.lambda$loadBox64PresetSpinner$5(context, updateSpinner, view2);
            }
        });
        view.findViewById(R.id.BTEditBox64Preset).setOnClickListener(new View.OnClickListener() { // from class: com.winlator.SettingsFragment$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                SettingsFragment.lambda$loadBox64PresetSpinner$6(context, sBox64Preset, updateSpinner, view2);
            }
        });
        view.findViewById(R.id.BTDuplicateBox64Preset).setOnClickListener(new View.OnClickListener() { // from class: com.winlator.SettingsFragment$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                SettingsFragment.lambda$loadBox64PresetSpinner$7(context, sBox64Preset, updateSpinner, view2);
            }
        });
        view.findViewById(R.id.BTRemoveBox64Preset).setOnClickListener(new View.OnClickListener() { // from class: com.winlator.SettingsFragment$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                SettingsFragment.lambda$loadBox64PresetSpinner$9(sBox64Preset, context, updateSpinner, view2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$loadBox64PresetSpinner$4(Spinner sBox64Preset) {
        Box64PresetManager.loadSpinner(sBox64Preset, this.preferences.getString("box64_preset", "COMPATIBILITY"));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$loadBox64PresetSpinner$5(Context context, Runnable updateSpinner, View v) {
        Box64EditPresetDialog dialog = new Box64EditPresetDialog(context, null);
        dialog.setOnConfirmCallback(updateSpinner);
        dialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$loadBox64PresetSpinner$6(Context context, Spinner sBox64Preset, Runnable updateSpinner, View v) {
        Box64EditPresetDialog dialog = new Box64EditPresetDialog(context, Box64PresetManager.getSpinnerSelectedId(sBox64Preset));
        dialog.setOnConfirmCallback(updateSpinner);
        dialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$loadBox64PresetSpinner$7(Context context, Spinner sBox64Preset, Runnable updateSpinner, View v) {
        Box64PresetManager.duplicatePreset(context, Box64PresetManager.getSpinnerSelectedId(sBox64Preset));
        updateSpinner.run();
        sBox64Preset.setSelection(sBox64Preset.getCount() - 1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$loadBox64PresetSpinner$9(Spinner sBox64Preset, final Context context, final Runnable updateSpinner, View v) {
        final String presetId = Box64PresetManager.getSpinnerSelectedId(sBox64Preset);
        if (!presetId.startsWith("CUSTOM")) {
            AppUtils.showToast(context, R.string.you_cannot_remove_this_preset);
        } else {
            ContentDialog.confirm(context, R.string.do_you_want_to_remove_this_preset, new Runnable() { // from class: com.winlator.SettingsFragment$$ExternalSyntheticLambda17
                @Override // java.lang.Runnable
                public final void run() {
                    SettingsFragment.lambda$loadBox64PresetSpinner$8(context, presetId, updateSpinner);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$loadBox64PresetSpinner$8(Context context, String presetId, Runnable updateSpinner) {
        Box64PresetManager.removePreset(context, presetId);
        updateSpinner.run();
    }

    private void removeInstalledWine(WineInfo wineInfo, final Runnable onSuccess) {
        final Activity activity = getActivity();
        ContainerManager manager = new ContainerManager(activity);
        ArrayList<Container> containers = manager.getContainers();
        Iterator<Container> it = containers.iterator();
        while (it.hasNext()) {
            Container container = it.next();
            if (container.getWineVersion().equals(wineInfo.identifier())) {
                AppUtils.showToast(activity, R.string.unable_to_remove_this_wine_version);
                return;
            }
        }
        File installedWineDir = ImageFs.find(activity).getInstalledWineDir();
        final File wineDir = new File(wineInfo.path);
        final File containerPatternFile = new File(installedWineDir, "container-pattern-" + wineInfo.fullVersion() + ".tzst");
        if (!wineDir.isDirectory() || !containerPatternFile.isFile()) {
            AppUtils.showToast(activity, R.string.unable_to_remove_this_wine_version);
        } else {
            this.preloaderDialog.lambda$showOnUiThread$0(R.string.removing_wine);
            Executors.newSingleThreadExecutor().execute(new Runnable() { // from class: com.winlator.SettingsFragment$$ExternalSyntheticLambda26
                @Override // java.lang.Runnable
                public final void run() {
                    SettingsFragment.this.lambda$removeInstalledWine$10(wineDir, containerPatternFile, onSuccess, activity);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$removeInstalledWine$10(File wineDir, File containerPatternFile, Runnable onSuccess, Activity activity) {
        FileUtils.delete(wineDir);
        FileUtils.delete(containerPatternFile);
        this.preloaderDialog.closeOnUiThread();
        if (onSuccess != null) {
            activity.runOnUiThread(onSuccess);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: loadWineVersionSpinner, reason: merged with bridge method [inline-methods] */
    public void lambda$loadWineVersionSpinner$12(final View view, final Spinner sWineVersion) {
        final Context context = getContext();
        final ArrayList<WineInfo> wineInfos = WineUtils.getInstalledWineInfos(context);
        sWineVersion.setAdapter((SpinnerAdapter) new ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, wineInfos));
        view.findViewById(R.id.BTInstallWine).setOnClickListener(new View.OnClickListener() { // from class: com.winlator.SettingsFragment$$ExternalSyntheticLambda4
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                SettingsFragment.this.lambda$loadWineVersionSpinner$11(context, view2);
            }
        });
        view.findViewById(R.id.BTRemoveWine).setOnClickListener(new View.OnClickListener() { // from class: com.winlator.SettingsFragment$$ExternalSyntheticLambda10
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                SettingsFragment.this.lambda$loadWineVersionSpinner$14(wineInfos, sWineVersion, view, view2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$loadWineVersionSpinner$11(Context context, View v) {
        ContentDialog.alert(context, R.string.msg_warning_install_wine, new Runnable() { // from class: com.winlator.SettingsFragment$$ExternalSyntheticLambda19
            @Override // java.lang.Runnable
            public final void run() {
                SettingsFragment.this.selectWineFileForInstall();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$loadWineVersionSpinner$14(ArrayList wineInfos, final Spinner sWineVersion, final View view, View v) {
        final WineInfo wineInfo = (WineInfo) wineInfos.get(sWineVersion.getSelectedItemPosition());
        if (wineInfo != WineInfo.MAIN_WINE_VERSION) {
            ContentDialog.confirm(getContext(), R.string.do_you_want_to_remove_this_wine_version, new Runnable() { // from class: com.winlator.SettingsFragment$$ExternalSyntheticLambda25
                @Override // java.lang.Runnable
                public final void run() {
                    SettingsFragment.this.lambda$loadWineVersionSpinner$13(wineInfo, view, sWineVersion);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$loadWineVersionSpinner$13(WineInfo wineInfo, final View view, final Spinner sWineVersion) {
        removeInstalledWine(wineInfo, new Runnable() { // from class: com.winlator.SettingsFragment$$ExternalSyntheticLambda21
            @Override // java.lang.Runnable
            public final void run() {
                SettingsFragment.this.lambda$loadWineVersionSpinner$12(view, sWineVersion);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void selectWineFileForInstall() {
        final Context context = getContext();
        this.selectWineFileCallback = new Callback() { // from class: com.winlator.SettingsFragment$$ExternalSyntheticLambda12
            @Override // com.winlator.core.Callback
            public final void call(Object obj) {
                SettingsFragment.this.lambda$selectWineFileForInstall$18(context, (Uri) obj);
            }
        };
        Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT");
        intent.addCategory("android.intent.category.OPENABLE");
        intent.setType("*/*");
        getActivity().startActivityFromFragment(this, intent, 2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$selectWineFileForInstall$18(final Context context, Uri uri) {
        this.preloaderDialog.lambda$showOnUiThread$0(R.string.preparing_installation);
        WineUtils.extractWineFileForInstallAsync(context, uri, new Callback() { // from class: com.winlator.SettingsFragment$$ExternalSyntheticLambda14
            @Override // com.winlator.core.Callback
            public final void call(Object obj) {
                SettingsFragment.this.lambda$selectWineFileForInstall$17(context, (File) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$selectWineFileForInstall$17(final Context context, File wineDir) {
        if (wineDir != null) {
            WineUtils.findWineVersionAsync(context, wineDir, new Callback() { // from class: com.winlator.SettingsFragment$$ExternalSyntheticLambda13
                @Override // com.winlator.core.Callback
                public final void call(Object obj) {
                    SettingsFragment.this.lambda$selectWineFileForInstall$16(context, (WineInfo) obj);
                }
            });
        } else {
            AppUtils.showToast(context, R.string.unable_to_install_wine);
            this.preloaderDialog.closeOnUiThread();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$selectWineFileForInstall$16(Context context, final WineInfo wineInfo) {
        this.preloaderDialog.closeOnUiThread();
        if (wineInfo == null) {
            AppUtils.showToast(context, R.string.unable_to_install_wine);
        } else {
            getActivity().runOnUiThread(new Runnable() { // from class: com.winlator.SettingsFragment$$ExternalSyntheticLambda23
                @Override // java.lang.Runnable
                public final void run() {
                    SettingsFragment.this.lambda$selectWineFileForInstall$15(wineInfo);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: installWine, reason: merged with bridge method [inline-methods] */
    public void lambda$showWineInstallDialog$21(WineInfo wineInfo) {
        Context context = getContext();
        File installedWineDir = ImageFs.find(context).getInstalledWineDir();
        File wineDir = new File(installedWineDir, wineInfo.identifier());
        if (wineDir.isDirectory()) {
            AppUtils.showToast(context, R.string.unable_to_install_wine);
            return;
        }
        Intent intent = new Intent(context, (Class<?>) XServerDisplayActivity.class);
        intent.putExtra("generate_wineprefix", true);
        intent.putExtra("wine_info", wineInfo);
        context.startActivity(intent);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: showWineInstallDialog, reason: merged with bridge method [inline-methods] */
    public void lambda$selectWineFileForInstall$15(final WineInfo wineInfo) {
        String str;
        Context context = getContext();
        ContentDialog dialog = new ContentDialog(context, R.layout.wine_install_dialog);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setTitle(R.string.install_wine);
        dialog.setIcon(R.drawable.icon_wine);
        EditText etVersion = (EditText) dialog.findViewById(R.id.ETVersion);
        StringBuilder sb = new StringBuilder();
        sb.append("Wine ");
        sb.append(wineInfo.version);
        if (wineInfo.subversion != null) {
            str = " (" + wineInfo.subversion + ")";
        } else {
            str = "";
        }
        sb.append(str);
        etVersion.setText(sb.toString());
        final EditText etSize = (EditText) dialog.findViewById(R.id.ETSize);
        final AtomicLong totalSizeRef = new AtomicLong();
        FileUtils.getSizeAsync(new File(wineInfo.path), new Callback() { // from class: com.winlator.SettingsFragment$$ExternalSyntheticLambda16
            @Override // com.winlator.core.Callback
            public final void call(Object obj) {
                SettingsFragment.lambda$showWineInstallDialog$20(totalSizeRef, etSize, (Long) obj);
            }
        });
        dialog.setOnConfirmCallback(new Runnable() { // from class: com.winlator.SettingsFragment$$ExternalSyntheticLambda24
            @Override // java.lang.Runnable
            public final void run() {
                SettingsFragment.this.lambda$showWineInstallDialog$21(wineInfo);
            }
        });
        dialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$showWineInstallDialog$20(final AtomicLong totalSizeRef, final EditText etSize, Long size) {
        totalSizeRef.addAndGet(size.longValue());
        etSize.post(new Runnable() { // from class: com.winlator.SettingsFragment$$ExternalSyntheticLambda18
            @Override // java.lang.Runnable
            public final void run() {
                SettingsFragment.lambda$showWineInstallDialog$19(etSize, totalSizeRef);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$showWineInstallDialog$19(EditText etSize, AtomicLong totalSizeRef) {
        etSize.setText(StringUtils.formatBytes(totalSizeRef.get()));
    }

    private void loadWineDebugChannels(final View view, final ArrayList<String> debugChannels) {
        final Context context = getContext();
        LinearLayout container = (LinearLayout) view.findViewById(R.id.LLWineDebugChannels);
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(context);
        int i = R.layout.wine_debug_channel_list_item;
        boolean z = false;
        View itemView = inflater.inflate(R.layout.wine_debug_channel_list_item, (ViewGroup) container, false);
        itemView.findViewById(R.id.TextView).setVisibility(8);
        itemView.findViewById(R.id.BTRemove).setVisibility(8);
        View addButton = itemView.findViewById(R.id.BTAdd);
        addButton.setVisibility(0);
        addButton.setOnClickListener(new View.OnClickListener() { // from class: com.winlator.SettingsFragment$$ExternalSyntheticLambda6
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                SettingsFragment.this.lambda$loadWineDebugChannels$23(context, debugChannels, view, view2);
            }
        });
        View resetButton = itemView.findViewById(R.id.BTReset);
        resetButton.setVisibility(0);
        resetButton.setOnClickListener(new View.OnClickListener() { // from class: com.winlator.SettingsFragment$$ExternalSyntheticLambda9
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                SettingsFragment.this.lambda$loadWineDebugChannels$24(debugChannels, view, view2);
            }
        });
        container.addView(itemView);
        int i2 = 0;
        while (i2 < debugChannels.size()) {
            View itemView2 = inflater.inflate(i, container, z);
            TextView textView = (TextView) itemView2.findViewById(R.id.TextView);
            textView.setText(debugChannels.get(i2));
            final int index = i2;
            itemView2.findViewById(R.id.BTRemove).setOnClickListener(new View.OnClickListener() { // from class: com.winlator.SettingsFragment$$ExternalSyntheticLambda8
                @Override // android.view.View.OnClickListener
                public final void onClick(View view2) {
                    SettingsFragment.this.lambda$loadWineDebugChannels$25(debugChannels, index, view, view2);
                }
            });
            container.addView(itemView2);
            i2++;
            i = R.layout.wine_debug_channel_list_item;
            z = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$loadWineDebugChannels$23(Context context, final ArrayList debugChannels, final View view, View v) {
        JSONArray jsonArray = null;
        try {
            jsonArray = new JSONArray(FileUtils.readString(context, "wine_debug_channels.json"));
        } catch (JSONException e) {
        }
        final String[] items = ArrayUtils.toStringArray(jsonArray);
        ContentDialog.showMultipleChoiceList(context, R.string.wine_debug_channel, items, new Callback() { // from class: com.winlator.SettingsFragment$$ExternalSyntheticLambda15
            @Override // com.winlator.core.Callback
            public final void call(Object obj) {
                SettingsFragment.this.lambda$loadWineDebugChannels$22(debugChannels, items, view, (ArrayList) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$loadWineDebugChannels$22(ArrayList debugChannels, String[] items, View view, ArrayList selectedPositions) {
        Iterator it = selectedPositions.iterator();
        while (it.hasNext()) {
            int selectedPosition = ((Integer) it.next()).intValue();
            if (!debugChannels.contains(items[selectedPosition])) {
                debugChannels.add(items[selectedPosition]);
            }
        }
        loadWineDebugChannels(view, debugChannels);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$loadWineDebugChannels$24(ArrayList debugChannels, View view, View v) {
        debugChannels.clear();
        debugChannels.addAll(Arrays.asList("warn,err,fixme".split(",")));
        loadWineDebugChannels(view, debugChannels);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$loadWineDebugChannels$25(ArrayList debugChannels, int index, View view, View v) {
        debugChannels.remove(index);
        loadWineDebugChannels(view, debugChannels);
    }

    public static void resetBox64Version(AppCompatActivity activity) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("box64_version", "0.3.0");
        editor.remove("current_box64_version");
        editor.apply();
    }
}
