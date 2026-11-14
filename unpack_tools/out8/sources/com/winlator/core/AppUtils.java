package com.winlator.core;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.inputmethod.InputMethodManager;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.tabs.TabLayout;
import com.winlatcn.R;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/* loaded from: classes.dex */
public abstract class AppUtils {
    public static final String DIRECTORY_DOWNLOADS = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
    private static WeakReference<Toast> globalToastReference = null;

    /* loaded from: classes.dex */
    public static class RestartApplicationOptions {
        public int containerId;
        public int selectedMenuItemId;
        public String startPath;
    }

    public static void keepScreenOn(Activity activity) {
        activity.getWindow().addFlags(128);
    }

    public static void restartActivity(AppCompatActivity activity) {
        Intent intent = activity.getIntent();
        activity.finish();
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
    }

    public static void restartApplication(Context context) {
        restartApplication(context, null);
    }

    public static void restartApplication(Context context, RestartApplicationOptions options) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        Intent mainIntent = Intent.makeRestartActivityTask(intent.getComponent());
        if (options != null) {
            int i = options.selectedMenuItemId;
            if (i > 0) {
                mainIntent.putExtra("selected_menu_item_id", i);
            }
            int i2 = options.containerId;
            if (i2 > 0) {
                mainIntent.putExtra("container_id", i2);
            }
            String str = options.startPath;
            if (str != null) {
                mainIntent.putExtra("start_path", str);
            }
        }
        context.startActivity(mainIntent);
        Runtime.getRuntime().exit(0);
    }

    public static void showKeyboard(AppCompatActivity activity) {
        final InputMethodManager imm = (InputMethodManager) activity.getSystemService("input_method");
        if (Build.VERSION.SDK_INT > 29) {
            activity.getWindow().getDecorView().postDelayed(new Runnable() { // from class: com.winlator.core.AppUtils$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    imm.toggleSoftInput(2, 0);
                }
            }, 500L);
        } else {
            imm.toggleSoftInput(2, 0);
        }
    }

    public static void hideSystemUI(Activity activity) {
        Window window = activity.getWindow();
        final View decorView = window.getDecorView();
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController insetsController = decorView.getWindowInsetsController();
            if (insetsController != null) {
                insetsController.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                insetsController.setSystemBarsBehavior(2);
                return;
            }
            return;
        }
        decorView.setSystemUiVisibility(5894);
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() { // from class: com.winlator.core.AppUtils$$ExternalSyntheticLambda0
            @Override // android.view.View.OnSystemUiVisibilityChangeListener
            public final void onSystemUiVisibilityChange(int i) {
                AppUtils.lambda$hideSystemUI$1(decorView, i);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$hideSystemUI$1(View decorView, int visibility) {
        if ((visibility & 4) == 0) {
            decorView.setSystemUiVisibility(5894);
        }
    }

    public static boolean isUiThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }

    public static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    public static int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    public static int getPreferredDialogWidth(Context context) {
        int orientation = context.getResources().getConfiguration().orientation;
        float scale = orientation == 1 ? 0.8f : 0.5f;
        return (int) UnitUtils.dpToPx(UnitUtils.pxToDp(getScreenWidth()) * scale);
    }

    public static Toast showToast(Context context, int textResId) {
        return showToast(context, context.getString(textResId));
    }

    public static Toast showToast(final Context context, final String text) {
        if (!isUiThread()) {
            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(new Runnable() { // from class: com.winlator.core.AppUtils$$ExternalSyntheticLambda3
                    @Override // java.lang.Runnable
                    public final void run() {
                        AppUtils.showToast(context, text);
                    }
                });
            }
            return null;
        }
        WeakReference<Toast> weakReference = globalToastReference;
        if (weakReference != null) {
            Toast toast = weakReference.get();
            if (toast != null) {
                toast.cancel();
            }
            globalToastReference = null;
        }
        View view = LayoutInflater.from(context).inflate(R.layout.custom_toast, (ViewGroup) null);
        ((TextView) view.findViewById(R.id.TextView)).setText(text);
        Toast toast2 = new Toast(context);
        toast2.setGravity(81, 0, 50);
        toast2.setDuration(text.length() >= 40 ? 1 : 0);
        toast2.setView(view);
        toast2.show();
        globalToastReference = new WeakReference<>(toast2);
        return toast2;
    }

    public static PopupWindow showPopupWindow(View anchor, View contentView, int width, int height) {
        Context context = anchor.getContext();
        PopupWindow popupWindow = new PopupWindow(context);
        popupWindow.setElevation(5.0f);
        if (width == 0 && height == 0) {
            int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
            int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
            contentView.measure(widthMeasureSpec, heightMeasureSpec);
            popupWindow.setWidth(contentView.getMeasuredWidth());
            popupWindow.setHeight(contentView.getMeasuredHeight());
        } else {
            if (width > 0) {
                popupWindow.setWidth((int) UnitUtils.dpToPx(width));
            } else {
                popupWindow.setWidth(-2);
            }
            if (height > 0) {
                popupWindow.setHeight((int) UnitUtils.dpToPx(height));
            } else {
                popupWindow.setHeight(-2);
            }
        }
        popupWindow.setContentView(contentView);
        popupWindow.setFocusable(false);
        popupWindow.setOutsideTouchable(true);
        popupWindow.update();
        popupWindow.showAsDropDown(anchor);
        popupWindow.setFocusable(true);
        popupWindow.update();
        return popupWindow;
    }

    public static void showHelpBox(Context context, View anchor, int textResId) {
        showHelpBox(context, anchor, context.getString(textResId));
    }

    public static void showHelpBox(Context context, View anchor, String text) {
        int padding = (int) UnitUtils.dpToPx(8.0f);
        TextView textView = new TextView(context);
        textView.setLayoutParams(new ViewGroup.LayoutParams((int) UnitUtils.dpToPx(284.0f), -2));
        textView.setPadding(padding, padding, padding, padding);
        textView.setTextSize(1, 16.0f);
        textView.setText(Html.fromHtml(text, 0));
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
        textView.measure(widthMeasureSpec, heightMeasureSpec);
        showPopupWindow(anchor, textView, 300, textView.getMeasuredHeight());
    }

    public static int getVersionCode(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    public static void observeSoftKeyboardVisibility(final View rootView, final Callback<Boolean> callback) {
        final boolean[] visible = {false};
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() { // from class: com.winlator.core.AppUtils$$ExternalSyntheticLambda1
            @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
            public final void onGlobalLayout() {
                AppUtils.lambda$observeSoftKeyboardVisibility$3(rootView, visible, callback);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$observeSoftKeyboardVisibility$3(View rootView, boolean[] visible, Callback callback) {
        Rect rect = new Rect();
        rootView.getWindowVisibleDisplayFrame(rect);
        int screenHeight = rootView.getRootView().getHeight();
        int keypadHeight = screenHeight - rect.bottom;
        if (keypadHeight > screenHeight * 0.15f) {
            if (!visible[0]) {
                visible[0] = true;
                callback.call(true);
                return;
            }
            return;
        }
        if (visible[0]) {
            visible[0] = false;
            callback.call(false);
        }
    }

    public static boolean setSpinnerSelectionFromValue(Spinner spinner, String value) {
        spinner.setSelection(0, false);
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(value)) {
                spinner.setSelection(i, false);
                return true;
            }
        }
        return false;
    }

    public static boolean setSpinnerSelectionFromIdentifier(Spinner spinner, String identifier) {
        spinner.setSelection(0, false);
        for (int i = 0; i < spinner.getCount(); i++) {
            if (StringUtils.parseIdentifier(spinner.getItemAtPosition(i)).equals(identifier)) {
                spinner.setSelection(i, false);
                return true;
            }
        }
        return false;
    }

    public static boolean setSpinnerSelectionFromNumber(Spinner spinner, String number) {
        spinner.setSelection(0, false);
        for (int i = 0; i < spinner.getCount(); i++) {
            if (StringUtils.parseNumber(spinner.getItemAtPosition(i)).equals(number)) {
                spinner.setSelection(i, false);
                return true;
            }
        }
        return false;
    }

    public static void setupTabLayout(View view, int tabLayoutResId, int... tabResIds) {
        setupTabLayout(view, tabLayoutResId, null, tabResIds);
    }

    public static void setupTabLayout(final View view, int tabLayoutResId, final Callback<Integer> onShowTab, final int... tabResIds) {
        final Callback<Integer> tabSelectedCallback = new Callback() { // from class: com.winlator.core.AppUtils$$ExternalSyntheticLambda2
            @Override // com.winlator.core.Callback
            public final void call(Object obj) {
                AppUtils.lambda$setupTabLayout$4(tabResIds, view, onShowTab, (Integer) obj);
            }
        };
        TabLayout tabLayout = (TabLayout) view.findViewById(tabLayoutResId);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() { // from class: com.winlator.core.AppUtils.1
            @Override // com.google.android.material.tabs.TabLayout.BaseOnTabSelectedListener
            public void onTabSelected(TabLayout.Tab tab) {
                Callback.this.call(Integer.valueOf(tab.getPosition()));
            }

            @Override // com.google.android.material.tabs.TabLayout.BaseOnTabSelectedListener
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override // com.google.android.material.tabs.TabLayout.BaseOnTabSelectedListener
            public void onTabReselected(TabLayout.Tab tab) {
                Callback.this.call(Integer.valueOf(tab.getPosition()));
            }
        });
        tabLayout.getTabAt(0).select();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$setupTabLayout$4(int[] tabResIds, View view, Callback onShowTab, Integer position) {
        int i = 0;
        while (i < tabResIds.length) {
            View tabView = view.findViewById(tabResIds[i]);
            int visibility = position.intValue() == i ? 0 : 8;
            tabView.setVisibility(visibility);
            if (visibility == 0 && onShowTab != null) {
                onShowTab.call(Integer.valueOf(tabResIds[i]));
            }
            i++;
        }
    }

    public static void findViewsWithClass(ViewGroup parent, Class viewClass, ArrayList<View> outViews) {
        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);
            Class _class = child.getClass();
            if (_class == viewClass || _class.getSuperclass() == viewClass) {
                outViews.add(child);
            } else if (child instanceof ViewGroup) {
                findViewsWithClass((ViewGroup) child, viewClass, outViews);
            }
        }
    }

    public static void runDelayed(final Runnable callback, long delay) {
        if (callback == null) {
            return;
        }
        new Timer().schedule(new TimerTask() { // from class: com.winlator.core.AppUtils.2
            @Override // java.util.TimerTask, java.lang.Runnable
            public void run() {
                callback.run();
            }
        }, delay);
    }
}
