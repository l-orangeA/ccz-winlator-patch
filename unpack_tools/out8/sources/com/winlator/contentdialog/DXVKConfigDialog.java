package com.winlator.contentdialog;

import android.content.Context;
import android.view.View;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import com.winlatcn.R;
import com.winlator.core.AppUtils;
import com.winlator.core.EnvVars;
import com.winlator.core.FileUtils;
import com.winlator.core.KeyValueSet;
import com.winlator.core.StringUtils;
import com.winlator.widget.GPUCardAdapter;
import com.winlator.xenvironment.ImageFs;
import java.io.File;

/* loaded from: classes.dex */
public class DXVKConfigDialog extends ContentDialog {
    public DXVKConfigDialog(final View anchor) {
        super(anchor.getContext(), R.layout.dxvk_config_dialog);
        setIcon(R.drawable.icon_display_settings);
        Context context = anchor.getContext();
        setTitle("DXVK " + context.getString(R.string.configuration));
        final Spinner sVersion = (Spinner) findViewById(R.id.SVersion);
        final Spinner sFramerate = (Spinner) findViewById(R.id.SFramerate);
        final Spinner sMaxDeviceMemory = (Spinner) findViewById(R.id.SMaxDeviceMemory);
        final Spinner sCustomDevice = (Spinner) findViewById(R.id.SCustomDevice);
        KeyValueSet config = new KeyValueSet(anchor.getTag());
        AppUtils.setSpinnerSelectionFromIdentifier(sVersion, config.get("version", "2.4"));
        AppUtils.setSpinnerSelectionFromIdentifier(sFramerate, config.get("framerate", "0"));
        AppUtils.setSpinnerSelectionFromNumber(sMaxDeviceMemory, config.get("maxDeviceMemory", "0"));
        GPUCardAdapter adapter = new GPUCardAdapter(context, android.R.layout.simple_spinner_dropdown_item, R.string.none);
        sCustomDevice.setAdapter((SpinnerAdapter) adapter);
        String customDevice = config.get("customDevice");
        if (customDevice.contains(":")) {
            try {
                int deviceId = Integer.parseInt(customDevice.split(":")[0], 16);
                sCustomDevice.setSelection(adapter.getPositionByDeviceId(deviceId));
            } catch (NumberFormatException e) {
            }
        }
        setOnConfirmCallback(new Runnable() { // from class: com.winlator.contentdialog.DXVKConfigDialog$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                DXVKConfigDialog.lambda$new$0(sVersion, sFramerate, sMaxDeviceMemory, sCustomDevice, anchor);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$new$0(Spinner sVersion, Spinner sFramerate, Spinner sMaxDeviceMemory, Spinner sCustomDevice, View anchor) {
        KeyValueSet newConfig = new KeyValueSet();
        newConfig.put("version", StringUtils.parseNumber(sVersion.getSelectedItem()));
        newConfig.put("framerate", StringUtils.parseNumber(sFramerate.getSelectedItem()));
        newConfig.put("maxDeviceMemory", StringUtils.parseNumber(sMaxDeviceMemory.getSelectedItem()));
        GPUCardAdapter.GPUCard gpuCard = (GPUCardAdapter.GPUCard) sCustomDevice.getSelectedItem();
        if (gpuCard.deviceId > 0) {
            newConfig.put("customDevice", String.format("%04x", Integer.valueOf(gpuCard.deviceId)) + ":" + String.format("%04x", Integer.valueOf(gpuCard.vendorId)) + ":" + gpuCard.name);
        }
        anchor.setTag(newConfig.toString());
    }

    public static void setEnvVars(Context context, KeyValueSet config, EnvVars envVars) {
        envVars.put("DXVK_STATE_CACHE_PATH", "/home/xuser/.cache");
        envVars.put("DXVK_LOG_LEVEL", "none");
        File rootDir = ImageFs.find(context).getRootDir();
        File dxvkConfigFile = new File(rootDir, "/home/xuser/.config/dxvk.conf");
        String content = "";
        String maxDeviceMemory = config.get("maxDeviceMemory");
        if (!maxDeviceMemory.isEmpty() && !maxDeviceMemory.equals("0")) {
            content = ("dxgi.maxDeviceMemory = " + maxDeviceMemory + "\n") + "dxgi.maxSharedMemory = " + maxDeviceMemory + "\n";
        }
        String framerate = config.get("framerate");
        if (!framerate.isEmpty() && !framerate.equals("0")) {
            content = (content + "dxgi.maxFrameRate = " + framerate + "\n") + "d3d9.maxFrameRate = " + framerate + "\n";
        }
        String customDevice = config.get("customDevice");
        if (customDevice.contains(":")) {
            String[] parts = customDevice.split(":");
            content = (((((content + "dxgi.customDeviceId = " + parts[0] + "\n") + "dxgi.customVendorId = " + parts[1] + "\n") + "d3d9.customDeviceId = " + parts[0] + "\n") + "d3d9.customVendorId = " + parts[1] + "\n") + "dxgi.customDeviceDesc = \"" + parts[2] + "\"\n") + "d3d9.customDeviceDesc = \"" + parts[2] + "\"\n";
        }
        FileUtils.delete(dxvkConfigFile);
        if (!content.isEmpty() && FileUtils.writeString(dxvkConfigFile, content)) {
            envVars.put("DXVK_CONFIG_FILE", "/home/xuser/.config/dxvk.conf");
        }
    }
}
