package com.winlator.contentdialog;

import android.widget.TextView;
import com.winlator.container.FileInfo;
import com.winlator.core.StringUtils;
import java.util.concurrent.atomic.AtomicLong;

/* loaded from: classes.dex */
public class FileInfoDialog extends ContentDialog {
    /* JADX WARN: Illegal instructions before constructor call */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public FileInfoDialog(android.content.Context r22, com.winlator.container.FileInfo r23, com.winlator.container.Container r24) {
        /*
            Method dump skipped, instructions count: 488
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.winlator.contentdialog.FileInfoDialog.<init>(android.content.Context, com.winlator.container.FileInfo, com.winlator.container.Container):void");
    }

    /* renamed from: com.winlator.contentdialog.FileInfoDialog$1, reason: invalid class name */
    /* loaded from: classes.dex */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$winlator$container$FileInfo$Type;

        static {
            int[] iArr = new int[FileInfo.Type.values().length];
            $SwitchMap$com$winlator$container$FileInfo$Type = iArr;
            try {
                iArr[FileInfo.Type.FILE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$winlator$container$FileInfo$Type[FileInfo.Type.DRIVE.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$winlator$container$FileInfo$Type[FileInfo.Type.DIRECTORY.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$new$1(final AtomicLong totalSize, AtomicLong lastTime, final TextView tvValue, Long size) {
        totalSize.addAndGet(size.longValue());
        long currTime = System.currentTimeMillis();
        int elapsedTime = (int) (currTime - lastTime.get());
        if (lastTime.get() == 0 || elapsedTime > 30) {
            tvValue.post(new Runnable() { // from class: com.winlator.contentdialog.FileInfoDialog$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    FileInfoDialog.lambda$new$0(tvValue, totalSize);
                }
            });
            lastTime.set(currTime);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void lambda$new$0(TextView tvValue, AtomicLong totalSize) {
        tvValue.setText(StringUtils.formatBytes(totalSize.get()));
    }
}
