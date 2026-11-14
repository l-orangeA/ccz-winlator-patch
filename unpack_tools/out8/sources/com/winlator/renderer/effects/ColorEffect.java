package com.winlator.renderer.effects;

import com.winlator.renderer.material.ScreenMaterial;
import com.winlator.renderer.material.ShaderMaterial;

/* loaded from: classes.dex */
public class ColorEffect extends Effect {
    private float brightness = 0.0f;
    private float contrast = 0.0f;
    private float gamma = 1.0f;

    @Override // com.winlator.renderer.effects.Effect
    public ShaderMaterial createMaterial() {
        ScreenMaterial material = new ScreenMaterial() { // from class: com.winlator.renderer.effects.ColorEffect.1
            @Override // com.winlator.renderer.material.ShaderMaterial
            protected String getFragmentShader() {
                return String.join("\n", "precision highp float;", "uniform sampler2D screenTexture;", "uniform float brightness;", "uniform float contrast;", "uniform float gamma;", "varying vec2 vUV;", "void main() {", "vec4 texelColor = texture2D(screenTexture, vUV);", "vec3 color = texelColor.rgb;", "color = clamp(color + brightness, 0.0, 1.0);", "color = (color - 0.5) * clamp(contrast + 1.0, 0.5, 2.0) + 0.5;", "color = pow(color, vec3(1.0 / gamma));", "gl_FragColor = vec4(color, texelColor.a);", "}");
            }

            @Override // com.winlator.renderer.material.ShaderMaterial
            public void use() {
                super.use();
                setUniformFloat("brightness", ColorEffect.this.brightness);
                setUniformFloat("contrast", ColorEffect.this.contrast);
                setUniformFloat("gamma", ColorEffect.this.gamma);
            }
        };
        material.setUniformNames("brightness", "contrast", "gamma", "screenTexture");
        return material;
    }

    public float getBrightness() {
        return this.brightness;
    }

    public void setBrightness(float brightness) {
        this.brightness = brightness;
    }

    public float getContrast() {
        return this.contrast;
    }

    public void setContrast(float contrast) {
        this.contrast = contrast;
    }

    public float getGamma() {
        return this.gamma;
    }

    public void setGamma(float gamma) {
        this.gamma = gamma;
    }
}
