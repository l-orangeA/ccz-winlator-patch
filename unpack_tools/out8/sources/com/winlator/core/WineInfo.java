package com.winlator.core;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import com.winlator.xenvironment.ImageFs;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* loaded from: classes.dex */
public class WineInfo implements Parcelable {
    public final String path;
    public final String subversion;
    public final String version;
    public static final WineInfo MAIN_WINE_VERSION = new WineInfo("9.2");
    private static final Pattern pattern = Pattern.compile("^wine\\-([0-9\\.]+)\\-?([0-9\\.]+)?\\-?(x86|x86_64)?$");
    public static final Parcelable.Creator<WineInfo> CREATOR = new Parcelable.Creator<WineInfo>() { // from class: com.winlator.core.WineInfo.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public WineInfo createFromParcel(Parcel in) {
            return new WineInfo(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public WineInfo[] newArray(int size) {
            return new WineInfo[size];
        }
    };

    public WineInfo(String version) {
        this.version = version;
        this.subversion = null;
        this.path = null;
    }

    public WineInfo(String version, String subversion, String path) {
        this.version = version;
        this.subversion = (subversion == null || subversion.isEmpty()) ? null : subversion;
        this.path = path;
    }

    private WineInfo(Parcel in) {
        this.version = in.readString();
        this.subversion = in.readString();
        this.path = in.readString();
    }

    public String getExecutable() {
        return "wine";
    }

    public String identifier() {
        StringBuilder sb = new StringBuilder();
        sb.append("wine-");
        sb.append(fullVersion());
        sb.append("-");
        sb.append(this == MAIN_WINE_VERSION ? "custom" : "");
        return sb.toString();
    }

    public String fullVersion() {
        String str;
        StringBuilder sb = new StringBuilder();
        sb.append(this.version);
        if (this.subversion != null) {
            str = "-" + this.subversion;
        } else {
            str = "";
        }
        sb.append(str);
        return sb.toString();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Wine ");
        sb.append(fullVersion());
        sb.append(this == MAIN_WINE_VERSION ? " (Custom)" : "");
        return sb.toString();
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.version);
        dest.writeString(this.subversion);
        dest.writeString(this.path);
    }

    public static WineInfo fromIdentifier(Context context, String identifier) {
        WineInfo wineInfo = MAIN_WINE_VERSION;
        if (identifier.equals(wineInfo.identifier())) {
            return wineInfo;
        }
        Matcher matcher = pattern.matcher(identifier);
        if (matcher.find()) {
            File installedWineDir = ImageFs.find(context).getInstalledWineDir();
            String path = new File(installedWineDir, identifier).getPath();
            return new WineInfo(matcher.group(1), matcher.group(2), path);
        }
        return wineInfo;
    }

    public static boolean isMainWineVersion(String wineVersion) {
        return wineVersion == null || wineVersion.equals(MAIN_WINE_VERSION.identifier());
    }
}
