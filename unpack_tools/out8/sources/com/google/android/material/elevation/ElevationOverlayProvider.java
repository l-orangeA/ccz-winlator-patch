package com.google.android.material.elevation;

import android.content.Context;
import android.graphics.Color;
import androidx.core.graphics.ColorUtils;
import com.google.android.material.R$attr;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.resources.MaterialAttributes;

/* loaded from: classes.dex */
public class ElevationOverlayProvider {
    private final int colorSurface;
    private final float displayDensity;
    private final int elevationOverlayColor;
    private final boolean elevationOverlayEnabled;

    public ElevationOverlayProvider(Context context) {
        this.elevationOverlayEnabled = MaterialAttributes.resolveBoolean(context, R$attr.elevationOverlayEnabled, false);
        this.elevationOverlayColor = MaterialColors.getColor(context, R$attr.elevationOverlayColor, 0);
        this.colorSurface = MaterialColors.getColor(context, R$attr.colorSurface, 0);
        this.displayDensity = context.getResources().getDisplayMetrics().density;
    }

    public int compositeOverlayIfNeeded(int backgroundColor, float elevation) {
        if (this.elevationOverlayEnabled && isThemeSurfaceColor(backgroundColor)) {
            return compositeOverlay(backgroundColor, elevation);
        }
        return backgroundColor;
    }

    public int compositeOverlay(int backgroundColor, float elevation) {
        float overlayAlphaFraction = calculateOverlayAlphaFraction(elevation);
        int backgroundAlpha = Color.alpha(backgroundColor);
        int backgroundColorOpaque = ColorUtils.setAlphaComponent(backgroundColor, 255);
        int overlayColorOpaque = MaterialColors.layer(backgroundColorOpaque, this.elevationOverlayColor, overlayAlphaFraction);
        return ColorUtils.setAlphaComponent(overlayColorOpaque, backgroundAlpha);
    }

    public float calculateOverlayAlphaFraction(float elevation) {
        float f = this.displayDensity;
        if (f <= 0.0f || elevation <= 0.0f) {
            return 0.0f;
        }
        float elevationDp = elevation / f;
        float alphaFraction = ((((float) Math.log1p(elevationDp)) * 4.5f) + 2.0f) / 100.0f;
        return Math.min(alphaFraction, 1.0f);
    }

    public boolean isThemeElevationOverlayEnabled() {
        return this.elevationOverlayEnabled;
    }

    private boolean isThemeSurfaceColor(int color) {
        return ColorUtils.setAlphaComponent(color, 255) == this.colorSurface;
    }
}
