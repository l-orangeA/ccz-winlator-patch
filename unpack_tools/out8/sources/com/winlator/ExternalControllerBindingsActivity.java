package com.winlator;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.winlatcn.R;
import com.winlator.ExternalControllerBindingsActivity;
import com.winlator.core.AppUtils;
import com.winlator.inputcontrols.Binding;
import com.winlator.inputcontrols.ControlsProfile;
import com.winlator.inputcontrols.ExternalController;
import com.winlator.inputcontrols.ExternalControllerBinding;
import com.winlator.inputcontrols.GamepadState;
import com.winlator.inputcontrols.InputControlsManager;
import com.winlator.math.Mathf;

/* loaded from: classes.dex */
public class ExternalControllerBindingsActivity extends AppCompatActivity {
    private ControllerBindingsAdapter adapter;
    private ExternalController controller;
    private TextView emptyTextView;
    private ControlsProfile profile;
    private RecyclerView recyclerView;

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.external_controller_bindings_activity);
        Intent intent = getIntent();
        int profileId = intent.getIntExtra("profile_id", 0);
        this.profile = InputControlsManager.loadProfile(this, ControlsProfile.getProfileFile(this, profileId));
        String controllerId = intent.getStringExtra("controller_id");
        ExternalController controller = this.profile.getController(controllerId);
        this.controller = controller;
        if (controller == null) {
            this.controller = this.profile.addController(controllerId);
            this.profile.save();
        }
        Toolbar toolbar = (Toolbar) findViewById(R.id.Toolbar);
        toolbar.setTitle(this.controller.getName());
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.icon_action_bar_back);
        this.emptyTextView = (TextView) findViewById(R.id.TVEmptyText);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.RecyclerView);
        this.recyclerView = recyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        this.recyclerView.addItemDecoration(new DividerItemDecoration(this, 1));
        RecyclerView recyclerView2 = this.recyclerView;
        ControllerBindingsAdapter controllerBindingsAdapter = new ControllerBindingsAdapter();
        this.adapter = controllerBindingsAdapter;
        recyclerView2.setAdapter(controllerBindingsAdapter);
        updateEmptyTextView();
    }

    private void updateControllerBinding(int keyCode, Binding binding) {
        int position;
        if (keyCode == 0) {
            return;
        }
        ExternalControllerBinding controllerBinding = this.controller.getControllerBinding(keyCode);
        if (controllerBinding == null) {
            ExternalControllerBinding controllerBinding2 = new ExternalControllerBinding();
            controllerBinding2.setKeyCode(keyCode);
            controllerBinding2.setBinding(binding);
            this.controller.addControllerBinding(controllerBinding2);
            this.profile.save();
            this.adapter.notifyDataSetChanged();
            updateEmptyTextView();
            position = this.controller.getPosition(controllerBinding2);
        } else {
            position = this.controller.getPosition(controllerBinding);
            animateItemView(position);
        }
        this.recyclerView.scrollToPosition(position);
    }

    private void processJoystickInput() {
        int keyCode = 0;
        Binding binding = Binding.NONE;
        int[] axes = {0, 1, 11, 14, 15, 16};
        GamepadState gamepadState = this.controller.state;
        float[] values = {gamepadState.thumbLX, gamepadState.thumbLY, gamepadState.thumbRX, gamepadState.thumbRY, gamepadState.getDPadX(), this.controller.state.getDPadY()};
        int i = 0;
        while (true) {
            if (i >= axes.length) {
                break;
            }
            byte sign = Mathf.sign(values[i]);
            if (sign == 0) {
                i++;
            } else {
                if (axes[i] == 0 || axes[i] == 11) {
                    binding = sign > 0 ? Binding.MOUSE_MOVE_RIGHT : Binding.MOUSE_MOVE_LEFT;
                } else if (axes[i] == 1 || axes[i] == 14) {
                    binding = sign > 0 ? Binding.MOUSE_MOVE_DOWN : Binding.MOUSE_MOVE_UP;
                } else if (axes[i] == 15) {
                    binding = sign > 0 ? Binding.KEY_D : Binding.KEY_A;
                } else if (axes[i] == 16) {
                    binding = sign > 0 ? Binding.KEY_S : Binding.KEY_W;
                }
                keyCode = ExternalControllerBinding.getKeyCodeForAxis(axes[i], sign);
            }
        }
        updateControllerBinding(keyCode, binding);
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if (event.getDeviceId() == this.controller.getDeviceId() && this.controller.updateStateFromMotionEvent(event)) {
            if (this.controller.state.isPressed(10)) {
                updateControllerBinding(androidx.constraintlayout.widget.R$styleable.Constraint_pathMotionArc, Binding.NONE);
            }
            if (this.controller.state.isPressed(11)) {
                updateControllerBinding(105, Binding.NONE);
            }
            processJoystickInput();
            return true;
        }
        return super.dispatchGenericMotionEvent(event);
    }

    @Override // androidx.appcompat.app.AppCompatActivity, androidx.core.app.ComponentActivity, android.app.Activity, android.view.Window.Callback
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getDeviceId() == this.controller.getDeviceId() && event.getRepeatCount() == 0) {
            if (event.getAction() == 0) {
                updateControllerBinding(event.getKeyCode(), Binding.NONE);
                return true;
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override // android.app.Activity
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        finish();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ControllerBindingsAdapter extends RecyclerView.Adapter<ViewHolder> {

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public class ViewHolder extends RecyclerView.ViewHolder {
            private final Spinner binding;
            private final Spinner bindingType;
            private final ImageButton removeButton;
            private final TextView title;

            private ViewHolder(View view) {
                super(view);
                this.title = (TextView) view.findViewById(R.id.TVTitle);
                this.bindingType = (Spinner) view.findViewById(R.id.SBindingType);
                this.binding = (Spinner) view.findViewById(R.id.SBinding);
                this.removeButton = (ImageButton) view.findViewById(R.id.BTRemove);
            }
        }

        private ControllerBindingsAdapter() {
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public final ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.external_controller_binding_list_item, parent, false));
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public void onBindViewHolder(ViewHolder holder, int position) {
            final ExternalControllerBinding item = ExternalControllerBindingsActivity.this.controller.getControllerBindingAt(position);
            holder.title.setText(item.toString());
            loadBindingSpinner(holder, item);
            holder.removeButton.setOnClickListener(new View.OnClickListener() { // from class: com.winlator.ExternalControllerBindingsActivity$ControllerBindingsAdapter$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    ExternalControllerBindingsActivity.ControllerBindingsAdapter.this.lambda$onBindViewHolder$0(item, view);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onBindViewHolder$0(ExternalControllerBinding item, View view) {
            ExternalControllerBindingsActivity.this.controller.removeControllerBinding(item);
            ExternalControllerBindingsActivity.this.profile.save();
            notifyDataSetChanged();
            ExternalControllerBindingsActivity.this.updateEmptyTextView();
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public final int getItemCount() {
            return ExternalControllerBindingsActivity.this.controller.getControllerBindingCount();
        }

        private void loadBindingSpinner(final ViewHolder holder, final ExternalControllerBinding item) {
            final Context $this = ExternalControllerBindingsActivity.this;
            final Runnable update = new Runnable() { // from class: com.winlator.ExternalControllerBindingsActivity$ControllerBindingsAdapter$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    ExternalControllerBindingsActivity.ControllerBindingsAdapter.lambda$loadBindingSpinner$1(ExternalControllerBindingsActivity.ControllerBindingsAdapter.ViewHolder.this, $this, item);
                }
            };
            holder.bindingType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // from class: com.winlator.ExternalControllerBindingsActivity.ControllerBindingsAdapter.1
                @Override // android.widget.AdapterView.OnItemSelectedListener
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    update.run();
                }

                @Override // android.widget.AdapterView.OnItemSelectedListener
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
            Binding selectedBinding = item.getBinding();
            if (selectedBinding.isKeyboard()) {
                holder.bindingType.setSelection(0, false);
            } else if (selectedBinding.isMouse()) {
                holder.bindingType.setSelection(1, false);
            } else if (selectedBinding.isGamepad()) {
                holder.bindingType.setSelection(2, false);
            }
            holder.binding.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // from class: com.winlator.ExternalControllerBindingsActivity.ControllerBindingsAdapter.2
                @Override // android.widget.AdapterView.OnItemSelectedListener
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Binding binding = Binding.NONE;
                    switch (holder.bindingType.getSelectedItemPosition()) {
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
                    if (binding != item.getBinding()) {
                        item.setBinding(binding);
                        ExternalControllerBindingsActivity.this.profile.save();
                    }
                }

                @Override // android.widget.AdapterView.OnItemSelectedListener
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
            update.run();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static /* synthetic */ void lambda$loadBindingSpinner$1(ViewHolder holder, Context $this, ExternalControllerBinding item) {
            String[] bindingEntries = null;
            switch (holder.bindingType.getSelectedItemPosition()) {
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
            holder.binding.setAdapter((SpinnerAdapter) new ArrayAdapter($this, android.R.layout.simple_spinner_dropdown_item, bindingEntries));
            AppUtils.setSpinnerSelectionFromValue(holder.binding, item.getBinding().toString());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateEmptyTextView() {
        this.emptyTextView.setVisibility(this.adapter.getItemCount() == 0 ? 0 : 8);
    }

    private void animateItemView(int position) {
        final ControllerBindingsAdapter.ViewHolder holder = (ControllerBindingsAdapter.ViewHolder) this.recyclerView.findViewHolderForAdapterPosition(position);
        if (holder != null) {
            final int color = ContextCompat.getColor(this, R.color.colorAccent);
            ValueAnimator animator = ValueAnimator.ofFloat(0.4f, 0.0f);
            animator.setDuration(200L);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.winlator.ExternalControllerBindingsActivity$$ExternalSyntheticLambda0
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    ExternalControllerBindingsActivity.lambda$animateItemView$0(ExternalControllerBindingsActivity.ControllerBindingsAdapter.ViewHolder.this, color, valueAnimator);
                }
            });
            animator.start();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$animateItemView$0(ControllerBindingsAdapter.ViewHolder holder, int color, ValueAnimator animation) {
        float alpha = ((Float) animation.getAnimatedValue()).floatValue();
        holder.itemView.setBackgroundColor(Color.argb((int) (255.0f * alpha), Color.red(color), Color.green(color), Color.blue(color)));
    }
}
