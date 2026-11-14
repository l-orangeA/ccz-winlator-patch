.class public Lcom/winlator/renderer/PerfStats;
.super Ljava/lang/Object;
.source "PerfStats.java"

# debug-only counters for frame/update/upload cadence (file: files/perf.txt)

.field public static updates:I
.field public static renders:I
.field public static uploads:I
.field public static bytes:J
.field public static lastUpdate:J
.field public static maxGapU:J
.field public static lastRender:J
.field public static maxGapR:J
.field public static lastLog:J

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
    if-eqz v6, :nogap

    sub-long v4, v0, v2
    sget-wide v6, Lcom/winlator/renderer/PerfStats;->maxGapU:J
    cmp-long v8, v4, v6
    if-lez v8, :nogap
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
    if-eqz v6, :nogap

    sub-long v4, v0, v2
    sget-wide v6, Lcom/winlator/renderer/PerfStats;->maxGapR:J
    cmp-long v8, v4, v6
    if-lez v8, :nogap
    sput-wide v4, Lcom/winlator/renderer/PerfStats;->maxGapR:J

    :nogap
    sput-wide v0, Lcom/winlator/renderer/PerfStats;->lastRender:J

    sget v4, Lcom/winlator/renderer/PerfStats;->renders:I
    add-int/lit8 v4, v4, 0x1
    sput v4, Lcom/winlator/renderer/PerfStats;->renders:I

    invoke-static {}, Lcom/winlator/renderer/PerfStats;->logIfDue()V
    return-void
.end method

.method public static onUpload(I)V
    .locals 4

    sget v0, Lcom/winlator/renderer/PerfStats;->uploads:I
    add-int/lit8 v0, v0, 0x1
    sput v0, Lcom/winlator/renderer/PerfStats;->uploads:I

    sget-wide v0, Lcom/winlator/renderer/PerfStats;->bytes:J
    int-to-long v2, p0
    add-long/2addr v0, v2
    sput-wide v0, Lcom/winlator/renderer/PerfStats;->bytes:J
    return-void
.end method

.method public static declared-synchronized logIfDue()V
    .locals 16

    invoke-static {}, Landroid/os/SystemClock;->uptimeMillis()J
    move-result-wide v0

    sget-wide v2, Lcom/winlator/renderer/PerfStats;->lastLog:J
    const-wide/16 v4, 0x0
    cmp-long v6, v2, v4
    if-eqz v6, :setlast

    sub-long v4, v0, v2
    const-wide/16 v6, 0x3e8
    cmp-long v8, v4, v6
    if-gez v8, :done

    sput-wide v0, Lcom/winlator/renderer/PerfStats;->lastLog:J

    const/4 v12, 0x6
    new-array v8, v12, [Ljava/lang/Object;

    const/4 v9, 0x0
    sget v10, Lcom/winlator/renderer/PerfStats;->updates:I
    invoke-static {v10}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v10
    aput-object v10, v8, v9

    const/4 v9, 0x1
    sget v10, Lcom/winlator/renderer/PerfStats;->renders:I
    invoke-static {v10}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v10
    aput-object v10, v8, v9

    const/4 v9, 0x2
    sget v10, Lcom/winlator/renderer/PerfStats;->uploads:I
    invoke-static {v10}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v10
    aput-object v10, v8, v9

    const/4 v9, 0x3
    sget-wide v10, Lcom/winlator/renderer/PerfStats;->bytes:J
    const-wide/16 v12, 0x400
    div-long/2addr v10, v12
    invoke-static {v10, v11}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;
    move-result-object v10
    aput-object v10, v8, v9

    const/4 v9, 0x4
    sget-wide v10, Lcom/winlator/renderer/PerfStats;->maxGapU:J
    invoke-static {v10, v11}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;
    move-result-object v10
    aput-object v10, v8, v9

    const/4 v9, 0x5
    sget-wide v10, Lcom/winlator/renderer/PerfStats;->maxGapR:J
    invoke-static {v10, v11}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;
    move-result-object v10
    aput-object v10, v8, v9

    const-string v11, "u=%d r=%d up=%d KB=%d gU=%d gR=%d\n"
    invoke-static {v11, v8}, Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
    move-result-object v11

    const-string v10, "CCZPERF"
    invoke-static {v10, v11}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

    :try_start_0
    const-string v10, "/data/data/com.winlatcn/files/perf.txt"
    new-instance v12, Ljava/io/FileOutputStream;
    const/4 v13, 0x1
    invoke-direct {v12, v10, v13}, Ljava/io/FileOutputStream;-><init>(Ljava/lang/String;Z)V
    invoke-virtual {v11}, Ljava/lang/String;->getBytes()[B
    move-result-object v10
    invoke-virtual {v12, v10}, Ljava/io/FileOutputStream;->write([B)V
    invoke-virtual {v12}, Ljava/io/FileOutputStream;->close()V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0
    goto :after_write

    :catch_0
    move-exception v10

    :after_write
    const/4 v10, 0x0
    sput v10, Lcom/winlator/renderer/PerfStats;->updates:I
    sput v10, Lcom/winlator/renderer/PerfStats;->renders:I
    sput v10, Lcom/winlator/renderer/PerfStats;->uploads:I

    const-wide/16 v10, 0x0
    sput-wide v10, Lcom/winlator/renderer/PerfStats;->bytes:J
    sput-wide v10, Lcom/winlator/renderer/PerfStats;->maxGapU:J
    sput-wide v10, Lcom/winlator/renderer/PerfStats;->maxGapR:J

    :done
    return-void

    :setlast
    sput-wide v0, Lcom/winlator/renderer/PerfStats;->lastLog:J
    return-void
.end method
