package com.winlator.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import androidx.appcompat.R$styleable;
import com.winlator.inputcontrols.Binding;
import com.winlator.inputcontrols.ControlElement;
import com.winlator.inputcontrols.ControlsProfile;
import com.winlator.inputcontrols.ExternalController;
import com.winlator.inputcontrols.ExternalControllerBinding;
import com.winlator.inputcontrols.GamepadState;
import com.winlator.math.Mathf;
import com.winlator.winhandler.WinHandler;
import com.winlator.xserver.Pointer;
import com.winlator.xserver.XServer;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

/* loaded from: classes.dex */
public class InputControlsView extends View {
    private final Point cursor;
    private ColorFilter darkColorFilter;
    private boolean editMode;
    private final Bitmap[] icons;
    private ColorFilter lightColorFilter;
    private final PointF mouseMoveOffset;
    private Timer mouseMoveTimer;
    private boolean moveCursor;
    private boolean moveElement;
    private float offsetX;
    private float offsetY;
    private float overlayOpacity;
    private final Paint paint;
    private ControlsProfile profile;
    private boolean readyToDraw;
    private ControlElement selectedElement;
    private boolean showTouchscreenControls;
    private int snappingSize;
    private float startX;
    private float startY;
    private TouchpadView touchpadView;
    private XServer xServer;

    public InputControlsView(Context context) {
        super(context);
        this.editMode = false;
        this.paint = new Paint(1);
        this.cursor = new Point();
        this.readyToDraw = false;
        this.moveCursor = false;
        this.moveElement = false;
        this.overlayOpacity = 0.4f;
        this.icons = new Bitmap[17];
        this.mouseMoveOffset = new PointF();
        this.showTouchscreenControls = true;
        setClickable(true);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setBackgroundColor(0);
        setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    public void setOverlayOpacity(float overlayOpacity) {
        this.overlayOpacity = overlayOpacity;
    }

    public int getSnappingSize() {
        return this.snappingSize;
    }

    @Override // android.view.View
    protected synchronized void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        if (width != 0 && height != 0) {
            this.snappingSize = width / 100;
            this.readyToDraw = true;
            if (this.editMode) {
                drawGrid(canvas);
                drawCursor(canvas);
            }
            ControlsProfile controlsProfile = this.profile;
            if (controlsProfile != null) {
                if (!controlsProfile.isElementsLoaded()) {
                    this.profile.loadElements(this);
                }
                if (this.showTouchscreenControls) {
                    for (ControlElement element : this.profile.getElements()) {
                        element.draw(canvas);
                    }
                }
            }
            super.onDraw(canvas);
            return;
        }
        this.readyToDraw = false;
    }

    private void drawGrid(Canvas canvas) {
        this.paint.setStyle(Paint.Style.FILL);
        this.paint.setStrokeWidth(this.snappingSize * 0.0625f);
        this.paint.setColor(-16777216);
        canvas.drawColor(-16777216);
        this.paint.setAntiAlias(false);
        this.paint.setColor(-13619152);
        int width = getMaxWidth();
        int height = getMaxHeight();
        int i = 0;
        while (i < width) {
            canvas.drawLine(i, 0.0f, i, height, this.paint);
            canvas.drawLine(0.0f, i, width, i, this.paint);
            i += this.snappingSize;
        }
        float cx = Mathf.roundTo(width * 0.5f, this.snappingSize);
        float cy = Mathf.roundTo(height * 0.5f, this.snappingSize);
        this.paint.setColor(-12434878);
        int i2 = 0;
        while (i2 < width) {
            canvas.drawLine(cx, i2, cx, this.snappingSize + i2, this.paint);
            canvas.drawLine(i2, cy, this.snappingSize + i2, cy, this.paint);
            i2 += this.snappingSize * 2;
        }
        this.paint.setAntiAlias(true);
    }

    private void drawCursor(Canvas canvas) {
        this.paint.setStyle(Paint.Style.FILL);
        this.paint.setStrokeWidth(this.snappingSize * 0.0625f);
        this.paint.setColor(-3790808);
        this.paint.setAntiAlias(false);
        canvas.drawLine(0.0f, this.cursor.y, getMaxWidth(), this.cursor.y, this.paint);
        int i = this.cursor.x;
        canvas.drawLine(i, 0.0f, i, getMaxHeight(), this.paint);
        this.paint.setAntiAlias(true);
    }

    public synchronized boolean addElement() {
        if (this.editMode && this.profile != null) {
            ControlElement element = new ControlElement(this);
            element.setX(this.cursor.x);
            element.setY(this.cursor.y);
            this.profile.addElement(element);
            this.profile.save();
            selectElement(element);
            return true;
        }
        return false;
    }

    public synchronized boolean removeElement() {
        ControlElement controlElement;
        ControlsProfile controlsProfile;
        if (this.editMode && (controlElement = this.selectedElement) != null && (controlsProfile = this.profile) != null) {
            controlsProfile.removeElement(controlElement);
            this.selectedElement = null;
            this.profile.save();
            invalidate();
            return true;
        }
        return false;
    }

    public ControlElement getSelectedElement() {
        return this.selectedElement;
    }

    private synchronized void deselectAllElements() {
        this.selectedElement = null;
        ControlsProfile controlsProfile = this.profile;
        if (controlsProfile != null) {
            for (ControlElement element : controlsProfile.getElements()) {
                element.setSelected(false);
            }
        }
    }

    private void selectElement(ControlElement element) {
        deselectAllElements();
        if (element != null) {
            this.selectedElement = element;
            element.setSelected(true);
        }
        invalidate();
    }

    public synchronized ControlsProfile getProfile() {
        return this.profile;
    }

    public synchronized void setProfile(ControlsProfile profile) {
        if (profile != null) {
            this.profile = profile;
            deselectAllElements();
        } else {
            this.profile = null;
        }
    }

    public boolean isShowTouchscreenControls() {
        return this.showTouchscreenControls;
    }

    public void setShowTouchscreenControls(boolean showTouchscreenControls) {
        this.showTouchscreenControls = showTouchscreenControls;
    }

    public int getLightColor() {
        return Color.argb((int) (this.overlayOpacity * 255.0f), 255, 255, 255);
    }

    public int getDarkColor() {
        return Color.argb((int) (this.overlayOpacity * 255.0f), 0, 0, 0);
    }

    public int getHighlightColor() {
        return Color.argb((int) (this.overlayOpacity * 255.0f), 2, R$styleable.AppCompatTheme_windowFixedHeightMinor, 189);
    }

    private synchronized ControlElement intersectElement(float x, float y) {
        ControlsProfile controlsProfile = this.profile;
        if (controlsProfile != null) {
            for (ControlElement element : controlsProfile.getElements()) {
                if (element.containsPoint(x, y)) {
                    return element;
                }
            }
        }
        return null;
    }

    public Paint getPaint() {
        return this.paint;
    }

    public ColorFilter getLightColorFilter() {
        if (this.lightColorFilter == null) {
            this.lightColorFilter = new PorterDuffColorFilter(-1, PorterDuff.Mode.SRC_IN);
        }
        return this.lightColorFilter;
    }

    public ColorFilter getDarkColorFilter() {
        if (this.darkColorFilter == null) {
            this.darkColorFilter = new PorterDuffColorFilter(-16777216, PorterDuff.Mode.SRC_IN);
        }
        return this.darkColorFilter;
    }

    public TouchpadView getTouchpadView() {
        return this.touchpadView;
    }

    public void setTouchpadView(TouchpadView touchpadView) {
        this.touchpadView = touchpadView;
    }

    public XServer getXServer() {
        return this.xServer;
    }

    public void setXServer(XServer xServer) {
        this.xServer = xServer;
        createMouseMoveTimer();
    }

    public int getMaxWidth() {
        return (int) Mathf.roundTo(getWidth(), this.snappingSize);
    }

    public int getMaxHeight() {
        return (int) Mathf.roundTo(getHeight(), this.snappingSize);
    }

    private void createMouseMoveTimer() {
        ControlsProfile controlsProfile = this.profile;
        if (controlsProfile != null && this.mouseMoveTimer == null) {
            final float cursorSpeed = controlsProfile.getCursorSpeed();
            Timer timer = new Timer();
            this.mouseMoveTimer = timer;
            timer.schedule(new TimerTask() { // from class: com.winlator.widget.InputControlsView.1
                @Override // java.util.TimerTask, java.lang.Runnable
                public void run() {
                    InputControlsView.this.xServer.injectPointerMoveDelta((int) (InputControlsView.this.mouseMoveOffset.x * 10.0f * cursorSpeed), (int) (InputControlsView.this.mouseMoveOffset.y * 10.0f * cursorSpeed));
                }
            }, 0L, 16L);
        }
    }

    private void processJoystickInput(ExternalController controller) {
        int[] axes = {0, 1, 11, 14, 15, 16};
        GamepadState gamepadState = controller.state;
        float[] values = {gamepadState.thumbLX, gamepadState.thumbLY, gamepadState.thumbRX, gamepadState.thumbRY, gamepadState.getDPadX(), controller.state.getDPadY()};
        for (byte i = 0; i < axes.length; i = (byte) (i + 1)) {
            if (Math.abs(values[i]) > 0.15f) {
                ExternalControllerBinding controllerBinding = controller.getControllerBinding(ExternalControllerBinding.getKeyCodeForAxis(axes[i], Mathf.sign(values[i])));
                if (controllerBinding != null) {
                    handleInputEvent(controllerBinding.getBinding(), true, values[i]);
                }
            } else {
                ExternalControllerBinding controllerBinding2 = controller.getControllerBinding(ExternalControllerBinding.getKeyCodeForAxis(axes[i], (byte) 1));
                if (controllerBinding2 != null) {
                    handleInputEvent(controllerBinding2.getBinding(), false, values[i]);
                }
                ExternalControllerBinding controllerBinding3 = controller.getControllerBinding(ExternalControllerBinding.getKeyCodeForAxis(axes[i], (byte) -1));
                if (controllerBinding3 != null) {
                    handleInputEvent(controllerBinding3.getBinding(), false, values[i]);
                }
            }
        }
    }

    @Override // android.view.View
    public boolean onGenericMotionEvent(MotionEvent event) {
        ControlsProfile controlsProfile;
        ExternalController controller;
        if (!this.editMode && (controlsProfile = this.profile) != null && (controller = controlsProfile.getController(event.getDeviceId())) != null && controller.updateStateFromMotionEvent(event)) {
            ExternalControllerBinding controllerBinding = controller.getControllerBinding(androidx.constraintlayout.widget.R$styleable.Constraint_pathMotionArc);
            if (controllerBinding != null) {
                handleInputEvent(controllerBinding.getBinding(), controller.state.isPressed(10));
            }
            ExternalControllerBinding controllerBinding2 = controller.getControllerBinding(105);
            if (controllerBinding2 != null) {
                handleInputEvent(controllerBinding2.getBinding(), controller.state.isPressed(11));
            }
            processJoystickInput(controller);
            return true;
        }
        return super.onGenericMotionEvent(event);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent event) {
        ControlsProfile controlsProfile;
        if (this.editMode && this.readyToDraw) {
            switch (event.getAction()) {
                case 0:
                    this.startX = event.getX();
                    float y = event.getY();
                    this.startY = y;
                    ControlElement element = intersectElement(this.startX, y);
                    this.moveCursor = true;
                    this.moveElement = false;
                    if (element != null) {
                        this.offsetX = this.startX - element.getX();
                        this.offsetY = this.startY - element.getY();
                        this.moveCursor = false;
                    }
                    selectElement(element);
                    break;
                case 1:
                    if (this.selectedElement != null && (controlsProfile = this.profile) != null && this.moveElement) {
                        controlsProfile.save();
                    }
                    if (this.moveCursor) {
                        this.cursor.set((int) Mathf.roundTo(event.getX(), this.snappingSize), (int) Mathf.roundTo(event.getY(), this.snappingSize));
                    }
                    invalidate();
                    break;
                case 2:
                    if (this.selectedElement != null) {
                        float dx = Math.abs(event.getX() - this.startX);
                        float dy = Math.abs(event.getY() - this.startY);
                        if (dx >= 10.0f || dy >= 10.0f) {
                            this.moveElement = true;
                        }
                        if (this.moveElement) {
                            this.selectedElement.setX((int) Mathf.roundTo(event.getX() - this.offsetX, this.snappingSize));
                            this.selectedElement.setY((int) Mathf.roundTo(event.getY() - this.offsetY, this.snappingSize));
                            invalidate();
                            break;
                        }
                    }
                    break;
            }
        }
        if (!this.editMode && this.profile != null) {
            int actionIndex = event.getActionIndex();
            int pointerId = event.getPointerId(actionIndex);
            int actionMasked = event.getActionMasked();
            boolean handled = false;
            switch (actionMasked) {
                case 0:
                case 5:
                    float x = event.getX(actionIndex);
                    float y2 = event.getY(actionIndex);
                    this.touchpadView.setPointerButtonLeftEnabled(true);
                    for (ControlElement element2 : this.profile.getElements()) {
                        if (element2.handleTouchDown(pointerId, x, y2)) {
                            handled = true;
                        }
                        if (element2.getBindingAt(0) == Binding.MOUSE_LEFT_BUTTON) {
                            this.touchpadView.setPointerButtonLeftEnabled(false);
                        }
                    }
                    if (!handled) {
                        this.touchpadView.onTouchEvent(event);
                        break;
                    }
                    break;
                case 1:
                case 3:
                case 6:
                    Iterator<ControlElement> it = this.profile.getElements().iterator();
                    while (it.hasNext()) {
                        if (it.next().handleTouchUp(pointerId)) {
                            handled = true;
                        }
                    }
                    if (!handled) {
                        this.touchpadView.onTouchEvent(event);
                        break;
                    }
                    break;
                case 2:
                    byte count = (byte) event.getPointerCount();
                    for (byte i = 0; i < count; i = (byte) (i + 1)) {
                        float x2 = event.getX(i);
                        float y3 = event.getY(i);
                        boolean handled2 = false;
                        Iterator<ControlElement> it2 = this.profile.getElements().iterator();
                        while (it2.hasNext()) {
                            if (it2.next().handleTouchMove(i, x2, y3)) {
                                handled2 = true;
                            }
                        }
                        if (!handled2) {
                            this.touchpadView.onTouchEvent(event);
                        }
                    }
                    break;
            }
        }
        return true;
    }

    public boolean onKeyEvent(KeyEvent event) {
        ExternalController controller;
        ExternalControllerBinding controllerBinding;
        if (this.profile == null || event.getRepeatCount() != 0 || (controller = this.profile.getController(event.getDeviceId())) == null || (controllerBinding = controller.getControllerBinding(event.getKeyCode())) == null) {
            return false;
        }
        int action = event.getAction();
        if (action == 0) {
            handleInputEvent(controllerBinding.getBinding(), true);
        } else if (action == 1) {
            handleInputEvent(controllerBinding.getBinding(), false);
        }
        return true;
    }

    public void handleInputEvent(Binding[] bindings, boolean isActionDown) {
        for (Binding binding : bindings) {
            if (binding != Binding.NONE) {
                handleInputEvent(binding, isActionDown, 0.0f);
            }
        }
    }

    public void handleInputEvent(Binding binding, boolean isActionDown) {
        handleInputEvent(binding, isActionDown, 0.0f);
    }

    public void handleInputEvent(Binding binding, boolean isActionDown, float offset) {
        if (binding.isGamepad()) {
            XServer xServer = this.xServer;
            WinHandler winHandler = xServer != null ? xServer.getWinHandler() : null;
            GamepadState state = this.profile.getGamepadState();
            int buttonIdx = binding.ordinal() - Binding.GAMEPAD_BUTTON_A.ordinal();
            if (buttonIdx <= 11) {
                state.setPressed(buttonIdx, isActionDown);
            } else if (binding == Binding.GAMEPAD_LEFT_THUMB_UP || binding == Binding.GAMEPAD_LEFT_THUMB_DOWN) {
                state.thumbLY = isActionDown ? offset : 0.0f;
            } else if (binding == Binding.GAMEPAD_LEFT_THUMB_LEFT || binding == Binding.GAMEPAD_LEFT_THUMB_RIGHT) {
                state.thumbLX = isActionDown ? offset : 0.0f;
            } else if (binding == Binding.GAMEPAD_RIGHT_THUMB_UP || binding == Binding.GAMEPAD_RIGHT_THUMB_DOWN) {
                state.thumbRY = isActionDown ? offset : 0.0f;
            } else if (binding == Binding.GAMEPAD_RIGHT_THUMB_LEFT || binding == Binding.GAMEPAD_RIGHT_THUMB_RIGHT) {
                state.thumbRX = isActionDown ? offset : 0.0f;
            } else {
                Binding binding2 = Binding.GAMEPAD_DPAD_UP;
                if (binding == binding2 || binding == Binding.GAMEPAD_DPAD_RIGHT || binding == Binding.GAMEPAD_DPAD_DOWN || binding == Binding.GAMEPAD_DPAD_LEFT) {
                    state.dpad[binding.ordinal() - binding2.ordinal()] = isActionDown;
                }
            }
            if (winHandler != null) {
                ExternalController controller = winHandler.getCurrentController();
                if (controller != null) {
                    controller.state.copy(state);
                }
                winHandler.sendGamepadState();
                return;
            }
            return;
        }
        Binding binding3 = Binding.MOUSE_MOVE_LEFT;
        if (binding == binding3 || binding == Binding.MOUSE_MOVE_RIGHT) {
            PointF pointF = this.mouseMoveOffset;
            if (isActionDown) {
                if (offset != 0.0f) {
                    r1 = offset;
                } else {
                    r1 = binding != binding3 ? 1 : -1;
                }
            }
            pointF.x = r1;
            if (isActionDown) {
                createMouseMoveTimer();
                return;
            }
            return;
        }
        if (binding == Binding.MOUSE_MOVE_DOWN || binding == Binding.MOUSE_MOVE_UP) {
            PointF pointF2 = this.mouseMoveOffset;
            if (isActionDown) {
                if (offset != 0.0f) {
                    r1 = offset;
                } else {
                    r1 = binding != Binding.MOUSE_MOVE_UP ? 1 : -1;
                }
            }
            pointF2.y = r1;
            if (isActionDown) {
                createMouseMoveTimer();
                return;
            }
            return;
        }
        Pointer.Button pointerButton = binding.getPointerButton();
        if (isActionDown) {
            if (pointerButton != null) {
                this.xServer.injectPointerButtonPress(pointerButton);
                return;
            } else {
                this.xServer.injectKeyPress(binding.keycode);
                return;
            }
        }
        if (pointerButton != null) {
            this.xServer.injectPointerButtonRelease(pointerButton);
        } else {
            this.xServer.injectKeyRelease(binding.keycode);
        }
    }

    public Bitmap getIcon(byte id) {
        if (this.icons[id] == null) {
            Context context = getContext();
            try {
                InputStream is = context.getAssets().open("inputcontrols/icons/" + ((int) id) + ".png");
                try {
                    this.icons[id] = BitmapFactory.decodeStream(is);
                    if (is != null) {
                        is.close();
                    }
                } finally {
                }
            } catch (IOException e) {
            }
        }
        return this.icons[id];
    }
}
