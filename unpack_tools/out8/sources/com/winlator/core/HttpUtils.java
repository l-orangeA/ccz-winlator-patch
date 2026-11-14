package com.winlator.core;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/* loaded from: classes.dex */
public abstract class HttpUtils {
    /* JADX INFO: Access modifiers changed from: private */
    public static void downloadAsync(String url, Callback<String> onDownloadComplete) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            if (connection.getResponseCode() != 200) {
                onDownloadComplete.call(null);
                return;
            }
            InputStream inStream = connection.getInputStream();
            try {
                byte[] bytes = StreamUtils.copyToByteArray(inStream);
                if (inStream != null) {
                    inStream.close();
                }
                onDownloadComplete.call(new String(bytes, StandardCharsets.UTF_8));
            } finally {
            }
        } catch (Exception e) {
            onDownloadComplete.call(null);
        }
    }

    public static void download(final String url, final Callback<String> onDownloadComplete) {
        Executors.newSingleThreadExecutor().execute(new Runnable() { // from class: com.winlator.core.HttpUtils$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                HttpUtils.downloadAsync(url, onDownloadComplete);
            }
        });
    }
}
