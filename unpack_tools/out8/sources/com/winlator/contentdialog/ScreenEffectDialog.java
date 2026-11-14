package com.winlator.contentdialog;

import android.content.SharedPreferences;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import androidx.preference.PreferenceManager;
import com.winlatcn.R;
import com.winlator.XServerDisplayActivity;
import com.winlator.core.AppUtils;
import com.winlator.core.Callback;
import com.winlator.core.KeyValueSet;
import com.winlator.renderer.GLRenderer;
import com.winlator.renderer.effects.CRTEffect;
import com.winlator.renderer.effects.ColorEffect;
import com.winlator.renderer.effects.FXAAEffect;
import com.winlator.widget.SeekBar;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;

/* loaded from: classes.dex */
public class ScreenEffectDialog extends ContentDialog {
    private final XServerDisplayActivity activity;
    private final CheckBox cbEnableCRTShader;
    private final CheckBox cbEnableFXAA;
    private final SharedPreferences preferences;
    private final Spinner sProfile;
    private final SeekBar sbBrightness;
    private final SeekBar sbContrast;
    private final SeekBar sbGamma;

    public ScreenEffectDialog(final XServerDisplayActivity activity) {
        super(activity, R.layout.screen_effect_dialog);
        this.activity = activity;
        setTitle(R.string.screen_effect);
        setIcon(R.drawable.icon_screen_effect);
        this.preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        final GLRenderer renderer = activity.getXServerView().getRenderer();
        ColorEffect currentColorEffect = (ColorEffect) renderer.effectComposer.getEffect(ColorEffect.class);
        final ColorEffect colorEffect = currentColorEffect != null ? currentColorEffect : new ColorEffect();
        final FXAAEffect fxaaEffect = (FXAAEffect) renderer.effectComposer.getEffect(FXAAEffect.class);
        final CRTEffect crtEffect = (CRTEffect) renderer.effectComposer.getEffect(CRTEffect.class);
        Spinner spinner = (Spinner) findViewById(R.id.SProfile);
        this.sProfile = spinner;
        SeekBar seekBar = (SeekBar) findViewById(R.id.SBBrightness);
        this.sbBrightness = seekBar;
        SeekBar seekBar2 = (SeekBar) findViewById(R.id.SBContrast);
        this.sbContrast = seekBar2;
        SeekBar seekBar3 = (SeekBar) findViewById(R.id.SBGamma);
        this.sbGamma = seekBar3;
        CheckBox checkBox = (CheckBox) findViewById(R.id.CBEnableFXAA);
        this.cbEnableFXAA = checkBox;
        CheckBox checkBox2 = (CheckBox) findViewById(R.id.CBEnableCRTShader);
        this.cbEnableCRTShader = checkBox2;
        seekBar.setValue(colorEffect.getBrightness() * 100.0f);
        seekBar2.setValue(colorEffect.getContrast() * 100.0f);
        seekBar3.setValue(colorEffect.getGamma());
        checkBox.setChecked(fxaaEffect != null);
        checkBox2.setChecked(crtEffect != null);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // from class: com.winlator.contentdialog.ScreenEffectDialog.1
            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedProfile = position > 0 ? ScreenEffectDialog.this.sProfile.getItemAtPosition(position).toString() : null;
                if (selectedProfile != null) {
                    ScreenEffectDialog.this.loadProfile(selectedProfile);
                }
            }

            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        loadProfileSpinner(spinner, activity.getScreenEffectProfile());
        findViewById(R.id.BTAddProfile).setOnClickListener(new View.OnClickListener() { // from class: com.winlator.contentdialog.ScreenEffectDialog$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ScreenEffectDialog.this.lambda$new$1(activity, view);
            }
        });
        findViewById(R.id.BTRemoveProfile).setOnClickListener(new View.OnClickListener() { // from class: com.winlator.contentdialog.ScreenEffectDialog$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ScreenEffectDialog.this.lambda$new$3(activity, view);
            }
        });
        setOnConfirmCallback(new Runnable() { // from class: com.winlator.contentdialog.ScreenEffectDialog$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                ScreenEffectDialog.this.lambda$new$4(colorEffect, renderer, fxaaEffect, crtEffect);
            }
        });
        Button resetButton = (Button) findViewById(R.id.BTReset);
        resetButton.setVisibility(0);
        resetButton.setOnClickListener(new View.OnClickListener() { // from class: com.winlator.contentdialog.ScreenEffectDialog$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                ScreenEffectDialog.this.lambda$new$5(view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$1(XServerDisplayActivity activity, View v) {
        ContentDialog.prompt(activity, R.string.profile_name, null, new Callback() { // from class: com.winlator.contentdialog.ScreenEffectDialog$$ExternalSyntheticLambda3
            @Override // com.winlator.core.Callback
            public final void call(Object obj) {
                ScreenEffectDialog.this.lambda$new$0((String) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$0(String name) {
        addProfile(name, this.sProfile);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$3(XServerDisplayActivity activity, View v) {
        final String selectedProfile = this.sProfile.getSelectedItemPosition() > 0 ? this.sProfile.getSelectedItem().toString() : null;
        if (selectedProfile != null) {
            ContentDialog.confirm(activity, R.string.do_you_want_to_remove_this_profile, new Runnable() { // from class: com.winlator.contentdialog.ScreenEffectDialog$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    ScreenEffectDialog.this.lambda$new$2(selectedProfile);
                }
            });
        } else {
            AppUtils.showToast(activity, R.string.no_profile_selected);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$2(String selectedProfile) {
        removeProfile(selectedProfile, this.sProfile);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$4(ColorEffect colorEffect, GLRenderer renderer, FXAAEffect fxaaEffect, CRTEffect crtEffect) {
        float brightness = this.sbBrightness.getValue();
        float contrast = this.sbContrast.getValue();
        float gamma = this.sbGamma.getValue();
        if (brightness != 0.0f || contrast != 0.0f || gamma != 1.0f) {
            colorEffect.setBrightness(brightness / 100.0f);
            colorEffect.setContrast(contrast / 100.0f);
            colorEffect.setGamma(gamma);
            renderer.effectComposer.addEffect(colorEffect);
        } else {
            renderer.effectComposer.removeEffect(colorEffect);
        }
        if (this.cbEnableFXAA.isChecked()) {
            if (fxaaEffect == null) {
                renderer.effectComposer.addEffect(new FXAAEffect());
            }
        } else if (fxaaEffect != null) {
            renderer.effectComposer.removeEffect(fxaaEffect);
        }
        if (this.cbEnableCRTShader.isChecked()) {
            if (crtEffect == null) {
                renderer.effectComposer.addEffect(new CRTEffect());
            }
        } else if (crtEffect != null) {
            renderer.effectComposer.removeEffect(crtEffect);
        }
        saveProfile(this.sProfile);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$5(View v) {
        resetSettings();
    }

    private void resetSettings() {
        this.sbBrightness.setValue(0.0f);
        this.sbContrast.setValue(0.0f);
        this.sbGamma.setValue(1.0f);
        this.cbEnableFXAA.setChecked(false);
        this.cbEnableCRTShader.setChecked(false);
    }

    private void saveProfile(Spinner sProfile) {
        String str;
        String selectedProfile = sProfile.getSelectedItemPosition() > 0 ? sProfile.getSelectedItem().toString() : null;
        if (selectedProfile != null) {
            LinkedHashSet<String> oldProfiles = new LinkedHashSet<>(this.preferences.getStringSet("screen_effect_profiles", new LinkedHashSet()));
            LinkedHashSet<String> newProfiles = new LinkedHashSet<>();
            KeyValueSet settings = new KeyValueSet();
            settings.put("brightness", Float.valueOf(this.sbBrightness.getValue()));
            settings.put("contrast", Float.valueOf(this.sbContrast.getValue()));
            settings.put("gamma", Float.valueOf(this.sbGamma.getValue()));
            settings.put("fxaa", Boolean.valueOf(this.cbEnableFXAA.isChecked()));
            settings.put("crt_shader", Boolean.valueOf(this.cbEnableCRTShader.isChecked()));
            Iterator<String> it = oldProfiles.iterator();
            while (it.hasNext()) {
                String profile = it.next();
                String name = profile.split(":")[0];
                if (name.equals(selectedProfile)) {
                    str = selectedProfile + ":" + settings;
                } else {
                    str = profile;
                }
                newProfiles.add(str);
            }
            this.preferences.edit().putStringSet("screen_effect_profiles", newProfiles).apply();
        }
        this.activity.setScreenEffectProfile(selectedProfile);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void loadProfile(String name) {
        LinkedHashSet<String> profiles = new LinkedHashSet<>(this.preferences.getStringSet("screen_effect_profiles", new LinkedHashSet()));
        Iterator<String> it = profiles.iterator();
        while (it.hasNext()) {
            String profile = it.next();
            String[] parts = profile.split(":");
            if (parts[0].equals(name)) {
                if (parts.length > 1 && !parts[1].isEmpty()) {
                    KeyValueSet settings = new KeyValueSet(parts[1]);
                    this.sbBrightness.setValue(settings.getFloat("brightness", 0.0f));
                    this.sbContrast.setValue(settings.getFloat("contrast", 1.0f));
                    this.sbGamma.setValue(settings.getFloat("gamma", 1.0f));
                    this.cbEnableFXAA.setChecked(settings.getBoolean("fxaa", false));
                    this.cbEnableCRTShader.setChecked(settings.getBoolean("crt_shader", false));
                    return;
                }
                return;
            }
        }
    }

    private void addProfile(String newName, Spinner sProfile) {
        LinkedHashSet<String> profiles = new LinkedHashSet<>(this.preferences.getStringSet("screen_effect_profiles", new LinkedHashSet()));
        Iterator<String> it = profiles.iterator();
        while (it.hasNext()) {
            String profile = it.next();
            String name = profile.split(":")[0];
            if (name.equals(newName)) {
                return;
            }
        }
        profiles.add(newName.replace(":", "") + ":");
        this.preferences.edit().putStringSet("screen_effect_profiles", profiles).apply();
        loadProfileSpinner(sProfile, newName);
    }

    private void removeProfile(String targetName, Spinner sProfile) {
        LinkedHashSet<String> profiles = new LinkedHashSet<>(this.preferences.getStringSet("screen_effect_profiles", new LinkedHashSet()));
        Iterator<String> it = profiles.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            String profile = it.next();
            String name = profile.split(":")[0];
            if (name.equals(targetName)) {
                profiles.remove(profile);
                break;
            }
        }
        this.preferences.edit().putStringSet("screen_effect_profiles", profiles).apply();
        loadProfileSpinner(sProfile, null);
        resetSettings();
    }

    private void loadProfileSpinner(Spinner sProfile, String selectedName) {
        LinkedHashSet<String> profiles = new LinkedHashSet<>(this.preferences.getStringSet("screen_effect_profiles", new LinkedHashSet()));
        ArrayList<String> items = new ArrayList<>();
        items.add("-- " + this.activity.getString(R.string.select_profile) + " --");
        int selectedPosition = 0;
        int position = 1;
        Iterator<String> it = profiles.iterator();
        while (it.hasNext()) {
            String profile = it.next();
            String name = profile.split(":")[0];
            items.add(name);
            if (name.equals(selectedName)) {
                selectedPosition = position;
            }
            position++;
        }
        sProfile.setAdapter((SpinnerAdapter) new ArrayAdapter(this.activity, android.R.layout.simple_spinner_dropdown_item, items));
        sProfile.setSelection(selectedPosition);
    }
}
