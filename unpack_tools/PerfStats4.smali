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
    .locals 14
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

    div-long/2addr v4, v6

    :try_start_g
    const-string v0, "/data/data/com.winlatcn/files/perf_gate.txt"
    new-instance v1, Ljava/io/FileOutputStream;
    invoke-direct {v1, v0}, Ljava/io/FileOutputStream;-><init>(Ljava/lang/String;)V
    const-string v0, "gate"
    invoke-virtual {v0}, Ljava/lang/String;->getBytes()[B
    move-result-object v0
    invoke-virtual {v1, v0}, Ljava/io/OutputStream;->write([B)V
    invoke-virtual {v1}, Ljava/io/OutputStream;->close()V
    :try_end_g
    .catch Ljava/lang/Exception; {:try_start_g .. :try_end_g} :catch_g
    goto :after_g

    :catch_g
    move-exception v0

    :after_g
    sget v6, Lcom/winlator/renderer/PerfStats;->uploads:I
    const-wide/16 v10, 0x0
    if-gtz v6, :noavg

    sget-wide v10, Lcom/winlator/renderer/PerfStats;->upSum:J
    int-to-long v12, v6
    div-long/2addr v10, v12
    const-wide/16 v12, 0x3e8
    div-long/2addr v10, v12

    :noavg
    sget-wide v12, Lcom/winlator/renderer/PerfStats;->upMax:J
    const-wide/16 v2, 0x3e8
    div-long/2addr v12, v2

    new-instance v8, Ljava/lang/StringBuilder;
    invoke-direct {v8}, Ljava/lang/StringBuilder;-><init>()V

    const-string v9, "t="
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v8, v4, v5}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;

    const-string v9, " u="
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    sget v9, Lcom/winlator/renderer/PerfStats;->updates:I
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v9, " r="
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    sget v9, Lcom/winlator/renderer/PerfStats;->renders:I
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v9, " up="
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    sget v9, Lcom/winlator/renderer/PerfStats;->uploads:I
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v9, " avg="
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v8, v10, v11}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;

    const-string v9, " max="
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v8, v12, v13}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;

    const-string v9, " big="
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    sget v9, Lcom/winlator/renderer/PerfStats;->bigUp:I
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v9, " sU="
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    sget v9, Lcom/winlator/renderer/PerfStats;->slowU:I
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v9, " sR="
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    sget v9, Lcom/winlator/renderer/PerfStats;->slowR:I
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v9, " gU="
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    sget-wide v10, Lcom/winlator/renderer/PerfStats;->maxGapU:J
    invoke-virtual {v8, v10, v11}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;

    const-string v9, " gR="
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    sget-wide v10, Lcom/winlator/renderer/PerfStats;->maxGapR:J
    invoke-virtual {v8, v10, v11}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;

    const-string v9, "\n"
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v8}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v9

    :try_start_0
    const-string v0, "/data/data/com.winlatcn/files/perf.txt"
    new-instance v1, Ljava/io/FileOutputStream;
    invoke-direct {v1, v0}, Ljava/io/FileOutputStream;-><init>(Ljava/lang/String;)V
    invoke-virtual {v9}, Ljava/lang/String;->getBytes()[B
    move-result-object v0
    invoke-virtual {v1, v0}, Ljava/io/OutputStream;->write([B)V
    invoke-virtual {v1}, Ljava/io/OutputStream;->close()V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0
    goto :reset

    :catch_0
    move-exception v0

    const-string v1, "/data/data/com.winlatcn/files/perf_err.txt"
    new-instance v2, Ljava/io/FileOutputStream;
    invoke-direct {v2, v1}, Ljava/io/FileOutputStream;-><init>(Ljava/lang/String;)V
    const-string v1, "err"
    invoke-virtual {v1}, Ljava/lang/String;->getBytes()[B
    move-result-object v1
    invoke-virtual {v2, v1}, Ljava/io/OutputStream;->write([B)V
    invoke-virtual {v2}, Ljava/io/OutputStream;->close()V

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
