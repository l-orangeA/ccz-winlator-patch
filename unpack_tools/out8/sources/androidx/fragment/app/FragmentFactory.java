package androidx.fragment.app;

import androidx.collection.SimpleArrayMap;
import androidx.fragment.app.Fragment;

/* loaded from: classes.dex */
public class FragmentFactory {
    private static final SimpleArrayMap<String, Class<?>> sClassMap = new SimpleArrayMap<>();

    public Fragment instantiate(ClassLoader classLoader, String str) {
        throw null;
    }

    private static Class<?> loadClass(ClassLoader classLoader, String className) throws ClassNotFoundException {
        SimpleArrayMap<String, Class<?>> simpleArrayMap = sClassMap;
        Class<?> clazz = simpleArrayMap.get(className);
        if (clazz == null) {
            Class<?> clazz2 = Class.forName(className, false, classLoader);
            simpleArrayMap.put(className, clazz2);
            return clazz2;
        }
        return clazz;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean isFragmentClass(ClassLoader classLoader, String className) {
        try {
            Class<?> clazz = loadClass(classLoader, className);
            return Fragment.class.isAssignableFrom(clazz);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static Class<? extends Fragment> loadFragmentClass(ClassLoader classLoader, String className) {
        try {
            return loadClass(classLoader, className);
        } catch (ClassCastException e) {
            throw new Fragment.InstantiationException("Unable to instantiate fragment " + className + ": make sure class is a valid subclass of Fragment", e);
        } catch (ClassNotFoundException e2) {
            throw new Fragment.InstantiationException("Unable to instantiate fragment " + className + ": make sure class name exists", e2);
        }
    }
}
