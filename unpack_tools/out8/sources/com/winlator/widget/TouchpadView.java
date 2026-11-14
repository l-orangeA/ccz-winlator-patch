package com.winlator.widget;

import android.R;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import com.winlator.core.AppUtils;
import com.winlator.math.Mathf;
import com.winlator.math.XForm;
import com.winlator.renderer.ViewTransformation;
import com.winlator.winhandler.WinHandler;
import com.winlator.xserver.Pointer;
import com.winlator.xserver.ScreenInfo;
import com.winlator.xserver.XServer;

/* loaded from: classes.dex */
public class TouchpadView extends View {
    private Finger fingerPointerButtonLeft;
    private Finger fingerPointerButtonRight;
    private final Finger[] fingers;
    private Runnable fourFingersTapCallback;
    private boolean moveCursorToTouchpoint;
    private byte numFingers;
    private boolean pointerButtonLeftEnabled;
    private boolean pointerButtonRightEnabled;
    private float scrollAccumY;
    private boolean scrolling;
    private float sensitivity;
    private final XServer xServer;
    private final float[] xform;

    public TouchpadView(Context context, XServer xServer) {
        super(context);
        this.fingers = new Finger[4];
        this.numFingers = (byte) 0;
        this.sensitivity = 1.0f;
        this.pointerButtonLeftEnabled = true;
        this.pointerButtonRightEnabled = true;
        this.moveCursorToTouchpoint = false;
        this.scrollAccumY = 0.0f;
        this.scrolling = false;
        this.xform = XForm.getInstance();
        this.xServer = xServer;
        setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        setBackground(createTransparentBackground());
        setClickable(true);
        setFocusable(true);
        setFocusableInTouchMode(false);
        int screenWidth = AppUtils.getScreenWidth();
        int screenHeight = AppUtils.getScreenHeight();
        ScreenInfo screenInfo = xServer.screenInfo;
        updateXform(screenWidth, screenHeight, screenInfo.width, screenInfo.height);
    }

    private static StateListDrawable createTransparentBackground() {
        StateListDrawable stateListDrawable = new StateListDrawable();
        ColorDrawable focusedDrawable = new ColorDrawable(0);
        ColorDrawable defaultDrawable = new ColorDrawable(0);
        stateListDrawable.addState(new int[]{R.attr.state_focused}, focusedDrawable);
        stateListDrawable.addState(new int[0], defaultDrawable);
        return stateListDrawable;
    }

    @Override // android.view.View
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        ScreenInfo screenInfo = this.xServer.screenInfo;
        updateXform(w, h, screenInfo.width, screenInfo.height);
    }

    private void updateXform(int outerWidth, int outerHeight, int innerWidth, int innerHeight) {
        ViewTransformation viewTransformation = new ViewTransformation();
        viewTransformation.update(outerWidth, outerHeight, innerWidth, innerHeight);
        float invAspect = 1.0f / viewTransformation.aspect;
        if (!this.xServer.getRenderer().isFullscreen()) {
            XForm.makeTranslation(this.xform, -viewTransformation.viewOffsetX, -viewTransformation.viewOffsetY);
            XForm.scale(this.xform, invAspect, invAspect);
        } else {
            XForm.makeScale(this.xform, invAspect, invAspect);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class Finger {
        private int lastX;
        private int lastY;
        private final int startX;
        private final int startY;
        private final long touchTime;
        private int x;
        private int y;

        public Finger(float x, float y) {
            float[] transformedPoint = XForm.transformPoint(TouchpadView.this.xform, x, y);
            int i = (int) transformedPoint[0];
            this.lastX = i;
            this.startX = i;
            this.x = i;
            int i2 = (int) transformedPoint[1];
            this.lastY = i2;
            this.startY = i2;
            this.y = i2;
            this.touchTime = System.currentTimeMillis();
        }

        public void update(float x, float y) {
            this.lastX = this.x;
            this.lastY = this.y;
            float[] transformedPoint = XForm.transformPoint(TouchpadView.this.xform, x, y);
            this.x = (int) transformedPoint[0];
            this.y = (int) transformedPoint[1];
        }

        /* JADX INFO: Access modifiers changed from: private */
        public int deltaX() {
            float dx = (this.x - this.lastX) * TouchpadView.this.sensitivity;
            if (Math.abs(dx) > 6.0f) {
                dx *= 1.25f;
            }
            return Mathf.roundPoint(dx);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public int deltaY() {
            float dy = (this.y - this.lastY) * TouchpadView.this.sensitivity;
            if (Math.abs(dy) > 6.0f) {
                dy *= 1.25f;
            }
            return Mathf.roundPoint(dy);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public boolean isTap() {
            return System.currentTimeMillis() - this.touchTime < 200 && travelDistance() < 10.0f;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public float travelDistance() {
            return (float) Math.hypot(this.x - this.startX, this.y - this.startY);
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent event) {
        int actionIndex = event.getActionIndex();
        int pointerId = event.getPointerId(actionIndex);
        int actionMasked = event.getActionMasked();
        if (pointerId >= 4) {
            return true;
        }
        switch (actionMasked) {
            case 0:
            case 5:
                if (event.isFromSource(8194)) {
                    return true;
                }
                this.scrollAccumY = 0.0f;
                this.scrolling = false;
                this.fingers[pointerId] = new Finger(event.getX(actionIndex), event.getY(actionIndex));
                this.numFingers = (byte) (this.numFingers + 1);
                return true;
            case 1:
            case 6:
                Finger[] fingerArr = this.fingers;
                if (fingerArr[pointerId] != null) {
                    fingerArr[pointerId].update(event.getX(actionIndex), event.getY(actionIndex));
                    handleFingerUp(this.fingers[pointerId]);
                    this.fingers[pointerId] = null;
                    this.numFingers = (byte) (this.numFingers - 1);
                }
                return true;
            case 2:
                if (event.isFromSource(8194)) {
                    float[] transformedPoint = XForm.transformPoint(this.xform, event.getX(), event.getY());
                    if (isEnabled()) {
                        this.xServer.injectPointerMove((int) transformedPoint[0], (int) transformedPoint[1]);
                    }
                } else {
                    for (byte i = 0; i < 4; i = (byte) (i + 1)) {
                        if (this.fingers[i] != null) {
                            int pointerIndex = event.findPointerIndex(i);
                            if (pointerIndex >= 0) {
                                this.fingers[i].update(event.getX(pointerIndex), event.getY(pointerIndex));
                                handleFingerMove(this.fingers[i]);
                            } else {
                                handleFingerUp(this.fingers[i]);
                                this.fingers[i] = null;
                                this.numFingers = (byte) (this.numFingers - 1);
                            }
                        }
                    }
                }
                return true;
            case 3:
                for (byte i2 = 0; i2 < 4; i2 = (byte) (i2 + 1)) {
                    this.fingers[i2] = null;
                }
                this.numFingers = (byte) 0;
                return true;
            case 4:
            default:
                return true;
        }
    }

    private void handleFingerUp(Finger finger1) {
        switch (this.numFingers) {
            case 1:
                if (finger1.isTap()) {
                    if (this.moveCursorToTouchpoint) {
                        this.xServer.injectPointerMove(finger1.x, finger1.y);
                    }
                    pressPointerButtonLeft(finger1);
                    break;
                }
                break;
            case 2:
                Finger finger2 = findSecondFinger(finger1);
                if (finger2 != null && finger1.isTap()) {
                    pressPointerButtonRight(finger1);
                    break;
                }
                break;
            case 4:
                if (this.fourFingersTapCallback != null) {
                    for (byte i = 0; i < 4; i = (byte) (i + 1)) {
                        Finger[] fingerArr = this.fingers;
                        if (fingerArr[i] != null && !fingerArr[i].isTap()) {
                            return;
                        }
                    }
                    this.fourFingersTapCallback.run();
                    break;
                }
                break;
        }
        releasePointerButtonLeft(finger1);
        releasePointerButtonRight(finger1);
    }

    private void handleFingerMove(Finger finger1) {
        byte b;
        if (isEnabled()) {
            boolean skipPointerMove = false;
            Finger finger2 = this.numFingers == 2 ? findSecondFinger(finger1) : null;
            if (finger2 != null) {
                ScreenInfo screenInfo = this.xServer.screenInfo;
                float resolutionScale = 1000.0f / Math.min((int) screenInfo.width, (int) screenInfo.height);
                float currDistance = ((float) Math.hypot(finger1.x - finger2.x, finger1.y - finger2.y)) * resolutionScale;
                if (currDistance < 350.0f) {
                    float f = this.scrollAccumY + (((finger1.y + finger2.y) * 0.5f) - ((finger1.lastY + finger2.lastY) * 0.5f));
                    this.scrollAccumY = f;
                    if (f < -100.0f) {
                        XServer xServer = this.xServer;
                        Pointer.Button button = Pointer.Button.BUTTON_SCROLL_DOWN;
                        xServer.injectPointerButtonPress(button);
                        this.xServer.injectPointerButtonRelease(button);
                        this.scrollAccumY = 0.0f;
                    } else if (f > 100.0f) {
                        XServer xServer2 = this.xServer;
                        Pointer.Button button2 = Pointer.Button.BUTTON_SCROLL_UP;
                        xServer2.injectPointerButtonPress(button2);
                        this.xServer.injectPointerButtonRelease(button2);
                        this.scrollAccumY = 0.0f;
                    }
                    this.scrolling = true;
                } else if (currDistance >= 350.0f && !this.xServer.pointer.isButtonPressed(Pointer.Button.BUTTON_LEFT) && finger2.travelDistance() < 10.0f) {
                    pressPointerButtonLeft(finger1);
                    skipPointerMove = true;
                }
            }
            if (!this.scrolling && (b = this.numFingers) <= 2 && !skipPointerMove) {
                if (!this.moveCursorToTouchpoint || b != 1) {
                    int dx = finger1.deltaX();
                    int dy = finger1.deltaY();
                    WinHandler winHandler = this.xServer.getWinHandler();
                    if (this.xServer.isRelativeMouseMovement()) {
                        winHandler.mouseEvent(1, dx, dy, 0);
                        return;
                    } else {
                        this.xServer.injectPointerMoveDelta(dx, dy);
                        return;
                    }
                }
                this.xServer.injectPointerMove(finger1.x, finger1.y);
            }
        }
    }

    private Finger findSecondFinger(Finger finger) {
        for (byte i = 0; i < 4; i = (byte) (i + 1)) {
            Finger[] fingerArr = this.fingers;
            if (fingerArr[i] != null && fingerArr[i] != finger) {
                return fingerArr[i];
            }
        }
        return null;
    }

    private void pressPointerButtonLeft(Finger finger) {
        if (isEnabled() && this.pointerButtonLeftEnabled) {
            Pointer pointer = this.xServer.pointer;
            Pointer.Button button = Pointer.Button.BUTTON_LEFT;
            if (!pointer.isButtonPressed(button)) {
                this.xServer.injectPointerButtonPress(button);
                this.fingerPointerButtonLeft = finger;
            }
        }
    }

    private void pressPointerButtonRight(Finger finger) {
        if (isEnabled() && this.pointerButtonRightEnabled) {
            Pointer pointer = this.xServer.pointer;
            Pointer.Button button = Pointer.Button.BUTTON_RIGHT;
            if (!pointer.isButtonPressed(button)) {
                this.xServer.injectPointerButtonPress(button);
                this.fingerPointerButtonRight = finger;
            }
        }
    }

    private void releasePointerButtonLeft(Finger finger) {
        if (isEnabled() && this.pointerButtonLeftEnabled && finger == this.fingerPointerButtonLeft && this.xServer.pointer.isButtonPressed(Pointer.Button.BUTTON_LEFT)) {
            postDelayed(new Runnable() { // from class: com.winlator.widget.TouchpadView$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    TouchpadView.this.lambda$releasePointerButtonLeft$0();
                }
            }, 30L);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$releasePointerButtonLeft$0() {
        this.xServer.injectPointerButtonRelease(Pointer.Button.BUTTON_LEFT);
        this.fingerPointerButtonLeft = null;
    }

    private void releasePointerButtonRight(Finger finger) {
        if (isEnabled() && this.pointerButtonRightEnabled && finger == this.fingerPointerButtonRight && this.xServer.pointer.isButtonPressed(Pointer.Button.BUTTON_RIGHT)) {
            postDelayed(new Runnable() { // from class: com.winlator.widget.TouchpadView$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    TouchpadView.this.lambda$releasePointerButtonRight$1();
                }
            }, 30L);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$releasePointerButtonRight$1() {
        this.xServer.injectPointerButtonRelease(Pointer.Button.BUTTON_RIGHT);
        this.fingerPointerButtonRight = null;
    }

    public void setSensitivity(float sensitivity) {
        this.sensitivity = sensitivity;
    }

    public void setPointerButtonLeftEnabled(boolean pointerButtonLeftEnabled) {
        this.pointerButtonLeftEnabled = pointerButtonLeftEnabled;
    }

    public void setPointerButtonRightEnabled(boolean pointerButtonRightEnabled) {
        this.pointerButtonRightEnabled = pointerButtonRightEnabled;
    }

    public void setFourFingersTapCallback(Runnable fourFingersTapCallback) {
        this.fourFingersTapCallback = fourFingersTapCallback;
    }

    public void setMoveCursorToTouchpoint(boolean moveCursorToTouchpoint) {
        this.moveCursorToTouchpoint = moveCursorToTouchpoint;
    }

    public boolean onExternalMouseEvent(MotionEvent event) {
        if (!isEnabled() || !event.isFromSource(8194)) {
            return false;
        }
        int actionButton = event.getActionButton();
        switch (event.getAction()) {
            case 7:
                float[] transformedPoint = XForm.transformPoint(this.xform, event.getX(), event.getY());
                this.xServer.injectPointerMove((int) transformedPoint[0], (int) transformedPoint[1]);
                return true;
            case 8:
                float scrollY = event.getAxisValue(9);
                if (scrollY <= -1.0f) {
                    XServer xServer = this.xServer;
                    Pointer.Button button = Pointer.Button.BUTTON_SCROLL_DOWN;
                    xServer.injectPointerButtonPress(button);
                    this.xServer.injectPointerButtonRelease(button);
                } else if (scrollY >= 1.0f) {
                    XServer xServer2 = this.xServer;
                    Pointer.Button button2 = Pointer.Button.BUTTON_SCROLL_UP;
                    xServer2.injectPointerButtonPress(button2);
                    this.xServer.injectPointerButtonRelease(button2);
                }
                return true;
            case 9:
            case 10:
            default:
                return false;
            case 11:
                if (actionButton == 1) {
                    this.xServer.injectPointerButtonPress(Pointer.Button.BUTTON_LEFT);
                } else if (actionButton == 2) {
                    this.xServer.injectPointerButtonPress(Pointer.Button.BUTTON_RIGHT);
                }
                return true;
            case 12:
                if (actionButton == 1) {
                    this.xServer.injectPointerButtonRelease(Pointer.Button.BUTTON_LEFT);
                } else if (actionButton == 2) {
                    this.xServer.injectPointerButtonRelease(Pointer.Button.BUTTON_RIGHT);
                }
                return true;
        }
    }

    public float[] computeDeltaPoint(float lastX, float lastY, float x, float y) {
        float[] result = {0.0f, 0.0f};
        XForm.transformPoint(this.xform, lastX, lastY, result);
        float lastX2 = result[0];
        float lastY2 = result[1];
        XForm.transformPoint(this.xform, x, y, result);
        float x2 = result[0];
        float y2 = result[1];
        result[0] = x2 - lastX2;
        result[1] = y2 - lastY2;
        return result;
    }
}
