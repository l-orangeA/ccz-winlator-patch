package com.winlator.contentdialog;

import android.content.Context;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import com.winlatcn.R;
import com.winlator.ContainerDetailFragment;
import com.winlator.ShortcutsFragment;
import com.winlator.box64.Box64PresetManager;
import com.winlator.container.Shortcut;
import com.winlator.core.AppUtils;
import com.winlator.core.EnvVars;
import com.winlator.core.StringUtils;
import com.winlator.inputcontrols.ControlsProfile;
import com.winlator.inputcontrols.InputControlsManager;
import com.winlator.widget.EnvVarsView;
import java.io.File;
import java.util.ArrayList;

/* loaded from: classes.dex */
public class ShortcutSettingsDialog extends ContentDialog {
    private final ShortcutsFragment fragment;
    private InputControlsManager inputControlsManager;
    private final Shortcut shortcut;

    public ShortcutSettingsDialog(ShortcutsFragment fragment, Shortcut shortcut) {
        super(fragment.getContext(), R.layout.shortcut_settings_dialog);
        this.fragment = fragment;
        this.shortcut = shortcut;
        setTitle(shortcut.name);
        setIcon(R.drawable.icon_settings);
        createContentView();
    }

    private void createContentView() {
        final Context context = this.fragment.getContext();
        this.inputControlsManager = new InputControlsManager(context);
        LinearLayout llContent = (LinearLayout) findViewById(R.id.LLContent);
        llContent.getLayoutParams().width = AppUtils.getPreferredDialogWidth(context);
        final EditText etName = (EditText) findViewById(R.id.ETName);
        etName.setText(this.shortcut.name);
        final EditText etExecArgs = (EditText) findViewById(R.id.ETExecArgs);
        etExecArgs.setText(this.shortcut.getExtra("execArgs"));
        View contentView = getContentView();
        Shortcut shortcut = this.shortcut;
        ContainerDetailFragment.loadScreenSizeSpinner(contentView, shortcut.getExtra("screenSize", shortcut.container.getScreenSize()));
        final Spinner sGraphicsDriver = (Spinner) findViewById(R.id.SGraphicsDriver);
        final Spinner sDXWrapper = (Spinner) findViewById(R.id.SDXWrapper);
        final View vDXWrapperConfig = findViewById(R.id.BTDXWrapperConfig);
        Shortcut shortcut2 = this.shortcut;
        vDXWrapperConfig.setTag(shortcut2.getExtra("dxwrapperConfig", shortcut2.container.getDXWrapperConfig()));
        final View vGraphicsDriverConfig = findViewById(R.id.BTGraphicsDriverConfig);
        Shortcut shortcut3 = this.shortcut;
        vGraphicsDriverConfig.setTag(shortcut3.getExtra("graphicsDriverConfig", shortcut3.container.getGraphicsDriverConfig()));
        final View vAudioDriverConfig = findViewById(R.id.BTAudioDriverConfig);
        Shortcut shortcut4 = this.shortcut;
        vAudioDriverConfig.setTag(shortcut4.getExtra("audioDriverConfig", shortcut4.container.getAudioDriverConfig()));
        vAudioDriverConfig.setOnClickListener(new View.OnClickListener() { // from class: com.winlator.contentdialog.ShortcutSettingsDialog$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ShortcutSettingsDialog.lambda$createContentView$0(view);
            }
        });
        ContainerDetailFragment.setupDXWrapperSpinner(sDXWrapper, vDXWrapperConfig);
        Shortcut shortcut5 = this.shortcut;
        String extra = shortcut5.getExtra("graphicsDriver", shortcut5.container.getGraphicsDriver());
        Shortcut shortcut6 = this.shortcut;
        ContainerDetailFragment.loadGraphicsDriverSpinner(sGraphicsDriver, sDXWrapper, vGraphicsDriverConfig, extra, shortcut6.getExtra("dxwrapper", shortcut6.container.getDXWrapper()));
        findViewById(R.id.BTHelpDXWrapper).setOnClickListener(new View.OnClickListener() { // from class: com.winlator.contentdialog.ShortcutSettingsDialog$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                AppUtils.showHelpBox(context, view, R.string.dxwrapper_help_content);
            }
        });
        final Spinner sAudioDriver = (Spinner) findViewById(R.id.SAudioDriver);
        Shortcut shortcut7 = this.shortcut;
        AppUtils.setSpinnerSelectionFromIdentifier(sAudioDriver, shortcut7.getExtra("audioDriver", shortcut7.container.getAudioDriver()));
        final CheckBox cbForceFullscreen = (CheckBox) findViewById(R.id.CBForceFullscreen);
        cbForceFullscreen.setChecked(this.shortcut.getExtra("forceFullscreen", "0").equals("1"));
        final Spinner sBox64Preset = (Spinner) findViewById(R.id.SBox64Preset);
        Shortcut shortcut8 = this.shortcut;
        Box64PresetManager.loadSpinner(sBox64Preset, shortcut8.getExtra("box64Preset", shortcut8.container.getBox64Preset()));
        final Spinner sControlsProfile = (Spinner) findViewById(R.id.SControlsProfile);
        loadControlsProfileSpinner(sControlsProfile, this.shortcut.getExtra("controlsProfile", "0"));
        final Spinner sDInputMapperType = (Spinner) findViewById(R.id.SDInputMapperType);
        sDInputMapperType.setSelection(Byte.parseByte(this.shortcut.getExtra("dinputMapperType", String.valueOf(1))));
        View contentView2 = getContentView();
        Shortcut shortcut9 = this.shortcut;
        ContainerDetailFragment.createWinComponentsTab(contentView2, shortcut9.getExtra("wincomponents", shortcut9.container.getWinComponents()));
        final EnvVarsView envVarsView = createEnvVarsTab();
        AppUtils.setupTabLayout(getContentView(), R.id.TabLayout, R.id.LLTabWinComponents, R.id.LLTabEnvVars, R.id.LLTabAdvanced);
        findViewById(R.id.BTExtraArgsMenu).setOnClickListener(new View.OnClickListener() { // from class: com.winlator.contentdialog.ShortcutSettingsDialog$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ShortcutSettingsDialog.lambda$createContentView$3(context, etExecArgs, view);
            }
        });
        setOnConfirmCallback(new Runnable() { // from class: com.winlator.contentdialog.ShortcutSettingsDialog$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                ShortcutSettingsDialog.this.lambda$createContentView$4(etName, sGraphicsDriver, sDXWrapper, vDXWrapperConfig, vGraphicsDriverConfig, vAudioDriverConfig, sAudioDriver, etExecArgs, cbForceFullscreen, envVarsView, sBox64Preset, sControlsProfile, sDInputMapperType);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$createContentView$0(View v) {
        new AudioDriverConfigDialog(v).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$createContentView$3(Context context, final EditText etExecArgs, View v) {
        PopupMenu popupMenu = new PopupMenu(context, v);
        popupMenu.inflate(R.menu.extra_args_popup_menu);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() { // from class: com.winlator.contentdialog.ShortcutSettingsDialog$$ExternalSyntheticLambda4
            @Override // android.widget.PopupMenu.OnMenuItemClickListener
            public final boolean onMenuItemClick(MenuItem menuItem) {
                boolean lambda$createContentView$2;
                lambda$createContentView$2 = ShortcutSettingsDialog.lambda$createContentView$2(etExecArgs, menuItem);
                return lambda$createContentView$2;
            }
        });
        popupMenu.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ boolean lambda$createContentView$2(EditText etExecArgs, MenuItem menuItem) {
        String str;
        String value = String.valueOf(menuItem.getTitle());
        String execArgs = etExecArgs.getText().toString();
        if (!execArgs.contains(value)) {
            if (execArgs.isEmpty()) {
                str = value;
            } else {
                str = execArgs + " " + value;
            }
            etExecArgs.setText(str);
            return true;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$createContentView$4(EditText etName, Spinner sGraphicsDriver, Spinner sDXWrapper, View vDXWrapperConfig, View vGraphicsDriverConfig, View vAudioDriverConfig, Spinner sAudioDriver, EditText etExecArgs, CheckBox cbForceFullscreen, EnvVarsView envVarsView, Spinner sBox64Preset, Spinner sControlsProfile, Spinner sDInputMapperType) {
        String name = etName.getText().toString().trim();
        if (!this.shortcut.name.equals(name) && !name.isEmpty()) {
            renameShortcut(name);
            return;
        }
        String graphicsDriver = StringUtils.parseIdentifier(sGraphicsDriver.getSelectedItem());
        String dxwrapper = StringUtils.parseIdentifier(sDXWrapper.getSelectedItem());
        String dxwrapperConfig = vDXWrapperConfig.getTag().toString();
        String graphicsDriverConfig = vGraphicsDriverConfig.getTag().toString();
        String audioDriverConfig = vAudioDriverConfig.getTag().toString();
        String audioDriver = StringUtils.parseIdentifier(sAudioDriver.getSelectedItem());
        String screenSize = ContainerDetailFragment.getScreenSize(getContentView());
        String execArgs = etExecArgs.getText().toString();
        this.shortcut.putExtra("execArgs", !execArgs.isEmpty() ? execArgs : null);
        Shortcut shortcut = this.shortcut;
        shortcut.putExtra("screenSize", !screenSize.equals(shortcut.container.getScreenSize()) ? screenSize : null);
        Shortcut shortcut2 = this.shortcut;
        shortcut2.putExtra("graphicsDriver", !graphicsDriver.equals(shortcut2.container.getGraphicsDriver()) ? graphicsDriver : null);
        Shortcut shortcut3 = this.shortcut;
        shortcut3.putExtra("dxwrapper", !dxwrapper.equals(shortcut3.container.getDXWrapper()) ? dxwrapper : null);
        Shortcut shortcut4 = this.shortcut;
        shortcut4.putExtra("dxwrapperConfig", !dxwrapperConfig.equals(shortcut4.container.getDXWrapperConfig()) ? dxwrapperConfig : null);
        Shortcut shortcut5 = this.shortcut;
        shortcut5.putExtra("graphicsDriverConfig", !graphicsDriverConfig.equals(shortcut5.container.getGraphicsDriverConfig()) ? graphicsDriverConfig : null);
        Shortcut shortcut6 = this.shortcut;
        shortcut6.putExtra("audioDriver", !audioDriver.equals(shortcut6.container.getAudioDriver()) ? audioDriver : null);
        Shortcut shortcut7 = this.shortcut;
        shortcut7.putExtra("audioDriverConfig", !audioDriverConfig.equals(shortcut7.container.getAudioDriverConfig()) ? audioDriverConfig : null);
        this.shortcut.putExtra("forceFullscreen", cbForceFullscreen.isChecked() ? "1" : null);
        String wincomponents = ContainerDetailFragment.getWinComponents(getContentView());
        Shortcut shortcut8 = this.shortcut;
        shortcut8.putExtra("wincomponents", !wincomponents.equals(shortcut8.container.getWinComponents()) ? wincomponents : null);
        String envVars = envVarsView.getEnvVars();
        this.shortcut.putExtra("envVars", !envVars.isEmpty() ? envVars : null);
        String box64Preset = Box64PresetManager.getSpinnerSelectedId(sBox64Preset);
        Shortcut shortcut9 = this.shortcut;
        shortcut9.putExtra("box64Preset", !box64Preset.equals(shortcut9.container.getBox64Preset()) ? box64Preset : null);
        ArrayList<ControlsProfile> profiles = this.inputControlsManager.getProfiles(true);
        int controlsProfile = sControlsProfile.getSelectedItemPosition() > 0 ? profiles.get(sControlsProfile.getSelectedItemPosition() - 1).id : 0;
        this.shortcut.putExtra("controlsProfile", controlsProfile > 0 ? String.valueOf(controlsProfile) : null);
        int dinputMapperType = sDInputMapperType.getSelectedItemPosition();
        this.shortcut.putExtra("dinputMapperType", dinputMapperType != 1 ? String.valueOf(dinputMapperType) : null);
        this.shortcut.saveData();
    }

    private void renameShortcut(String newName) {
        String newName2 = StringUtils.clearReservedChars(newName);
        File parent = this.shortcut.file.getParentFile();
        File newFile = new File(parent, newName2 + ".desktop");
        if (!newFile.isFile()) {
            this.shortcut.file.renameTo(newFile);
        }
        File linkFile = new File(parent, this.shortcut.name + ".lnk");
        if (linkFile.isFile()) {
            File newFile2 = new File(parent, newName2 + ".lnk");
            if (!newFile2.isFile()) {
                linkFile.renameTo(newFile2);
            }
        }
        this.fragment.refreshContent();
    }

    private EnvVarsView createEnvVarsTab() {
        View view = getContentView();
        final Context context = view.getContext();
        final EnvVarsView envVarsView = (EnvVarsView) view.findViewById(R.id.EnvVarsView);
        envVarsView.setEnvVars(new EnvVars(this.shortcut.getExtra("envVars")));
        view.findViewById(R.id.BTAddEnvVar).setOnClickListener(new View.OnClickListener() { // from class: com.winlator.contentdialog.ShortcutSettingsDialog$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                ShortcutSettingsDialog.lambda$createEnvVarsTab$5(context, envVarsView, view2);
            }
        });
        return envVarsView;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$createEnvVarsTab$5(Context context, EnvVarsView envVarsView, View v) {
        new AddEnvVarDialog(context, envVarsView).show();
    }

    private void loadControlsProfileSpinner(Spinner spinner, String selectedValue) {
        Context context = this.fragment.getContext();
        ArrayList<ControlsProfile> profiles = this.inputControlsManager.getProfiles(true);
        ArrayList<String> values = new ArrayList<>();
        values.add(context.getString(R.string.none));
        int selectedPosition = 0;
        int selectedId = Integer.parseInt(selectedValue);
        for (int i = 0; i < profiles.size(); i++) {
            ControlsProfile profile = profiles.get(i);
            if (profile.id == selectedId) {
                selectedPosition = i + 1;
            }
            values.add(profile.getName());
        }
        spinner.setAdapter((SpinnerAdapter) new ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, values));
        spinner.setSelection(selectedPosition, false);
    }
}
