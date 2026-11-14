package com.winlator.box64;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import androidx.preference.PreferenceManager;
import com.winlatcn.R;
import com.winlator.core.EnvVars;
import java.util.ArrayList;
import java.util.Iterator;

/* loaded from: classes.dex */
public abstract class Box64PresetManager {
    public static EnvVars getEnvVars(Context context, String id) {
        EnvVars envVars = new EnvVars();
        if (id.equals("STABILITY")) {
            envVars.put("BOX64_DYNAREC_SAFEFLAGS", "2");
            envVars.put("BOX64_DYNAREC_FASTNAN", "0");
            envVars.put("BOX64_DYNAREC_FASTROUND", "0");
            envVars.put("BOX64_DYNAREC_X87DOUBLE", "1");
            envVars.put("BOX64_DYNAREC_BIGBLOCK", "3");
            envVars.put("BOX64_DYNAREC_STRONGMEM", "2");
            envVars.put("BOX64_DYNAREC_FORWARD", "128");
            envVars.put("BOX64_DYNAREC_CALLRET", "0");
            envVars.put("BOX64_DYNAREC_WAIT", "0");
        } else if (id.equals("COMPATIBILITY")) {
            envVars.put("BOX64_DYNAREC_SAFEFLAGS", "2");
            envVars.put("BOX64_DYNAREC_FASTNAN", "0");
            envVars.put("BOX64_DYNAREC_FASTROUND", "0");
            envVars.put("BOX64_DYNAREC_X87DOUBLE", "1");
            envVars.put("BOX64_DYNAREC_BIGBLOCK", "1");
            envVars.put("BOX64_DYNAREC_STRONGMEM", "1");
            envVars.put("BOX64_DYNAREC_FORWARD", "128");
            envVars.put("BOX64_DYNAREC_CALLRET", "0");
            envVars.put("BOX64_DYNAREC_WAIT", "1");
        } else if (id.equals("INTERMEDIATE")) {
            envVars.put("BOX64_DYNAREC_SAFEFLAGS", "2");
            envVars.put("BOX64_DYNAREC_FASTNAN", "1");
            envVars.put("BOX64_DYNAREC_FASTROUND", "0");
            envVars.put("BOX64_DYNAREC_X87DOUBLE", "1");
            envVars.put("BOX64_DYNAREC_BIGBLOCK", "1");
            envVars.put("BOX64_DYNAREC_STRONGMEM", "0");
            envVars.put("BOX64_DYNAREC_FORWARD", "128");
            envVars.put("BOX64_DYNAREC_CALLRET", "0");
            envVars.put("BOX64_DYNAREC_WAIT", "1");
        } else if (id.equals("PERFORMANCE")) {
            envVars.put("BOX64_DYNAREC_SAFEFLAGS", "1");
            envVars.put("BOX64_DYNAREC_FASTNAN", "1");
            envVars.put("BOX64_DYNAREC_FASTROUND", "1");
            envVars.put("BOX64_DYNAREC_X87DOUBLE", "0");
            envVars.put("BOX64_DYNAREC_BIGBLOCK", "3");
            envVars.put("BOX64_DYNAREC_STRONGMEM", "0");
            envVars.put("BOX64_DYNAREC_FORWARD", "512");
            envVars.put("BOX64_DYNAREC_CALLRET", "1");
            envVars.put("BOX64_DYNAREC_WAIT", "1");
        } else if (id.startsWith("CUSTOM")) {
            Iterator<String[]> it = customPresetsIterator(context).iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                String[] preset = it.next();
                if (preset[0].equals(id)) {
                    envVars.putAll(preset[2]);
                    break;
                }
            }
        }
        return envVars;
    }

    public static ArrayList<Box64Preset> getPresets(Context context) {
        ArrayList<Box64Preset> presets = new ArrayList<>();
        presets.add(new Box64Preset("STABILITY", context.getString(R.string.stability)));
        presets.add(new Box64Preset("COMPATIBILITY", context.getString(R.string.compatibility)));
        presets.add(new Box64Preset("INTERMEDIATE", context.getString(R.string.intermediate)));
        presets.add(new Box64Preset("PERFORMANCE", context.getString(R.string.performance)));
        for (String[] preset : customPresetsIterator(context)) {
            presets.add(new Box64Preset(preset[0], preset[1]));
        }
        return presets;
    }

    public static Box64Preset getPreset(Context context, String id) {
        Iterator<Box64Preset> it = getPresets(context).iterator();
        while (it.hasNext()) {
            Box64Preset preset = it.next();
            if (preset.id.equals(id)) {
                return preset;
            }
        }
        return null;
    }

    private static Iterable<String[]> customPresetsIterator(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        final String customPresetsStr = preferences.getString("box64_custom_presets", "");
        final String[] customPresets = customPresetsStr.split(",");
        final int[] index = {0};
        return new Iterable() { // from class: com.winlator.box64.Box64PresetManager$$ExternalSyntheticLambda0
            @Override // java.lang.Iterable
            public final Iterator iterator() {
                Iterator lambda$customPresetsIterator$0;
                lambda$customPresetsIterator$0 = Box64PresetManager.lambda$customPresetsIterator$0(index, customPresets, customPresetsStr);
                return lambda$customPresetsIterator$0;
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ Iterator lambda$customPresetsIterator$0(final int[] index, final String[] customPresets, final String customPresetsStr) {
        return new Iterator<String[]>() { // from class: com.winlator.box64.Box64PresetManager.1
            @Override // java.util.Iterator
            public boolean hasNext() {
                return index[0] < customPresets.length && !customPresetsStr.isEmpty();
            }

            @Override // java.util.Iterator
            public String[] next() {
                String[] strArr = customPresets;
                int[] iArr = index;
                int i = iArr[0];
                iArr[0] = i + 1;
                return strArr[i].split("\\|");
            }
        };
    }

    public static int getNextPresetId(Context context) {
        int maxId = 0;
        for (String[] preset : customPresetsIterator(context)) {
            maxId = Math.max(maxId, Integer.parseInt(preset[0].replace("CUSTOM-", "")));
        }
        return maxId + 1;
    }

    public static void editPreset(Context context, String id, String name, EnvVars envVars) {
        String customPresetsStr;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String customPresetsStr2 = preferences.getString("box64_custom_presets", "");
        if (id != null) {
            String[] customPresets = customPresetsStr2.split(",");
            int i = 0;
            while (true) {
                if (i >= customPresets.length) {
                    break;
                }
                String[] preset = customPresets[i].split("\\|");
                if (!preset[0].equals(id)) {
                    i++;
                } else {
                    customPresets[i] = id + "|" + name + "|" + envVars.toString();
                    break;
                }
            }
            customPresetsStr = String.join(",", customPresets);
        } else {
            String preset2 = "CUSTOM-" + getNextPresetId(context) + "|" + name + "|" + envVars.toString();
            StringBuilder sb = new StringBuilder();
            sb.append(customPresetsStr2);
            sb.append(customPresetsStr2.isEmpty() ? "" : ",");
            sb.append(preset2);
            customPresetsStr = sb.toString();
        }
        preferences.edit().putString("box64_custom_presets", customPresetsStr).apply();
    }

    public static void duplicatePreset(Context context, String id) {
        ArrayList<Box64Preset> presets = getPresets(context);
        Box64Preset originPreset = null;
        Iterator<Box64Preset> it = presets.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            Box64Preset preset = it.next();
            if (preset.id.equals(id)) {
                originPreset = preset;
                break;
            }
        }
        if (originPreset == null) {
            return;
        }
        int i = 1;
        while (true) {
            String newName = originPreset.name + " (" + i + ")";
            boolean found = false;
            Iterator<Box64Preset> it2 = presets.iterator();
            while (true) {
                if (!it2.hasNext()) {
                    break;
                } else if (it2.next().name.equals(newName)) {
                    found = true;
                    break;
                }
            }
            if (found) {
                i++;
            } else {
                editPreset(context, null, newName, getEnvVars(context, originPreset.id));
                return;
            }
        }
    }

    public static void removePreset(Context context, String id) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String oldCustomPresetsStr = preferences.getString("box64_custom_presets", "");
        String newCustomPresetsStr = "";
        String[] customPresets = oldCustomPresetsStr.split(",");
        for (int i = 0; i < customPresets.length; i++) {
            String[] preset = customPresets[i].split("\\|");
            if (!preset[0].equals(id)) {
                StringBuilder sb = new StringBuilder();
                sb.append(newCustomPresetsStr);
                sb.append(!newCustomPresetsStr.isEmpty() ? "," : "");
                sb.append(customPresets[i]);
                newCustomPresetsStr = sb.toString();
            }
        }
        preferences.edit().putString("box64_custom_presets", newCustomPresetsStr).apply();
    }

    public static void loadSpinner(Spinner spinner, String selectedId) {
        Context context = spinner.getContext();
        ArrayList<Box64Preset> presets = getPresets(context);
        int selectedPosition = 0;
        int i = 0;
        while (true) {
            if (i >= presets.size()) {
                break;
            }
            if (!presets.get(i).id.equals(selectedId)) {
                i++;
            } else {
                selectedPosition = i;
                break;
            }
        }
        spinner.setAdapter((SpinnerAdapter) new ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, presets));
        spinner.setSelection(selectedPosition);
    }

    public static String getSpinnerSelectedId(Spinner spinner) {
        SpinnerAdapter adapter = spinner.getAdapter();
        int selectedPosition = spinner.getSelectedItemPosition();
        if (adapter != null && adapter.getCount() > 0 && selectedPosition >= 0) {
            return ((Box64Preset) adapter.getItem(selectedPosition)).id;
        }
        return "COMPATIBILITY";
    }
}
