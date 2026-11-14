package androidx.appcompat.widget;

import android.R;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import androidx.appcompat.R$attr;
import androidx.appcompat.R$color;
import androidx.appcompat.R$drawable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.ResourceManagerInternal;
import androidx.core.graphics.ColorUtils;

/* loaded from: classes.dex */
public final class AppCompatDrawableManager {
    private static final PorterDuff.Mode DEFAULT_MODE = PorterDuff.Mode.SRC_IN;
    private static AppCompatDrawableManager INSTANCE;
    private ResourceManagerInternal mResourceManager;

    public static synchronized void preload() {
        synchronized (AppCompatDrawableManager.class) {
            if (INSTANCE == null) {
                AppCompatDrawableManager appCompatDrawableManager = new AppCompatDrawableManager();
                INSTANCE = appCompatDrawableManager;
                appCompatDrawableManager.mResourceManager = ResourceManagerInternal.get();
                INSTANCE.mResourceManager.setHooks(new ResourceManagerInternal.ResourceManagerHooks() { // from class: androidx.appcompat.widget.AppCompatDrawableManager.1
                    private final int[] COLORFILTER_TINT_COLOR_CONTROL_NORMAL = {R$drawable.abc_textfield_search_default_mtrl_alpha, R$drawable.abc_textfield_default_mtrl_alpha, R$drawable.abc_ab_share_pack_mtrl_alpha};
                    private final int[] TINT_COLOR_CONTROL_NORMAL = {R$drawable.abc_ic_commit_search_api_mtrl_alpha, R$drawable.abc_seekbar_tick_mark_material, R$drawable.abc_ic_menu_share_mtrl_alpha, R$drawable.abc_ic_menu_copy_mtrl_am_alpha, R$drawable.abc_ic_menu_cut_mtrl_alpha, R$drawable.abc_ic_menu_selectall_mtrl_alpha, R$drawable.abc_ic_menu_paste_mtrl_am_alpha};
                    private final int[] COLORFILTER_COLOR_CONTROL_ACTIVATED = {R$drawable.abc_textfield_activated_mtrl_alpha, R$drawable.abc_textfield_search_activated_mtrl_alpha, R$drawable.abc_cab_background_top_mtrl_alpha, R$drawable.abc_text_cursor_material, R$drawable.abc_text_select_handle_left_mtrl_dark, R$drawable.abc_text_select_handle_middle_mtrl_dark, R$drawable.abc_text_select_handle_right_mtrl_dark, R$drawable.abc_text_select_handle_left_mtrl_light, R$drawable.abc_text_select_handle_middle_mtrl_light, R$drawable.abc_text_select_handle_right_mtrl_light};
                    private final int[] COLORFILTER_COLOR_BACKGROUND_MULTIPLY = {R$drawable.abc_popup_background_mtrl_mult, R$drawable.abc_cab_background_internal_bg, R$drawable.abc_menu_hardkey_panel_mtrl_mult};
                    private final int[] TINT_COLOR_CONTROL_STATE_LIST = {R$drawable.abc_tab_indicator_material, R$drawable.abc_textfield_search_material};
                    private final int[] TINT_CHECKABLE_BUTTON_LIST = {R$drawable.abc_btn_check_material, R$drawable.abc_btn_radio_material, R$drawable.abc_btn_check_material_anim, R$drawable.abc_btn_radio_material_anim};

                    private ColorStateList createDefaultButtonColorStateList(Context context) {
                        return createButtonColorStateList(context, ThemeUtils.getThemeAttrColor(context, R$attr.colorButtonNormal));
                    }

                    private ColorStateList createBorderlessButtonColorStateList(Context context) {
                        return createButtonColorStateList(context, 0);
                    }

                    private ColorStateList createColoredButtonColorStateList(Context context) {
                        return createButtonColorStateList(context, ThemeUtils.getThemeAttrColor(context, R$attr.colorAccent));
                    }

                    private ColorStateList createButtonColorStateList(Context context, int baseColor) {
                        int[][] states = new int[4];
                        int[] colors = new int[4];
                        int colorControlHighlight = ThemeUtils.getThemeAttrColor(context, R$attr.colorControlHighlight);
                        int disabledColor = ThemeUtils.getDisabledThemeAttrColor(context, R$attr.colorButtonNormal);
                        states[0] = ThemeUtils.DISABLED_STATE_SET;
                        colors[0] = disabledColor;
                        int i = 0 + 1;
                        states[i] = ThemeUtils.PRESSED_STATE_SET;
                        colors[i] = ColorUtils.compositeColors(colorControlHighlight, baseColor);
                        int i2 = i + 1;
                        states[i2] = ThemeUtils.FOCUSED_STATE_SET;
                        colors[i2] = ColorUtils.compositeColors(colorControlHighlight, baseColor);
                        int i3 = i2 + 1;
                        states[i3] = ThemeUtils.EMPTY_STATE_SET;
                        colors[i3] = baseColor;
                        int i4 = i3 + 1;
                        return new ColorStateList(states, colors);
                    }

                    private ColorStateList createSwitchThumbColorStateList(Context context) {
                        int[][] states = new int[3];
                        int[] colors = new int[3];
                        int i = R$attr.colorSwitchThumbNormal;
                        ColorStateList thumbColor = ThemeUtils.getThemeAttrColorStateList(context, i);
                        if (thumbColor == null || !thumbColor.isStateful()) {
                            states[0] = ThemeUtils.DISABLED_STATE_SET;
                            colors[0] = ThemeUtils.getDisabledThemeAttrColor(context, i);
                            int i2 = 0 + 1;
                            states[i2] = ThemeUtils.CHECKED_STATE_SET;
                            colors[i2] = ThemeUtils.getThemeAttrColor(context, R$attr.colorControlActivated);
                            int i3 = i2 + 1;
                            states[i3] = ThemeUtils.EMPTY_STATE_SET;
                            colors[i3] = ThemeUtils.getThemeAttrColor(context, i);
                            int i4 = i3 + 1;
                        } else {
                            states[0] = ThemeUtils.DISABLED_STATE_SET;
                            colors[0] = thumbColor.getColorForState(states[0], 0);
                            int i5 = 0 + 1;
                            states[i5] = ThemeUtils.CHECKED_STATE_SET;
                            colors[i5] = ThemeUtils.getThemeAttrColor(context, R$attr.colorControlActivated);
                            int i6 = i5 + 1;
                            states[i6] = ThemeUtils.EMPTY_STATE_SET;
                            colors[i6] = thumbColor.getDefaultColor();
                            int i7 = i6 + 1;
                        }
                        return new ColorStateList(states, colors);
                    }

                    @Override // androidx.appcompat.widget.ResourceManagerInternal.ResourceManagerHooks
                    public Drawable createDrawableFor(ResourceManagerInternal resourceManager, Context context, int resId) {
                        if (resId == R$drawable.abc_cab_background_top_material) {
                            return new LayerDrawable(new Drawable[]{resourceManager.getDrawable(context, R$drawable.abc_cab_background_internal_bg), resourceManager.getDrawable(context, R$drawable.abc_cab_background_top_mtrl_alpha)});
                        }
                        return null;
                    }

                    private void setPorterDuffColorFilter(Drawable d, int color, PorterDuff.Mode mode) {
                        PorterDuff.Mode mode2;
                        if (DrawableUtils.canSafelyMutateDrawable(d)) {
                            d = d.mutate();
                        }
                        if (mode == null) {
                            mode2 = AppCompatDrawableManager.DEFAULT_MODE;
                        } else {
                            mode2 = mode;
                        }
                        d.setColorFilter(AppCompatDrawableManager.getPorterDuffColorFilter(color, mode2));
                    }

                    @Override // androidx.appcompat.widget.ResourceManagerInternal.ResourceManagerHooks
                    public boolean tintDrawable(Context context, int resId, Drawable drawable) {
                        if (resId == R$drawable.abc_seekbar_track_material) {
                            LayerDrawable ld = (LayerDrawable) drawable;
                            Drawable findDrawableByLayerId = ld.findDrawableByLayerId(R.id.background);
                            int i = R$attr.colorControlNormal;
                            setPorterDuffColorFilter(findDrawableByLayerId, ThemeUtils.getThemeAttrColor(context, i), AppCompatDrawableManager.DEFAULT_MODE);
                            setPorterDuffColorFilter(ld.findDrawableByLayerId(R.id.secondaryProgress), ThemeUtils.getThemeAttrColor(context, i), AppCompatDrawableManager.DEFAULT_MODE);
                            setPorterDuffColorFilter(ld.findDrawableByLayerId(R.id.progress), ThemeUtils.getThemeAttrColor(context, R$attr.colorControlActivated), AppCompatDrawableManager.DEFAULT_MODE);
                            return true;
                        }
                        if (resId == R$drawable.abc_ratingbar_material || resId == R$drawable.abc_ratingbar_indicator_material || resId == R$drawable.abc_ratingbar_small_material) {
                            LayerDrawable ld2 = (LayerDrawable) drawable;
                            setPorterDuffColorFilter(ld2.findDrawableByLayerId(R.id.background), ThemeUtils.getDisabledThemeAttrColor(context, R$attr.colorControlNormal), AppCompatDrawableManager.DEFAULT_MODE);
                            Drawable findDrawableByLayerId2 = ld2.findDrawableByLayerId(R.id.secondaryProgress);
                            int i2 = R$attr.colorControlActivated;
                            setPorterDuffColorFilter(findDrawableByLayerId2, ThemeUtils.getThemeAttrColor(context, i2), AppCompatDrawableManager.DEFAULT_MODE);
                            setPorterDuffColorFilter(ld2.findDrawableByLayerId(R.id.progress), ThemeUtils.getThemeAttrColor(context, i2), AppCompatDrawableManager.DEFAULT_MODE);
                            return true;
                        }
                        return false;
                    }

                    private boolean arrayContains(int[] array, int value) {
                        for (int id : array) {
                            if (id == value) {
                                return true;
                            }
                        }
                        return false;
                    }

                    @Override // androidx.appcompat.widget.ResourceManagerInternal.ResourceManagerHooks
                    public ColorStateList getTintListForDrawableRes(Context context, int resId) {
                        if (resId == R$drawable.abc_edit_text_material) {
                            return AppCompatResources.getColorStateList(context, R$color.abc_tint_edittext);
                        }
                        if (resId == R$drawable.abc_switch_track_mtrl_alpha) {
                            return AppCompatResources.getColorStateList(context, R$color.abc_tint_switch_track);
                        }
                        if (resId == R$drawable.abc_switch_thumb_material) {
                            return createSwitchThumbColorStateList(context);
                        }
                        if (resId == R$drawable.abc_btn_default_mtrl_shape) {
                            return createDefaultButtonColorStateList(context);
                        }
                        if (resId == R$drawable.abc_btn_borderless_material) {
                            return createBorderlessButtonColorStateList(context);
                        }
                        if (resId == R$drawable.abc_btn_colored_material) {
                            return createColoredButtonColorStateList(context);
                        }
                        if (resId == R$drawable.abc_spinner_mtrl_am_alpha || resId == R$drawable.abc_spinner_textfield_background_material) {
                            return AppCompatResources.getColorStateList(context, R$color.abc_tint_spinner);
                        }
                        if (arrayContains(this.TINT_COLOR_CONTROL_NORMAL, resId)) {
                            return ThemeUtils.getThemeAttrColorStateList(context, R$attr.colorControlNormal);
                        }
                        if (arrayContains(this.TINT_COLOR_CONTROL_STATE_LIST, resId)) {
                            return AppCompatResources.getColorStateList(context, R$color.abc_tint_default);
                        }
                        if (arrayContains(this.TINT_CHECKABLE_BUTTON_LIST, resId)) {
                            return AppCompatResources.getColorStateList(context, R$color.abc_tint_btn_checkable);
                        }
                        if (resId == R$drawable.abc_seekbar_thumb_material) {
                            return AppCompatResources.getColorStateList(context, R$color.abc_tint_seek_thumb);
                        }
                        return null;
                    }

                    @Override // androidx.appcompat.widget.ResourceManagerInternal.ResourceManagerHooks
                    public boolean tintDrawableUsingColorFilter(Context context, int resId, Drawable drawable) {
                        PorterDuff.Mode tintMode = AppCompatDrawableManager.DEFAULT_MODE;
                        boolean colorAttrSet = false;
                        int colorAttr = 0;
                        int alpha = -1;
                        if (arrayContains(this.COLORFILTER_TINT_COLOR_CONTROL_NORMAL, resId)) {
                            colorAttr = R$attr.colorControlNormal;
                            colorAttrSet = true;
                        } else if (arrayContains(this.COLORFILTER_COLOR_CONTROL_ACTIVATED, resId)) {
                            colorAttr = R$attr.colorControlActivated;
                            colorAttrSet = true;
                        } else if (arrayContains(this.COLORFILTER_COLOR_BACKGROUND_MULTIPLY, resId)) {
                            colorAttr = R.attr.colorBackground;
                            colorAttrSet = true;
                            tintMode = PorterDuff.Mode.MULTIPLY;
                        } else if (resId == R$drawable.abc_list_divider_mtrl_alpha) {
                            colorAttr = R.attr.colorForeground;
                            colorAttrSet = true;
                            alpha = Math.round(40.8f);
                        } else if (resId == R$drawable.abc_dialog_material_background) {
                            colorAttr = R.attr.colorBackground;
                            colorAttrSet = true;
                        }
                        if (colorAttrSet) {
                            if (DrawableUtils.canSafelyMutateDrawable(drawable)) {
                                drawable = drawable.mutate();
                            }
                            int color = ThemeUtils.getThemeAttrColor(context, colorAttr);
                            drawable.setColorFilter(AppCompatDrawableManager.getPorterDuffColorFilter(color, tintMode));
                            if (alpha != -1) {
                                drawable.setAlpha(alpha);
                                return true;
                            }
                            return true;
                        }
                        return false;
                    }

                    @Override // androidx.appcompat.widget.ResourceManagerInternal.ResourceManagerHooks
                    public PorterDuff.Mode getTintModeForDrawableRes(int resId) {
                        if (resId != R$drawable.abc_switch_thumb_material) {
                            return null;
                        }
                        PorterDuff.Mode mode = PorterDuff.Mode.MULTIPLY;
                        return mode;
                    }
                });
            }
        }
    }

    public static synchronized AppCompatDrawableManager get() {
        AppCompatDrawableManager appCompatDrawableManager;
        synchronized (AppCompatDrawableManager.class) {
            if (INSTANCE == null) {
                preload();
            }
            appCompatDrawableManager = INSTANCE;
        }
        return appCompatDrawableManager;
    }

    public synchronized Drawable getDrawable(Context context, int resId) {
        return this.mResourceManager.getDrawable(context, resId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized Drawable getDrawable(Context context, int resId, boolean failIfNotKnown) {
        return this.mResourceManager.getDrawable(context, resId, failIfNotKnown);
    }

    public synchronized void onConfigurationChanged(Context context) {
        this.mResourceManager.onConfigurationChanged(context);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized ColorStateList getTintList(Context context, int resId) {
        return this.mResourceManager.getTintList(context, resId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void tintDrawable(Drawable drawable, TintInfo tint, int[] state) {
        ResourceManagerInternal.tintDrawable(drawable, tint, state);
    }

    public static synchronized PorterDuffColorFilter getPorterDuffColorFilter(int color, PorterDuff.Mode mode) {
        PorterDuffColorFilter porterDuffColorFilter;
        synchronized (AppCompatDrawableManager.class) {
            porterDuffColorFilter = ResourceManagerInternal.getPorterDuffColorFilter(color, mode);
        }
        return porterDuffColorFilter;
    }
}
