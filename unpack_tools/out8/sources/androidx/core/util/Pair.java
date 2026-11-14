package androidx.core.util;

/* loaded from: classes.dex */
public class Pair<F, S> {
    public final F first;
    public final S second;

    public boolean equals(Object o) {
        if (!(o instanceof Pair)) {
            return false;
        }
        Pair<?, ?> p = (Pair) o;
        return ObjectsCompat.equals(p.first, this.first) && ObjectsCompat.equals(p.second, this.second);
    }

    public int hashCode() {
        F f = this.first;
        int hashCode = f == null ? 0 : f.hashCode();
        S s = this.second;
        return hashCode ^ (s != null ? s.hashCode() : 0);
    }

    public String toString() {
        return "Pair{" + this.first + " " + this.second + "}";
    }
}
