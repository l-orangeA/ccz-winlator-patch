package androidx.viewpager.widget;

import android.database.DataSetObserver;
import android.view.View;

/* loaded from: classes.dex */
public abstract class PagerAdapter {
    public abstract int getCount();

    public abstract float getPageWidth(int i);

    public abstract boolean isViewFromObject(View view, Object obj);

    public abstract void registerDataSetObserver(DataSetObserver dataSetObserver);
}
