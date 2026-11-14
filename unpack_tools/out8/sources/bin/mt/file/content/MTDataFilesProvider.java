package bin.mt.file.content;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsProvider;
import android.system.Os;
import android.system.StructStat;
import android.webkit.MimeTypeMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/* loaded from: classes2.dex */
public class MTDataFilesProvider extends DocumentsProvider {
    public static final String[] g = {"root_id", "mime_types", "flags", "icon", "title", "summary", "document_id"};
    public static final String[] h = {"document_id", "mime_type", "_display_name", "last_modified", "flags", "_size", "mt_extras"};
    public String b;
    public File c;
    public File d;
    public File e;
    public File f;

    /* JADX WARN: Removed duplicated region for block: B:14:0x002f  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static boolean a(java.io.File r5) {
        /*
            boolean r0 = r5.isDirectory()
            if (r0 == 0) goto L3b
            r0 = 0
            java.lang.String r1 = r5.getPath()     // Catch: android.system.ErrnoException -> L1e
            android.system.StructStat r1 = android.system.Os.lstat(r1)     // Catch: android.system.ErrnoException -> L1e
            int r1 = r1.st_mode     // Catch: android.system.ErrnoException -> L1e
            r2 = 61440(0xf000, float:8.6096E-41)
            r1 = r1 & r2
            r2 = 40960(0xa000, float:5.7397E-41)
            if (r1 != r2) goto L1c
            r1 = 1
            goto L23
        L1c:
            r1 = 0
            goto L23
        L1e:
            r1 = move-exception
            r1.printStackTrace()
            goto L1c
        L23:
            if (r1 != 0) goto L3b
            java.io.File[] r1 = r5.listFiles()
            if (r1 == 0) goto L3b
            int r2 = r1.length
            r3 = 0
        L2d:
            if (r3 >= r2) goto L3b
            r4 = r1[r3]
            boolean r4 = a(r4)
            if (r4 != 0) goto L38
            return r0
        L38:
            int r3 = r3 + 1
            goto L2d
        L3b:
            boolean r5 = r5.delete()
            return r5
        */
        throw new UnsupportedOperationException("Method not decompiled: bin.mt.file.content.MTDataFilesProvider.a(java.io.File):boolean");
    }

    public static String c(File file) {
        if (file.isDirectory()) {
            return "vnd.android.document/directory";
        }
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(46);
        if (lastIndexOf < 0) {
            return "application/octet-stream";
        }
        String mimeTypeFromExtension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substring(lastIndexOf + 1).toLowerCase());
        return mimeTypeFromExtension != null ? mimeTypeFromExtension : "application/octet-stream";
    }

    @Override // android.provider.DocumentsProvider, android.content.ContentProvider
    public final void attachInfo(Context context, ProviderInfo providerInfo) {
        super.attachInfo(context, providerInfo);
        this.b = context.getPackageName();
        File parentFile = context.getFilesDir().getParentFile();
        this.c = parentFile;
        String path = parentFile.getPath();
        if (path.startsWith("/data/user/")) {
            this.d = new File("/data/user_de/" + path.substring(11));
        }
        File externalFilesDir = context.getExternalFilesDir(null);
        if (externalFilesDir != null) {
            this.e = externalFilesDir.getParentFile();
        }
        this.f = context.getObbDir();
    }

    public final File b(String str, boolean z) {
        String substring;
        if (!str.startsWith(this.b)) {
            throw new FileNotFoundException(str.concat(" not found"));
        }
        String substring2 = str.substring(this.b.length());
        if (substring2.startsWith("/")) {
            substring2 = substring2.substring(1);
        }
        File file = null;
        if (substring2.isEmpty()) {
            return null;
        }
        int indexOf = substring2.indexOf(47);
        if (indexOf == -1) {
            substring = "";
        } else {
            String substring3 = substring2.substring(0, indexOf);
            substring = substring2.substring(indexOf + 1);
            substring2 = substring3;
        }
        if (substring2.equalsIgnoreCase("data")) {
            file = new File(this.c, substring);
        } else if (substring2.equalsIgnoreCase("android_data") && this.e != null) {
            file = new File(this.e, substring);
        } else if (substring2.equalsIgnoreCase("android_obb") && this.f != null) {
            file = new File(this.f, substring);
        } else if (substring2.equalsIgnoreCase("user_de_data") && this.d != null) {
            file = new File(this.d, substring);
        }
        if (file == null) {
            throw new FileNotFoundException(str.concat(" not found"));
        }
        if (z) {
            try {
                Os.lstat(file.getPath());
            } catch (Exception unused) {
                throw new FileNotFoundException(str.concat(" not found"));
            }
        }
        return file;
    }

    /* JADX WARN: Removed duplicated region for block: B:23:0x0074  */
    /* JADX WARN: Removed duplicated region for block: B:46:0x00c8 A[Catch: Exception -> 0x00dd, TryCatch #1 {Exception -> 0x00dd, blocks: (B:10:0x001d, B:12:0x0031, B:13:0x0036, B:14:0x003e, B:25:0x0078, B:26:0x007f, B:28:0x0083, B:30:0x0089, B:31:0x008d, B:33:0x0093, B:36:0x009f, B:37:0x00a7, B:40:0x00ae, B:42:0x00b4, B:45:0x00c0, B:46:0x00c8, B:49:0x00cf, B:50:0x0053, B:53:0x005d, B:56:0x0067, B:59:0x0039), top: B:9:0x001d, inners: #0, #2 }] */
    @Override // android.provider.DocumentsProvider, android.content.ContentProvider
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public final android.os.Bundle call(java.lang.String r10, java.lang.String r11, android.os.Bundle r12) {
        /*
            r9 = this;
            java.lang.String r0 = "message"
            java.lang.String r1 = "result"
            java.lang.String r2 = "Unsupported method: "
            android.os.Bundle r11 = super.call(r10, r11, r12)
            if (r11 == 0) goto Ld
            return r11
        Ld:
            java.lang.String r11 = "mt:"
            boolean r11 = r10.startsWith(r11)
            if (r11 != 0) goto L17
            r10 = 0
            return r10
        L17:
            android.os.Bundle r11 = new android.os.Bundle
            r11.<init>()
            r3 = 0
            java.lang.String r4 = "uri"
            android.os.Parcelable r4 = r12.getParcelable(r4)     // Catch: java.lang.Exception -> Ldd
            android.net.Uri r4 = (android.net.Uri) r4     // Catch: java.lang.Exception -> Ldd
            java.util.List r4 = r4.getPathSegments()     // Catch: java.lang.Exception -> Ldd
            int r5 = r4.size()     // Catch: java.lang.Exception -> Ldd
            r6 = 4
            r7 = 1
            if (r5 < r6) goto L39
            r5 = 3
            java.lang.Object r4 = r4.get(r5)     // Catch: java.lang.Exception -> Ldd
        L36:
            java.lang.String r4 = (java.lang.String) r4     // Catch: java.lang.Exception -> Ldd
            goto L3e
        L39:
            java.lang.Object r4 = r4.get(r7)     // Catch: java.lang.Exception -> Ldd
            goto L36
        L3e:
            int r5 = r10.hashCode()     // Catch: java.lang.Exception -> Ldd
            r6 = -1645162251(0xffffffff9df0d0f5, float:-6.3743528E-21)
            r8 = 2
            if (r5 == r6) goto L67
            r6 = 214442514(0xcc82212, float:3.0835384E-31)
            if (r5 == r6) goto L5d
            r6 = 1713485102(0x6621b52e, float:1.909108E23)
            if (r5 == r6) goto L53
            goto L71
        L53:
            java.lang.String r5 = "mt:setLastModified"
            boolean r5 = r10.equals(r5)     // Catch: java.lang.Exception -> Ldd
            if (r5 == 0) goto L71
            r5 = 0
            goto L72
        L5d:
            java.lang.String r5 = "mt:createSymlink"
            boolean r5 = r10.equals(r5)     // Catch: java.lang.Exception -> Ldd
            if (r5 == 0) goto L71
            r5 = 2
            goto L72
        L67:
            java.lang.String r5 = "mt:setPermissions"
            boolean r5 = r10.equals(r5)     // Catch: java.lang.Exception -> Ldd
            if (r5 == 0) goto L71
            r5 = 1
            goto L72
        L71:
            r5 = -1
        L72:
            if (r5 == 0) goto Lc8
            if (r5 == r7) goto La7
            if (r5 == r8) goto L83
            r11.putBoolean(r1, r3)     // Catch: java.lang.Exception -> Ldd
            java.lang.String r10 = r2.concat(r10)     // Catch: java.lang.Exception -> Ldd
        L7f:
            r11.putString(r0, r10)     // Catch: java.lang.Exception -> Ldd
            goto Le8
        L83:
            java.io.File r10 = r9.b(r4, r3)     // Catch: java.lang.Exception -> Ldd
            if (r10 != 0) goto L8d
        L89:
            r11.putBoolean(r1, r3)     // Catch: java.lang.Exception -> Ldd
            goto Le8
        L8d:
            java.lang.String r2 = "path"
            java.lang.String r12 = r12.getString(r2)     // Catch: java.lang.Exception -> Ldd
            java.lang.String r10 = r10.getPath()     // Catch: android.system.ErrnoException -> L9e java.lang.Exception -> Ldd
            android.system.Os.symlink(r12, r10)     // Catch: android.system.ErrnoException -> L9e java.lang.Exception -> Ldd
            r11.putBoolean(r1, r7)     // Catch: android.system.ErrnoException -> L9e java.lang.Exception -> Ldd
            goto Le8
        L9e:
            r10 = move-exception
            r11.putBoolean(r1, r3)     // Catch: java.lang.Exception -> Ldd
            java.lang.String r10 = r10.getMessage()     // Catch: java.lang.Exception -> Ldd
            goto L7f
        La7:
            java.io.File r10 = r9.b(r4, r7)     // Catch: java.lang.Exception -> Ldd
            if (r10 != 0) goto Lae
            goto L89
        Lae:
            java.lang.String r2 = "permissions"
            int r12 = r12.getInt(r2)     // Catch: java.lang.Exception -> Ldd
            java.lang.String r10 = r10.getPath()     // Catch: android.system.ErrnoException -> Lbf java.lang.Exception -> Ldd
            android.system.Os.chmod(r10, r12)     // Catch: android.system.ErrnoException -> Lbf java.lang.Exception -> Ldd
            r11.putBoolean(r1, r7)     // Catch: android.system.ErrnoException -> Lbf java.lang.Exception -> Ldd
            goto Le8
        Lbf:
            r10 = move-exception
            r11.putBoolean(r1, r3)     // Catch: java.lang.Exception -> Ldd
            java.lang.String r10 = r10.getMessage()     // Catch: java.lang.Exception -> Ldd
            goto L7f
        Lc8:
            java.io.File r10 = r9.b(r4, r7)     // Catch: java.lang.Exception -> Ldd
            if (r10 != 0) goto Lcf
            goto L89
        Lcf:
            java.lang.String r2 = "time"
            long r4 = r12.getLong(r2)     // Catch: java.lang.Exception -> Ldd
            boolean r10 = r10.setLastModified(r4)     // Catch: java.lang.Exception -> Ldd
            r11.putBoolean(r1, r10)     // Catch: java.lang.Exception -> Ldd
            goto Le8
        Ldd:
            r10 = move-exception
            r11.putBoolean(r1, r3)
            java.lang.String r10 = r10.toString()
            r11.putString(r0, r10)
        Le8:
            return r11
        */
        throw new UnsupportedOperationException("Method not decompiled: bin.mt.file.content.MTDataFilesProvider.call(java.lang.String, java.lang.String, android.os.Bundle):android.os.Bundle");
    }

    @Override // android.provider.DocumentsProvider
    public final String createDocument(String str, String str2, String str3) {
        StringBuilder sb;
        File b = b(str, true);
        if (b != null) {
            File file = new File(b, str3);
            int i = 2;
            while (file.exists()) {
                file = new File(b, str3 + " (" + i + ")");
                i++;
            }
            try {
                if ("vnd.android.document/directory".equals(str2) ? file.mkdir() : file.createNewFile()) {
                    if (str.endsWith("/")) {
                        sb = new StringBuilder();
                        sb.append(str);
                        sb.append(file.getName());
                    } else {
                        sb = new StringBuilder();
                        sb.append(str);
                        sb.append("/");
                        sb.append(file.getName());
                    }
                    str = sb.toString();
                    return str;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        throw new FileNotFoundException("Failed to create document in " + str + " with name " + str3);
    }

    public final void d(MatrixCursor matrixCursor, String str, File file) {
        int i;
        String name;
        if (file == null) {
            file = b(str, true);
        }
        boolean z = false;
        if (file == null) {
            MatrixCursor.RowBuilder newRow = matrixCursor.newRow();
            newRow.add("document_id", this.b);
            newRow.add("_display_name", this.b);
            newRow.add("_size", 0L);
            newRow.add("mime_type", "vnd.android.document/directory");
            newRow.add("last_modified", 0);
            newRow.add("flags", 0);
            return;
        }
        if (file.isDirectory()) {
            if (file.canWrite()) {
                i = 8;
            }
            i = 0;
        } else {
            if (file.canWrite()) {
                i = 2;
            }
            i = 0;
        }
        if (file.getParentFile().canWrite()) {
            i = i | 4 | 64;
        }
        String path = file.getPath();
        if (path.equals(this.c.getPath())) {
            name = "data";
        } else {
            File file2 = this.e;
            if (file2 == null || !path.equals(file2.getPath())) {
                File file3 = this.f;
                if (file3 == null || !path.equals(file3.getPath())) {
                    File file4 = this.d;
                    if (file4 == null || !path.equals(file4.getPath())) {
                        name = file.getName();
                        z = true;
                    } else {
                        name = "user_de_data";
                    }
                } else {
                    name = "android_obb";
                }
            } else {
                name = "android_data";
            }
        }
        MatrixCursor.RowBuilder newRow2 = matrixCursor.newRow();
        newRow2.add("document_id", str);
        newRow2.add("_display_name", name);
        newRow2.add("_size", Long.valueOf(file.length()));
        newRow2.add("mime_type", c(file));
        newRow2.add("last_modified", Long.valueOf(file.lastModified()));
        newRow2.add("flags", Integer.valueOf(i));
        newRow2.add("mt_path", file.getAbsolutePath());
        if (z) {
            try {
                StringBuilder sb = new StringBuilder();
                StructStat lstat = Os.lstat(path);
                sb.append(lstat.st_mode);
                sb.append("|");
                sb.append(lstat.st_uid);
                sb.append("|");
                sb.append(lstat.st_gid);
                if ((lstat.st_mode & 61440) == 40960) {
                    sb.append("|");
                    sb.append(Os.readlink(path));
                }
                newRow2.add("mt_extras", sb.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override // android.provider.DocumentsProvider
    public final void deleteDocument(String str) {
        File b = b(str, true);
        if (b == null || !a(b)) {
            throw new FileNotFoundException("Failed to delete document ".concat(str));
        }
    }

    @Override // android.provider.DocumentsProvider
    public final String getDocumentType(String str) {
        File b = b(str, true);
        return b == null ? "vnd.android.document/directory" : c(b);
    }

    @Override // android.provider.DocumentsProvider
    public final boolean isChildDocument(String str, String str2) {
        return str2.startsWith(str);
    }

    @Override // android.provider.DocumentsProvider
    public final String moveDocument(String str, String str2, String str3) {
        File b = b(str, true);
        File b2 = b(str3, true);
        if (b != null && b2 != null) {
            File file = new File(b2, b.getName());
            if (!file.exists() && b.renameTo(file)) {
                if (str3.endsWith("/")) {
                    return str3 + file.getName();
                }
                return str3 + "/" + file.getName();
            }
        }
        throw new FileNotFoundException("Filed to move document " + str + " to " + str3);
    }

    @Override // android.content.ContentProvider
    public final boolean onCreate() {
        return true;
    }

    @Override // android.provider.DocumentsProvider
    public final ParcelFileDescriptor openDocument(String str, String str2, CancellationSignal cancellationSignal) {
        File b = b(str, false);
        if (b != null) {
            return ParcelFileDescriptor.open(b, ParcelFileDescriptor.parseMode(str2));
        }
        throw new FileNotFoundException(str.concat(" not found"));
    }

    @Override // android.provider.DocumentsProvider
    public final Cursor queryChildDocuments(String str, String[] strArr, String str2) {
        if (str.endsWith("/")) {
            str = str.substring(0, str.length() - 1);
        }
        if (strArr == null) {
            strArr = h;
        }
        MatrixCursor matrixCursor = new MatrixCursor(strArr);
        File b = b(str, true);
        if (b == null) {
            d(matrixCursor, str.concat("/data"), this.c);
            File file = this.e;
            if (file != null && file.exists()) {
                d(matrixCursor, str.concat("/android_data"), this.e);
            }
            File file2 = this.f;
            if (file2 != null && file2.exists()) {
                d(matrixCursor, str.concat("/android_obb"), this.f);
            }
            File file3 = this.d;
            if (file3 != null && file3.exists()) {
                d(matrixCursor, str.concat("/user_de_data"), this.d);
            }
        } else {
            File[] listFiles = b.listFiles();
            if (listFiles != null) {
                for (File file4 : listFiles) {
                    d(matrixCursor, str + "/" + file4.getName(), file4);
                }
            }
        }
        return matrixCursor;
    }

    @Override // android.provider.DocumentsProvider
    public final Cursor queryDocument(String str, String[] strArr) {
        if (strArr == null) {
            strArr = h;
        }
        MatrixCursor matrixCursor = new MatrixCursor(strArr);
        d(matrixCursor, str, null);
        return matrixCursor;
    }

    @Override // android.provider.DocumentsProvider
    public final Cursor queryRoots(String[] strArr) {
        ApplicationInfo applicationInfo = getContext().getApplicationInfo();
        String charSequence = applicationInfo.loadLabel(getContext().getPackageManager()).toString();
        if (strArr == null) {
            strArr = g;
        }
        MatrixCursor matrixCursor = new MatrixCursor(strArr);
        MatrixCursor.RowBuilder newRow = matrixCursor.newRow();
        newRow.add("root_id", this.b);
        newRow.add("document_id", this.b);
        newRow.add("summary", this.b);
        newRow.add("flags", 17);
        newRow.add("title", charSequence);
        newRow.add("mime_types", "*/*");
        newRow.add("icon", Integer.valueOf(applicationInfo.icon));
        return matrixCursor;
    }

    @Override // android.provider.DocumentsProvider
    public final void removeDocument(String str, String str2) {
        deleteDocument(str);
    }

    @Override // android.provider.DocumentsProvider
    public final String renameDocument(String str, String str2) {
        File b = b(str, true);
        if (b == null || !b.renameTo(new File(b.getParentFile(), str2))) {
            throw new FileNotFoundException("Failed to rename document " + str + " to " + str2);
        }
        return str.substring(0, str.lastIndexOf(47, str.length() - 2)) + "/" + str2;
    }
}
