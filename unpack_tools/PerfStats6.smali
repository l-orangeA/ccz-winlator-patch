.class public Lcom/winlator/renderer/PerfStats;
.super Ljava/lang/Object;
.source "PerfStats.java"

.field public static updates:I
.field public static renders:I
.field public static n:I

.method public constructor <init>()V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public static onUpdate()V
    .locals 1
    sget v0, Lcom/winlator/renderer/PerfStats;->updates:I
    add-int/lit8 v0, v0, 0x1
    sput v0, Lcom/winlator/renderer/PerfStats;->updates:I
    return-void
.end method

.method public static onRender()V
    .locals 4
    sget v0, Lcom/winlator/renderer/PerfStats;->renders:I
    add-int/lit8 v0, v0, 0x1
    sput v0, Lcom/winlator/renderer/PerfStats;->renders:I

    :try_start_c
    const-string v1, "/data/data/com.winlatcn/files/perf_cnt.bin"
    new-instance v2, Ljava/io/FileOutputStream;
    invoke-direct {v2, v1}, Ljava/io/FileOutputStream;-><init>(Ljava/lang/String;)V
    and-int/lit16 v1, v0, 0xff
    invoke-virtual {v2, v1}, Ljava/io/OutputStream;->write(I)V
    invoke-virtual {v2}, Ljava/io/OutputStream;->close()V
    :try_end_c
    .catch Ljava/lang/Exception; {:try_start_c .. :try_end_c} :catch_c
    goto :after_c

    :catch_c
    move-exception v1

    :after_c
    :try_start_r
    const-string v1, "/data/data/com.winlatcn/files/perf_raw.txt"
    new-instance v2, Ljava/io/FileOutputStream;
    invoke-direct {v2, v1}, Ljava/io/FileOutputStream;-><init>(Ljava/lang/String;)V
    const-string v1, "x"
    invoke-virtual {v1}, Ljava/lang/String;->getBytes()[B
    move-result-object v1
    invoke-virtual {v2, v1}, Ljava/io/OutputStream;->write([B)V
    invoke-virtual {v2}, Ljava/io/OutputStream;->close()V
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
