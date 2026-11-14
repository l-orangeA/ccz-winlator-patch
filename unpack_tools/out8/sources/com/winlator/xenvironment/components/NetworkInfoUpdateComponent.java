package com.winlator.xenvironment.components;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.winlator.core.FileUtils;
import com.winlator.core.NetworkHelper;
import com.winlator.xenvironment.EnvironmentComponent;
import java.io.File;

/* loaded from: classes.dex */
public class NetworkInfoUpdateComponent extends EnvironmentComponent {
    private BroadcastReceiver broadcastReceiver;

    @Override // com.winlator.xenvironment.EnvironmentComponent
    public void start() {
        Context context = this.environment.getContext();
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        final NetworkHelper networkHelper = new NetworkHelper(context);
        updateAdapterInfoFile(0, 0, 0);
        this.broadcastReceiver = new BroadcastReceiver() { // from class: com.winlator.xenvironment.components.NetworkInfoUpdateComponent.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                int ipAddress = 0;
                int netmask = 0;
                int gateway = 0;
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected() && networkInfo.getType() == 1) {
                    ipAddress = networkHelper.getIpAddress();
                    netmask = networkHelper.getNetmask();
                    gateway = networkHelper.getGateway();
                }
                NetworkInfoUpdateComponent.this.updateAdapterInfoFile(ipAddress, netmask, gateway);
                NetworkInfoUpdateComponent.this.updateEtcHostsFile(ipAddress);
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        context.registerReceiver(this.broadcastReceiver, filter);
    }

    @Override // com.winlator.xenvironment.EnvironmentComponent
    public void stop() {
        if (this.broadcastReceiver != null) {
            this.environment.getContext().unregisterReceiver(this.broadcastReceiver);
            this.broadcastReceiver = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateAdapterInfoFile(int ipAddress, int netmask, int gateway) {
        File file = new File(this.environment.getImageFs().getTmpDir(), "adapterinfo");
        FileUtils.writeString(file, "Android Wi-Fi Adapter," + NetworkHelper.formatIpAddress(ipAddress) + "," + NetworkHelper.formatNetmask(netmask) + "," + NetworkHelper.formatIpAddress(gateway));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateEtcHostsFile(int ipAddress) {
        String ip = ipAddress != 0 ? NetworkHelper.formatIpAddress(ipAddress) : "127.0.0.1";
        File file = new File(this.environment.getImageFs().getRootDir(), "etc/hosts");
        FileUtils.writeString(file, ip + "\tlocalhost\n");
    }
}
