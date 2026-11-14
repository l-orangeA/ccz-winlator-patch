package androidx.preference;

import android.content.Context;
import android.content.SharedPreferences;

/* loaded from: classes.dex */
public class PreferenceManager {

    /* loaded from: classes.dex */
    public interface OnNavigateToScreenListener {
    }

    /* loaded from: classes.dex */
    public interface OnPreferenceTreeClickListener {
        boolean onPreferenceTreeClick(Preference preference);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SharedPreferences.Editor getEditor() {
        throw null;
    }

    public OnNavigateToScreenListener getOnNavigateToScreenListener() {
        throw null;
    }

    public OnPreferenceTreeClickListener getOnPreferenceTreeClickListener() {
        throw null;
    }

    public SharedPreferences getSharedPreferences() {
        throw null;
    }

    public void showDialog(Preference preference) {
        throw null;
    }

    public static SharedPreferences getDefaultSharedPreferences(Context context) {
        return context.getSharedPreferences(getDefaultSharedPreferencesName(context), getDefaultSharedPreferencesMode());
    }

    private static String getDefaultSharedPreferencesName(Context context) {
        return context.getPackageName() + "_preferences";
    }

    private static int getDefaultSharedPreferencesMode() {
        return 0;
    }
}
