package com.winlator;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.winlatcn.R;
import com.winlator.container.ContainerManager;
import com.winlator.core.AppUtils;
import com.winlator.core.FileUtils;
import com.winlator.core.PreloaderDialog;
import com.winlator.core.UnitUtils;
import java.io.File;
import java.util.Stack;
import java.util.concurrent.Executors;

/* loaded from: classes.dex */
public abstract class BaseFileManagerFragment<T> extends Fragment {
    protected Clipboard clipboard;
    protected TextView emptyTextView;
    protected DividerItemDecoration itemDecoration;
    protected ContainerManager manager;
    protected FloatingActionButton pasteButton;
    protected SharedPreferences preferences;
    protected PreloaderDialog preloaderDialog;
    protected RecyclerView recyclerView;
    protected ViewStyle viewStyle = ViewStyle.GRID;
    protected boolean viewStyleNeedsUpdate = true;
    protected final Stack<T> folderStack = new Stack<>();

    /* JADX INFO: Access modifiers changed from: protected */
    /* loaded from: classes.dex */
    public enum ViewStyle {
        LIST,
        GRID
    }

    protected abstract String getHomeTitle();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class Clipboard {
        final boolean cutMode;
        final File[] files;
        File targetDir;

        /* JADX INFO: Access modifiers changed from: package-private */
        public Clipboard(File[] files, boolean cutMode) {
            this.files = files;
            this.cutMode = cutMode;
        }
    }

    @Override // androidx.fragment.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        Activity activity = getActivity();
        this.preloaderDialog = new PreloaderDialog(activity);
        this.preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        this.manager = new ContainerManager(activity);
    }

    @Override // androidx.fragment.app.Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.base_file_manager_fragment, container, false);
        this.recyclerView = (RecyclerView) rootView.findViewById(R.id.RecyclerView);
        this.emptyTextView = (TextView) rootView.findViewById(R.id.TVEmptyText);
        FloatingActionButton floatingActionButton = (FloatingActionButton) rootView.findViewById(R.id.BTPaste);
        this.pasteButton = floatingActionButton;
        floatingActionButton.setOnClickListener(new View.OnClickListener() { // from class: com.winlator.BaseFileManagerFragment$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                BaseFileManagerFragment.this.lambda$onCreateView$0(view);
            }
        });
        if (this.itemDecoration == null) {
            this.itemDecoration = new DividerItemDecoration(getContext(), 1);
        }
        return rootView;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreateView$0(View v) {
        pasteFiles();
    }

    @Override // androidx.fragment.app.Fragment
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getHomeTitle());
        refreshContent();
    }

    public void setViewStyle(ViewStyle viewStyle) {
        this.viewStyle = viewStyle;
        this.viewStyleNeedsUpdate = true;
        refreshContent();
    }

    public boolean onBackPressed() {
        clearClipboard();
        return onOptionsMenuClicked();
    }

    public boolean onOptionsMenuClicked() {
        if (!this.folderStack.isEmpty()) {
            this.folderStack.pop();
            refreshContent();
            if (this.folderStack.isEmpty()) {
                ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
                actionBar.setHomeAsUpIndicator(R.drawable.icon_action_bar_menu);
                actionBar.setTitle(getHomeTitle());
                return true;
            }
            return true;
        }
        return false;
    }

    public void onOrientationChanged() {
        this.viewStyleNeedsUpdate = true;
        refreshContent();
    }

    public void refreshViewStyleMenuItem(MenuItem menuItem) {
        ViewStyle viewStyle = this.viewStyle;
        if (viewStyle == ViewStyle.LIST) {
            menuItem.setIcon(R.drawable.icon_action_bar_grid);
        } else if (viewStyle == ViewStyle.GRID) {
            menuItem.setIcon(R.drawable.icon_action_bar_list);
        }
    }

    public void refreshContent() {
        if (this.viewStyleNeedsUpdate) {
            Context context = getContext();
            this.recyclerView.removeItemDecoration(this.itemDecoration);
            ViewStyle viewStyle = this.viewStyle;
            if (viewStyle == ViewStyle.LIST) {
                this.recyclerView.setLayoutManager(new LinearLayoutManager(context));
                this.recyclerView.addItemDecoration(this.itemDecoration);
            } else if (viewStyle == ViewStyle.GRID) {
                int spanCount = Math.max(2, (int) (AppUtils.getScreenWidth() / UnitUtils.dpToPx(200.0f)));
                this.recyclerView.setLayoutManager(new GridLayoutManager(context, spanCount));
            }
            this.viewStyleNeedsUpdate = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void pasteFiles() {
        Clipboard clipboard = this.clipboard;
        if (clipboard == null) {
            return;
        }
        for (File file : clipboard.files) {
            File targetFile = new File(this.clipboard.targetDir, file.getName());
            if (targetFile.exists()) {
                AppUtils.showToast(getContext(), R.string.there_already_file_with_that_name);
                return;
            }
        }
        this.preloaderDialog.lambda$showOnUiThread$0(R.string.copying_files);
        Executors.newSingleThreadExecutor().execute(new Runnable() { // from class: com.winlator.BaseFileManagerFragment$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                BaseFileManagerFragment.this.lambda$pasteFiles$2();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$pasteFiles$2() {
        for (File originFile : this.clipboard.files) {
            if (originFile.exists()) {
                File targetFile = new File(this.clipboard.targetDir, originFile.getName());
                if (FileUtils.copy(originFile, targetFile) && this.clipboard.cutMode) {
                    FileUtils.delete(originFile);
                }
            }
        }
        getActivity().runOnUiThread(new Runnable() { // from class: com.winlator.BaseFileManagerFragment$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                BaseFileManagerFragment.this.lambda$pasteFiles$1();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$pasteFiles$1() {
        clearClipboard();
        refreshContent();
        this.preloaderDialog.close();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void removeFile(final File file) {
        this.preloaderDialog.lambda$showOnUiThread$0(R.string.removing_files);
        Executors.newSingleThreadExecutor().execute(new Runnable() { // from class: com.winlator.BaseFileManagerFragment$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                BaseFileManagerFragment.this.lambda$removeFile$4(file);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$removeFile$4(File file) {
        FileUtils.delete(file);
        getActivity().runOnUiThread(new Runnable() { // from class: com.winlator.BaseFileManagerFragment$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                BaseFileManagerFragment.this.lambda$removeFile$3();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$removeFile$3() {
        clearClipboard();
        refreshContent();
        this.preloaderDialog.close();
    }

    public void clearClipboard() {
        if (this.clipboard != null) {
            this.clipboard = null;
            this.pasteButton.setVisibility(8);
        }
    }
}
