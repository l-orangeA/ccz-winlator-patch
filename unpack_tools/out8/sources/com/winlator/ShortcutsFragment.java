package com.winlator;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.winlatcn.R;
import com.winlator.BaseFileManagerFragment;
import com.winlator.ShortcutsFragment;
import com.winlator.container.Container;
import com.winlator.container.Shortcut;
import com.winlator.contentdialog.CreateFolderDialog;
import com.winlator.core.AppUtils;
import com.winlator.core.ArrayUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class ShortcutsFragment extends BaseFileManagerFragment<Shortcut> {
    @Override // com.winlator.BaseFileManagerFragment, androidx.fragment.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.viewStyle = BaseFileManagerFragment.ViewStyle.valueOf(this.preferences.getString("shortcuts_view_style", "GRID"));
    }

    @Override // com.winlator.BaseFileManagerFragment
    public void refreshContent() {
        super.refreshContent();
        Shortcut selectedFolder = !this.folderStack.isEmpty() ? (Shortcut) this.folderStack.peek() : null;
        ArrayList<Shortcut> shortcuts = this.manager.loadShortcuts(selectedFolder);
        this.recyclerView.setAdapter(new ShortcutsAdapter(shortcuts));
        this.emptyTextView.setVisibility(shortcuts.isEmpty() ? 0 : 8);
    }

    @Override // androidx.fragment.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.shortcuts_menu, menu);
        refreshViewStyleMenuItem(menu.findItem(R.id.menu_item_view_style));
    }

    private void createFolder() {
        clearClipboard();
        if (this.manager.getContainers().isEmpty()) {
            return;
        }
        final Context context = getContext();
        CreateFolderDialog createFolderDialog = new CreateFolderDialog(this.manager);
        createFolderDialog.setOnCreateFolderListener(new CreateFolderDialog.OnCreateFolderListener() { // from class: com.winlator.ShortcutsFragment$$ExternalSyntheticLambda0
            @Override // com.winlator.contentdialog.CreateFolderDialog.OnCreateFolderListener
            public final void onCreateFolder(Container container, String str) {
                ShortcutsFragment.this.lambda$createFolder$0(context, container, str);
            }
        });
        createFolderDialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$createFolder$0(Context context, Container container, String name) {
        File desktopDir = new File(container.getUserDir(), context.getString(R.string.desktop));
        File parent = !this.folderStack.isEmpty() ? ((Shortcut) this.folderStack.peek()).file : desktopDir;
        File file = new File(parent, name);
        if (file.isDirectory()) {
            AppUtils.showToast(getContext(), R.string.there_already_file_with_that_name);
        } else {
            file.mkdir();
            refreshContent();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.winlator.BaseFileManagerFragment
    public void pasteFiles() {
        if (this.folderStack.isEmpty()) {
            clearClipboard();
            AppUtils.showToast(getContext(), R.string.you_cannot_paste_files_here);
        } else {
            this.clipboard.targetDir = ((Shortcut) this.folderStack.peek()).file;
            super.pasteFiles();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void instantiateClipboard(Shortcut shortcut, boolean cutMode) {
        clearClipboard();
        File linkFile = shortcut.getLinkFile();
        File[] files = {new File(shortcut.file.getParentFile(), shortcut.file.getName())};
        if (shortcut.file.isFile()) {
            files = (File[]) ArrayUtils.concat(files, new File[]{new File(linkFile.getParentFile(), linkFile.getName())});
        }
        this.clipboard = new BaseFileManagerFragment.Clipboard(files, cutMode);
        this.pasteButton.setVisibility(0);
    }

    @Override // androidx.fragment.app.Fragment
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.menu_item_view_style) {
            BaseFileManagerFragment.ViewStyle viewStyle = this.viewStyle;
            BaseFileManagerFragment.ViewStyle viewStyle2 = BaseFileManagerFragment.ViewStyle.GRID;
            if (viewStyle == viewStyle2) {
                viewStyle2 = BaseFileManagerFragment.ViewStyle.LIST;
            }
            setViewStyle(viewStyle2);
            this.preferences.edit().putString("shortcuts_view_style", this.viewStyle.name()).apply();
            refreshViewStyleMenuItem(menuItem);
            return true;
        }
        if (itemId == R.id.menu_item_new_folder) {
            createFolder();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override // com.winlator.BaseFileManagerFragment
    protected String getHomeTitle() {
        return getString(R.string.shortcuts);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ShortcutsAdapter extends RecyclerView.Adapter<ViewHolder> {
        private final List<Shortcut> data;

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public class ViewHolder extends RecyclerView.ViewHolder {
            private final ImageView imageView;
            private final ImageView menuButton;
            private final ImageView runButton;
            private final TextView subtitle;
            private final TextView title;

            private ViewHolder(View view) {
                super(view);
                this.imageView = (ImageView) view.findViewById(R.id.ImageView);
                this.title = (TextView) view.findViewById(R.id.TVTitle);
                this.subtitle = (TextView) view.findViewById(R.id.TVSubtitle);
                this.runButton = (ImageView) view.findViewById(R.id.BTRun);
                this.menuButton = (ImageView) view.findViewById(R.id.BTMenu);
            }
        }

        public ShortcutsAdapter(List<Shortcut> data) {
            this.data = data;
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            int resource = ShortcutsFragment.this.viewStyle == BaseFileManagerFragment.ViewStyle.LIST ? R.layout.file_list_item : R.layout.file_grid_item;
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(resource, parent, false));
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public void onBindViewHolder(ViewHolder holder, int position) {
            final Shortcut item = this.data.get(position);
            if (item.icon != null) {
                holder.imageView.setImageBitmap(item.icon);
            } else {
                int iconResId = item.file.isDirectory() ? R.drawable.container_folder : R.drawable.container_file_link;
                holder.imageView.setImageResource(iconResId);
            }
            holder.title.setText(item.name);
            holder.subtitle.setText(item.container.getName());
            if (item.file.isDirectory()) {
                holder.runButton.setImageResource(R.drawable.icon_open);
            } else {
                holder.runButton.setImageResource(R.drawable.icon_run);
            }
            holder.imageView.setOnClickListener(new View.OnClickListener() { // from class: com.winlator.ShortcutsFragment$ShortcutsAdapter$$ExternalSyntheticLambda1
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    ShortcutsFragment.ShortcutsAdapter.this.lambda$onBindViewHolder$0(item, view);
                }
            });
            holder.runButton.setOnClickListener(new View.OnClickListener() { // from class: com.winlator.ShortcutsFragment$ShortcutsAdapter$$ExternalSyntheticLambda2
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    ShortcutsFragment.ShortcutsAdapter.this.lambda$onBindViewHolder$1(item, view);
                }
            });
            holder.menuButton.setOnClickListener(new View.OnClickListener() { // from class: com.winlator.ShortcutsFragment$ShortcutsAdapter$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    ShortcutsFragment.ShortcutsAdapter.this.lambda$onBindViewHolder$2(item, view);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onBindViewHolder$0(Shortcut item, View v) {
            runFromShortcut(item);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onBindViewHolder$1(Shortcut item, View v) {
            runFromShortcut(item);
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public final int getItemCount() {
            return this.data.size();
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* renamed from: showListItemMenu, reason: merged with bridge method [inline-methods] */
        public void lambda$onBindViewHolder$2(View anchorView, final Shortcut shortcut) {
            final Context context = ShortcutsFragment.this.getContext();
            PopupMenu listItemMenu = new PopupMenu(context, anchorView);
            if (Build.VERSION.SDK_INT >= 29) {
                listItemMenu.setForceShowIcon(true);
            }
            listItemMenu.inflate(R.menu.file_manager_popup_menu);
            Menu menu = listItemMenu.getMenu();
            menu.findItem(R.id.menu_item_rename).setVisible(false);
            menu.findItem(R.id.menu_item_add_favorite).setVisible(false);
            menu.findItem(R.id.menu_item_info).setVisible(false);
            listItemMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() { // from class: com.winlator.ShortcutsFragment$ShortcutsAdapter$$ExternalSyntheticLambda3
                @Override // android.widget.PopupMenu.OnMenuItemClickListener
                public final boolean onMenuItemClick(MenuItem menuItem) {
                    boolean lambda$showListItemMenu$4;
                    lambda$showListItemMenu$4 = ShortcutsFragment.ShortcutsAdapter.this.lambda$showListItemMenu$4(shortcut, context, menuItem);
                    return lambda$showListItemMenu$4;
                }
            });
            listItemMenu.show();
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        /* JADX WARN: Code restructure failed: missing block: B:10:0x0038, code lost:
        
            return true;
         */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct add '--show-bad-code' argument
        */
        public /* synthetic */ boolean lambda$showListItemMenu$4(final com.winlator.container.Shortcut r5, android.content.Context r6, android.view.MenuItem r7) {
            /*
                r4 = this;
                int r0 = r7.getItemId()
                r1 = 1
                switch(r0) {
                    case 2131296677: goto L2a;
                    case 2131296678: goto L2a;
                    case 2131296698: goto L19;
                    case 2131296701: goto L9;
                    default: goto L8;
                }
            L8:
                goto L38
            L9:
                com.winlator.ShortcutsFragment r2 = com.winlator.ShortcutsFragment.this
                r2.clearClipboard()
                com.winlator.contentdialog.ShortcutSettingsDialog r2 = new com.winlator.contentdialog.ShortcutSettingsDialog
                com.winlator.ShortcutsFragment r3 = com.winlator.ShortcutsFragment.this
                r2.<init>(r3, r5)
                r2.show()
                goto L38
            L19:
                com.winlator.ShortcutsFragment r2 = com.winlator.ShortcutsFragment.this
                r2.clearClipboard()
                r2 = 2131755117(0x7f10006d, float:1.9141104E38)
                com.winlator.ShortcutsFragment$ShortcutsAdapter$$ExternalSyntheticLambda4 r3 = new com.winlator.ShortcutsFragment$ShortcutsAdapter$$ExternalSyntheticLambda4
                r3.<init>()
                com.winlator.contentdialog.ContentDialog.confirm(r6, r2, r3)
                goto L38
            L2a:
                com.winlator.ShortcutsFragment r2 = com.winlator.ShortcutsFragment.this
                r3 = 2131296678(0x7f0901a6, float:1.821128E38)
                if (r0 != r3) goto L33
                r3 = r1
                goto L34
            L33:
                r3 = 0
            L34:
                com.winlator.ShortcutsFragment.access$600(r2, r5, r3)
            L38:
                return r1
            */
            throw new UnsupportedOperationException("Method not decompiled: com.winlator.ShortcutsFragment.ShortcutsAdapter.lambda$showListItemMenu$4(com.winlator.container.Shortcut, android.content.Context, android.view.MenuItem):boolean");
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$showListItemMenu$3(Shortcut shortcut) {
            shortcut.remove();
            ShortcutsFragment.this.refreshContent();
        }

        private void runFromShortcut(Shortcut shortcut) {
            AppCompatActivity activity = (AppCompatActivity) ShortcutsFragment.this.getActivity();
            if (shortcut.file.isDirectory()) {
                ShortcutsFragment.this.folderStack.push(shortcut);
                ShortcutsFragment.this.refreshContent();
                ActionBar actionBar = activity.getSupportActionBar();
                actionBar.setHomeAsUpIndicator(R.drawable.icon_action_bar_back);
                actionBar.setTitle(shortcut.name);
                return;
            }
            Intent intent = new Intent(activity, (Class<?>) XServerDisplayActivity.class);
            intent.putExtra("container_id", shortcut.container.id);
            intent.putExtra("shortcut_path", shortcut.file.getPath());
            activity.startActivity(intent);
        }
    }
}
