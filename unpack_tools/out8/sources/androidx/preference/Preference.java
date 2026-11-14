package androidx.preference;

import android.R;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.content.res.TypedArrayUtils;
import androidx.preference.PreferenceManager;
import java.util.List;

/* loaded from: classes.dex */
public class Preference implements Comparable<Preference> {
    private boolean mAllowDividerAbove;
    private boolean mAllowDividerBelow;
    private final View.OnClickListener mClickListener;
    private Context mContext;
    private boolean mCopyingEnabled;
    private Object mDefaultValue;
    private String mDependencyKey;
    private boolean mDependencyMet;
    private List<Preference> mDependents;
    private boolean mEnabled;
    private String mFragment;
    private boolean mHasSingleLineTitleAttr;
    private int mIconResId;
    private boolean mIconSpaceReserved;
    private Intent mIntent;
    private String mKey;
    private int mLayoutResId;
    private OnPreferenceChangeInternalListener mListener;
    private OnPreferenceChangeListener mOnChangeListener;
    private OnPreferenceClickListener mOnClickListener;
    private int mOrder;
    private boolean mParentDependencyMet;
    private boolean mPersistent;
    private boolean mSelectable;
    private boolean mShouldDisableView;
    private boolean mSingleLineTitle;
    private CharSequence mSummary;
    private SummaryProvider mSummaryProvider;
    private CharSequence mTitle;
    private int mViewId;
    private boolean mVisible;
    private int mWidgetLayoutResId;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface OnPreferenceChangeInternalListener {
        void onPreferenceChange(Preference preference);
    }

    /* loaded from: classes.dex */
    public interface OnPreferenceChangeListener {
        boolean onPreferenceChange(Preference preference, Object obj);
    }

    /* loaded from: classes.dex */
    public interface OnPreferenceClickListener {
        boolean onPreferenceClick(Preference preference);
    }

    /* loaded from: classes.dex */
    public interface SummaryProvider<T extends Preference> {
        CharSequence provideSummary(T t);
    }

    public Preference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        this.mOrder = Integer.MAX_VALUE;
        this.mViewId = 0;
        this.mEnabled = true;
        this.mSelectable = true;
        this.mPersistent = true;
        this.mDependencyMet = true;
        this.mParentDependencyMet = true;
        this.mVisible = true;
        this.mAllowDividerAbove = true;
        this.mAllowDividerBelow = true;
        this.mSingleLineTitle = true;
        this.mShouldDisableView = true;
        int i = R$layout.preference;
        this.mLayoutResId = i;
        this.mClickListener = new View.OnClickListener() { // from class: androidx.preference.Preference.1
            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                Preference.this.performClick(v);
            }
        };
        this.mContext = context;
        TypedArray a = context.obtainStyledAttributes(attrs, R$styleable.Preference, defStyleAttr, defStyleRes);
        this.mIconResId = TypedArrayUtils.getResourceId(a, R$styleable.Preference_icon, R$styleable.Preference_android_icon, 0);
        this.mKey = TypedArrayUtils.getString(a, R$styleable.Preference_key, R$styleable.Preference_android_key);
        this.mTitle = TypedArrayUtils.getText(a, R$styleable.Preference_title, R$styleable.Preference_android_title);
        this.mSummary = TypedArrayUtils.getText(a, R$styleable.Preference_summary, R$styleable.Preference_android_summary);
        this.mOrder = TypedArrayUtils.getInt(a, R$styleable.Preference_order, R$styleable.Preference_android_order, Integer.MAX_VALUE);
        this.mFragment = TypedArrayUtils.getString(a, R$styleable.Preference_fragment, R$styleable.Preference_android_fragment);
        this.mLayoutResId = TypedArrayUtils.getResourceId(a, R$styleable.Preference_layout, R$styleable.Preference_android_layout, i);
        this.mWidgetLayoutResId = TypedArrayUtils.getResourceId(a, R$styleable.Preference_widgetLayout, R$styleable.Preference_android_widgetLayout, 0);
        this.mEnabled = TypedArrayUtils.getBoolean(a, R$styleable.Preference_enabled, R$styleable.Preference_android_enabled, true);
        this.mSelectable = TypedArrayUtils.getBoolean(a, R$styleable.Preference_selectable, R$styleable.Preference_android_selectable, true);
        this.mPersistent = TypedArrayUtils.getBoolean(a, R$styleable.Preference_persistent, R$styleable.Preference_android_persistent, true);
        this.mDependencyKey = TypedArrayUtils.getString(a, R$styleable.Preference_dependency, R$styleable.Preference_android_dependency);
        int i2 = R$styleable.Preference_allowDividerAbove;
        this.mAllowDividerAbove = TypedArrayUtils.getBoolean(a, i2, i2, this.mSelectable);
        int i3 = R$styleable.Preference_allowDividerBelow;
        this.mAllowDividerBelow = TypedArrayUtils.getBoolean(a, i3, i3, this.mSelectable);
        int i4 = R$styleable.Preference_defaultValue;
        if (a.hasValue(i4)) {
            this.mDefaultValue = onGetDefaultValue(a, i4);
        } else {
            int i5 = R$styleable.Preference_android_defaultValue;
            if (a.hasValue(i5)) {
                this.mDefaultValue = onGetDefaultValue(a, i5);
            }
        }
        this.mShouldDisableView = TypedArrayUtils.getBoolean(a, R$styleable.Preference_shouldDisableView, R$styleable.Preference_android_shouldDisableView, true);
        int i6 = R$styleable.Preference_singleLineTitle;
        boolean hasValue = a.hasValue(i6);
        this.mHasSingleLineTitleAttr = hasValue;
        if (hasValue) {
            this.mSingleLineTitle = TypedArrayUtils.getBoolean(a, i6, R$styleable.Preference_android_singleLineTitle, true);
        }
        this.mIconSpaceReserved = TypedArrayUtils.getBoolean(a, R$styleable.Preference_iconSpaceReserved, R$styleable.Preference_android_iconSpaceReserved, false);
        int i7 = R$styleable.Preference_isPreferenceVisible;
        this.mVisible = TypedArrayUtils.getBoolean(a, i7, i7, true);
        int i8 = R$styleable.Preference_enableCopying;
        this.mCopyingEnabled = TypedArrayUtils.getBoolean(a, i8, i8, false);
        a.recycle();
    }

    public Preference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public Preference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context, R$attr.preferenceStyle, R.attr.preferenceStyle));
    }

    protected Object onGetDefaultValue(TypedArray a, int index) {
        return null;
    }

    public Intent getIntent() {
        return this.mIntent;
    }

    public String getFragment() {
        return this.mFragment;
    }

    public PreferenceDataStore getPreferenceDataStore() {
        return null;
    }

    public CharSequence getTitle() {
        return this.mTitle;
    }

    public CharSequence getSummary() {
        if (getSummaryProvider() != null) {
            return getSummaryProvider().provideSummary(this);
        }
        return this.mSummary;
    }

    public boolean isEnabled() {
        return this.mEnabled && this.mDependencyMet && this.mParentDependencyMet;
    }

    public boolean isSelectable() {
        return this.mSelectable;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void onClick() {
    }

    public boolean hasKey() {
        return !TextUtils.isEmpty(this.mKey);
    }

    protected boolean shouldPersist() {
        return false;
    }

    public final void setSummaryProvider(SummaryProvider summaryProvider) {
        this.mSummaryProvider = summaryProvider;
        notifyChanged();
    }

    public final SummaryProvider getSummaryProvider() {
        return this.mSummaryProvider;
    }

    public boolean callChangeListener(Object newValue) {
        OnPreferenceChangeListener onPreferenceChangeListener = this.mOnChangeListener;
        return onPreferenceChangeListener == null || onPreferenceChangeListener.onPreferenceChange(this, newValue);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void performClick(View view) {
        performClick();
    }

    public void performClick() {
        PreferenceManager.OnPreferenceTreeClickListener listener;
        if (!isEnabled() || !isSelectable()) {
            return;
        }
        onClick();
        OnPreferenceClickListener onPreferenceClickListener = this.mOnClickListener;
        if (onPreferenceClickListener != null && onPreferenceClickListener.onPreferenceClick(this)) {
            return;
        }
        getPreferenceManager();
        PreferenceManager preferenceManager = null;
        if ((0 == 0 || (listener = preferenceManager.getOnPreferenceTreeClickListener()) == null || !listener.onPreferenceTreeClick(this)) && this.mIntent != null) {
            Context context = getContext();
            context.startActivity(this.mIntent);
        }
    }

    public Context getContext() {
        return this.mContext;
    }

    @Override // java.lang.Comparable
    public int compareTo(Preference another) {
        int i = this.mOrder;
        int i2 = another.mOrder;
        if (i != i2) {
            return i - i2;
        }
        CharSequence charSequence = this.mTitle;
        CharSequence charSequence2 = another.mTitle;
        if (charSequence == charSequence2) {
            return 0;
        }
        if (charSequence == null) {
            return 1;
        }
        if (charSequence2 == null) {
            return -1;
        }
        return charSequence.toString().compareToIgnoreCase(another.mTitle.toString());
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void notifyChanged() {
        OnPreferenceChangeInternalListener onPreferenceChangeInternalListener = this.mListener;
        if (onPreferenceChangeInternalListener != null) {
            onPreferenceChangeInternalListener.onPreferenceChange(this);
        }
    }

    public PreferenceManager getPreferenceManager() {
        return null;
    }

    public void notifyDependencyChange(boolean disableDependents) {
        List<Preference> dependents = this.mDependents;
        if (dependents == null) {
            return;
        }
        int dependentsCount = dependents.size();
        for (int i = 0; i < dependentsCount; i++) {
            dependents.get(i).onDependencyChanged(this, disableDependents);
        }
    }

    public void onDependencyChanged(Preference dependency, boolean disableDependent) {
        if (this.mDependencyMet == disableDependent) {
            this.mDependencyMet = !disableDependent;
            notifyDependencyChange(shouldDisableDependents());
            notifyChanged();
        }
    }

    public void onParentChanged(Preference parent, boolean disableChild) {
        if (this.mParentDependencyMet == disableChild) {
            this.mParentDependencyMet = !disableChild;
            notifyDependencyChange(shouldDisableDependents());
            notifyChanged();
        }
    }

    public boolean shouldDisableDependents() {
        return !isEnabled();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean persistString(String value) {
        if (!shouldPersist()) {
            return false;
        }
        PreferenceManager preferenceManager = null;
        if (TextUtils.equals(value, getPersistedString(null))) {
            return true;
        }
        getPreferenceDataStore();
        PreferenceDataStore dataStore = null;
        if (0 != 0) {
            dataStore.putString(this.mKey, value);
            return true;
        }
        preferenceManager.getEditor();
        throw null;
    }

    protected String getPersistedString(String defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }
        getPreferenceDataStore();
        PreferenceDataStore dataStore = null;
        if (0 != 0) {
            return dataStore.getString(this.mKey, defaultReturnValue);
        }
        PreferenceManager preferenceManager = null;
        preferenceManager.getSharedPreferences();
        throw null;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean persistInt(int value) {
        if (!shouldPersist()) {
            return false;
        }
        if (value == getPersistedInt(~value)) {
            return true;
        }
        getPreferenceDataStore();
        PreferenceDataStore dataStore = null;
        if (0 != 0) {
            dataStore.putInt(this.mKey, value);
            return true;
        }
        PreferenceManager preferenceManager = null;
        preferenceManager.getEditor();
        throw null;
    }

    protected int getPersistedInt(int defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }
        getPreferenceDataStore();
        PreferenceDataStore dataStore = null;
        if (0 != 0) {
            return dataStore.getInt(this.mKey, defaultReturnValue);
        }
        PreferenceManager preferenceManager = null;
        preferenceManager.getSharedPreferences();
        throw null;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public boolean persistBoolean(boolean value) {
        if (!shouldPersist()) {
            return false;
        }
        if (value == getPersistedBoolean(!value)) {
            return true;
        }
        getPreferenceDataStore();
        PreferenceDataStore dataStore = null;
        if (0 != 0) {
            dataStore.putBoolean(this.mKey, value);
            return true;
        }
        PreferenceManager preferenceManager = null;
        preferenceManager.getEditor();
        throw null;
    }

    protected boolean getPersistedBoolean(boolean defaultReturnValue) {
        if (!shouldPersist()) {
            return defaultReturnValue;
        }
        getPreferenceDataStore();
        PreferenceDataStore dataStore = null;
        if (0 != 0) {
            return dataStore.getBoolean(this.mKey, defaultReturnValue);
        }
        PreferenceManager preferenceManager = null;
        preferenceManager.getSharedPreferences();
        throw null;
    }

    public String toString() {
        return getFilterableStringBuilder().toString();
    }

    StringBuilder getFilterableStringBuilder() {
        StringBuilder sb = new StringBuilder();
        CharSequence title = getTitle();
        if (!TextUtils.isEmpty(title)) {
            sb.append(title);
            sb.append(' ');
        }
        CharSequence summary = getSummary();
        if (!TextUtils.isEmpty(summary)) {
            sb.append(summary);
            sb.append(' ');
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb;
    }
}
