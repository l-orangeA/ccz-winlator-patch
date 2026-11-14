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
public class VirGLConfigDialog extends ContentDialog {
    public VirGLConfigDialog(final View anchor) {
        super(anchor.getContext(), R.layout.virgl_config_dialog);
        Context context = anchor.getContext();
        setIcon(R.drawable.icon_settings);
        setTitle("VirGL " + context.getString(R.string.configuration));
        final Spinner sGLVersion = (Spinner) findViewById(R.id.SVersion);
        final CheckBox cbDisableVertexArrayBGRA = (CheckBox) findViewById(R.id.CBDisableVertexArrayBGRA);
        KeyValueSet config = new KeyValueSet(anchor.getTag());
        AppUtils.setSpinnerSelectionFromIdentifier(sGLVersion, config.get("glVersion", "3.1"));
        cbDisableVertexArrayBGRA.setChecked(config.getBoolean("disableVertexArrayBGRA", true));
        setOnConfirmCallback(new Runnable() { // from class: com.winlator.contentdialog.VirGLConfigDialog$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                VirGLConfigDialog.lambda$new$0(sGLVersion, cbDisableVertexArrayBGRA, anchor);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$new$0(Spinner sGLVersion, CheckBox cbDisableVertexArrayBGRA, View anchor) {
        KeyValueSet newConfig = new KeyValueSet();
        newConfig.put("glVersion", StringUtils.parseNumber(sGLVersion.getSelectedItem()));
        newConfig.put("disableVertexArrayBGRA", cbDisableVertexArrayBGRA.isChecked() ? "1" : "0");
        anchor.setTag(newConfig.toString());
    }
}
