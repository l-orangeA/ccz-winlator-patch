package com.google.android.material.card;

import android.R;
import android.content.res.ColorStateList;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Checkable;
import android.widget.FrameLayout;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.cardview.widget.CardView;
import com.google.android.material.R$attr;
import com.google.android.material.R$style;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.shape.Shapeable;

/* loaded from: classes.dex */
public class MaterialCardView extends CardView implements Checkable, Shapeable {
    private boolean checked;
    private boolean dragged;
    private boolean isParentCardViewDoneInitializing;
    private OnCheckedChangeListener onCheckedChangeListener;
    private static final int[] CHECKABLE_STATE_SET = {R.attr.state_checkable};
    private static final int[] CHECKED_STATE_SET = {R.attr.state_checked};
    private static final int[] DRAGGED_STATE_SET = {R$attr.state_dragged};
    private static final int DEF_STYLE_RES = R$style.Widget_MaterialComponents_CardView;

    /* loaded from: classes.dex */
    public interface OnCheckedChangeListener {
        void onCheckedChanged(MaterialCardView materialCardView, boolean z);
    }

    @Override // android.view.View
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName("androidx.cardview.widget.CardView");
        info.setCheckable(isCheckable());
        info.setClickable(isClickable());
        info.setChecked(isChecked());
    }

    @Override // android.view.View
    public void onInitializeAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        super.onInitializeAccessibilityEvent(accessibilityEvent);
        accessibilityEvent.setClassName("androidx.cardview.widget.CardView");
        accessibilityEvent.setChecked(isChecked());
    }

    @Override // androidx.cardview.widget.CardView, android.widget.FrameLayout, android.view.View
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthMeasureSpec2 = getMeasuredWidth();
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.onMeasure(widthMeasureSpec2, getMeasuredHeight());
        throw null;
    }

    public void setStrokeColor(int strokeColor) {
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.setStrokeColor(ColorStateList.valueOf(strokeColor));
        throw null;
    }

    public void setStrokeColor(ColorStateList strokeColor) {
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.setStrokeColor(strokeColor);
        throw null;
    }

    @Deprecated
    public int getStrokeColor() {
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.getStrokeColor();
        throw null;
    }

    public ColorStateList getStrokeColorStateList() {
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.getStrokeColorStateList();
        throw null;
    }

    public void setStrokeWidth(int strokeWidth) {
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.setStrokeWidth(strokeWidth);
        throw null;
    }

    public int getStrokeWidth() {
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.getStrokeWidth();
        throw null;
    }

    @Override // androidx.cardview.widget.CardView
    public void setRadius(float radius) {
        super.setRadius(radius);
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.setCornerRadius(radius);
        throw null;
    }

    @Override // androidx.cardview.widget.CardView
    public float getRadius() {
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.getCornerRadius();
        throw null;
    }

    float getCardViewRadius() {
        return super.getRadius();
    }

    public void setProgress(float progress) {
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.setProgress(progress);
        throw null;
    }

    public float getProgress() {
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.getProgress();
        throw null;
    }

    @Override // androidx.cardview.widget.CardView
    public int getContentPaddingLeft() {
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.getUserContentPadding();
        throw null;
    }

    @Override // androidx.cardview.widget.CardView
    public int getContentPaddingTop() {
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.getUserContentPadding();
        throw null;
    }

    @Override // androidx.cardview.widget.CardView
    public int getContentPaddingRight() {
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.getUserContentPadding();
        throw null;
    }

    @Override // androidx.cardview.widget.CardView
    public int getContentPaddingBottom() {
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.getUserContentPadding();
        throw null;
    }

    @Override // androidx.cardview.widget.CardView
    public void setCardBackgroundColor(int color) {
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.setCardBackgroundColor(ColorStateList.valueOf(color));
        throw null;
    }

    @Override // androidx.cardview.widget.CardView
    public void setCardBackgroundColor(ColorStateList color) {
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.setCardBackgroundColor(color);
        throw null;
    }

    @Override // androidx.cardview.widget.CardView
    public ColorStateList getCardBackgroundColor() {
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.getCardBackgroundColor();
        throw null;
    }

    public void setCardForegroundColor(ColorStateList foregroundColor) {
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.setCardForegroundColor(foregroundColor);
        throw null;
    }

    public ColorStateList getCardForegroundColor() {
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.getCardForegroundColor();
        throw null;
    }

    @Override // android.view.View
    public void setClickable(boolean clickable) {
        super.setClickable(clickable);
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.getBackground();
        throw null;
    }

    @Override // androidx.cardview.widget.CardView
    public void setCardElevation(float elevation) {
        super.setCardElevation(elevation);
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.updateElevation();
        throw null;
    }

    @Override // androidx.cardview.widget.CardView
    public void setMaxCardElevation(float maxCardElevation) {
        super.setMaxCardElevation(maxCardElevation);
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.updateInsets();
        throw null;
    }

    @Override // androidx.cardview.widget.CardView
    public void setUseCompatPadding(boolean useCompatPadding) {
        super.setUseCompatPadding(useCompatPadding);
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.updateInsets();
        throw null;
    }

    @Override // androidx.cardview.widget.CardView
    public void setPreventCornerOverlap(boolean preventCornerOverlap) {
        super.setPreventCornerOverlap(preventCornerOverlap);
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.updateInsets();
        throw null;
    }

    @Override // android.view.View
    public void setBackground(Drawable drawable) {
        setBackgroundDrawable(drawable);
    }

    @Override // android.view.View
    public void setBackgroundDrawable(Drawable drawable) {
        if (this.isParentCardViewDoneInitializing) {
            MaterialCardViewHelper materialCardViewHelper = null;
            materialCardViewHelper.isBackgroundOverwritten();
            throw null;
        }
    }

    void setBackgroundInternal(Drawable drawable) {
        super.setBackgroundDrawable(drawable);
    }

    @Override // android.widget.Checkable
    public boolean isChecked() {
        return this.checked;
    }

    @Override // android.widget.Checkable
    public void setChecked(boolean checked) {
        if (this.checked != checked) {
            toggle();
        }
    }

    public void setDragged(boolean dragged) {
        if (this.dragged != dragged) {
            this.dragged = dragged;
            refreshDrawableState();
            forceRippleRedrawIfNeeded();
            invalidate();
        }
    }

    public boolean isDragged() {
        return this.dragged;
    }

    public boolean isCheckable() {
        return false;
    }

    public void setCheckable(boolean checkable) {
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.setCheckable(checkable);
        throw null;
    }

    @Override // android.widget.Checkable
    public void toggle() {
        if (isCheckable() && isEnabled()) {
            this.checked = !this.checked;
            refreshDrawableState();
            forceRippleRedrawIfNeeded();
            OnCheckedChangeListener onCheckedChangeListener = this.onCheckedChangeListener;
            if (onCheckedChangeListener != null) {
                onCheckedChangeListener.onCheckedChanged(this, this.checked);
            }
        }
    }

    @Override // android.view.ViewGroup, android.view.View
    protected int[] onCreateDrawableState(int extraSpace) {
        int[] drawableState = super.onCreateDrawableState(extraSpace + 3);
        if (isCheckable()) {
            FrameLayout.mergeDrawableStates(drawableState, CHECKABLE_STATE_SET);
        }
        if (isChecked()) {
            FrameLayout.mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        if (isDragged()) {
            FrameLayout.mergeDrawableStates(drawableState, DRAGGED_STATE_SET);
        }
        return drawableState;
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        this.onCheckedChangeListener = listener;
    }

    public void setRippleColor(ColorStateList rippleColor) {
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.setRippleColor(rippleColor);
        throw null;
    }

    public void setRippleColorResource(int rippleColorResourceId) {
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.setRippleColor(AppCompatResources.getColorStateList(getContext(), rippleColorResourceId));
        throw null;
    }

    public ColorStateList getRippleColor() {
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.getRippleColor();
        throw null;
    }

    public Drawable getCheckedIcon() {
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.getCheckedIcon();
        throw null;
    }

    public void setCheckedIconResource(int id) {
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.setCheckedIcon(AppCompatResources.getDrawable(getContext(), id));
        throw null;
    }

    public void setCheckedIcon(Drawable checkedIcon) {
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.setCheckedIcon(checkedIcon);
        throw null;
    }

    public ColorStateList getCheckedIconTint() {
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.getCheckedIconTint();
        throw null;
    }

    public void setCheckedIconTint(ColorStateList checkedIconTint) {
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.setCheckedIconTint(checkedIconTint);
        throw null;
    }

    public int getCheckedIconSize() {
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.getCheckedIconSize();
        throw null;
    }

    public void setCheckedIconSize(int checkedIconSize) {
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.setCheckedIconSize(checkedIconSize);
        throw null;
    }

    public void setCheckedIconSizeResource(int checkedIconSizeResId) {
        if (checkedIconSizeResId != 0) {
            MaterialCardViewHelper materialCardViewHelper = null;
            materialCardViewHelper.setCheckedIconSize(getResources().getDimensionPixelSize(checkedIconSizeResId));
            throw null;
        }
    }

    public int getCheckedIconMargin() {
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.getCheckedIconMargin();
        throw null;
    }

    public void setCheckedIconMargin(int checkedIconMargin) {
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.setCheckedIconMargin(checkedIconMargin);
        throw null;
    }

    public void setCheckedIconMarginResource(int checkedIconMarginResId) {
        if (checkedIconMarginResId != -1) {
            MaterialCardViewHelper materialCardViewHelper = null;
            materialCardViewHelper.setCheckedIconMargin(getResources().getDimensionPixelSize(checkedIconMarginResId));
            throw null;
        }
    }

    private RectF getBoundsAsRectF() {
        new RectF();
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.getBackground();
        throw null;
    }

    @Override // com.google.android.material.shape.Shapeable
    public void setShapeAppearanceModel(ShapeAppearanceModel shapeAppearanceModel) {
        setClipToOutline(shapeAppearanceModel.isRoundRect(getBoundsAsRectF()));
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.setShapeAppearanceModel(shapeAppearanceModel);
        throw null;
    }

    public ShapeAppearanceModel getShapeAppearanceModel() {
        MaterialCardViewHelper materialCardViewHelper = null;
        materialCardViewHelper.getShapeAppearanceModel();
        throw null;
    }

    private void forceRippleRedrawIfNeeded() {
        if (Build.VERSION.SDK_INT > 26) {
            MaterialCardViewHelper materialCardViewHelper = null;
            materialCardViewHelper.forceRippleRedraw();
            throw null;
        }
    }
}
