package com.winlator;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.winlatcn.R;
import com.winlator.ContainersFragment;
import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.contentdialog.ContentDialog;
import com.winlator.contentdialog.StorageInfoDialog;
import com.winlator.core.PreloaderDialog;
import com.winlator.xenvironment.ImageFs;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class ContainersFragment extends Fragment {
    private TextView emptyTextView;
    private ContainerManager manager;
    private PreloaderDialog preloaderDialog;
    private RecyclerView recyclerView;

    @Override // androidx.fragment.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        this.preloaderDialog = new PreloaderDialog(getActivity());
    }

    @Override // androidx.fragment.app.Fragment
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.manager = new ContainerManager(getContext());
        loadContainersList();
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.containers);
    }

    @Override // androidx.fragment.app.Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FrameLayout frameLayout = (FrameLayout) inflater.inflate(R.layout.containers_fragment, container, false);
        this.recyclerView = (RecyclerView) frameLayout.findViewById(R.id.RecyclerView);
        this.emptyTextView = (TextView) frameLayout.findViewById(R.id.TVEmptyText);
        RecyclerView recyclerView = this.recyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));
        this.recyclerView.addItemDecoration(new DividerItemDecoration(this.recyclerView.getContext(), 1));
        return frameLayout;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void loadContainersList() {
        ArrayList<Container> containers = this.manager.getContainers();
        this.recyclerView.setAdapter(new ContainersAdapter(containers));
        if (containers.isEmpty()) {
            this.emptyTextView.setVisibility(0);
        }
    }

    @Override // androidx.fragment.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.containers_menu, menu);
    }

    @Override // androidx.fragment.app.Fragment
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.menu_item_add) {
            if (!ImageFs.find(getContext()).isValid()) {
                return false;
            }
            FragmentManager fragmentManager = getParentFragmentManager();
            fragmentManager.beginTransaction().addToBackStack(null).replace(R.id.FLFragmentContainer, new ContainerDetailFragment()).commit();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ContainersAdapter extends RecyclerView.Adapter<ViewHolder> {
        private final List<Container> data;

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public class ViewHolder extends RecyclerView.ViewHolder {
            private final ImageView imageView;
            private final ImageView menuButton;
            private final ImageView runButton;
            private final TextView title;

            private ViewHolder(View view) {
                super(view);
                this.imageView = (ImageView) view.findViewById(R.id.ImageView);
                this.title = (TextView) view.findViewById(R.id.TVTitle);
                this.runButton = (ImageView) view.findViewById(R.id.BTRun);
                this.menuButton = (ImageView) view.findViewById(R.id.BTMenu);
            }
        }

        public ContainersAdapter(List<Container> data) {
            this.data = data;
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public final ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.container_list_item, parent, false));
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public void onBindViewHolder(ViewHolder holder, int position) {
            final Container item = this.data.get(position);
            holder.imageView.setImageResource(R.drawable.icon_container);
            holder.title.setText(item.getName());
            holder.runButton.setOnClickListener(new View.OnClickListener() { // from class: com.winlator.ContainersFragment$ContainersAdapter$$ExternalSyntheticLambda1
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    ContainersFragment.ContainersAdapter.this.lambda$onBindViewHolder$0(item, view);
                }
            });
            holder.menuButton.setOnClickListener(new View.OnClickListener() { // from class: com.winlator.ContainersFragment$ContainersAdapter$$ExternalSyntheticLambda0
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    ContainersFragment.ContainersAdapter.this.lambda$onBindViewHolder$1(item, view);
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$onBindViewHolder$0(Container item, View view) {
            runContainer(item);
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public final int getItemCount() {
            return this.data.size();
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* renamed from: showListItemMenu, reason: merged with bridge method [inline-methods] */
        public void lambda$onBindViewHolder$1(View anchorView, final Container container) {
            final MainActivity activity = (MainActivity) ContainersFragment.this.getActivity();
            PopupMenu listItemMenu = new PopupMenu(activity, anchorView);
            listItemMenu.inflate(R.menu.container_popup_menu);
            if (Build.VERSION.SDK_INT >= 29) {
                listItemMenu.setForceShowIcon(true);
            }
            listItemMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() { // from class: com.winlator.ContainersFragment$ContainersAdapter$$ExternalSyntheticLambda2
                @Override // android.widget.PopupMenu.OnMenuItemClickListener
                public final boolean onMenuItemClick(MenuItem menuItem) {
                    boolean lambda$showListItemMenu$6;
                    lambda$showListItemMenu$6 = ContainersFragment.ContainersAdapter.this.lambda$showListItemMenu$6(activity, container, menuItem);
                    return lambda$showListItemMenu$6;
                }
            });
            listItemMenu.show();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ boolean lambda$showListItemMenu$6(MainActivity activity, final Container container, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.menu_item_duplicate /* 2131296681 */:
                    ContentDialog.confirm(ContainersFragment.this.getContext(), R.string.do_you_want_to_duplicate_this_container, new Runnable() { // from class: com.winlator.ContainersFragment$ContainersAdapter$$ExternalSyntheticLambda5
                        @Override // java.lang.Runnable
                        public final void run() {
                            ContainersFragment.ContainersAdapter.this.lambda$showListItemMenu$3(container);
                        }
                    });
                    return true;
                case R.id.menu_item_edit /* 2131296682 */:
                    activity.showFragment(new ContainerDetailFragment(container.id));
                    return true;
                case R.id.menu_item_file_manager /* 2131296685 */:
                    activity.showFragment(new ContainerFileManagerFragment(container.id));
                    return true;
                case R.id.menu_item_info /* 2131296687 */:
                    new StorageInfoDialog(activity, container).show();
                    return true;
                case R.id.menu_item_remove /* 2131296698 */:
                    ContentDialog.confirm(ContainersFragment.this.getContext(), R.string.do_you_want_to_remove_this_container, new Runnable() { // from class: com.winlator.ContainersFragment$ContainersAdapter$$ExternalSyntheticLambda6
                        @Override // java.lang.Runnable
                        public final void run() {
                            ContainersFragment.ContainersAdapter.this.lambda$showListItemMenu$5(container);
                        }
                    });
                    return true;
                default:
                    return true;
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$showListItemMenu$3(Container container) {
            ContainersFragment.this.preloaderDialog.lambda$showOnUiThread$0(R.string.duplicating_container);
            ContainersFragment.this.manager.duplicateContainerAsync(container, new Runnable() { // from class: com.winlator.ContainersFragment$ContainersAdapter$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    ContainersFragment.ContainersAdapter.this.lambda$showListItemMenu$2();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$showListItemMenu$2() {
            ContainersFragment.this.preloaderDialog.close();
            ContainersFragment.this.loadContainersList();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$showListItemMenu$5(Container container) {
            ContainersFragment.this.preloaderDialog.lambda$showOnUiThread$0(R.string.removing_container);
            ContainersFragment.this.manager.removeContainerAsync(container, new Runnable() { // from class: com.winlator.ContainersFragment$ContainersAdapter$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    ContainersFragment.ContainersAdapter.this.lambda$showListItemMenu$4();
                }
            });
        }

        /* JADX INFO: Access modifiers changed from: private */
        public /* synthetic */ void lambda$showListItemMenu$4() {
            ContainersFragment.this.preloaderDialog.close();
            ContainersFragment.this.loadContainersList();
        }

        private void runContainer(Container container) {
            Activity activity = ContainersFragment.this.getActivity();
            Intent intent = new Intent(activity, (Class<?>) XServerDisplayActivity.class);
            intent.putExtra("container_id", container.id);
            activity.startActivity(intent);
        }
    }
}
