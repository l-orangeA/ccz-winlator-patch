package com.winlator;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import com.winlatcn.R;
import com.winlator.contentdialog.ContentDialog;
import com.winlator.core.AppUtils;
import com.winlator.core.Callback;
import com.winlator.core.FileUtils;
import com.winlator.core.HttpUtils;
import com.winlator.inputcontrols.ControlsProfile;
import com.winlator.inputcontrols.ExternalController;
import com.winlator.inputcontrols.InputControlsManager;
import com.winlator.widget.SeekBar;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONException;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class InputControlsFragment extends Fragment {
    private ControlsProfile currentProfile;
    private Callback<ControlsProfile> importProfileCallback;
    private InputControlsManager manager;
    private final int selectedProfileId;
    private Runnable updateLayout;

    public InputControlsFragment(int selectedProfileId) {
        this.selectedProfileId = selectedProfileId;
    }

    @Override // androidx.fragment.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
        this.manager = new InputControlsManager(getContext());
    }

    @Override // androidx.fragment.app.Fragment
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.input_controls);
    }

    @Override // androidx.fragment.app.Fragment
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 2 && resultCode == -1) {
            try {
                ControlsProfile importedProfile = this.manager.importProfile(new JSONObject(FileUtils.readString(getContext(), data.getData())));
                Callback<ControlsProfile> callback = this.importProfileCallback;
                if (callback != null) {
                    callback.call(importedProfile);
                }
            } catch (Exception e) {
                AppUtils.showToast(getContext(), R.string.unable_to_import_profile);
            }
            this.importProfileCallback = null;
        }
    }

    @Override // androidx.fragment.app.Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.input_controls_fragment, container, false);
        final Context context = getContext();
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int i = this.selectedProfileId;
        this.currentProfile = i > 0 ? this.manager.getProfile(i) : null;
        final Spinner sProfile = (Spinner) view.findViewById(R.id.SProfile);
        loadProfileSpinner(sProfile);
        final SeekBar sbCursorSpeed = (SeekBar) view.findViewById(R.id.SBCursorSpeed);
        sbCursorSpeed.setOnValueChangeListener(new SeekBar.OnValueChangeListener() { // from class: com.winlator.InputControlsFragment$$ExternalSyntheticLambda18
            @Override // com.winlator.widget.SeekBar.OnValueChangeListener
            public final void onValueChangeListener(SeekBar seekBar, float f) {
                InputControlsFragment.this.lambda$onCreateView$0(seekBar, f);
            }
        });
        final CheckBox cbDisableMouseInput = (CheckBox) view.findViewById(R.id.CBDisableMouseInput);
        cbDisableMouseInput.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: com.winlator.InputControlsFragment$$ExternalSyntheticLambda9
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                InputControlsFragment.this.lambda$onCreateView$1(compoundButton, z);
            }
        });
        Runnable runnable = new Runnable() { // from class: com.winlator.InputControlsFragment$$ExternalSyntheticLambda25
            @Override // java.lang.Runnable
            public final void run() {
                InputControlsFragment.this.lambda$onCreateView$2(sbCursorSpeed, cbDisableMouseInput, view);
            }
        };
        this.updateLayout = runnable;
        runnable.run();
        SeekBar sbOverlayOpacity = (SeekBar) view.findViewById(R.id.SBOverlayOpacity);
        sbOverlayOpacity.setOnValueChangeListener(new SeekBar.OnValueChangeListener() { // from class: com.winlator.InputControlsFragment$$ExternalSyntheticLambda17
            @Override // com.winlator.widget.SeekBar.OnValueChangeListener
            public final void onValueChangeListener(SeekBar seekBar, float f) {
                InputControlsFragment.lambda$onCreateView$3(preferences, seekBar, f);
            }
        });
        sbOverlayOpacity.setValue(preferences.getFloat("overlay_opacity", 0.4f) * 100.0f);
        view.findViewById(R.id.BTAddProfile).setOnClickListener(new View.OnClickListener() { // from class: com.winlator.InputControlsFragment$$ExternalSyntheticLambda4
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                InputControlsFragment.this.lambda$onCreateView$5(context, sProfile, view2);
            }
        });
        view.findViewById(R.id.BTEditProfile).setOnClickListener(new View.OnClickListener() { // from class: com.winlator.InputControlsFragment$$ExternalSyntheticLambda6
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                InputControlsFragment.this.lambda$onCreateView$7(context, sProfile, view2);
            }
        });
        view.findViewById(R.id.BTDuplicateProfile).setOnClickListener(new View.OnClickListener() { // from class: com.winlator.InputControlsFragment$$ExternalSyntheticLambda3
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                InputControlsFragment.this.lambda$onCreateView$9(context, sProfile, view2);
            }
        });
        view.findViewById(R.id.BTRemoveProfile).setOnClickListener(new View.OnClickListener() { // from class: com.winlator.InputControlsFragment$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                InputControlsFragment.this.lambda$onCreateView$11(context, sProfile, view2);
            }
        });
        view.findViewById(R.id.BTImportProfile).setOnClickListener(new View.OnClickListener() { // from class: com.winlator.InputControlsFragment$$ExternalSyntheticLambda5
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                InputControlsFragment.this.lambda$onCreateView$13(context, sProfile, view2);
            }
        });
        view.findViewById(R.id.BTExportProfile).setOnClickListener(new View.OnClickListener() { // from class: com.winlator.InputControlsFragment$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                InputControlsFragment.this.lambda$onCreateView$14(context, view2);
            }
        });
        view.findViewById(R.id.BTControlsEditor).setOnClickListener(new View.OnClickListener() { // from class: com.winlator.InputControlsFragment$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                InputControlsFragment.this.lambda$onCreateView$15(context, view2);
            }
        });
        return view;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreateView$0(SeekBar seekBar, float value) {
        ControlsProfile controlsProfile = this.currentProfile;
        if (controlsProfile != null) {
            controlsProfile.setCursorSpeed(value / 100.0f);
            this.currentProfile.save();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreateView$1(CompoundButton buttonView, boolean isChecked) {
        ControlsProfile controlsProfile = this.currentProfile;
        if (controlsProfile != null) {
            controlsProfile.setDisableMouseInput(isChecked);
            this.currentProfile.save();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreateView$2(SeekBar sbCursorSpeed, CheckBox cbDisableMouseInput, View view) {
        ControlsProfile controlsProfile = this.currentProfile;
        if (controlsProfile != null) {
            sbCursorSpeed.setValue(controlsProfile.getCursorSpeed() * 100.0f);
            cbDisableMouseInput.setChecked(this.currentProfile.isDisableMouseInput());
        } else {
            sbCursorSpeed.setValue(100.0f);
            cbDisableMouseInput.setChecked(false);
        }
        loadExternalControllers(view);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$onCreateView$3(SharedPreferences preferences, SeekBar seekBar, float value) {
        preferences.edit().putFloat("overlay_opacity", value / 100.0f).apply();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreateView$5(Context context, final Spinner sProfile, View v) {
        ContentDialog.prompt(context, R.string.profile_name, null, new Callback() { // from class: com.winlator.InputControlsFragment$$ExternalSyntheticLambda13
            @Override // com.winlator.core.Callback
            public final void call(Object obj) {
                InputControlsFragment.this.lambda$onCreateView$4(sProfile, (String) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreateView$4(Spinner sProfile, String name) {
        this.currentProfile = this.manager.createProfile(name);
        loadProfileSpinner(sProfile);
        this.updateLayout.run();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreateView$7(Context context, final Spinner sProfile, View v) {
        ControlsProfile controlsProfile = this.currentProfile;
        if (controlsProfile != null) {
            ContentDialog.prompt(context, R.string.profile_name, controlsProfile.getName(), new Callback() { // from class: com.winlator.InputControlsFragment$$ExternalSyntheticLambda12
                @Override // com.winlator.core.Callback
                public final void call(Object obj) {
                    InputControlsFragment.this.lambda$onCreateView$6(sProfile, (String) obj);
                }
            });
        } else {
            AppUtils.showToast(context, R.string.no_profile_selected);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreateView$6(Spinner sProfile, String name) {
        this.currentProfile.setName(name);
        this.currentProfile.save();
        loadProfileSpinner(sProfile);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreateView$9(Context context, final Spinner sProfile, View v) {
        if (this.currentProfile != null) {
            ContentDialog.confirm(context, R.string.do_you_want_to_duplicate_this_profile, new Runnable() { // from class: com.winlator.InputControlsFragment$$ExternalSyntheticLambda19
                @Override // java.lang.Runnable
                public final void run() {
                    InputControlsFragment.this.lambda$onCreateView$8(sProfile);
                }
            });
        } else {
            AppUtils.showToast(context, R.string.no_profile_selected);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreateView$8(Spinner sProfile) {
        this.currentProfile = this.manager.duplicateProfile(this.currentProfile);
        loadProfileSpinner(sProfile);
        this.updateLayout.run();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreateView$11(Context context, final Spinner sProfile, View v) {
        if (this.currentProfile != null) {
            ContentDialog.confirm(context, R.string.do_you_want_to_remove_this_profile, new Runnable() { // from class: com.winlator.InputControlsFragment$$ExternalSyntheticLambda20
                @Override // java.lang.Runnable
                public final void run() {
                    InputControlsFragment.this.lambda$onCreateView$10(sProfile);
                }
            });
        } else {
            AppUtils.showToast(context, R.string.no_profile_selected);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreateView$10(Spinner sProfile) {
        this.manager.removeProfile(this.currentProfile);
        this.currentProfile = null;
        loadProfileSpinner(sProfile);
        this.updateLayout.run();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreateView$13(Context context, final Spinner sProfile, View v) {
        PopupMenu popupMenu = new PopupMenu(context, v);
        if (Build.VERSION.SDK_INT >= 29) {
            popupMenu.setForceShowIcon(true);
        }
        popupMenu.inflate(R.menu.open_file_popup_menu);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() { // from class: com.winlator.InputControlsFragment$$ExternalSyntheticLambda10
            @Override // android.widget.PopupMenu.OnMenuItemClickListener
            public final boolean onMenuItemClick(MenuItem menuItem) {
                boolean lambda$onCreateView$12;
                lambda$onCreateView$12 = InputControlsFragment.this.lambda$onCreateView$12(sProfile, menuItem);
                return lambda$onCreateView$12;
            }
        });
        popupMenu.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean lambda$onCreateView$12(Spinner sProfile, MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.menu_item_open_file) {
            openProfileFile(sProfile);
            return true;
        }
        if (itemId == R.id.menu_item_download_file) {
            downloadProfileList(sProfile);
            return true;
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreateView$14(Context context, View v) {
        ControlsProfile controlsProfile = this.currentProfile;
        if (controlsProfile != null) {
            File exportedFile = this.manager.exportProfile(controlsProfile);
            if (exportedFile != null) {
                String path = exportedFile.getPath().substring(exportedFile.getPath().indexOf(Environment.DIRECTORY_DOWNLOADS));
                AppUtils.showToast(context, context.getString(R.string.profile_exported_to) + " " + path);
                return;
            }
            return;
        }
        AppUtils.showToast(context, R.string.no_profile_selected);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreateView$15(Context context, View v) {
        if (this.currentProfile != null) {
            Intent intent = new Intent(context, (Class<?>) ControlsEditorActivity.class);
            intent.putExtra("profile_id", this.currentProfile.id);
            startActivity(intent);
            return;
        }
        AppUtils.showToast(context, R.string.no_profile_selected);
    }

    private void openProfileFile(final Spinner sProfile) {
        this.importProfileCallback = new Callback() { // from class: com.winlator.InputControlsFragment$$ExternalSyntheticLambda11
            @Override // com.winlator.core.Callback
            public final void call(Object obj) {
                InputControlsFragment.this.lambda$openProfileFile$16(sProfile, (ControlsProfile) obj);
            }
        };
        Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT");
        intent.addCategory("android.intent.category.OPENABLE");
        intent.setType("*/*");
        getActivity().startActivityFromFragment(this, intent, 2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$openProfileFile$16(Spinner sProfile, ControlsProfile importedProfile) {
        this.currentProfile = importedProfile;
        loadProfileSpinner(sProfile);
        this.updateLayout.run();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: downloadSelectedProfiles, reason: merged with bridge method [inline-methods] */
    public void lambda$downloadProfileList$19(final Spinner sProfile, String[] items, final ArrayList<Integer> positions) {
        final MainActivity activity = (MainActivity) getActivity();
        activity.preloaderDialog.lambda$showOnUiThread$0(R.string.downloading_file);
        this.currentProfile = null;
        final AtomicInteger processedItemCount = new AtomicInteger();
        Iterator<Integer> it = positions.iterator();
        while (it.hasNext()) {
            int position = it.next().intValue();
            HttpUtils.download(String.format("https://raw.githubusercontent.com/brunodev85/winlator/main/input_controls/%s", items[position]), new Callback() { // from class: com.winlator.InputControlsFragment$$ExternalSyntheticLambda16
                @Override // com.winlator.core.Callback
                public final void call(Object obj) {
                    InputControlsFragment.this.lambda$downloadSelectedProfiles$18(processedItemCount, positions, activity, sProfile, (String) obj);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$downloadSelectedProfiles$18(AtomicInteger processedItemCount, ArrayList positions, final MainActivity activity, final Spinner sProfile, String content) {
        if (content != null) {
            try {
                this.manager.importProfile(new JSONObject(content));
            } catch (JSONException e) {
            }
        }
        if (processedItemCount.incrementAndGet() == positions.size()) {
            activity.runOnUiThread(new Runnable() { // from class: com.winlator.InputControlsFragment$$ExternalSyntheticLambda22
                @Override // java.lang.Runnable
                public final void run() {
                    InputControlsFragment.this.lambda$downloadSelectedProfiles$17(activity, sProfile);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$downloadSelectedProfiles$17(MainActivity activity, Spinner sProfile) {
        activity.preloaderDialog.close();
        loadProfileSpinner(sProfile);
        this.updateLayout.run();
    }

    private void downloadProfileList(final Spinner sProfile) {
        final MainActivity activity = (MainActivity) getActivity();
        activity.preloaderDialog.lambda$showOnUiThread$0(R.string.loading);
        HttpUtils.download(String.format("https://raw.githubusercontent.com/brunodev85/winlator/main/input_controls/%s", "index.txt"), new Callback() { // from class: com.winlator.InputControlsFragment$$ExternalSyntheticLambda14
            @Override // com.winlator.core.Callback
            public final void call(Object obj) {
                InputControlsFragment.this.lambda$downloadProfileList$22(activity, sProfile, (String) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$downloadProfileList$22(final MainActivity activity, final Spinner sProfile, final String content) {
        activity.runOnUiThread(new Runnable() { // from class: com.winlator.InputControlsFragment$$ExternalSyntheticLambda23
            @Override // java.lang.Runnable
            public final void run() {
                InputControlsFragment.this.lambda$downloadProfileList$21(activity, content, sProfile);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$downloadProfileList$21(final MainActivity activity, String content, final Spinner sProfile) {
        activity.preloaderDialog.close();
        if (content != null) {
            final String[] items = content.split("\n");
            ContentDialog.showMultipleChoiceList(activity, R.string.import_profile, items, new Callback() { // from class: com.winlator.InputControlsFragment$$ExternalSyntheticLambda15
                @Override // com.winlator.core.Callback
                public final void call(Object obj) {
                    InputControlsFragment.this.lambda$downloadProfileList$20(activity, sProfile, items, (ArrayList) obj);
                }
            });
        } else {
            AppUtils.showToast(activity, R.string.unable_to_load_profile_list);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$downloadProfileList$20(MainActivity activity, final Spinner sProfile, final String[] items, final ArrayList positions) {
        if (!positions.isEmpty()) {
            ContentDialog.confirm(activity, R.string.do_you_want_to_download_the_selected_profiles, new Runnable() { // from class: com.winlator.InputControlsFragment$$ExternalSyntheticLambda21
                @Override // java.lang.Runnable
                public final void run() {
                    InputControlsFragment.this.lambda$downloadProfileList$19(sProfile, items, positions);
                }
            });
        }
    }

    @Override // androidx.fragment.app.Fragment
    public void onStart() {
        super.onStart();
        Runnable runnable = this.updateLayout;
        if (runnable != null) {
            runnable.run();
        }
    }

    private void loadProfileSpinner(Spinner spinner) {
        final ArrayList<ControlsProfile> profiles = this.manager.getProfiles();
        ArrayList<String> values = new ArrayList<>();
        values.add("-- " + getString(R.string.select_profile) + " --");
        int selectedPosition = 0;
        for (int i = 0; i < profiles.size(); i++) {
            ControlsProfile profile = profiles.get(i);
            if (profile == this.currentProfile) {
                selectedPosition = i + 1;
            }
            values.add(profile.getName());
        }
        spinner.setAdapter((SpinnerAdapter) new ArrayAdapter(getContext(), android.R.layout.simple_spinner_dropdown_item, values));
        spinner.setSelection(selectedPosition, false);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // from class: com.winlator.InputControlsFragment.1
            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                InputControlsFragment.this.currentProfile = position > 0 ? (ControlsProfile) profiles.get(position - 1) : null;
                InputControlsFragment.this.updateLayout.run();
            }

            @Override // android.widget.AdapterView.OnItemSelectedListener
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r9v0 */
    /* JADX WARN: Type inference failed for: r9v1, types: [boolean, int] */
    /* JADX WARN: Type inference failed for: r9v3 */
    private void loadExternalControllers(final View view) {
        LinearLayout container = (LinearLayout) view.findViewById(R.id.LLExternalControllers);
        container.removeAllViews();
        Context context = getContext();
        LayoutInflater from = LayoutInflater.from(context);
        ArrayList<ExternalController> connectedControllers = ExternalController.getControllers();
        ControlsProfile controlsProfile = this.currentProfile;
        ArrayList<ExternalController> controllers = controlsProfile != null ? controlsProfile.loadControllers() : new ArrayList<>();
        Iterator<ExternalController> it = connectedControllers.iterator();
        while (it.hasNext()) {
            ExternalController controller = it.next();
            if (!controllers.contains(controller)) {
                controllers.add(controller);
            }
        }
        ?? r9 = 0;
        if (!controllers.isEmpty()) {
            view.findViewById(R.id.TVEmptyText).setVisibility(8);
            String bindingsText = context.getString(R.string.bindings);
            Iterator<ExternalController> it2 = controllers.iterator();
            while (it2.hasNext()) {
                final ExternalController controller2 = it2.next();
                View itemView = from.inflate(R.layout.external_controller_list_item, container, (boolean) r9);
                ((TextView) itemView.findViewById(R.id.TVTitle)).setText(controller2.getName());
                int controllerBindingCount = controller2.getControllerBindingCount();
                ((TextView) itemView.findViewById(R.id.TVSubtitle)).setText(controllerBindingCount + " " + bindingsText);
                ImageView imageView = (ImageView) itemView.findViewById(R.id.ImageView);
                int tintColor = controller2.isConnected() ? ContextCompat.getColor(context, R.color.colorAccent) : -1739917;
                ImageViewCompat.setImageTintList(imageView, ColorStateList.valueOf(tintColor));
                if (controllerBindingCount > 0) {
                    ImageButton imageButton = (ImageButton) itemView.findViewById(R.id.BTRemove);
                    imageButton.setVisibility(r9);
                    imageButton.setOnClickListener(new View.OnClickListener() { // from class: com.winlator.InputControlsFragment$$ExternalSyntheticLambda8
                        @Override // android.view.View.OnClickListener
                        public final void onClick(View view2) {
                            InputControlsFragment.this.lambda$loadExternalControllers$24(controller2, view, view2);
                        }
                    });
                }
                itemView.setOnClickListener(new View.OnClickListener() { // from class: com.winlator.InputControlsFragment$$ExternalSyntheticLambda7
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view2) {
                        InputControlsFragment.this.lambda$loadExternalControllers$25(controller2, view2);
                    }
                });
                container.addView(itemView);
                r9 = 0;
            }
            return;
        }
        view.findViewById(R.id.TVEmptyText).setVisibility(0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$loadExternalControllers$24(final ExternalController controller, final View view, View v) {
        ContentDialog.confirm(getContext(), R.string.do_you_want_to_remove_this_controller, new Runnable() { // from class: com.winlator.InputControlsFragment$$ExternalSyntheticLambda24
            @Override // java.lang.Runnable
            public final void run() {
                InputControlsFragment.this.lambda$loadExternalControllers$23(controller, view);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$loadExternalControllers$23(ExternalController controller, View view) {
        this.currentProfile.removeController(controller);
        this.currentProfile.save();
        loadExternalControllers(view);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$loadExternalControllers$25(ExternalController controller, View v) {
        if (this.currentProfile != null) {
            Intent intent = new Intent(getContext(), (Class<?>) ExternalControllerBindingsActivity.class);
            intent.putExtra("profile_id", this.currentProfile.id);
            intent.putExtra("controller_id", controller.getId());
            startActivity(intent);
            return;
        }
        AppUtils.showToast(getContext(), R.string.no_profile_selected);
    }
}
