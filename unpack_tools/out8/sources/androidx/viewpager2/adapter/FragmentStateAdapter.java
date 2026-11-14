package androidx.viewpager2.adapter;

import android.os.Handler;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

/* loaded from: classes.dex */
public abstract class FragmentStateAdapter extends RecyclerView.Adapter<Object> {
    abstract boolean shouldDelayFragmentTransactions();

    /* renamed from: androidx.viewpager2.adapter.FragmentStateAdapter$2, reason: invalid class name */
    /* loaded from: classes.dex */
    class AnonymousClass2 implements LifecycleEventObserver {
        @Override // androidx.lifecycle.LifecycleEventObserver
        public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
            FragmentStateAdapter fragmentStateAdapter = null;
            fragmentStateAdapter.shouldDelayFragmentTransactions();
            throw null;
        }
    }

    /* renamed from: androidx.viewpager2.adapter.FragmentStateAdapter$5, reason: invalid class name */
    /* loaded from: classes.dex */
    class AnonymousClass5 implements LifecycleEventObserver {
        final /* synthetic */ Handler val$handler;
        final /* synthetic */ Runnable val$runnable;

        @Override // androidx.lifecycle.LifecycleEventObserver
        public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
            if (event == Lifecycle.Event.ON_DESTROY) {
                this.val$handler.removeCallbacks(this.val$runnable);
                source.getLifecycle().removeObserver(this);
            }
        }
    }

    /* loaded from: classes.dex */
    class FragmentMaxLifecycleEnforcer {
        void updateFragmentMaxLifecycle(boolean z) {
            throw null;
        }

        /* renamed from: androidx.viewpager2.adapter.FragmentStateAdapter$FragmentMaxLifecycleEnforcer$3, reason: invalid class name */
        /* loaded from: classes.dex */
        class AnonymousClass3 implements LifecycleEventObserver {
            @Override // androidx.lifecycle.LifecycleEventObserver
            public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
                FragmentMaxLifecycleEnforcer fragmentMaxLifecycleEnforcer = null;
                fragmentMaxLifecycleEnforcer.updateFragmentMaxLifecycle(false);
                throw null;
            }
        }
    }
}
