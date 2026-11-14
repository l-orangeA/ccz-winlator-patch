package com.winlator.box64;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.ToggleButton;
import com.winlatcn.R;
import com.winlator.contentdialog.ContentDialog;
import com.winlator.core.AppUtils;
import com.winlator.core.ArrayUtils;
import com.winlator.core.EnvVars;
import com.winlator.core.FileUtils;
import com.winlator.core.StringUtils;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class Box64EditPresetDialog extends ContentDialog {
    private final Context context;
    private Runnable onConfirmCallback;
    private final Box64Preset preset;
    private final boolean readonly;

    public Box64EditPresetDialog(final Context context, String presetId) {
        super(context, R.layout.box64_edit_preset_dialog);
        this.context = context;
        Box64Preset preset = presetId != null ? Box64PresetManager.getPreset(context, presetId) : null;
        this.preset = preset;
        boolean z = (preset == null || preset.isCustom()) ? false : true;
        this.readonly = z;
        setTitle(StringUtils.getString(context, "box64_preset"));
        setIcon(R.drawable.icon_env_var);
        final EditText etName = (EditText) findViewById(R.id.ETName);
        etName.getLayoutParams().width = AppUtils.getPreferredDialogWidth(context);
        etName.setEnabled(true ^ z);
        if (preset != null) {
            etName.setText(preset.name);
        } else {
            etName.setText(context.getString(R.string.preset) + "-" + Box64PresetManager.getNextPresetId(context));
        }
        loadEnvVarsList();
        super.setOnConfirmCallback(new Runnable() { // from class: com.winlator.box64.Box64EditPresetDialog$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                Box64EditPresetDialog.this.lambda$new$0(etName, context);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$0(EditText etName, Context context) {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            return;
        }
        String name2 = name.replaceAll("[,\\|]+", "");
        Box64Preset box64Preset = this.preset;
        Box64PresetManager.editPreset(context, box64Preset != null ? box64Preset.id : null, name2, getEnvVars());
        Runnable runnable = this.onConfirmCallback;
        if (runnable != null) {
            runnable.run();
        }
    }

    @Override // com.winlator.contentdialog.ContentDialog
    public void setOnConfirmCallback(Runnable onConfirmCallback) {
        this.onConfirmCallback = onConfirmCallback;
    }

    private EnvVars getEnvVars() {
        EnvVars envVars = new EnvVars();
        LinearLayout parent = (LinearLayout) findViewById(R.id.LLContent);
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            String name = ((TextView) child.findViewById(R.id.TextView)).getText().toString();
            Spinner spinner = (Spinner) child.findViewById(R.id.Spinner);
            ToggleButton toggleButton = (ToggleButton) child.findViewById(R.id.ToggleButton);
            boolean toggleSwitch = toggleButton.getVisibility() == 0;
            String value = toggleSwitch ? toggleButton.isChecked() ? "1" : "0" : spinner.getSelectedItem().toString();
            envVars.put(name, value);
        }
        return envVars;
    }

    private void loadEnvVarsList() {
        try {
            LinearLayout parent = (LinearLayout) findViewById(R.id.LLContent);
            LayoutInflater inflater = LayoutInflater.from(this.context);
            JSONArray data = new JSONArray(FileUtils.readString(this.context, "box64_env_vars.json"));
            Box64Preset box64Preset = this.preset;
            EnvVars envVars = box64Preset != null ? Box64PresetManager.getEnvVars(this.context, box64Preset.id) : null;
            for (int i = 0; i < data.length(); i++) {
                JSONObject item = data.getJSONObject(i);
                final String name = item.getString("name");
                View child = inflater.inflate(R.layout.box64_env_var_list_item, (ViewGroup) parent, false);
                ((TextView) child.findViewById(R.id.TextView)).setText(name);
                child.findViewById(R.id.BTHelp).setOnClickListener(new View.OnClickListener() { // from class: com.winlator.box64.Box64EditPresetDialog$$ExternalSyntheticLambda0
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view) {
                        Box64EditPresetDialog.this.lambda$loadEnvVarsList$1(name, view);
                    }
                });
                Spinner spinner = (Spinner) child.findViewById(R.id.Spinner);
                ToggleButton toggleButton = (ToggleButton) child.findViewById(R.id.ToggleButton);
                String[] values = ArrayUtils.toStringArray(item.getJSONArray("values"));
                String value = (envVars == null || !envVars.has(name)) ? item.getString("defaultValue") : envVars.get(name);
                if (item.optBoolean("toggleSwitch", false)) {
                    toggleButton.setVisibility(0);
                    toggleButton.setEnabled(this.readonly ? false : true);
                    toggleButton.setChecked(value.equals("1"));
                    if (this.readonly) {
                        toggleButton.setAlpha(0.5f);
                    }
                } else {
                    spinner.setVisibility(0);
                    spinner.setEnabled(this.readonly ? false : true);
                    spinner.setAdapter((SpinnerAdapter) new ArrayAdapter(this.context, android.R.layout.simple_spinner_dropdown_item, values));
                    AppUtils.setSpinnerSelectionFromValue(spinner, value);
                }
                parent.addView(child);
            }
        } catch (JSONException e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$loadEnvVarsList$1(String name, View v) {
        String suffix = name.replace("BOX64_", "").toLowerCase(Locale.ENGLISH);
        String value = StringUtils.getString(this.context, "box64_env_var_help__" + suffix);
        AppUtils.showHelpBox(this.context, v, value);
    }
}
