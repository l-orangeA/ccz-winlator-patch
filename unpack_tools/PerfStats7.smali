.class public Lcom/winlator/renderer/PerfStats;
.super Ljava/lang/Object;
.source "PerfStats.java"

.field public static updates:I
.field public static renders:I

.method public constructor <init>()V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public static onUpdate()V
    .locals 4
    sget v0, Lcom/winlator/renderer/PerfStats;->updates:I
    add-int/lit8 v0, v0, 0x1
    sput v0, Lcom/winlator/renderer/PerfStats;->updates:I

    :try_start_u
    const-string v1, "/data/data/com.winlatcn/files/upd_cnt.bin"
    new-instance v2, Ljava/io/FileOutputStream;
    invoke-direct {v2, v1}, Ljava/io/FileOutputStream;-><init>(Ljava/lang/String;)V
    new-instance v3, Ljava/io/DataOutputStream;
    invoke-direct {v3, v2}, Ljava/io/DataOutputStream;-><init>(Ljava/io/OutputStream;)V
    invoke-virtual {v3, v0}, Ljava/io/DataOutputStream;->writeInt(I)V
    invoke-virtual {v3}, Ljava/io/DataOutputStream;->close()V
    :try_end_u
    .catch Ljava/lang/Exception; {:try_start_u .. :try_end_u} :catch_u
    goto :after_u

    :catch_u
    move-exception v1

    :after_u
    return-void
.end method

.method public static onRender()V
    .locals 4
    sget v0, Lcom/winlator/renderer/PerfStats;->renders:I
    add-int/lit8 v0, v0, 0x1
    sput v0, Lcom/winlator/renderer/PerfStats;->renders:I

    :try_start_r
    const-string v1, "/data/data/com.winlatcn/files/ren_cnt.bin"
    new-instance v2, Ljava/io/FileOutputStream;
    invoke-direct {v2, v1}, Ljava/io/FileOutputStream;-><init>(Ljava/lang/String;)V
    new-instance v3, Ljava/io/DataOutputStream;
    invoke-direct {v3, v2}, Ljava/io/DataOutputStream;-><init>(Ljava/io/OutputStream;)V
    invoke-virtual {v3, v0}, Ljava/io/DataOutputStream;->writeInt(I)V
    invoke-virtual {v3}, Ljava/io/DataOutputStream;->close()V
    :try_end_r
    .catch Ljava/lang/Exception; {:try_start_r .. :try_end_r} :catch_r
    goto :after_r

    :catch_r
    move-exception v1

    :after_r
    return-void
.end method

.method public static onUploadStart()V
    .locals 0
    return-void
.end method

.method public static onUploadDone()V
    .locals 0
    return-void
.end method
