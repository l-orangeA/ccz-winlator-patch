package com.winlator.container;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.winlator.core.FileUtils;
import com.winlator.core.StringUtils;
import java.io.File;
import java.util.Iterator;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class Shortcut {
    public final Container container;
    private final JSONObject extraData = new JSONObject();
    public final File file;
    public final Bitmap icon;
    public final File iconFile;
    public final String name;
    public final String path;
    public final String wmClass;

    public Shortcut(Container container, File file) {
        String execArgs;
        Container container2 = container;
        this.container = container2;
        this.file = file;
        if (file.isDirectory()) {
            this.name = file.getName();
            this.path = null;
            this.icon = null;
            this.iconFile = null;
            this.wmClass = null;
            return;
        }
        short[] iconSizes = {64, 48, 32, 24, 16, 128, 256};
        Iterator<String> it = FileUtils.readLines(file, true).iterator();
        String section = "";
        String wmClass = "";
        File iconFile = null;
        Bitmap icon = null;
        String execArgs2 = "";
        while (it.hasNext()) {
            String line = it.next();
            if (!line.startsWith("#")) {
                int index = line.indexOf("[");
                if (index != -1) {
                    section = line.substring(index + 1, line.indexOf("]", index));
                } else {
                    int index2 = line.indexOf("=");
                    if (index2 != -1) {
                        int i = 0;
                        String key = line.substring(0, index2);
                        String value = line.substring(index2 + 1);
                        if (section.equals("Desktop Entry")) {
                            execArgs2 = key.equals("Exec") ? value : execArgs2;
                            if (!key.equals("Icon")) {
                                execArgs = execArgs2;
                            } else {
                                int length = iconSizes.length;
                                while (true) {
                                    if (i >= length) {
                                        execArgs = execArgs2;
                                        break;
                                    }
                                    short iconSize = iconSizes[i];
                                    execArgs = execArgs2;
                                    File iconFile2 = container2.getIconsDir(iconSize);
                                    iconFile = new File(iconFile2, value + ".png");
                                    if (!iconFile.isFile()) {
                                        i++;
                                        container2 = container;
                                        execArgs2 = execArgs;
                                    } else {
                                        icon = BitmapFactory.decodeFile(iconFile.getPath());
                                        break;
                                    }
                                }
                            }
                            if (key.equals("StartupWMClass")) {
                                wmClass = value;
                                execArgs2 = execArgs;
                            } else {
                                execArgs2 = execArgs;
                            }
                        } else if (section.equals("Extra Data")) {
                            try {
                                this.extraData.put(key, value);
                            } catch (JSONException e) {
                            }
                        }
                    }
                }
                container2 = container;
            }
        }
        this.name = FileUtils.getBasename(file.getPath());
        this.icon = icon;
        this.iconFile = iconFile;
        this.path = StringUtils.unescapeDOSPath(execArgs2.substring(execArgs2.lastIndexOf("wine ") + 4));
        this.wmClass = wmClass;
        Container.checkObsoleteOrMissingProperties(this.extraData);
    }

    public String getExtra(String name) {
        return getExtra(name, "");
    }

    public String getExtra(String name, String fallback) {
        try {
            return this.extraData.has(name) ? this.extraData.getString(name) : fallback;
        } catch (JSONException e) {
            return fallback;
        }
    }

    public void putExtra(String name, String value) {
        try {
            if (value != null) {
                this.extraData.put(name, value);
            } else {
                this.extraData.remove(name);
            }
        } catch (JSONException e) {
        }
    }

    public void saveData() {
        String content = "[Desktop Entry]\n";
        Iterator<String> it = FileUtils.readLines(this.file).iterator();
        while (it.hasNext()) {
            String line = it.next();
            if (line.contains("[Extra Data]")) {
                break;
            }
            if (!line.contains("[Desktop Entry]") && !line.isEmpty()) {
                content = content + line + "\n";
            }
        }
        if (this.extraData.length() > 0) {
            content = content + "\n[Extra Data]\n";
            Iterator<String> keys = this.extraData.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                try {
                    content = content + key + "=" + this.extraData.getString(key) + "\n";
                } catch (JSONException e) {
                }
            }
        }
        FileUtils.writeString(this.file, content);
    }

    public File getLinkFile() {
        String name = this.file.getName().replace(".desktop", ".lnk");
        return new File(this.file.getParentFile(), name);
    }

    public void remove() {
        if (this.file.isDirectory()) {
            FileUtils.delete(this.file);
            return;
        }
        File linkFile = getLinkFile();
        if (this.file.delete()) {
            File file = this.iconFile;
            if (file != null) {
                file.delete();
            }
            if (linkFile.isFile()) {
                linkFile.delete();
            }
        }
    }
}
