package com.winlator.contentdialog;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import com.winlatcn.R;
import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import java.util.ArrayList;
import java.util.Iterator;

/* loaded from: classes.dex */
public class CreateFolderDialog extends ContentDialog {
    private OnCreateFolderListener onCreateFolderListener;

    /* loaded from: classes.dex */
    public interface OnCreateFolderListener {
        void onCreateFolder(Container container, String str);
    }

    public CreateFolderDialog(ContainerManager manager) {
        super(manager.getContext(), R.layout.create_folder_dialog);
        Context context = manager.getContext();
        final Spinner sContainer = (Spinner) findViewById(R.id.SContainer);
        final EditText etName = (EditText) findViewById(R.id.ETName);
        setTitle(context.getString(R.string.new_folder));
        final ArrayList<Container> containers = manager.getContainers();
        ArrayList<String> items = new ArrayList<>();
        Iterator<Container> it = containers.iterator();
        while (it.hasNext()) {
            Container container = it.next();
            items.add(container.getName());
        }
        sContainer.setAdapter((SpinnerAdapter) new ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, items));
        setOnConfirmCallback(new Runnable() { // from class: com.winlator.contentdialog.CreateFolderDialog$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                CreateFolderDialog.this.lambda$new$0(etName, containers, sContainer);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$0(EditText etName, ArrayList containers, Spinner sContainer) {
        String name = etName.getText().toString().trim();
        Container container = (Container) containers.get(sContainer.getSelectedItemPosition());
        if (this.onCreateFolderListener == null || name.isEmpty()) {
            return;
        }
        this.onCreateFolderListener.onCreateFolder(container, name);
    }

    public void setOnCreateFolderListener(OnCreateFolderListener onCreateFolderListener) {
        this.onCreateFolderListener = onCreateFolderListener;
    }
}
