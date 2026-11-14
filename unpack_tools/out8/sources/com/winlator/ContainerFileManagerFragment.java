package com.winlator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
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
import com.winlator.ContainerFileManagerFragment;
import com.winlator.container.Container;
import com.winlator.container.FileInfo;
import com.winlator.contentdialog.ContentDialog;
import com.winlator.core.AppUtils;
import com.winlator.core.Callback;
import com.winlator.core.FileUtils;
import com.winlator.core.StringUtils;
import com.winlator.core.WineUtils;
import com.winlator.win32.MSIcon;
import com.winlator.win32.MSLink;
import com.winlator.win32.PEParser;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/* loaded from: classes.dex */
public class ContainerFileManagerFragment extends BaseFileManagerFragment<FileInfo> {
    private Container container;
    private final int containerId;
    private String startPath;

    public ContainerFileManagerFragment(int containerId) {
        this(containerId, null);
    }

    public ContainerFileManagerFragment(int containerId, String startPath) {
        this.containerId = containerId;
        this.startPath = startPath;
    }

    @Override // com.winlator.BaseFileManagerFragment, androidx.fragment.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.container = this.manager.getContainerById(this.containerId);
        this.viewStyle = BaseFileManagerFragment.ViewStyle.valueOf(this.preferences.getString("container_file_manager_view_style", "GRID"));
        String str = this.startPath;
        if (str != null) {
            setCurrentWorkingPath(WineUtils.unixToDOSPath(str, this.container));
            this.startPath = null;
        }
    }

    @Override // com.winlator.BaseFileManagerFragment
    public void refreshContent() {
        super.refreshContent();
        FileInfo parent = !this.folderStack.isEmpty() ? (FileInfo) this.folderStack.peek() : null;
        ArrayList<FileInfo> files = this.manager.loadFiles(this.container, parent);
        this.recyclerView.setAdapter(new FileInfoAdapter(files));
        this.emptyTextView.setVisibility(files.isEmpty() ? 0 : 8);
        updateActionBarTitle();
    }

    @Override // androidx.fragment.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.container_file_manager_menu, menu);
        refreshViewStyleMenuItem(menu.findItem(R.id.menu_item_view_style));
    }

    private void createFolder() {
        clearClipboard();
        if (this.folderStack.isEmpty()) {
            return;
        }
        ContentDialog.prompt(getContext(), R.string.new_folder, null, new Callback() { // from class: com.winlator.ContainerFileManagerFragment$$ExternalSyntheticLambda0
            @Override // com.winlator.core.Callback
            public final void call(Object obj) {
                ContainerFileManagerFragment.this.lambda$createFolder$0((String) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$createFolder$0(String name) {
        File file = new File(((FileInfo) this.folderStack.peek()).toFile(), name);
        if (file.isDirectory()) {
            AppUtils.showToast(getContext(), R.string.there_already_file_with_that_name);
        } else {
            file.mkdir();
            refreshContent();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void instantiateClipboard(FileInfo file, boolean cutMode) {
        clearClipboard();
        this.clipboard = new BaseFileManagerFragment.Clipboard(new File[]{new File(file.path)}, cutMode);
        this.pasteButton.setVisibility(0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void addFavorite(FileInfo file) {
        File favoritesDir = new File(this.container.getUserDir(), getContext().getString(R.string.favorites));
        File targetFile = new File(favoritesDir, FileUtils.getBasename(file.name) + ".lnk");
        if (!targetFile.exists()) {
            MSLink.LinkInfo linkInfo = new MSLink.LinkInfo();
            linkInfo.targetPath = WineUtils.unixToDOSPath(file.path, this.container);
            linkInfo.isDirectory = file.type == FileInfo.Type.DIRECTORY;
            boolean success = MSLink.createFile(linkInfo, targetFile);
            if (success) {
                AppUtils.showToast(getContext(), R.string.file_added_to_favorites);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.winlator.BaseFileManagerFragment
    public void pasteFiles() {
        if (this.folderStack.isEmpty()) {
            clearClipboard();
            AppUtils.showToast(getContext(), R.string.you_cannot_paste_files_here);
        } else {
            this.clipboard.targetDir = ((FileInfo) this.folderStack.peek()).toFile();
            super.pasteFiles();
        }
    }

    @Override // androidx.fragment.app.Fragment
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        switch (itemId) {
            case R.id.menu_item_home /* 2131296686 */:
                this.folderStack.clear();
                refreshContent();
                return true;
            case R.id.menu_item_new_folder /* 2131296694 */:
                createFolder();
                return true;
            case R.id.menu_item_view_style /* 2131296706 */:
                BaseFileManagerFragment.ViewStyle viewStyle = this.viewStyle;
                BaseFileManagerFragment.ViewStyle viewStyle2 = BaseFileManagerFragment.ViewStyle.GRID;
                if (viewStyle == viewStyle2) {
                    viewStyle2 = BaseFileManagerFragment.ViewStyle.LIST;
                }
                setViewStyle(viewStyle2);
                this.preferences.edit().putString("container_file_manager_view_style", this.viewStyle.name()).apply();
                refreshViewStyleMenuItem(menuItem);
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    private void setCurrentWorkingPath(String dosPath) {
        String[] names = StringUtils.removeEndSlash(dosPath).split("\\\\");
        String basePath = "";
        this.folderStack.clear();
        for (String name : names) {
            if (!name.isEmpty()) {
                String dosPath2 = WineUtils.dosToUnixPath(basePath + name, this.container);
                if (basePath.isEmpty() && name.matches("[A-Za-z]:")) {
                    this.folderStack.push(new FileInfo(this.container, name, dosPath2, FileInfo.Type.DRIVE));
                } else {
                    this.folderStack.push(new FileInfo(this.container, dosPath2, FileInfo.Type.DIRECTORY));
                }
                basePath = basePath + name + "\\";
            }
        }
        updateActionBarTitle();
    }

    private String getCurrentWorkingPath() {
        if (!this.folderStack.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < this.folderStack.size(); i++) {
                if (i > 0) {
                    sb.append("\\");
                }
                sb.append(((FileInfo) this.folderStack.elementAt(i)).getDisplayName());
            }
            if (this.folderStack.size() == 1) {
                sb.append("\\");
            }
            return sb.toString();
        }
        return "";
    }

    private void updateActionBarTitle() {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        ActionBar actionBar = activity.getSupportActionBar();
        if (!this.folderStack.isEmpty()) {
            actionBar.setHomeAsUpIndicator(R.drawable.icon_action_bar_back);
            actionBar.setTitle(getCurrentWorkingPath());
        } else {
            actionBar.setHomeAsUpIndicator(R.drawable.icon_action_bar_menu);
            actionBar.setTitle(getHomeTitle());
        }
    }

    @Override // com.winlator.BaseFileManagerFragment
    protected String getHomeTitle() {
        return this.container.getName();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class LoadIconTask {
        private boolean canceled;
        private final Executor executor;
        private final ContainerFileManagerFragment fragment;
        private final WeakReference<ImageView> imageViewWeakRef;

        private LoadIconTask(ContainerFileManagerFragment fragment, ImageView imageView) {
            this.executor = Executors.newSingleThreadExecutor();
            this.canceled = false;
            this.fragment = fragment;
            this.imageViewWeakRef = new WeakReference<>(imageView);
        }

        public void loadAsync(final FileInfo file) {
            this.executor.execute(new Runnable() { // from class: com.winlator.ContainerFileManagerFragment$LoadIconTask$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    ContainerFileManagerFragment.LoadIconTask.this.lambda$loadAsync$1(file);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$loadAsync$1(FileInfo file) {
            final Object icon = this.fragment.getIconForFile(file);
            final ImageView imageView = this.imageViewWeakRef.get();
            if (imageView != null && !this.canceled) {
                imageView.post(new Runnable() { // from class: com.winlator.ContainerFileManagerFragment$LoadIconTask$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        ContainerFileManagerFragment.LoadIconTask.this.lambda$loadAsync$0(icon, imageView);
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$loadAsync$0(Object icon, ImageView imageView) {
            if (this.canceled) {
                return;
            }
            if (icon instanceof Bitmap) {
                imageView.setImageBitmap((Bitmap) icon);
            } else {
                imageView.setImageResource(((Integer) icon).intValue());
            }
        }

        public void cancel() {
            this.canceled = true;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class FileInfoAdapter extends RecyclerView.Adapter<ViewHolder> {
        private final List<FileInfo> data;

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

        public FileInfoAdapter(List<FileInfo> data) {
            this.data = data;
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            int resource = ContainerFileManagerFragment.this.viewStyle == BaseFileManagerFragment.ViewStyle.LIST ? R.layout.file_list_item : R.layout.file_grid_item;
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(resource, parent, false));
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public void onBindViewHolder(ViewHolder holder, int position) {
            Context context = ContainerFileManagerFragment.this.getContext();
            final FileInfo item = this.data.get(position);
            FileInfo.Type type = item.type;
            if (item.type == FileInfo.Type.DRIVE) {
                String driveText = ContainerFileManagerFragment.this.getContext().getString(R.string.drive);
                holder.title.setText(driveText + " (" + item.name + ")");
            } else {
                MSLink.LinkInfo linkInfo = item.getLinkinfo();
                if (linkInfo != null && linkInfo.isDirectory) {
                    type = FileInfo.Type.DIRECTORY;
                }
                holder.title.setText(item.getDisplayName());
            }
            holder.subtitle.setVisibility(8);
            holder.runButton.setImageResource(R.drawable.icon_open);
            if (type == FileInfo.Type.DIRECTORY && !ContainerFileManagerFragment.this.folderStack.isEmpty()) {
                holder.subtitle.setText(item.getItemCount() + " " + context.getString(R.string.items));
                holder.subtitle.setVisibility(0);
            } else if (type == FileInfo.Type.FILE) {
                holder.runButton.setImageResource(R.drawable.icon_run);
                holder.subtitle.setText(StringUtils.formatBytes(item.getSize()));
                holder.subtitle.setVisibility(0);
            }
            if (type == FileInfo.Type.FILE) {
                holder.imageView.setImageResource(R.drawable.container_file);
                LoadIconTask loadIconTask = (LoadIconTask) holder.imageView.getTag();
                if (loadIconTask != null) {
                    loadIconTask.cancel();
                }
                LoadIconTask loadIconTask2 = new LoadIconTask(holder.imageView);
                loadIconTask2.loadAsync(item);
                holder.imageView.setTag(loadIconTask2);
            } else {
                holder.imageView.setImageResource(((Integer) ContainerFileManagerFragment.this.getIconForFile(item)).intValue());
            }
            holder.imageView.setOnClickListener(new View.OnClickListener() { // from class: com.winlator.ContainerFileManagerFragment$FileInfoAdapter$$ExternalSyntheticLambda2
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    ContainerFileManagerFragment.FileInfoAdapter.this.lambda$onBindViewHolder$0(item, view);
                }
            });
            holder.runButton.setOnClickListener(new View.OnClickListener() { // from class: com.winlator.ContainerFileManagerFragment$FileInfoAdapter$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    ContainerFileManagerFragment.FileInfoAdapter.this.lambda$onBindViewHolder$1(item, view);
                }
            });
            holder.menuButton.setOnClickListener(new View.OnClickListener() { // from class: com.winlator.ContainerFileManagerFragment$FileInfoAdapter$$ExternalSyntheticLambda1
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    ContainerFileManagerFragment.FileInfoAdapter.this.lambda$onBindViewHolder$2(item, view);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onBindViewHolder$0(FileInfo item, View v) {
            openFile(item);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onBindViewHolder$1(FileInfo item, View v) {
            openFile(item);
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public final int getItemCount() {
            return this.data.size();
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* renamed from: showListItemMenu, reason: merged with bridge method [inline-methods] */
        public void lambda$onBindViewHolder$2(View anchorView, final FileInfo file) {
            final Context context = ContainerFileManagerFragment.this.getContext();
            PopupMenu listItemMenu = new PopupMenu(context, anchorView);
            if (Build.VERSION.SDK_INT >= 29) {
                listItemMenu.setForceShowIcon(true);
            }
            listItemMenu.inflate(R.menu.file_manager_popup_menu);
            Menu menu = listItemMenu.getMenu();
            menu.findItem(R.id.menu_item_settings).setVisible(false);
            if (ContainerFileManagerFragment.this.folderStack.isEmpty()) {
                menu.findItem(R.id.menu_item_cut).setVisible(false);
                menu.findItem(R.id.menu_item_remove).setVisible(false);
                menu.findItem(R.id.menu_item_rename).setVisible(false);
                menu.findItem(R.id.menu_item_add_favorite).setVisible(false);
            } else if (((FileInfo) ContainerFileManagerFragment.this.folderStack.peek()).name.equals("Favorites")) {
                menu.findItem(R.id.menu_item_add_favorite).setVisible(false);
            }
            listItemMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() { // from class: com.winlator.ContainerFileManagerFragment$FileInfoAdapter$$ExternalSyntheticLambda3
                @Override // android.widget.PopupMenu.OnMenuItemClickListener
                public final boolean onMenuItemClick(MenuItem menuItem) {
                    boolean lambda$showListItemMenu$5;
                    lambda$showListItemMenu$5 = ContainerFileManagerFragment.FileInfoAdapter.this.lambda$showListItemMenu$5(file, context, menuItem);
                    return lambda$showListItemMenu$5;
                }
            });
            listItemMenu.show();
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        /* JADX WARN: Code restructure failed: missing block: B:12:0x0050, code lost:
        
            return true;
         */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct add '--show-bad-code' argument
        */
        public /* synthetic */ boolean lambda$showListItemMenu$5(final com.winlator.container.FileInfo r6, android.content.Context r7, android.view.MenuItem r8) {
            /*
                r5 = this;
                int r0 = r8.getItemId()
                r1 = 1
                switch(r0) {
                    case 2131296674: goto L4a;
                    case 2131296677: goto L3c;
                    case 2131296678: goto L3c;
                    case 2131296687: goto L2d;
                    case 2131296698: goto L1c;
                    case 2131296699: goto L9;
                    default: goto L8;
                }
            L8:
                goto L50
            L9:
                com.winlator.ContainerFileManagerFragment r2 = com.winlator.ContainerFileManagerFragment.this
                r2.clearClipboard()
                r2 = 2131755303(0x7f100127, float:1.9141481E38)
                java.lang.String r3 = r6.name
                com.winlator.ContainerFileManagerFragment$FileInfoAdapter$$ExternalSyntheticLambda4 r4 = new com.winlator.ContainerFileManagerFragment$FileInfoAdapter$$ExternalSyntheticLambda4
                r4.<init>()
                com.winlator.contentdialog.ContentDialog.prompt(r7, r2, r3, r4)
                goto L50
            L1c:
                com.winlator.ContainerFileManagerFragment r2 = com.winlator.ContainerFileManagerFragment.this
                r2.clearClipboard()
                r2 = 2131755117(0x7f10006d, float:1.9141104E38)
                com.winlator.ContainerFileManagerFragment$FileInfoAdapter$$ExternalSyntheticLambda5 r3 = new com.winlator.ContainerFileManagerFragment$FileInfoAdapter$$ExternalSyntheticLambda5
                r3.<init>()
                com.winlator.contentdialog.ContentDialog.confirm(r7, r2, r3)
                goto L50
            L2d:
                com.winlator.contentdialog.FileInfoDialog r2 = new com.winlator.contentdialog.FileInfoDialog
                com.winlator.ContainerFileManagerFragment r3 = com.winlator.ContainerFileManagerFragment.this
                com.winlator.container.Container r3 = com.winlator.ContainerFileManagerFragment.access$700(r3)
                r2.<init>(r7, r6, r3)
                r2.show()
                goto L50
            L3c:
                com.winlator.ContainerFileManagerFragment r2 = com.winlator.ContainerFileManagerFragment.this
                r3 = 2131296678(0x7f0901a6, float:1.821128E38)
                if (r0 != r3) goto L45
                r3 = r1
                goto L46
            L45:
                r3 = 0
            L46:
                com.winlator.ContainerFileManagerFragment.access$800(r2, r6, r3)
                goto L50
            L4a:
                com.winlator.ContainerFileManagerFragment r2 = com.winlator.ContainerFileManagerFragment.this
                com.winlator.ContainerFileManagerFragment.access$900(r2, r6)
            L50:
                return r1
            */
            throw new UnsupportedOperationException("Method not decompiled: com.winlator.ContainerFileManagerFragment.FileInfoAdapter.lambda$showListItemMenu$5(com.winlator.container.FileInfo, android.content.Context, android.view.MenuItem):boolean");
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$showListItemMenu$3(FileInfo file) {
            ContainerFileManagerFragment.this.removeFile(file.toFile());
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$showListItemMenu$4(FileInfo file, String newName) {
            file.renameTo(newName);
            ContainerFileManagerFragment.this.refreshContent();
        }

        private void openFile(FileInfo file) {
            Activity activity = ContainerFileManagerFragment.this.getActivity();
            MSLink.LinkInfo linkInfo = file.getLinkinfo();
            boolean isFile = true;
            if (linkInfo == null ? file.type != FileInfo.Type.FILE : linkInfo.isDirectory) {
                isFile = false;
            }
            if (isFile) {
                Intent intent = new Intent(activity, (Class<?>) XServerDisplayActivity.class);
                intent.putExtra("container_id", ContainerFileManagerFragment.this.container.id);
                intent.putExtra("exec_path", file.path);
                activity.startActivity(intent);
                return;
            }
            ContainerFileManagerFragment.this.folderStack.push(file);
            ContainerFileManagerFragment.this.refreshContent();
        }
    }

    public Object getIconForFile(FileInfo file) {
        Bitmap bitmap;
        FileInfo.Type type = file.type;
        FileInfo.Type type2 = FileInfo.Type.DIRECTORY;
        Integer valueOf = Integer.valueOf(R.drawable.container_folder);
        if (type == type2) {
            Context context = getContext();
            if (file.path.endsWith("xuser/" + context.getString(R.string.documents))) {
                return Integer.valueOf(R.drawable.container_folder_documents);
            }
            if (file.path.endsWith("xuser/" + context.getString(R.string.favorites))) {
                return Integer.valueOf(R.drawable.container_folder_favorites);
            }
            return valueOf;
        }
        if (type == FileInfo.Type.DRIVE) {
            return Integer.valueOf(R.drawable.container_drive);
        }
        String extension = FileUtils.getExtension(file.path);
        char c = 65535;
        switch (extension.hashCode()) {
            case 97301:
                if (extension.equals("bat")) {
                    c = 1;
                    break;
                }
                break;
            case 99556:
                if (extension.equals("dll")) {
                    c = 3;
                    break;
                }
                break;
            case 100882:
                if (extension.equals("exe")) {
                    c = 0;
                    break;
                }
                break;
            case 104085:
                if (extension.equals("ico")) {
                    c = 2;
                    break;
                }
                break;
            case 107305:
                if (extension.equals("lnk")) {
                    c = 4;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                Bitmap bitmap2 = PEParser.extractIcon(file.toFile());
                return bitmap2 != null ? bitmap2 : Integer.valueOf(R.drawable.container_file_window);
            case 1:
                return Integer.valueOf(R.drawable.container_file_window);
            case 2:
                Bitmap bitmap3 = MSIcon.decodeFile(file.toFile());
                if (bitmap3 != null) {
                    return bitmap3;
                }
                break;
            case 3:
                return Integer.valueOf(R.drawable.container_file_library);
            case 4:
                MSLink.LinkInfo linkInfo = file.getLinkinfo();
                if (linkInfo != null) {
                    if (linkInfo.isDirectory) {
                        return valueOf;
                    }
                    String targetPath = linkInfo.iconLocation;
                    if (targetPath == null) {
                        targetPath = linkInfo.targetPath;
                    }
                    String targetPath2 = WineUtils.dosToUnixPath(targetPath, this.container);
                    if (targetPath2.endsWith(".ico")) {
                        bitmap = MSIcon.decodeFile(new File(targetPath2));
                    } else {
                        bitmap = PEParser.extractIcon(new File(targetPath2), linkInfo.iconIndex);
                    }
                    if (bitmap != null) {
                        return bitmap;
                    }
                }
                return Integer.valueOf(R.drawable.container_file_link);
        }
        return Integer.valueOf(R.drawable.container_file);
    }
}
