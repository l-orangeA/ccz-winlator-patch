package com.winlator.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.winlatcn.R;
import java.util.Arrays;
import java.util.List;

/* loaded from: classes.dex */
public class CPUListView extends LinearLayout {
    private List<String> checkedCPUList;
    private final byte numProcessors;

    public CPUListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CPUListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(0);
        this.numProcessors = (byte) Runtime.getRuntime().availableProcessors();
        refreshContent();
    }

    private void refreshContent() {
        removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (int i = 0; i < this.numProcessors; i++) {
            boolean z = false;
            View itemView = inflater.inflate(R.layout.cpu_list_item, (ViewGroup) this, false);
            String tag = "CPU" + i;
            CheckBox checkBox = (CheckBox) itemView.findViewById(R.id.CheckBox);
            checkBox.setTag(tag);
            List<String> list = this.checkedCPUList;
            if (list == null || list.contains(String.valueOf(i))) {
                z = true;
            }
            checkBox.setChecked(z);
            ((TextView) itemView.findViewById(R.id.TextView)).setText(tag);
            addView(itemView);
        }
    }

    public void setCheckedCPUList(String checkedCPUList) {
        this.checkedCPUList = Arrays.asList(checkedCPUList.split(","));
        refreshContent();
    }

    public String getCheckedCPUListAsString() {
        String cpuList = "";
        for (int i = 0; i < this.numProcessors; i++) {
            CheckBox checkBox = (CheckBox) findViewWithTag("CPU" + i);
            if (checkBox.isChecked()) {
                StringBuilder sb = new StringBuilder();
                sb.append(cpuList);
                sb.append(!cpuList.isEmpty() ? "," : "");
                sb.append(i);
                cpuList = sb.toString();
            }
        }
        return cpuList;
    }

    public boolean[] getCheckedCPUList() {
        boolean[] cpuList = new boolean[this.numProcessors];
        for (int i = 0; i < this.numProcessors; i++) {
            CheckBox checkBox = (CheckBox) findViewWithTag("CPU" + i);
            cpuList[i] = checkBox.isChecked();
        }
        return cpuList;
    }

    public byte getNumProcessors() {
        return this.numProcessors;
    }
}
