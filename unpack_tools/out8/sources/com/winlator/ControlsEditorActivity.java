package com.winlator;

import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.winlatcn.R;
import com.winlator.core.AppUtils;
import com.winlator.core.FileUtils;
import com.winlator.core.UnitUtils;
import com.winlator.inputcontrols.Binding;
import com.winlator.inputcontrols.ControlElement;
import com.winlator.inputcontrols.ControlsProfile;
import com.winlator.inputcontrols.InputControlsManager;
import com.winlator.math.Mathf;
import com.winlator.widget.InputControlsView;
import com.winlator.widget.NumberPicker;
import com.winlator.widget.SeekBar;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/* loaded from: classes.dex */
public class ControlsEditorActivity extends AppCompatActivity implements View.OnClickListener {
    private InputControlsView inputControlsView;
    private ControlsProfile profile;
    private View toolbox;

    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        AppUtils.hideSystemUI(this);
        setContentView(R.layout.controls_editor_activity);
        InputControlsView inputControlsView = new InputControlsView(this);
        this.inputControlsView = inputControlsView;
        inputControlsView.setEditMode(true);
        this.inputControlsView.setOverlayOpacity(0.6f);
        this.profile = InputControlsManager.loadProfile(this, ControlsProfile.getProfileFile(this, getIntent().getIntExtra("profile_id", 0)));
        ((TextView) findViewById(R.id.TVProfileName)).setText(this.profile.getName());
        this.inputControlsView.setProfile(this.profile);
        FrameLayout container = (FrameLayout) findViewById(R.id.FLContainer);
        container.addView(this.inputControlsView, 0);
        container.findViewById(R.id.BTAddElement).setOnClickListener(this);
        container.findViewById(R.id.BTRemoveElement).setOnClickListener(this);
        container.findViewById(R.id.BTElementSettings).setOnClickListener(this);
        this.toolbox = container.findViewById(R.id.Toolbox);
        final PointF startPoint = new PointF();
        final boolean[] isActionDown = {false};
        container.findViewById(R.id.BTMove).setOnTouchListener(new View.OnTouchListener() { // from class: com.winlator.ControlsEditorActivity$$ExternalSyntheticLambda2
            @Override // android.view.View.OnTouchListener
            public final boolean onTouch(View view, MotionEvent motionEvent) {
                boolean lambda$onCreate$0;
                lambda$onCreate$0 = ControlsEditorActivity.this.lambda$onCreate$0(startPoint, isActionDown, view, motionEvent);
                return lambda$onCreate$0;
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:8:0x0040, code lost:
    
        return true;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public /* synthetic */ boolean lambda$onCreate$0(android.graphics.PointF r6, boolean[] r7, android.view.View r8, android.view.MotionEvent r9) {
        /*
            r5 = this;
            int r0 = r9.getAction()
            r1 = 1
            r2 = 0
            switch(r0) {
                case 0: goto L31;
                case 1: goto L2e;
                case 2: goto La;
                default: goto L9;
            }
        L9:
            goto L40
        La:
            boolean r0 = r7[r2]
            if (r0 == 0) goto L40
            android.view.View r0 = r5.toolbox
            float r0 = r0.getX()
            float r2 = r9.getX()
            float r3 = r6.x
            float r2 = r2 - r3
            float r0 = r0 + r2
            android.view.View r2 = r5.toolbox
            float r2 = r2.getY()
            float r3 = r9.getY()
            float r4 = r6.y
            float r3 = r3 - r4
            float r2 = r2 + r3
            r5.moveToolbox(r0, r2)
            goto L40
        L2e:
            r7[r2] = r2
            goto L40
        L31:
            float r0 = r9.getX()
            r6.x = r0
            float r0 = r9.getY()
            r6.y = r0
            r7[r2] = r1
        L40:
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.winlator.ControlsEditorActivity.lambda$onCreate$0(android.graphics.PointF, boolean[], android.view.View, android.view.MotionEvent):boolean");
    }

    private void moveToolbox(float x, float y) {
        int padding = (int) UnitUtils.dpToPx(8.0f);
        ViewGroup parent = (ViewGroup) this.toolbox.getParent();
        int width = this.toolbox.getWidth();
        int height = this.toolbox.getHeight();
        int parentWidth = parent.getWidth();
        int parentHeight = parent.getHeight();
        float x2 = Mathf.clamp(x, padding, (parentWidth - padding) - width);
        float y2 = Mathf.clamp(y, padding, (parentHeight - padding) - height);
        this.toolbox.setX(x2);
        this.toolbox.setY(y2);
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.BTAddElement /* 2131296262 */:
                if (!this.inputControlsView.addElement()) {
                    AppUtils.showToast(this, R.string.no_profile_selected);
                    return;
                }
                return;
            case R.id.BTElementSettings /* 2131296277 */:
                ControlElement selectedElement = this.inputControlsView.getSelectedElement();
                if (selectedElement != null) {
                    showControlElementSettings(v);
                    return;
                } else {
                    AppUtils.showToast(this, R.string.no_control_element_selected);
                    return;
                }
            case R.id.BTRemoveElement /* 2131296295 */:
                if (!this.inputControlsView.removeElement()) {
                    AppUtils.showToast(this, R.string.no_control_element_selected);
                    return;
                }
                return;
            default:
                return;
        }
    }

    private void showControlElementSettings(View anchorView) {
        final ControlElement element = this.inputControlsView.getSelectedElement();
        final View view = LayoutInflater.from(this).inflate(R.layout.control_element_settings, (ViewGroup) null);
        Runnable updateLayout = new Runnable() { // from class: com.winlator.ControlsEditorActivity$$ExternalSyntheticLambda9
            @Override // java.lang.Runnable
            public final void run() {
                ControlsEditorActivity.this.lambda$showControlElementSettings$1(element, view);
            }
        };
        loadTypeSpinner(element, (Spinner) view.findViewById(R.id.SType), updateLayout);
        loadShapeSpinner(element, (Spinner) view.findViewById(R.id.SShape));
        loadRangeSpinner(element, (Spinner) view.findViewById(R.id.SRange));
        RadioGroup rgOrientation = (RadioGroup) view.findViewById(R.id.RGOrientation);
        rgOrientation.check(element.getOrientation() == 1 ? R.id.RBVertical : R.id.RBHorizontal);
        rgOrientation.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() { // from class: com.winlator.ControlsEditorActivity$$ExternalSyntheticLambda5
            @Override // android.widget.RadioGroup.OnCheckedChangeListener
            public final void onCheckedChanged(RadioGroup radioGroup, int i) {
                ControlsEditorActivity.this.lambda$showControlElementSettings$2(element, radioGroup, i);
            }
        });
        NumberPicker npColumns = (NumberPicker) view.findViewById(R.id.NPColumns);
        npColumns.setValue(element.getBindingCount());
        npColumns.setOnValueChangeListener(new NumberPicker.OnValueChangeListener() { // from class: com.winlator.ControlsEditorActivity$$ExternalSyntheticLambda6
            @Override // com.winlator.widget.NumberPicker.OnValueChangeListener
            public final void onValueChange(NumberPicker numberPicker, int i) {
                ControlsEditorActivity.this.lambda$showControlElementSettings$3(element, numberPicker, i);
            }
        });
        SeekBar sbScale = (SeekBar) view.findViewById(R.id.SBScale);
        sbScale.setOnValueChangeListener(new SeekBar.OnValueChangeListener() { // from class: com.winlator.ControlsEditorActivity$$ExternalSyntheticLambda7
            @Override // com.winlator.widget.SeekBar.OnValueChangeListener
            public final void onValueChangeListener(SeekBar seekBar, float f) {
                ControlsEditorActivity.this.lambda$showControlElementSettings$4(element, seekBar, f);
            }
        });
        sbScale.setValue(element.getScale() * 100.0f);
        CheckBox cbToggleSwitch = (CheckBox) view.findViewById(R.id.CBToggleSwitch);
        cbToggleSwitch.setChecked(element.isToggleSwitch());
        cbToggleSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: com.winlator.ControlsEditorActivity$$ExternalSyntheticLambda3
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                ControlsEditorActivity.this.lambda$showControlElementSettings$5(element, compoundButton, z);
            }
        });
        final EditText etCustomText = (EditText) view.findViewById(R.id.ETCustomText);
        etCustomText.setText(element.getText());
        final LinearLayout llIconList = (LinearLayout) view.findViewById(R.id.LLIconList);
        loadIcons(llIconList, element.getIconId());
        updateLayout.run();
        PopupWindow popupWindow = AppUtils.showPopupWindow(anchorView, view, 340, 0);
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() { // from class: com.winlator.ControlsEditorActivity$$ExternalSyntheticLambda4
            @Override // android.widget.PopupWindow.OnDismissListener
            public final void onDismiss() {
                ControlsEditorActivity.this.lambda$showControlElementSettings$6(etCustomText, llIconList, element);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showControlElementSettings$1(ControlElement element, View view) {
        ControlElement.Type type = element.getType();
        view.findViewById(R.id.LLShape).setVisibility(8);
        view.findViewById(R.id.CBToggleSwitch).setVisibility(8);
        view.findViewById(R.id.LLCustomTextIcon).setVisibility(8);
        view.findViewById(R.id.LLRangeOptions).setVisibility(8);
        if (type == ControlElement.Type.BUTTON) {
            view.findViewById(R.id.LLShape).setVisibility(0);
            view.findViewById(R.id.CBToggleSwitch).setVisibility(0);
            view.findViewById(R.id.LLCustomTextIcon).setVisibility(0);
        } else if (type == ControlElement.Type.RANGE_BUTTON) {
            view.findViewById(R.id.LLRangeOptions).setVisibility(0);
        }
        loadBindingSpinners(element, view);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showControlElementSettings$2(ControlElement element, RadioGroup group, int checkedId) {
        element.setOrientation((byte) (checkedId == R.id.RBVertical ? 1 : 0));
        this.profile.save();
        this.inputControlsView.invalidate();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showControlElementSettings$3(ControlElement element, NumberPicker numberPicker, int value) {
        element.setBindingCount(value);
        this.profile.save();
        this.inputControlsView.invalidate();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showControlElementSettings$4(ControlElement element, SeekBar seekBar, float value) {
        element.setScale(value / 100.0f);
        this.profile.save();
        this.inputControlsView.invalidate();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showControlElementSettings$5(ControlElement element, CompoundButton buttonView, boolean isChecked) {
        element.setToggleSwitch(isChecked);
        this.profile.save();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showControlElementSettings$6(EditText etCustomText, LinearLayout llIconList, ControlElement element) {
        String text = etCustomText.getText().toString().trim();
        byte iconId = 0;
        int i = 0;
        while (true) {
            if (i >= llIconList.getChildCount()) {
                break;
            }
            View child = llIconList.getChildAt(i);
            if (!child.isSelected()) {
                i++;
            } else {
                iconId = ((Byte) child.getTag()).byteValue();
                break;
            }
        }
        element.setText(text);
        element.setIconId(iconId);
        this.profile.save();
        this.inputControlsView.invalidate();
    }

    private void loadTypeSpinner(final ControlElement element, Spinner spinner, final Runnable callback) {
        spinner.setAdapter((SpinnerAdapter) new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ControlElement.Type.names()));
        spinner.setSelection(element.getType().ordinal(), false);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // from class: com.winlator.ControlsEditorActivity.1
            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                element.setType(ControlElement.Type.values()[position]);
                ControlsEditorActivity.this.profile.save();
                callback.run();
                ControlsEditorActivity.this.inputControlsView.invalidate();
            }

            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadShapeSpinner(final ControlElement element, Spinner spinner) {
        spinner.setAdapter((SpinnerAdapter) new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ControlElement.Shape.names()));
        spinner.setSelection(element.getShape().ordinal(), false);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // from class: com.winlator.ControlsEditorActivity.2
            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                element.setShape(ControlElement.Shape.values()[position]);
                ControlsEditorActivity.this.profile.save();
                ControlsEditorActivity.this.inputControlsView.invalidate();
            }

            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadBindingSpinners(ControlElement element, View view) {
        LinearLayout container = (LinearLayout) view.findViewById(R.id.LLBindings);
        container.removeAllViews();
        ControlElement.Type type = element.getType();
        if (type == ControlElement.Type.BUTTON) {
            byte first = element.getFirstBindingIndex();
            byte count = 0;
            for (byte i = 0; i < element.getBindingCount(); i = (byte) (i + 1)) {
                if (i <= first || element.getBindingAt(i) != Binding.NONE) {
                    byte count2 = (byte) (count + 1);
                    loadBindingSpinner(element, container, i, count == 0 ? R.string.binding : 0);
                    count = count2;
                }
            }
            return;
        }
        if (type == ControlElement.Type.D_PAD || type == ControlElement.Type.STICK || type == ControlElement.Type.TRACKPAD) {
            loadBindingSpinner(element, container, 0, R.string.binding_up);
            loadBindingSpinner(element, container, 1, R.string.binding_right);
            loadBindingSpinner(element, container, 2, R.string.binding_down);
            loadBindingSpinner(element, container, 3, R.string.binding_left);
        }
    }

    private void loadBindingSpinner(final ControlElement element, final LinearLayout container, final int index, int titleResId) {
        View view = LayoutInflater.from(this).inflate(R.layout.binding_field, (ViewGroup) container, false);
        LinearLayout titleBar = (LinearLayout) view.findViewById(R.id.LLTitleBar);
        if (titleResId > 0) {
            titleBar.setVisibility(0);
            ((TextView) view.findViewById(R.id.TVTitle)).setText(titleResId);
        } else {
            titleBar.setVisibility(8);
        }
        final Spinner sBindingType = (Spinner) view.findViewById(R.id.SBindingType);
        final Spinner sBinding = (Spinner) view.findViewById(R.id.SBinding);
        if (element.getType() == ControlElement.Type.BUTTON) {
            ImageButton addButton = (ImageButton) view.findViewById(R.id.BTAdd);
            addButton.setVisibility(0);
            addButton.setOnClickListener(new View.OnClickListener() { // from class: com.winlator.ControlsEditorActivity$$ExternalSyntheticLambda1
                @Override // android.view.View.OnClickListener
                public final void onClick(View view2) {
                    ControlsEditorActivity.this.lambda$loadBindingSpinner$7(container, element, view2);
                }
            });
        }
        final Runnable update = new Runnable() { // from class: com.winlator.ControlsEditorActivity$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                ControlsEditorActivity.this.lambda$loadBindingSpinner$8(sBindingType, sBinding, element, index);
            }
        };
        sBindingType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // from class: com.winlator.ControlsEditorActivity.3
            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onItemSelected(AdapterView<?> parent, View view2, int position, long id) {
                update.run();
            }

            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        Binding selectedBinding = element.getBindingAt(index);
        if (selectedBinding.isKeyboard()) {
            sBindingType.setSelection(0, false);
        } else if (selectedBinding.isMouse()) {
            sBindingType.setSelection(1, false);
        } else if (selectedBinding.isGamepad()) {
            sBindingType.setSelection(2, false);
        }
        sBinding.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // from class: com.winlator.ControlsEditorActivity.4
            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onItemSelected(AdapterView<?> parent, View view2, int position, long id) {
                Binding binding = Binding.NONE;
                switch (sBindingType.getSelectedItemPosition()) {
                    case 0:
                        binding = Binding.keyboardBindingValues()[position];
                        break;
                    case 1:
                        binding = Binding.mouseBindingValues()[position];
                        break;
                    case 2:
                        binding = Binding.gamepadBindingValues()[position];
                        break;
                }
                if (binding != element.getBindingAt(index)) {
                    element.setBindingAt(index, binding);
                    ControlsEditorActivity.this.profile.save();
                    ControlsEditorActivity.this.inputControlsView.invalidate();
                }
            }

            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        update.run();
        container.addView(view);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$loadBindingSpinner$7(LinearLayout container, ControlElement element, View v) {
        int nextIndex = container.getChildCount();
        if (nextIndex < element.getBindingCount()) {
            loadBindingSpinner(element, container, nextIndex, 0);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$loadBindingSpinner$8(Spinner sBindingType, Spinner sBinding, ControlElement element, int index) {
        String[] bindingEntries = null;
        switch (sBindingType.getSelectedItemPosition()) {
            case 0:
                bindingEntries = Binding.keyboardBindingLabels();
                break;
            case 1:
                bindingEntries = Binding.mouseBindingLabels();
                break;
            case 2:
                bindingEntries = Binding.gamepadBindingLabels();
                break;
        }
        sBinding.setAdapter((SpinnerAdapter) new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, bindingEntries));
        AppUtils.setSpinnerSelectionFromValue(sBinding, element.getBindingAt(index).toString());
    }

    private void loadRangeSpinner(final ControlElement element, Spinner spinner) {
        spinner.setAdapter((SpinnerAdapter) new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ControlElement.Range.names()));
        spinner.setSelection(element.getRange().ordinal(), false);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // from class: com.winlator.ControlsEditorActivity.5
            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                element.setRange(ControlElement.Range.values()[position]);
                ControlsEditorActivity.this.profile.save();
                ControlsEditorActivity.this.inputControlsView.invalidate();
            }

            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadIcons(final LinearLayout parent, byte selectedId) {
        String str;
        String str2 = "inputcontrols/icons/";
        boolean z = false;
        byte[] iconIds = new byte[0];
        try {
            String[] filenames = getAssets().list("inputcontrols/icons/");
            iconIds = new byte[filenames.length];
            for (int i = 0; i < filenames.length; i++) {
                iconIds[i] = Byte.parseByte(FileUtils.getBasename(filenames[i]));
            }
        } catch (IOException e) {
        }
        Arrays.sort(iconIds);
        int size = (int) UnitUtils.dpToPx(40.0f);
        int margin = (int) UnitUtils.dpToPx(2.0f);
        int padding = (int) UnitUtils.dpToPx(4.0f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMargins(margin, 0, margin, 0);
        int length = iconIds.length;
        int i2 = 0;
        while (i2 < length) {
            byte id = iconIds[i2];
            final ImageView imageView = new ImageView(this);
            imageView.setLayoutParams(params);
            imageView.setPadding(padding, padding, padding, padding);
            imageView.setBackgroundResource(R.drawable.icon_background);
            imageView.setTag(Byte.valueOf(id));
            imageView.setSelected(id == selectedId ? true : z);
            imageView.setOnClickListener(new View.OnClickListener() { // from class: com.winlator.ControlsEditorActivity$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    ControlsEditorActivity.lambda$loadIcons$9(parent, imageView, view);
                }
            });
            try {
                InputStream is = getAssets().open(str2 + ((int) id) + ".png");
                try {
                    imageView.setImageBitmap(BitmapFactory.decodeStream(is));
                    if (is != null) {
                        is.close();
                    }
                    str = str2;
                } catch (Throwable th) {
                    if (is != null) {
                        try {
                            try {
                                is.close();
                                str = str2;
                            } catch (Throwable th2) {
                                str = str2;
                                th.addSuppressed(th2);
                            }
                        } catch (IOException e2) {
                        }
                    } else {
                        str = str2;
                    }
                    throw th;
                    break;
                }
            } catch (IOException e3) {
                str = str2;
            }
            parent.addView(imageView);
            i2++;
            str2 = str;
            z = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$loadIcons$9(LinearLayout parent, ImageView imageView, View v) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            parent.getChildAt(i).setSelected(false);
        }
        imageView.setSelected(true);
    }
}
