.class public Lcom/winlator/renderer/PerfStats;
.super Ljava/lang/Object;
.source "PerfStats.java"

.field public static updates:I
.field public static renders:I
.field public static uploads:I
.field public static n:I
.field public static upT0:J
.field public static upSum:J
.field public static upMax:J

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
    .locals 2
    sget v0, Lcom/winlator/renderer/PerfStats;->renders:I
    add-int/lit8 v0, v0, 0x1
    sput v0, Lcom/winlator/renderer/PerfStats;->renders:I

    sget v0, Lcom/winlator/renderer/PerfStats;->n:I
    add-int/lit8 v0, v0, 0x1
    sput v0, Lcom/winlator/renderer/PerfStats;->n:I
    const/16 v1, 0x20
    if-lt v0, v1, :done

    invoke-static {}, Lcom/winlator/renderer/PerfStats;->logNow()V

    :done
    return-void
.end method

.method public static onUploadStart()V
    .locals 2
    invoke-static {}, Landroid/os/SystemClock;->elapsedRealtimeNanos()J
    move-result-wide v0
    sput-wide v0, Lcom/winlator/renderer/PerfStats;->upT0:J
    return-void
.end method

.method public static onUploadDone()V
    .locals 10
    invoke-static {}, Landroid/os/SystemClock;->elapsedRealtimeNanos()J
    move-result-wide v0

    sget-wide v2, Lcom/winlator/renderer/PerfStats;->upT0:J
    const-wide/16 v4, 0x0
    cmp-long v6, v2, v4
    if-nez v6, :done

    sub-long v2, v0, v2
    const-wide/16 v4, 0x0
    sput-wide v4, Lcom/winlator/renderer/PerfStats;->upT0:J

    sget-wide v4, Lcom/winlator/renderer/PerfStats;->upSum:J
    add-long/2addr v4, v2
    sput-wide v4, Lcom/winlator/renderer/PerfStats;->upSum:J

    sget-wide v4, Lcom/winlator/renderer/PerfStats;->upMax:J
    cmp-long v6, v2, v4
    if-lez v6, :nomax
    sput-wide v2, Lcom/winlator/renderer/PerfStats;->upMax:J

    :nomax
    sget v4, Lcom/winlator/renderer/PerfStats;->uploads:I
    add-int/lit8 v4, v4, 0x1
    sput v4, Lcom/winlator/renderer/PerfStats;->uploads:I

    :done
    return-void
.end method

.method public static declared-synchronized logNow()V
    .locals 9
    sget v0, Lcom/winlator/renderer/PerfStats;->uploads:I
    const/4 v1, 0x0
    if-lez v0, :noavg

    sget-wide v2, Lcom/winlator/renderer/PerfStats;->upSum:J
    int-to-long v4, v0
    div-long/2addr v2, v4
    const-wide/16 v4, 0x3e8
    div-long/2addr v2, v4
    long-to-int v1, v2

    :noavg
    sget-wide v2, Lcom/winlator/renderer/PerfStats;->upMax:J
    const-wide/16 v4, 0x3e8
    div-long/2addr v2, v4
    long-to-int v2, v2

    :try_start_0
    const-string v3, "/data/data/com.winlatcn/files/counters.bin"
    new-instance v4, Ljava/io/FileOutputStream;
    invoke-direct {v4, v3}, Ljava/io/FileOutputStream;-><init>(Ljava/lang/String;)V
    new-instance v5, Ljava/io/DataOutputStream;
    invoke-direct {v5, v4}, Ljava/io/DataOutputStream;-><init>(Ljava/io/OutputStream;)V

    sget v6, Lcom/winlator/renderer/PerfStats;->renders:I
    invoke-virtual {v5, v6}, Ljava/io/DataOutputStream;->writeInt(I)V

    sget v6, Lcom/winlator/renderer/PerfStats;->updates:I
    invoke-virtual {v5, v6}, Ljava/io/DataOutputStream;->writeInt(I)V

    sget v6, Lcom/winlator/renderer/PerfStats;->uploads:I
    invoke-virtual {v5, v6}, Ljava/io/DataOutputStream;->writeInt(I)V

    invoke-virtual {v5, v1}, Ljava/io/DataOutputStream;->writeInt(I)V

    invoke-virtual {v5, v2}, Ljava/io/DataOutputStream;->writeInt(I)V

    invoke-virtual {v5}, Ljava/io/DataOutputStream;->close()V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0
    goto :reset

    :catch_0
    move-exception v3

    :reset
    const/4 v0, 0x0
    sput v0, Lcom/winlator/renderer/PerfStats;->n:I
    sput v0, Lcom/winlator/renderer/PerfStats;->uploads:I

    const-wide/16 v2, 0x0
    sput-wide v2, Lcom/winlator/renderer/PerfStats;->upSum:J
    sput-wide v2, Lcom/winlator/renderer/PerfStats;->upMax:J

    return-void
.end method
