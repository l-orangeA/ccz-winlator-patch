package com.winlator.contentdialog;

import android.content.Context;
import android.view.View;
import android.widget.Spinner;
import com.winlatcn.R;
import com.winlator.core.KeyValueSet;
import com.winlator.widget.SeekBar;

/* loaded from: classes.dex */
public class AudioDriverConfigDialog extends ContentDialog {
    public AudioDriverConfigDialog(final View anchor) {
        super(anchor.getContext(), R.layout.audio_driver_config_dialog);
        Context context = anchor.getContext();
        setIcon(R.drawable.icon_audio_settings);
        setTitle(context.getString(R.string.audio) + " " + context.getString(R.string.configuration));
        final Spinner sPerformanceMode = (Spinner) findViewById(R.id.SPerformanceMode);
        final SeekBar sbVolume = (SeekBar) findViewById(R.id.SBVolume);
        final SeekBar sbLatencyMillis = (SeekBar) findViewById(R.id.SBLatencyMillis);
        KeyValueSet config = new KeyValueSet(anchor.getTag());
        sPerformanceMode.setSelection(config.getInt("performanceMode", 1));
        sbVolume.setValue(config.getFloat("volume", 1.0f) * 100.0f);
        sbLatencyMillis.setValue(config.getInt("latencyMillis", 40));
        setOnConfirmCallback(new Runnable() { // from class: com.winlator.contentdialog.AudioDriverConfigDialog$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                AudioDriverConfigDialog.lambda$new$0(sPerformanceMode, sbVolume, sbLatencyMillis, anchor);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$new$0(Spinner sPerformanceMode, SeekBar sbVolume, SeekBar sbLatencyMillis, View anchor) {
        KeyValueSet newConfig = new KeyValueSet();
        newConfig.put("performanceMode", Integer.valueOf(sPerformanceMode.getSelectedItemPosition()));
        newConfig.put("volume", Float.valueOf(sbVolume.getValue() / 100.0f));
        newConfig.put("latencyMillis", Integer.valueOf((int) sbLatencyMillis.getValue()));
        anchor.setTag(newConfig.toString());
    }
}
