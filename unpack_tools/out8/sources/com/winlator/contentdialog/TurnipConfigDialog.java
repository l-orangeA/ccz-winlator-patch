package com.winlator.contentdialog;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Spinner;
import com.winlatcn.R;
import com.winlator.core.AppUtils;
import com.winlator.core.KeyValueSet;
import com.winlator.core.StringUtils;

/* loaded from: classes.dex */
public class TurnipConfigDialog extends ContentDialog {
    public TurnipConfigDialog(final View anchor) {
        super(anchor.getContext(), R.layout.turnip_config_dialog);
        Context context = anchor.getContext();
        setIcon(R.drawable.icon_display_settings);
        setTitle("Turnip " + context.getString(R.string.configuration));
        final Spinner sVersion = (Spinner) findViewById(R.id.SVersion);
        final CheckBox cbForceWaitForFences = (CheckBox) findViewById(R.id.CBForceWaitForFences);
        KeyValueSet config = new KeyValueSet(anchor.getTag());
        AppUtils.setSpinnerSelectionFromIdentifier(sVersion, config.get("version", "24.1.0"));
        cbForceWaitForFences.setChecked(config.getBoolean("forceWaitForFences"));
        setOnConfirmCallback(new Runnable() { // from class: com.winlator.contentdialog.TurnipConfigDialog$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                TurnipConfigDialog.lambda$new$0(sVersion, cbForceWaitForFences, anchor);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$new$0(Spinner sVersion, CheckBox cbForceWaitForFences, View anchor) {
        KeyValueSet newConfig = new KeyValueSet();
        newConfig.put("version", StringUtils.parseNumber(sVersion.getSelectedItem()));
        newConfig.put("forceWaitForFences", cbForceWaitForFences.isChecked() ? "1" : "0");
        anchor.setTag(newConfig.toString());
    }
}
