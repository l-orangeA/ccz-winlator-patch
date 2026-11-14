package com.winlator.core;

import java.util.Iterator;
import java.util.LinkedHashMap;

/* loaded from: classes.dex */
public class EnvVars implements Iterable<String> {
    private final LinkedHashMap<String, String> data = new LinkedHashMap<>();

    public EnvVars() {
    }

    public EnvVars(String values) {
        putAll(values);
    }

    public EnvVars put(String name, Object value) {
        this.data.put(name, String.valueOf(value));
        return this;
    }

    public void putAll(String[] items) {
        if (items == null) {
            return;
        }
        for (String item : items) {
            int index = item.indexOf("=");
            String name = item.substring(0, index);
            String value = item.substring(index + 1);
            this.data.put(name, value);
        }
    }

    public void putAll(String values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        putAll(values.split(" "));
    }

    public void putAll(EnvVars envVars) {
        this.data.putAll(envVars.data);
    }

    public String get(String name) {
        return this.data.getOrDefault(name, "");
    }

    public void remove(String name) {
        this.data.remove(name);
    }

    public boolean has(String name) {
        return this.data.containsKey(name);
    }

    public void clear() {
        this.data.clear();
    }

    public String toString() {
        return String.join(" ", toStringArray());
    }

    public String toEscapedString() {
        String result = "";
        for (String key : this.data.keySet()) {
            if (!result.isEmpty()) {
                result = result + " ";
            }
            String value = this.data.get(key);
            result = result + key + "=" + value.replace(" ", "\\ ");
        }
        return result;
    }

    public String[] toStringArray() {
        String[] stringArray = new String[this.data.size()];
        int index = 0;
        for (String key : this.data.keySet()) {
            stringArray[index] = key + "=" + this.data.get(key);
            index++;
        }
        return stringArray;
    }

    @Override // java.lang.Iterable
    public Iterator<String> iterator() {
        return this.data.keySet().iterator();
    }
}
