.class public Lcom/winlator/renderer/PerfStats;
.super Ljava/lang/Object;
.source "PerfStats.java"

.field public static updates:I
.field public static renders:I
.field public static uploads:I
.field public static lastUpdate:J
.field public static maxGapU:J
.field public static lastRender:J
.field public static maxGapR:J
.field public static lastLog:J
.field public static upT0:J
.field public static upSum:J
.field public static upMax:J
.field public static slowU:I
.field public static slowR:I
.field public static bigUp:I

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
    .locals 1
    sget v0, Lcom/winlator/renderer/PerfStats;->renders:I
    add-int/lit8 v0, v0, 0x1
    sput v0, Lcom/winlator/renderer/PerfStats;->renders:I
    invoke-static {}, Lcom/winlator/renderer/PerfStats;->logIfDue()V
    return-void
.end method

.method public static onUploadStart()V
    .locals 0
    return-void
.end method

.method public static onUploadDone()V
    .locals 1
    sget v0, Lcom/winlator/renderer/PerfStats;->uploads:I
    add-int/lit8 v0, v0, 0x1
    sput v0, Lcom/winlator/renderer/PerfStats;->uploads:I
    return-void
.end method

.method public static declared-synchronized logIfDue()V
    .locals 3
    :try_start_0
    const-string v0, "/data/data/com.winlatcn/files/perf.txt"
    new-instance v1, Ljava/io/FileOutputStream;
    invoke-direct {v1, v0}, Ljava/io/FileOutputStream;-><init>(Ljava/lang/String;)V
    const-string v0, "tick\n"
    invoke-virtual {v0}, Ljava/lang/String;->getBytes()[B
    move-result-object v0
    invoke-virtual {v1, v0}, Ljava/io/OutputStream;->write([B)V
    invoke-virtual {v1}, Ljava/io/OutputStream;->close()V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0
    goto :done

    :catch_0
    move-exception v0

    :done
    return-void
.end method
