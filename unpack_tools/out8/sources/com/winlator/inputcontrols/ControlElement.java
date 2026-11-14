package com.winlator.inputcontrols;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import androidx.core.graphics.ColorUtils;
import com.winlator.core.CubicBezierInterpolator;
import com.winlator.math.Mathf;
import com.winlator.widget.InputControlsView;
import com.winlator.widget.TouchpadView;
import java.util.Arrays;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class ControlElement {
    private Binding[] bindings;
    private final Rect boundingBox;
    private boolean boundingBoxNeedsUpdate;
    private int currentPointerId;
    private PointF currentPosition;
    private byte iconId;
    private final InputControlsView inputControlsView;
    private CubicBezierInterpolator interpolator;
    private byte orientation;
    private Path path;
    private boolean pressed;
    private Range range;
    private float scale;
    private RangeScroller scroller;
    private boolean selected;
    private boolean[] states;
    private String text;
    private boolean toggleSwitch;
    private Object touchTime;
    private short x;
    private short y;
    private Type type = Type.BUTTON;
    private Shape shape = Shape.CIRCLE;

    /* loaded from: classes.dex */
    public enum Type {
        BUTTON,
        D_PAD,
        RANGE_BUTTON,
        STICK,
        TRACKPAD;

        public static String[] names() {
            Type[] types = values();
            String[] names = new String[types.length];
            for (int i = 0; i < types.length; i++) {
                names[i] = types[i].name().replace("_", "-");
            }
            return names;
        }
    }

    /* loaded from: classes.dex */
    public enum Shape {
        CIRCLE,
        RECT,
        ROUND_RECT,
        SQUARE;

        public static String[] names() {
            Shape[] shapes = values();
            String[] names = new String[shapes.length];
            for (int i = 0; i < shapes.length; i++) {
                names[i] = shapes[i].name().replace("_", " ");
            }
            return names;
        }
    }

    /* loaded from: classes.dex */
    public enum Range {
        FROM_A_TO_Z(26),
        FROM_0_TO_9(10),
        FROM_F1_TO_F12(12),
        FROM_NP0_TO_NP9(10);

        public final byte max;

        Range(int max) {
            this.max = (byte) max;
        }

        public static String[] names() {
            Range[] ranges = values();
            String[] names = new String[ranges.length];
            for (int i = 0; i < ranges.length; i++) {
                names[i] = ranges[i].name().replace("_", " ");
            }
            return names;
        }
    }

    public ControlElement(InputControlsView inputControlsView) {
        Binding binding = Binding.NONE;
        this.bindings = new Binding[]{binding, binding, binding, binding};
        this.scale = 1.0f;
        this.selected = false;
        this.pressed = false;
        this.toggleSwitch = false;
        this.currentPointerId = -1;
        this.boundingBox = new Rect();
        this.states = new boolean[4];
        this.boundingBoxNeedsUpdate = true;
        this.text = "";
        this.inputControlsView = inputControlsView;
    }

    private void reset() {
        this.bindings = new Binding[4];
        setBinding(Binding.NONE);
        this.scroller = null;
        Type type = this.type;
        if (type == Type.D_PAD || type == Type.STICK) {
            Binding[] bindingArr = this.bindings;
            bindingArr[0] = Binding.KEY_W;
            bindingArr[1] = Binding.KEY_D;
            bindingArr[2] = Binding.KEY_S;
            bindingArr[3] = Binding.KEY_A;
        } else if (type == Type.TRACKPAD) {
            Binding[] bindingArr2 = this.bindings;
            bindingArr2[0] = Binding.MOUSE_MOVE_UP;
            bindingArr2[1] = Binding.MOUSE_MOVE_RIGHT;
            bindingArr2[2] = Binding.MOUSE_MOVE_DOWN;
            bindingArr2[3] = Binding.MOUSE_MOVE_LEFT;
        } else if (type == Type.RANGE_BUTTON) {
            this.scroller = new RangeScroller(this.inputControlsView, this);
        }
        this.text = "";
        this.iconId = (byte) 0;
        this.range = null;
        this.boundingBoxNeedsUpdate = true;
    }

    public Type getType() {
        return this.type;
    }

    public void setType(Type type) {
        this.type = type;
        reset();
    }

    public byte getBindingCount() {
        return (byte) this.bindings.length;
    }

    public byte getFirstBindingIndex() {
        byte i = 0;
        while (true) {
            Binding[] bindingArr = this.bindings;
            if (i >= bindingArr.length) {
                return (byte) 0;
            }
            if (bindingArr[i] != Binding.NONE) {
                return i;
            }
            i = (byte) (i + 1);
        }
    }

    public void setBindingCount(int bindingCount) {
        this.bindings = new Binding[bindingCount];
        setBinding(Binding.NONE);
        this.states = new boolean[bindingCount];
        this.boundingBoxNeedsUpdate = true;
    }

    public Shape getShape() {
        return this.shape;
    }

    public void setShape(Shape shape) {
        this.shape = shape;
        this.boundingBoxNeedsUpdate = true;
    }

    public Range getRange() {
        Range range = this.range;
        return range != null ? range : Range.FROM_A_TO_Z;
    }

    public void setRange(Range range) {
        this.range = range;
    }

    public byte getOrientation() {
        return this.orientation;
    }

    public void setOrientation(byte orientation) {
        this.orientation = orientation;
        this.boundingBoxNeedsUpdate = true;
    }

    public boolean isToggleSwitch() {
        return this.toggleSwitch;
    }

    public void setToggleSwitch(boolean toggleSwitch) {
        this.toggleSwitch = toggleSwitch;
    }

    public Binding getBindingAt(int index) {
        Binding[] bindingArr = this.bindings;
        return index < bindingArr.length ? bindingArr[index] : Binding.NONE;
    }

    public void setBindingAt(int index, Binding binding) {
        Binding[] bindingArr = this.bindings;
        if (index >= bindingArr.length) {
            int oldLength = bindingArr.length;
            Binding[] bindingArr2 = (Binding[]) Arrays.copyOf(bindingArr, index + 1);
            this.bindings = bindingArr2;
            Arrays.fill(bindingArr2, oldLength - 1, bindingArr2.length, Binding.NONE);
            this.states = new boolean[this.bindings.length];
            this.boundingBoxNeedsUpdate = true;
        }
        this.bindings[index] = binding;
    }

    public void setBinding(Binding binding) {
        Arrays.fill(this.bindings, binding);
    }

    public float getScale() {
        return this.scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
        this.boundingBoxNeedsUpdate = true;
    }

    public short getX() {
        return this.x;
    }

    public void setX(int x) {
        this.x = (short) x;
        this.boundingBoxNeedsUpdate = true;
    }

    public short getY() {
        return this.y;
    }

    public void setY(int y) {
        this.y = (short) y;
        this.boundingBoxNeedsUpdate = true;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getText() {
        return this.text;
    }

    public void setText(String text) {
        this.text = text != null ? text : "";
    }

    public byte getIconId() {
        return this.iconId;
    }

    public void setIconId(int iconId) {
        this.iconId = (byte) iconId;
    }

    public Rect getBoundingBox() {
        if (this.boundingBoxNeedsUpdate) {
            computeBoundingBox();
        }
        return this.boundingBox;
    }

    private Rect computeBoundingBox() {
        int snappingSize = this.inputControlsView.getSnappingSize();
        int halfWidth = 0;
        int halfHeight = 0;
        switch (AnonymousClass1.$SwitchMap$com$winlator$inputcontrols$ControlElement$Type[this.type.ordinal()]) {
            case 1:
                switch (AnonymousClass1.$SwitchMap$com$winlator$inputcontrols$ControlElement$Shape[this.shape.ordinal()]) {
                    case 1:
                    case 2:
                        halfWidth = snappingSize * 4;
                        halfHeight = snappingSize * 2;
                        break;
                    case 3:
                        halfWidth = (int) (snappingSize * 2.5f);
                        halfHeight = (int) (snappingSize * 2.5f);
                        break;
                    case 4:
                        halfWidth = snappingSize * 3;
                        halfHeight = snappingSize * 3;
                        break;
                }
            case 2:
                halfWidth = snappingSize * 7;
                halfHeight = snappingSize * 7;
                break;
            case 3:
            case 4:
                halfWidth = snappingSize * 6;
                halfHeight = snappingSize * 6;
                break;
            case 5:
                halfWidth = snappingSize * ((this.bindings.length * 4) / 2);
                halfHeight = snappingSize * 2;
                if (this.orientation == 1) {
                    halfWidth = halfHeight;
                    halfHeight = halfWidth;
                    break;
                }
                break;
        }
        float f = this.scale;
        int halfWidth2 = (int) (halfWidth * f);
        int halfHeight2 = (int) (halfHeight * f);
        Rect rect = this.boundingBox;
        short s = this.x;
        short s2 = this.y;
        rect.set(s - halfWidth2, s2 - halfHeight2, s + halfWidth2, s2 + halfHeight2);
        this.boundingBoxNeedsUpdate = false;
        this.path = null;
        return this.boundingBox;
    }

    private String getBindingTextAt(int index) {
        Binding binding = getBindingAt(index);
        String text = binding.toString().replace("NUMPAD ", "NP").replace("BUTTON ", "");
        if (text.length() > 7) {
            String[] parts = text.split(" ");
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                sb.append(part.charAt(0));
            }
            StringBuilder sb2 = new StringBuilder();
            sb2.append(binding.isMouse() ? "M" : "");
            sb2.append((Object) sb);
            return sb2.toString();
        }
        return text;
    }

    private String getDisplayText() {
        String str = this.text;
        if (str != null && !str.isEmpty()) {
            return this.text;
        }
        if (this.type == Type.BUTTON) {
            StringBuilder sb = new StringBuilder();
            byte i = 0;
            while (true) {
                Binding[] bindingArr = this.bindings;
                if (i >= bindingArr.length) {
                    break;
                }
                if (bindingArr[i] != Binding.NONE) {
                    if (sb.length() > 0) {
                        sb.append("+");
                    }
                    sb.append(getBindingTextAt(i));
                }
                i = (byte) (i + 1);
            }
            if (sb.length() > 0) {
                return sb.toString();
            }
        }
        return getBindingTextAt(0);
    }

    private static float getTextSizeForWidth(Paint paint, String text, float desiredWidth) {
        paint.setTextSize(48.0f);
        return (48.0f * desiredWidth) / paint.measureText(text);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.winlator.inputcontrols.ControlElement$1, reason: invalid class name */
    /* loaded from: classes.dex */
    public static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$winlator$inputcontrols$ControlElement$Range;
        static final /* synthetic */ int[] $SwitchMap$com$winlator$inputcontrols$ControlElement$Shape;
        static final /* synthetic */ int[] $SwitchMap$com$winlator$inputcontrols$ControlElement$Type;

        static {
            int[] iArr = new int[Range.values().length];
            $SwitchMap$com$winlator$inputcontrols$ControlElement$Range = iArr;
            try {
                iArr[Range.FROM_A_TO_Z.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$winlator$inputcontrols$ControlElement$Range[Range.FROM_0_TO_9.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$winlator$inputcontrols$ControlElement$Range[Range.FROM_F1_TO_F12.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$winlator$inputcontrols$ControlElement$Range[Range.FROM_NP0_TO_NP9.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            int[] iArr2 = new int[Type.values().length];
            $SwitchMap$com$winlator$inputcontrols$ControlElement$Type = iArr2;
            try {
                iArr2[Type.BUTTON.ordinal()] = 1;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$winlator$inputcontrols$ControlElement$Type[Type.D_PAD.ordinal()] = 2;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$winlator$inputcontrols$ControlElement$Type[Type.TRACKPAD.ordinal()] = 3;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$com$winlator$inputcontrols$ControlElement$Type[Type.STICK.ordinal()] = 4;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$com$winlator$inputcontrols$ControlElement$Type[Type.RANGE_BUTTON.ordinal()] = 5;
            } catch (NoSuchFieldError e9) {
            }
            int[] iArr3 = new int[Shape.values().length];
            $SwitchMap$com$winlator$inputcontrols$ControlElement$Shape = iArr3;
            try {
                iArr3[Shape.RECT.ordinal()] = 1;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$com$winlator$inputcontrols$ControlElement$Shape[Shape.ROUND_RECT.ordinal()] = 2;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$com$winlator$inputcontrols$ControlElement$Shape[Shape.SQUARE.ordinal()] = 3;
            } catch (NoSuchFieldError e12) {
            }
            try {
                $SwitchMap$com$winlator$inputcontrols$ControlElement$Shape[Shape.CIRCLE.ordinal()] = 4;
            } catch (NoSuchFieldError e13) {
            }
        }
    }

    private static Binding getRangeBindingForIndex(Range range, int index) {
        switch (AnonymousClass1.$SwitchMap$com$winlator$inputcontrols$ControlElement$Range[range.ordinal()]) {
            case 1:
                return Binding.valueOf("KEY_" + ((char) (index + 65)));
            case 2:
                return Binding.valueOf("KEY_" + ((index + 1) % 10));
            case 3:
                return Binding.valueOf("KEY_F" + (index + 1));
            case 4:
                return Binding.valueOf("KEY_KP_" + ((index + 1) % 10));
            default:
                return Binding.NONE;
        }
    }

    private static String getRangeTextForIndex(Range range, int index) {
        switch (AnonymousClass1.$SwitchMap$com$winlator$inputcontrols$ControlElement$Range[range.ordinal()]) {
            case 1:
                return String.valueOf((char) (index + 65));
            case 2:
                return String.valueOf((index + 1) % 10);
            case 3:
                return "F" + (index + 1);
            case 4:
                return "NP" + ((index + 1) % 10);
            default:
                return "";
        }
    }

    public void draw(Canvas canvas) {
        Binding selectedBinding;
        boolean z;
        Rect boundingBox;
        int lightColor;
        int oldColor;
        Paint paint;
        float minTextSize;
        Range range;
        Paint paint2;
        String text;
        Range range2;
        Rect boundingBox2;
        float strokeWidth;
        int oldColor2;
        Binding selectedBinding2;
        int snappingSize;
        int lightColor2;
        Paint paint3;
        float minTextSize2;
        Paint paint4;
        Paint paint5;
        String text2;
        int snappingSize2 = this.inputControlsView.getSnappingSize();
        Paint paint6 = this.inputControlsView.getPaint();
        int lightColor3 = this.inputControlsView.getLightColor();
        paint6.setColor(this.selected ? this.inputControlsView.getHighlightColor() : lightColor3);
        paint6.setStyle(Paint.Style.STROKE);
        float strokeWidth2 = snappingSize2 * 0.25f;
        paint6.setStrokeWidth(strokeWidth2);
        Rect boundingBox3 = getBoundingBox();
        switch (AnonymousClass1.$SwitchMap$com$winlator$inputcontrols$ControlElement$Type[this.type.ordinal()]) {
            case 1:
                if (this.pressed) {
                    paint6.setStyle(Paint.Style.FILL);
                }
                float cx = boundingBox3.centerX();
                float cy = boundingBox3.centerY();
                switch (AnonymousClass1.$SwitchMap$com$winlator$inputcontrols$ControlElement$Shape[this.shape.ordinal()]) {
                    case 1:
                        canvas.drawRect(boundingBox3, paint6);
                        break;
                    case 2:
                        float radius = boundingBox3.height() * 0.5f;
                        canvas.drawRoundRect(boundingBox3.left, boundingBox3.top, boundingBox3.right, boundingBox3.bottom, radius, radius, paint6);
                        break;
                    case 3:
                        float radius2 = snappingSize2 * 0.75f * this.scale;
                        canvas.drawRoundRect(boundingBox3.left, boundingBox3.top, boundingBox3.right, boundingBox3.bottom, radius2, radius2, paint6);
                        break;
                    case 4:
                        canvas.drawCircle(cx, cy, boundingBox3.width() * 0.5f, paint6);
                        break;
                }
                if (this.iconId > 0) {
                    drawIcon(canvas, cx, cy, boundingBox3.width(), boundingBox3.height(), this.iconId);
                    return;
                }
                String text3 = getDisplayText();
                paint6.setTextSize(Math.min(getTextSizeForWidth(paint6, text3, boundingBox3.width() - (strokeWidth2 * 2.0f)), snappingSize2 * 2 * this.scale));
                paint6.setTextAlign(Paint.Align.CENTER);
                paint6.setStyle(Paint.Style.FILL);
                paint6.setColor(this.pressed ? this.inputControlsView.getDarkColor() : lightColor3);
                canvas.drawText(text3, this.x, this.y - ((paint6.descent() + paint6.ascent()) * 0.5f), paint6);
                return;
            case 2:
                float cx2 = boundingBox3.centerX();
                float cy2 = boundingBox3.centerY();
                float f = this.scale;
                float offsetX = snappingSize2 * 2 * f;
                float offsetY = snappingSize2 * 3 * f;
                float start = snappingSize2 * f;
                if (this.path == null) {
                    Path path = new Path();
                    this.path = path;
                    path.moveTo(cx2, cy2 - start);
                    this.path.lineTo(cx2 - offsetX, cy2 - offsetY);
                    this.path.lineTo(cx2 - offsetX, boundingBox3.top);
                    this.path.lineTo(cx2 + offsetX, boundingBox3.top);
                    this.path.lineTo(cx2 + offsetX, cy2 - offsetY);
                    this.path.close();
                    this.path.moveTo(cx2 - start, cy2);
                    this.path.lineTo(cx2 - offsetY, cy2 - offsetX);
                    this.path.lineTo(boundingBox3.left, cy2 - offsetX);
                    this.path.lineTo(boundingBox3.left, cy2 + offsetX);
                    this.path.lineTo(cx2 - offsetY, cy2 + offsetX);
                    this.path.close();
                    this.path.moveTo(cx2, cy2 + start);
                    this.path.lineTo(cx2 - offsetX, cy2 + offsetY);
                    this.path.lineTo(cx2 - offsetX, boundingBox3.bottom);
                    this.path.lineTo(cx2 + offsetX, boundingBox3.bottom);
                    this.path.lineTo(cx2 + offsetX, cy2 + offsetY);
                    this.path.close();
                    this.path.moveTo(cx2 + start, cy2);
                    this.path.lineTo(cx2 + offsetY, cy2 - offsetX);
                    this.path.lineTo(boundingBox3.right, cy2 - offsetX);
                    this.path.lineTo(boundingBox3.right, cy2 + offsetX);
                    this.path.lineTo(cx2 + offsetY, cy2 + offsetX);
                    this.path.close();
                }
                canvas.drawPath(this.path, paint6);
                return;
            case 3:
                float radius3 = boundingBox3.height() * 0.15f;
                canvas.drawRoundRect(boundingBox3.left, boundingBox3.top, boundingBox3.right, boundingBox3.bottom, radius3, radius3, paint6);
                float offset = strokeWidth2 * 2.5f;
                float innerStrokeWidth = strokeWidth2 * 2.0f;
                float innerHeight = boundingBox3.height() - (2.0f * offset);
                float radius4 = ((innerHeight / boundingBox3.height()) * radius3) - ((innerStrokeWidth * 0.5f) + (strokeWidth2 * 0.5f));
                paint6.setStrokeWidth(innerStrokeWidth);
                canvas.drawRoundRect(boundingBox3.left + offset, boundingBox3.top + offset, boundingBox3.right - offset, boundingBox3.bottom - offset, radius4, radius4, paint6);
                return;
            case 4:
                int cx3 = boundingBox3.centerX();
                int cy3 = boundingBox3.centerY();
                int oldColor3 = paint6.getColor();
                canvas.drawCircle(cx3, cy3, boundingBox3.height() * 0.5f, paint6);
                PointF pointF = this.currentPosition;
                float thumbstickX = pointF != null ? pointF.x : cx3;
                float thumbstickY = pointF != null ? pointF.y : cy3;
                short thumbRadius = (short) (snappingSize2 * 3.5f * this.scale);
                paint6.setStyle(Paint.Style.FILL);
                paint6.setColor(ColorUtils.setAlphaComponent(lightColor3, 50));
                canvas.drawCircle(thumbstickX, thumbstickY, thumbRadius, paint6);
                paint6.setStyle(Paint.Style.STROKE);
                paint6.setColor(oldColor3);
                canvas.drawCircle(thumbstickX, thumbstickY, thumbRadius + (strokeWidth2 * 0.5f), paint6);
                return;
            case 5:
                Range range3 = getRange();
                int oldColor4 = paint6.getColor();
                int darkColor = this.inputControlsView.getDarkColor();
                float radius5 = snappingSize2 * 0.75f * this.scale;
                float elementSize = this.scroller.getElementSize();
                float minTextSize3 = snappingSize2 * 2 * this.scale;
                float scrollOffset = this.scroller.getScrollOffset();
                byte[] rangeIndex = this.scroller.getRangeIndex();
                Binding selectedBinding3 = this.scroller.getBinding();
                boolean wasPressed = false;
                if (this.orientation == 0) {
                    int i = boundingBox3.top;
                    float lineTop = i + (strokeWidth2 * 0.5f);
                    int i2 = boundingBox3.bottom;
                    float lineBottom = i2 - (strokeWidth2 * 0.5f);
                    float startX = boundingBox3.left;
                    int snappingSize3 = snappingSize2;
                    Binding selectedBinding4 = selectedBinding3;
                    float minTextSize4 = minTextSize3;
                    int lightColor4 = lightColor3;
                    int oldColor5 = oldColor4;
                    canvas.drawRoundRect(startX, i, boundingBox3.right, i2, radius5, radius5, paint6);
                    if (this.path == null) {
                        Path path2 = new Path();
                        this.path = path2;
                        path2.addRoundRect(startX, boundingBox3.top, boundingBox3.right, boundingBox3.bottom, radius5, radius5, Path.Direction.CW);
                    }
                    canvas.save();
                    canvas.clipPath(this.path);
                    float startX2 = startX - (scrollOffset % elementSize);
                    byte i3 = rangeIndex[0];
                    while (i3 < rangeIndex[1]) {
                        int index = i3 % range3.max;
                        paint6.setStyle(Paint.Style.STROKE);
                        paint6.setColor(oldColor5);
                        boolean pressed = this.pressed && selectedBinding4 == getRangeBindingForIndex(range3, index);
                        if (startX2 <= boundingBox3.left || startX2 >= boundingBox3.right || pressed || wasPressed) {
                            range2 = range3;
                            boundingBox2 = boundingBox3;
                            strokeWidth = strokeWidth2;
                            oldColor2 = oldColor5;
                            selectedBinding2 = selectedBinding4;
                            snappingSize = snappingSize3;
                            lightColor2 = lightColor4;
                            paint3 = paint6;
                        } else {
                            range2 = range3;
                            boundingBox2 = boundingBox3;
                            strokeWidth = strokeWidth2;
                            oldColor2 = oldColor5;
                            lightColor2 = lightColor4;
                            paint3 = paint6;
                            selectedBinding2 = selectedBinding4;
                            snappingSize = snappingSize3;
                            canvas.drawLine(startX2, lineTop, startX2, lineBottom, paint3);
                        }
                        String text4 = getRangeTextForIndex(range2, index);
                        if (startX2 >= boundingBox2.right || startX2 + elementSize <= boundingBox2.left) {
                            minTextSize2 = minTextSize4;
                            paint4 = paint3;
                        } else {
                            Paint paint7 = paint3;
                            paint7.setStyle(Paint.Style.FILL);
                            if (!pressed) {
                                paint5 = paint7;
                                text2 = text4;
                            } else {
                                paint7.setColor(lightColor2);
                                paint5 = paint7;
                                text2 = text4;
                                canvas.drawRect(startX2, lineTop, startX2 + elementSize, lineBottom, paint5);
                            }
                            paint4 = paint5;
                            paint4.setColor(pressed ? darkColor : lightColor2);
                            minTextSize2 = minTextSize4;
                            paint4.setTextSize(Math.min(getTextSizeForWidth(paint4, text2, elementSize - (strokeWidth * 2.0f)), minTextSize2));
                            paint4.setTextAlign(Paint.Align.CENTER);
                            canvas.drawText(text2, (elementSize * 0.5f) + startX2, this.y - ((paint4.descent() + paint4.ascent()) * 0.5f), paint4);
                        }
                        startX2 += elementSize;
                        wasPressed = pressed;
                        i3 = (byte) (i3 + 1);
                        minTextSize4 = minTextSize2;
                        paint6 = paint4;
                        oldColor5 = oldColor2;
                        lightColor4 = lightColor2;
                        boundingBox3 = boundingBox2;
                        range3 = range2;
                        strokeWidth2 = strokeWidth;
                        snappingSize3 = snappingSize;
                        selectedBinding4 = selectedBinding2;
                    }
                    Paint paint8 = paint6;
                    paint8.setStyle(Paint.Style.STROKE);
                    paint8.setColor(oldColor5);
                    canvas.restore();
                    return;
                }
                Binding selectedBinding5 = selectedBinding3;
                Paint paint9 = paint6;
                int i4 = boundingBox3.left;
                float lineLeft = i4 + (strokeWidth2 * 0.5f);
                int i5 = boundingBox3.right;
                float lineRight = i5 - (strokeWidth2 * 0.5f);
                float startY = boundingBox3.top;
                float f2 = i4;
                float f3 = i5;
                float f4 = boundingBox3.bottom;
                int oldColor6 = oldColor4;
                int lightColor5 = lightColor3;
                Rect boundingBox4 = boundingBox3;
                float minTextSize5 = minTextSize3;
                Range range4 = range3;
                canvas.drawRoundRect(f2, startY, f3, f4, radius5, radius5, paint9);
                if (this.path == null) {
                    Path path3 = new Path();
                    this.path = path3;
                    path3.addRoundRect(boundingBox4.left, startY, boundingBox4.right, boundingBox4.bottom, radius5, radius5, Path.Direction.CW);
                }
                canvas.save();
                canvas.clipPath(this.path);
                byte i6 = rangeIndex[0];
                float startY2 = startY - (scrollOffset % elementSize);
                while (i6 < rangeIndex[1]) {
                    int index2 = i6 % range4.max;
                    paint9.setStyle(Paint.Style.STROKE);
                    paint9.setColor(oldColor6);
                    if (this.pressed) {
                        selectedBinding = selectedBinding5;
                        if (selectedBinding == getRangeBindingForIndex(range4, index2)) {
                            z = true;
                            boolean pressed2 = z;
                            if (startY2 > boundingBox4.top || startY2 >= boundingBox4.bottom || pressed2 || wasPressed) {
                                boundingBox = boundingBox4;
                                lightColor = lightColor5;
                                oldColor = oldColor6;
                                paint = paint9;
                                selectedBinding5 = selectedBinding;
                                minTextSize = minTextSize5;
                                range = range4;
                            } else {
                                boundingBox = boundingBox4;
                                lightColor = lightColor5;
                                selectedBinding5 = selectedBinding;
                                oldColor = oldColor6;
                                minTextSize = minTextSize5;
                                range = range4;
                                paint = paint9;
                                canvas.drawLine(lineLeft, startY2, lineRight, startY2, paint9);
                            }
                            String text5 = getRangeTextForIndex(range, index2);
                            if (startY2 < boundingBox.bottom || startY2 + elementSize <= boundingBox.top) {
                                paint9 = paint;
                            } else {
                                Paint paint10 = paint;
                                paint10.setStyle(Paint.Style.FILL);
                                if (!pressed2) {
                                    paint2 = paint10;
                                    text = text5;
                                } else {
                                    paint10.setColor(lightColor);
                                    paint2 = paint10;
                                    text = text5;
                                    canvas.drawRect(lineLeft, startY2, lineRight, startY2 + elementSize, paint2);
                                }
                                paint9 = paint2;
                                paint9.setColor(pressed2 ? darkColor : lightColor);
                                paint9.setTextSize(Math.min(getTextSizeForWidth(paint9, text, boundingBox.width() - (strokeWidth2 * 2.0f)), minTextSize));
                                paint9.setTextAlign(Paint.Align.CENTER);
                                canvas.drawText(text, this.x, ((elementSize * 0.5f) + startY2) - ((paint9.descent() + paint9.ascent()) * 0.5f), paint9);
                            }
                            startY2 += elementSize;
                            wasPressed = pressed2;
                            i6 = (byte) (i6 + 1);
                            range4 = range;
                            minTextSize5 = minTextSize;
                            lightColor5 = lightColor;
                            boundingBox4 = boundingBox;
                            oldColor6 = oldColor;
                        }
                    } else {
                        selectedBinding = selectedBinding5;
                    }
                    z = false;
                    boolean pressed22 = z;
                    if (startY2 > boundingBox4.top) {
                    }
                    boundingBox = boundingBox4;
                    lightColor = lightColor5;
                    oldColor = oldColor6;
                    paint = paint9;
                    selectedBinding5 = selectedBinding;
                    minTextSize = minTextSize5;
                    range = range4;
                    String text52 = getRangeTextForIndex(range, index2);
                    if (startY2 < boundingBox.bottom) {
                    }
                    paint9 = paint;
                    startY2 += elementSize;
                    wasPressed = pressed22;
                    i6 = (byte) (i6 + 1);
                    range4 = range;
                    minTextSize5 = minTextSize;
                    lightColor5 = lightColor;
                    boundingBox4 = boundingBox;
                    oldColor6 = oldColor;
                }
                paint9.setStyle(Paint.Style.STROKE);
                paint9.setColor(oldColor6);
                canvas.restore();
                return;
            default:
                return;
        }
    }

    private void drawIcon(Canvas canvas, float cx, float cy, float width, float height, int iconId) {
        Paint paint = this.inputControlsView.getPaint();
        Bitmap icon = this.inputControlsView.getIcon((byte) iconId);
        paint.setColorFilter(this.pressed ? this.inputControlsView.getDarkColorFilter() : this.inputControlsView.getLightColorFilter());
        float snappingSize = this.inputControlsView.getSnappingSize();
        Shape shape = this.shape;
        int margin = (int) (snappingSize * ((shape == Shape.CIRCLE || shape == Shape.SQUARE) ? 2.0f : 1.0f) * this.scale);
        int halfSize = (int) ((Math.min(width, height) - margin) * 0.5f);
        Rect srcRect = new Rect(0, 0, icon.getWidth(), icon.getHeight());
        Rect dstRect = new Rect((int) (cx - halfSize), (int) (cy - halfSize), (int) (halfSize + cx), (int) (halfSize + cy));
        canvas.drawBitmap(icon, srcRect, dstRect, paint);
        paint.setColorFilter(null);
    }

    public JSONObject toJSONObject() {
        Range range;
        try {
            JSONObject elementJSONObject = new JSONObject();
            elementJSONObject.put("type", this.type.name());
            elementJSONObject.put("shape", this.shape.name());
            JSONArray bindingsJSONArray = new JSONArray();
            for (Binding binding : this.bindings) {
                bindingsJSONArray.put(binding.name());
            }
            elementJSONObject.put("bindings", bindingsJSONArray);
            elementJSONObject.put("scale", Float.valueOf(this.scale));
            elementJSONObject.put("x", this.x / this.inputControlsView.getMaxWidth());
            elementJSONObject.put("y", this.y / this.inputControlsView.getMaxHeight());
            elementJSONObject.put("toggleSwitch", this.toggleSwitch);
            elementJSONObject.put("text", this.text);
            elementJSONObject.put("iconId", this.iconId);
            if (this.type == Type.RANGE_BUTTON && (range = this.range) != null) {
                elementJSONObject.put("range", range.name());
                int i = this.orientation;
                if (i != 0) {
                    elementJSONObject.put("orientation", i);
                }
            }
            return elementJSONObject;
        } catch (JSONException e) {
            return null;
        }
    }

    public boolean containsPoint(float x, float y) {
        return getBoundingBox().contains((int) (x + 0.5f), (int) (0.5f + y));
    }

    private boolean isKeepButtonPressedAfterMinTime() {
        Binding binding = getBindingAt(0);
        if (this.toggleSwitch) {
            return false;
        }
        return binding == Binding.GAMEPAD_BUTTON_L3 || binding == Binding.GAMEPAD_BUTTON_R3;
    }

    public boolean handleTouchDown(int pointerId, float x, float y) {
        if (this.currentPointerId == -1 && containsPoint(x, y)) {
            this.currentPointerId = pointerId;
            Type type = this.type;
            if (type == Type.BUTTON) {
                if (isKeepButtonPressedAfterMinTime()) {
                    this.touchTime = Long.valueOf(System.currentTimeMillis());
                }
                if (!this.toggleSwitch || !this.selected) {
                    this.inputControlsView.handleInputEvent(this.bindings, true);
                }
                this.pressed = true;
                this.inputControlsView.invalidate();
                return true;
            }
            if (type == Type.RANGE_BUTTON) {
                this.scroller.handleTouchDown(x, y);
                this.pressed = true;
                this.inputControlsView.invalidate();
                return true;
            }
            if (type == Type.TRACKPAD) {
                if (this.currentPosition == null) {
                    this.currentPosition = new PointF();
                }
                this.currentPosition.set(x, y);
            }
            return handleTouchMove(pointerId, x, y);
        }
        return false;
    }

    public boolean handleTouchMove(int pointerId, float x, float y) {
        Type type;
        float localY;
        float offsetX;
        boolean state;
        Rect boundingBox;
        float radius;
        int i = this.currentPointerId;
        if (pointerId == i && ((type = this.type) == Type.D_PAD || type == Type.STICK || type == Type.TRACKPAD)) {
            Rect boundingBox2 = getBoundingBox();
            float radius2 = boundingBox2.width() * 0.5f;
            TouchpadView touchpadView = this.inputControlsView.getTouchpadView();
            Type type2 = this.type;
            Type type3 = Type.TRACKPAD;
            if (type2 == type3) {
                if (this.currentPosition == null) {
                    this.currentPosition = new PointF();
                }
                PointF pointF = this.currentPosition;
                float[] deltaPoint = touchpadView.computeDeltaPoint(pointF.x, pointF.y, x, y);
                localY = deltaPoint[0];
                offsetX = deltaPoint[1];
                this.currentPosition.set(x, y);
            } else {
                float localX = x - boundingBox2.left;
                float localY2 = y - boundingBox2.top;
                float offsetX2 = localX - radius2;
                float offsetY = localY2 - radius2;
                float distance = Mathf.lengthSq(radius2 - localX, radius2 - localY2);
                if (distance > radius2 * radius2) {
                    float angle = (float) Math.atan2(offsetY, offsetX2);
                    radius2 = radius2;
                    offsetX2 = (float) (Math.cos(angle) * radius2);
                    offsetY = (float) (Math.sin(angle) * radius2);
                }
                float deltaX = Mathf.clamp(offsetX2 / radius2, -1.0f, 1.0f);
                localY = deltaX;
                offsetX = Mathf.clamp(offsetY / radius2, -1.0f, 1.0f);
            }
            Type type4 = this.type;
            float f = 3.0f;
            byte b = 3;
            if (type4 == Type.STICK) {
                if (this.currentPosition == null) {
                    this.currentPosition = new PointF();
                }
                PointF pointF2 = this.currentPosition;
                pointF2.x = boundingBox2.left + (localY * radius2) + radius2;
                pointF2.y = boundingBox2.top + (offsetX * radius2) + radius2;
                boolean[] states = new boolean[4];
                states[0] = offsetX <= -0.15f;
                states[1] = localY >= 0.15f;
                states[2] = offsetX >= 0.15f;
                states[3] = localY <= -0.15f;
                byte i2 = 0;
                while (i2 < 4) {
                    float value = (i2 == 1 || i2 == b) ? localY : offsetX;
                    Binding binding = getBindingAt(i2);
                    if (binding.isGamepad()) {
                        this.inputControlsView.handleInputEvent(binding, true, Mathf.clamp(Math.max(0.0f, Math.abs(value) - 0.01f) * Mathf.sign(value) * f, -1.0f, 1.0f));
                        this.states[i2] = true;
                    } else {
                        boolean state2 = binding.isMouseMove() ? states[i2] || states[(i2 + 2) % 4] : states[i2];
                        this.inputControlsView.handleInputEvent(binding, state2, value);
                        this.states[i2] = state2;
                    }
                    i2 = (byte) (i2 + 1);
                    f = 3.0f;
                    b = 3;
                }
                this.inputControlsView.invalidate();
                return true;
            }
            if (type4 == type3) {
                boolean[] states2 = new boolean[4];
                states2[0] = offsetX <= -0.8f;
                states2[1] = localY >= 0.8f;
                states2[2] = offsetX >= 0.8f;
                states2[3] = localY <= -0.8f;
                int cursorDx = 0;
                int cursorDy = 0;
                byte i3 = 0;
                for (byte b2 = 4; i3 < b2; b2 = 4) {
                    float value2 = (i3 == 1 || i3 == 3) ? localY : offsetX;
                    Binding binding2 = getBindingAt(i3);
                    if (binding2.isGamepad()) {
                        if (this.interpolator == null) {
                            this.interpolator = new CubicBezierInterpolator();
                        }
                        if (Math.abs(value2) > 4.0f) {
                            value2 *= 3.0f;
                        }
                        boundingBox = boundingBox2;
                        radius = radius2;
                        this.interpolator.set(0.075f, 0.95f, 0.45f, 0.95f);
                        float interpolatedValue = this.interpolator.getInterpolation(Math.min(1.0f, Math.abs(value2 / 20.0f)));
                        this.inputControlsView.handleInputEvent(binding2, true, Mathf.clamp(Mathf.sign(value2) * interpolatedValue, -1.0f, 1.0f));
                        this.states[i3] = true;
                    } else {
                        boundingBox = boundingBox2;
                        radius = radius2;
                        if (Math.abs(value2) > 6.0f) {
                            value2 *= 1.25f;
                        }
                        if (binding2 == Binding.MOUSE_MOVE_LEFT || binding2 == Binding.MOUSE_MOVE_RIGHT) {
                            int cursorDy2 = Mathf.roundPoint(value2);
                            cursorDx = cursorDy2;
                        } else if (binding2 == Binding.MOUSE_MOVE_UP || binding2 == Binding.MOUSE_MOVE_DOWN) {
                            cursorDy = Mathf.roundPoint(value2);
                        } else {
                            this.inputControlsView.handleInputEvent(binding2, states2[i3], value2);
                            this.states[i3] = states2[i3];
                        }
                    }
                    i3 = (byte) (i3 + 1);
                    boundingBox2 = boundingBox;
                    radius2 = radius;
                }
                if (cursorDx != 0 || cursorDy != 0) {
                    this.inputControlsView.getXServer().injectPointerMoveDelta(cursorDx, cursorDy);
                    return true;
                }
                return true;
            }
            boolean[] states3 = new boolean[4];
            states3[0] = offsetX <= -0.3f;
            states3[1] = localY >= 0.3f;
            states3[2] = offsetX >= 0.3f;
            states3[3] = localY <= -0.3f;
            byte i4 = 0;
            while (i4 < 4) {
                float value3 = (i4 == 1 || i4 == 3) ? localY : offsetX;
                Binding binding3 = getBindingAt(i4);
                if (binding3.isMouseMove()) {
                    if (!states3[i4] && !states3[(i4 + 2) % 4]) {
                        state = false;
                    }
                    state = true;
                } else {
                    state = states3[i4];
                }
                this.inputControlsView.handleInputEvent(binding3, state, value3);
                this.states[i4] = state;
                i4 = (byte) (i4 + 1);
            }
            return true;
        }
        if (pointerId == i && this.type == Type.RANGE_BUTTON) {
            this.scroller.handleTouchMove(x, y);
            if (this.scroller.isScrolling()) {
                this.pressed = false;
                this.inputControlsView.invalidate();
                return true;
            }
            return true;
        }
        return false;
    }

    public boolean handleTouchUp(int pointerId) {
        if (pointerId != this.currentPointerId) {
            return false;
        }
        Type type = this.type;
        if (type == Type.BUTTON) {
            if (isKeepButtonPressedAfterMinTime() && this.touchTime != null) {
                boolean z = System.currentTimeMillis() - ((Long) this.touchTime).longValue() > 300;
                this.selected = z;
                if (!z) {
                    this.inputControlsView.handleInputEvent(this.bindings, false);
                }
                this.touchTime = null;
            } else if (!this.toggleSwitch || this.selected) {
                this.inputControlsView.handleInputEvent(this.bindings, false);
            }
            if (this.toggleSwitch) {
                this.selected = !this.selected;
            }
            this.pressed = false;
            this.inputControlsView.invalidate();
        } else if (type == Type.RANGE_BUTTON || type == Type.D_PAD || type == Type.STICK || type == Type.TRACKPAD) {
            byte i = 0;
            while (true) {
                boolean[] zArr = this.states;
                if (i >= zArr.length) {
                    break;
                }
                if (zArr[i]) {
                    this.inputControlsView.handleInputEvent(getBindingAt(i), false);
                }
                this.states[i] = false;
                i = (byte) (i + 1);
            }
            Type type2 = this.type;
            if (type2 == Type.RANGE_BUTTON) {
                this.scroller.handleTouchUp();
                this.pressed = false;
                this.inputControlsView.invalidate();
            } else if (type2 == Type.STICK) {
                this.inputControlsView.invalidate();
            }
            if (this.currentPosition != null) {
                this.currentPosition = null;
            }
        }
        this.currentPointerId = -1;
        return true;
    }
}
