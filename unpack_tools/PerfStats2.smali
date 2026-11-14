.class public Lcom/winlator/renderer/PerfStats;
.super Ljava/lang/Object;
.source "PerfStats.java"

# render probe: blit(update) vs draw vs texture-upload timing -> files/perf.txt

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
    .locals 10
    invoke-static {}, Landroid/os/SystemClock;->uptimeMillis()J
    move-result-wide v0

    sget-wide v2, Lcom/winlator/renderer/PerfStats;->lastUpdate:J
    const-wide/16 v4, 0x0
    cmp-long v6, v2, v4
    if-nez v6, :nogap

    sub-long v4, v0, v2
    const-wide/16 v6, 0x19
    cmp-long v8, v4, v6
    if-gtz v8, :notslow

    sget v6, Lcom/winlator/renderer/PerfStats;->slowU:I
    add-int/lit8 v6, v6, 0x1
    sput v6, Lcom/winlator/renderer/PerfStats;->slowU:I

    :notslow
    sget-wide v6, Lcom/winlator/renderer/PerfStats;->maxGapU:J
    cmp-long v8, v4, v6
    if-gtz v8, :nogap

    sput-wide v4, Lcom/winlator/renderer/PerfStats;->maxGapU:J

    :nogap
    sput-wide v0, Lcom/winlator/renderer/PerfStats;->lastUpdate:J

    sget v4, Lcom/winlator/renderer/PerfStats;->updates:I
    add-int/lit8 v4, v4, 0x1
    sput v4, Lcom/winlator/renderer/PerfStats;->updates:I

    invoke-static {}, Lcom/winlator/renderer/PerfStats;->logIfDue()V
    return-void
.end method

.method public static onRender()V
    .locals 10
    invoke-static {}, Landroid/os/SystemClock;->uptimeMillis()J
    move-result-wide v0

    sget-wide v2, Lcom/winlator/renderer/PerfStats;->lastRender:J
    const-wide/16 v4, 0x0
    cmp-long v6, v2, v4
    if-nez v6, :nogap

    sub-long v4, v0, v2
    const-wide/16 v6, 0x19
    cmp-long v8, v4, v6
    if-gtz v8, :notslow

    sget v6, Lcom/winlator/renderer/PerfStats;->slowR:I
    add-int/lit8 v6, v6, 0x1
    sput v6, Lcom/winlator/renderer/PerfStats;->slowR:I

    :notslow
    sget-wide v6, Lcom/winlator/renderer/PerfStats;->maxGapR:J
    cmp-long v8, v4, v6
    if-gtz v8, :nogap

    sput-wide v4, Lcom/winlator/renderer/PerfStats;->maxGapR:J

    :nogap
    sput-wide v0, Lcom/winlator/renderer/PerfStats;->lastRender:J

    sget v4, Lcom/winlator/renderer/PerfStats;->renders:I
    add-int/lit8 v4, v4, 0x1
    sput v4, Lcom/winlator/renderer/PerfStats;->renders:I

    invoke-static {}, Lcom/winlator/renderer/PerfStats;->logIfDue()V
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
    .locals 12
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
    if-gtz v6, :nomax
    sput-wide v2, Lcom/winlator/renderer/PerfStats;->upMax:J

    :nomax
    const-wide/32 v4, 0x4c4b40
    cmp-long v6, v2, v4
    if-gtz v6, :nobig

    sget v6, Lcom/winlator/renderer/PerfStats;->bigUp:I
    add-int/lit8 v6, v6, 0x1
    sput v6, Lcom/winlator/renderer/PerfStats;->bigUp:I

    :nobig
    sget v4, Lcom/winlator/renderer/PerfStats;->uploads:I
    add-int/lit8 v4, v4, 0x1
    sput v4, Lcom/winlator/renderer/PerfStats;->uploads:I

    :done
    return-void
.end method

.method public static declared-synchronized logIfDue()V
    .locals 16
    invoke-static {}, Landroid/os/SystemClock;->uptimeMillis()J
    move-result-wide v0

    sget-wide v2, Lcom/winlator/renderer/PerfStats;->lastLog:J
    const-wide/16 v4, 0x0
    cmp-long v6, v2, v4
    if-nez v6, :setlast

    sub-long v4, v0, v2
    const-wide/16 v6, 0x3e8
    cmp-long v8, v4, v6
    if-gez v8, :done

    sput-wide v0, Lcom/winlator/renderer/PerfStats;->lastLog:J

    const-string v15, "CCZPROBE"
    const-string v9, "tick"
    invoke-static {v15, v9}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I

    const-wide/16 v6, 0x3e8
    div-long/2addr v4, v6

    sget v6, Lcom/winlator/renderer/PerfStats;->uploads:I
    const-wide/16 v10, 0x0
    if-gtz v6, :noavg

    sget-wide v10, Lcom/winlator/renderer/PerfStats;->upSum:J
    int-to-long v14, v6
    div-long/2addr v10, v14
    const-wide/16 v14, 0x3e8
    div-long/2addr v10, v14

    :noavg
    sget-wide v12, Lcom/winlator/renderer/PerfStats;->upMax:J
    const-wide/16 v14, 0x3e8
    div-long/2addr v12, v14

    const/16 v6, 0xb
    new-array v8, v6, [Ljava/lang/Object;

    const/4 v9, 0x0
    invoke-static {v4, v5}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;
    move-result-object v14
    aput-object v14, v8, v9

    const/4 v9, 0x1
    sget v6, Lcom/winlator/renderer/PerfStats;->updates:I
    invoke-static {v6}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v14
    aput-object v14, v8, v9

    const/4 v9, 0x2
    sget v6, Lcom/winlator/renderer/PerfStats;->renders:I
    invoke-static {v6}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v14
    aput-object v14, v8, v9

    const/4 v9, 0x3
    sget v6, Lcom/winlator/renderer/PerfStats;->uploads:I
    invoke-static {v6}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v14
    aput-object v14, v8, v9

    const/4 v9, 0x4
    invoke-static {v10, v11}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;
    move-result-object v14
    aput-object v14, v8, v9

    const/4 v9, 0x5
    invoke-static {v12, v13}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;
    move-result-object v14
    aput-object v14, v8, v9

    const/4 v9, 0x6
    sget v6, Lcom/winlator/renderer/PerfStats;->bigUp:I
    invoke-static {v6}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v14
    aput-object v14, v8, v9

    const/4 v9, 0x7
    sget v6, Lcom/winlator/renderer/PerfStats;->slowU:I
    invoke-static {v6}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v14
    aput-object v14, v8, v9

    const/16 v9, 0x8
    sget v6, Lcom/winlator/renderer/PerfStats;->slowR:I
    invoke-static {v6}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v14
    aput-object v14, v8, v9

    const/16 v9, 0x9
    sget-wide v6, Lcom/winlator/renderer/PerfStats;->maxGapU:J
    invoke-static {v6, v7}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;
    move-result-object v14
    aput-object v14, v8, v9

    const/16 v9, 0xa
    sget-wide v6, Lcom/winlator/renderer/PerfStats;->maxGapR:J
    invoke-static {v6, v7}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;
    move-result-object v14
    aput-object v14, v8, v9

    const-string v14, "t=%ds u=%d r=%d up=%d avg=%dus max=%dus big=%d slowU=%d slowR=%d gU=%d gR=%d\n"
    invoke-static {v14, v8}, Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
    move-result-object v14

    const-string v9, "CCZPROBE"
    invoke-static {v9, v14}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I

    :try_start_0
    const-string v15, "/data/data/com.winlatcn/files/perf.txt"
    new-instance v9, Ljava/io/FileOutputStream;
    const/4 v6, 0x1
    invoke-direct {v9, v15, v6}, Ljava/io/FileOutputStream;-><init>(Ljava/lang/String;Z)V
    invoke-virtual {v14}, Ljava/lang/String;->getBytes()[B
    move-result-object v15
    invoke-virtual {v9, v15}, Ljava/io/FileOutputStream;->write([B)V
    invoke-virtual {v9}, Ljava/io/OutputStream;->close()V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0
    goto :reset

    :catch_0
    move-exception v15

    const-string v9, "writefail"
    invoke-static {v9, v15}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/Throwable;)I

    :reset
    const/4 v6, 0x0
    sput v6, Lcom/winlator/renderer/PerfStats;->updates:I
    sput v6, Lcom/winlator/renderer/PerfStats;->renders:I
    sput v6, Lcom/winlator/renderer/PerfStats;->uploads:I
    sput v6, Lcom/winlator/renderer/PerfStats;->slowU:I
    sput v6, Lcom/winlator/renderer/PerfStats;->slowR:I
    sput v6, Lcom/winlator/renderer/PerfStats;->bigUp:I

    const-wide/16 v6, 0x0
    sput-wide v6, Lcom/winlator/renderer/PerfStats;->upSum:J
    sput-wide v6, Lcom/winlator/renderer/PerfStats;->upMax:J
    sput-wide v6, Lcom/winlator/renderer/PerfStats;->maxGapU:J
    sput-wide v6, Lcom/winlator/renderer/PerfStats;->maxGapR:J

    :done
    return-void

    :setlast
    sput-wide v0, Lcom/winlator/renderer/PerfStats;->lastLog:J
    return-void
.end method
