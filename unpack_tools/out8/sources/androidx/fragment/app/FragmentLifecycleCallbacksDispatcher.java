package androidx.fragment.app;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import androidx.fragment.app.FragmentManager;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class FragmentLifecycleCallbacksDispatcher {
    private final FragmentManager mFragmentManager;
    private final CopyOnWriteArrayList<FragmentLifecycleCallbacksHolder> mLifecycleCallbacks = new CopyOnWriteArrayList<>();

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class FragmentLifecycleCallbacksHolder {
        final boolean mRecursive;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public FragmentLifecycleCallbacksDispatcher(FragmentManager fragmentManager) {
        this.mFragmentManager = fragmentManager;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchOnFragmentPreAttached(Fragment f, Context context, boolean onlyRecursive) {
        Fragment parent = this.mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher().dispatchOnFragmentPreAttached(f, context, true);
        }
        Iterator<FragmentLifecycleCallbacksHolder> it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            FragmentLifecycleCallbacksHolder holder = it.next();
            if (!onlyRecursive || holder.mRecursive) {
                Objects.requireNonNull(holder);
                FragmentManager.FragmentLifecycleCallbacks fragmentLifecycleCallbacks = null;
                fragmentLifecycleCallbacks.onFragmentPreAttached(this.mFragmentManager, f, context);
                throw null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchOnFragmentAttached(Fragment f, Context context, boolean onlyRecursive) {
        Fragment parent = this.mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher().dispatchOnFragmentAttached(f, context, true);
        }
        Iterator<FragmentLifecycleCallbacksHolder> it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            FragmentLifecycleCallbacksHolder holder = it.next();
            if (!onlyRecursive || holder.mRecursive) {
                Objects.requireNonNull(holder);
                FragmentManager.FragmentLifecycleCallbacks fragmentLifecycleCallbacks = null;
                fragmentLifecycleCallbacks.onFragmentAttached(this.mFragmentManager, f, context);
                throw null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchOnFragmentPreCreated(Fragment f, Bundle savedInstanceState, boolean onlyRecursive) {
        Fragment parent = this.mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher().dispatchOnFragmentPreCreated(f, savedInstanceState, true);
        }
        Iterator<FragmentLifecycleCallbacksHolder> it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            FragmentLifecycleCallbacksHolder holder = it.next();
            if (!onlyRecursive || holder.mRecursive) {
                Objects.requireNonNull(holder);
                FragmentManager.FragmentLifecycleCallbacks fragmentLifecycleCallbacks = null;
                fragmentLifecycleCallbacks.onFragmentPreCreated(this.mFragmentManager, f, savedInstanceState);
                throw null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchOnFragmentCreated(Fragment f, Bundle savedInstanceState, boolean onlyRecursive) {
        Fragment parent = this.mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher().dispatchOnFragmentCreated(f, savedInstanceState, true);
        }
        Iterator<FragmentLifecycleCallbacksHolder> it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            FragmentLifecycleCallbacksHolder holder = it.next();
            if (!onlyRecursive || holder.mRecursive) {
                Objects.requireNonNull(holder);
                FragmentManager.FragmentLifecycleCallbacks fragmentLifecycleCallbacks = null;
                fragmentLifecycleCallbacks.onFragmentCreated(this.mFragmentManager, f, savedInstanceState);
                throw null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchOnFragmentActivityCreated(Fragment f, Bundle savedInstanceState, boolean onlyRecursive) {
        Fragment parent = this.mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher().dispatchOnFragmentActivityCreated(f, savedInstanceState, true);
        }
        Iterator<FragmentLifecycleCallbacksHolder> it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            FragmentLifecycleCallbacksHolder holder = it.next();
            if (!onlyRecursive || holder.mRecursive) {
                Objects.requireNonNull(holder);
                FragmentManager.FragmentLifecycleCallbacks fragmentLifecycleCallbacks = null;
                fragmentLifecycleCallbacks.onFragmentActivityCreated(this.mFragmentManager, f, savedInstanceState);
                throw null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchOnFragmentViewCreated(Fragment f, View v, Bundle savedInstanceState, boolean onlyRecursive) {
        Fragment parent = this.mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher().dispatchOnFragmentViewCreated(f, v, savedInstanceState, true);
        }
        Iterator<FragmentLifecycleCallbacksHolder> it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            FragmentLifecycleCallbacksHolder holder = it.next();
            if (!onlyRecursive || holder.mRecursive) {
                Objects.requireNonNull(holder);
                FragmentManager.FragmentLifecycleCallbacks fragmentLifecycleCallbacks = null;
                fragmentLifecycleCallbacks.onFragmentViewCreated(this.mFragmentManager, f, v, savedInstanceState);
                throw null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchOnFragmentStarted(Fragment f, boolean onlyRecursive) {
        Fragment parent = this.mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher().dispatchOnFragmentStarted(f, true);
        }
        Iterator<FragmentLifecycleCallbacksHolder> it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            FragmentLifecycleCallbacksHolder holder = it.next();
            if (!onlyRecursive || holder.mRecursive) {
                Objects.requireNonNull(holder);
                FragmentManager.FragmentLifecycleCallbacks fragmentLifecycleCallbacks = null;
                fragmentLifecycleCallbacks.onFragmentStarted(this.mFragmentManager, f);
                throw null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchOnFragmentResumed(Fragment f, boolean onlyRecursive) {
        Fragment parent = this.mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher().dispatchOnFragmentResumed(f, true);
        }
        Iterator<FragmentLifecycleCallbacksHolder> it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            FragmentLifecycleCallbacksHolder holder = it.next();
            if (!onlyRecursive || holder.mRecursive) {
                Objects.requireNonNull(holder);
                FragmentManager.FragmentLifecycleCallbacks fragmentLifecycleCallbacks = null;
                fragmentLifecycleCallbacks.onFragmentResumed(this.mFragmentManager, f);
                throw null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchOnFragmentPaused(Fragment f, boolean onlyRecursive) {
        Fragment parent = this.mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher().dispatchOnFragmentPaused(f, true);
        }
        Iterator<FragmentLifecycleCallbacksHolder> it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            FragmentLifecycleCallbacksHolder holder = it.next();
            if (!onlyRecursive || holder.mRecursive) {
                Objects.requireNonNull(holder);
                FragmentManager.FragmentLifecycleCallbacks fragmentLifecycleCallbacks = null;
                fragmentLifecycleCallbacks.onFragmentPaused(this.mFragmentManager, f);
                throw null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchOnFragmentStopped(Fragment f, boolean onlyRecursive) {
        Fragment parent = this.mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher().dispatchOnFragmentStopped(f, true);
        }
        Iterator<FragmentLifecycleCallbacksHolder> it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            FragmentLifecycleCallbacksHolder holder = it.next();
            if (!onlyRecursive || holder.mRecursive) {
                Objects.requireNonNull(holder);
                FragmentManager.FragmentLifecycleCallbacks fragmentLifecycleCallbacks = null;
                fragmentLifecycleCallbacks.onFragmentStopped(this.mFragmentManager, f);
                throw null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchOnFragmentSaveInstanceState(Fragment f, Bundle outState, boolean onlyRecursive) {
        Fragment parent = this.mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher().dispatchOnFragmentSaveInstanceState(f, outState, true);
        }
        Iterator<FragmentLifecycleCallbacksHolder> it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            FragmentLifecycleCallbacksHolder holder = it.next();
            if (!onlyRecursive || holder.mRecursive) {
                Objects.requireNonNull(holder);
                FragmentManager.FragmentLifecycleCallbacks fragmentLifecycleCallbacks = null;
                fragmentLifecycleCallbacks.onFragmentSaveInstanceState(this.mFragmentManager, f, outState);
                throw null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchOnFragmentViewDestroyed(Fragment f, boolean onlyRecursive) {
        Fragment parent = this.mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher().dispatchOnFragmentViewDestroyed(f, true);
        }
        Iterator<FragmentLifecycleCallbacksHolder> it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            FragmentLifecycleCallbacksHolder holder = it.next();
            if (!onlyRecursive || holder.mRecursive) {
                Objects.requireNonNull(holder);
                FragmentManager.FragmentLifecycleCallbacks fragmentLifecycleCallbacks = null;
                fragmentLifecycleCallbacks.onFragmentViewDestroyed(this.mFragmentManager, f);
                throw null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchOnFragmentDestroyed(Fragment f, boolean onlyRecursive) {
        Fragment parent = this.mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher().dispatchOnFragmentDestroyed(f, true);
        }
        Iterator<FragmentLifecycleCallbacksHolder> it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            FragmentLifecycleCallbacksHolder holder = it.next();
            if (!onlyRecursive || holder.mRecursive) {
                Objects.requireNonNull(holder);
                FragmentManager.FragmentLifecycleCallbacks fragmentLifecycleCallbacks = null;
                fragmentLifecycleCallbacks.onFragmentDestroyed(this.mFragmentManager, f);
                throw null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dispatchOnFragmentDetached(Fragment f, boolean onlyRecursive) {
        Fragment parent = this.mFragmentManager.getParent();
        if (parent != null) {
            FragmentManager parentManager = parent.getParentFragmentManager();
            parentManager.getLifecycleCallbacksDispatcher().dispatchOnFragmentDetached(f, true);
        }
        Iterator<FragmentLifecycleCallbacksHolder> it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            FragmentLifecycleCallbacksHolder holder = it.next();
            if (!onlyRecursive || holder.mRecursive) {
                Objects.requireNonNull(holder);
                FragmentManager.FragmentLifecycleCallbacks fragmentLifecycleCallbacks = null;
                fragmentLifecycleCallbacks.onFragmentDetached(this.mFragmentManager, f);
                throw null;
            }
        }
    }
}
