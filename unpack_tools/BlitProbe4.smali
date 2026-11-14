.class public Lcom/winlator/xserver/BlitProbe;
.super Ljava/lang/Object;
.source "BlitProbe.java"

.field public static opCounts:[I
.field public static lastLog:J

.field public static seen:I

.method static constructor <clinit>()V
    .locals 1
    const/16 v0, 0x100
    new-array v0, v0, [I
    sput-object v0, Lcom/winlator/xserver/BlitProbe;->opCounts:[I
    return-void
.end method

.method public constructor <init>()V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public static onOpcode(I)V
    .locals 3

    sget v0, Lcom/winlator/xserver/BlitProbe;->seen:I
    if-nez v0, :go

    const/4 v0, 0x1
    sput v0, Lcom/winlator/xserver/BlitProbe;->seen:I

    :try_start_m
    const-string v0, "/data/data/com.winlatcn/files/op_seen.txt"
    new-instance v1, Ljava/io/FileOutputStream;
    invoke-direct {v1, v0}, Ljava/io/FileOutputStream;-><init>(Ljava/lang/String;)V
    const-string v0, "x"
    invoke-virtual {v0}, Ljava/lang/String;->getBytes()[B
    move-result-object v0
    invoke-virtual {v1, v0}, Ljava/io/OutputStream;->write([B)V
    invoke-virtual {v1}, Ljava/io/OutputStream;->close()V
    :try_end_m
    .catch Ljava/lang/Exception; {:try_start_m .. :try_end_m} :catch_m
    goto :go

    :catch_m
    move-exception v0

    :go
    and-int/lit16 v0, p0, 0xff
    sget-object v1, Lcom/winlator/xserver/BlitProbe;->opCounts:[I
    aget v2, v1, v0
    add-int/lit8 v2, v2, 0x1
    aput v2, v1, v0

    invoke-static {}, Lcom/winlator/xserver/BlitProbe;->logIfDue()V
    return-void
.end method


.method public static onImage(II)V
    .locals 0
    return-void
.end method

.method public static onCopy(II)V
    .locals 0
    return-void
.end method

.method public static onFill(II)V
    .locals 0
    return-void
.end method

.method public static onAlpha()V
    .locals 0
    return-void
.end method

.method public static onLine()V
    .locals 0
    return-void
.end method

.method public static declared-synchronized logIfDue()V
    .locals 12

    invoke-static {}, Landroid/os/SystemClock;->uptimeMillis()J
    move-result-wide v0

    sget-wide v2, Lcom/winlator/xserver/BlitProbe;->lastLog:J
    const-wide/16 v4, 0x0
    cmp-long v6, v2, v4
    if-nez v6, :setlast

    sub-long v4, v0, v2
    const-wide/16 v6, 0x3e8
    cmp-long v8, v4, v6
    if-gez v8, :done

    sput-wide v0, Lcom/winlator/xserver/BlitProbe;->lastLog:J

    :try_start_0
    const-string v0, "/data/data/com.winlatcn/files/ops.bin"
    new-instance v1, Ljava/io/FileOutputStream;
    invoke-direct {v1, v0}, Ljava/io/FileOutputStream;-><init>(Ljava/lang/String;)V
    new-instance v2, Ljava/io/DataOutputStream;
    invoke-direct {v2, v1}, Ljava/io/DataOutputStream;-><init>(Ljava/io/OutputStream;)V

    const/4 v3, 0x0
    :loop
    const/16 v4, 0x100
    if-ge v3, v4, :after

    sget-object v4, Lcom/winlator/xserver/BlitProbe;->opCounts:[I
    aget v4, v4, v3
    invoke-virtual {v2, v4}, Ljava/io/DataOutputStream;->writeInt(I)V
    add-int/lit8 v3, v3, 0x1
    goto :loop

    :after
    invoke-virtual {v2}, Ljava/io/DataOutputStream;->close()V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0
    goto :reset

    :catch_0
    move-exception v0

    :reset
    const/4 v3, 0x0
    :zloop
    const/16 v4, 0x100
    if-ge v3, v4, :done

    sget-object v4, Lcom/winlator/xserver/BlitProbe;->opCounts:[I
    const/4 v5, 0x0
    aput v5, v4, v3
    add-int/lit8 v3, v3, 0x1
    goto :zloop

    :done
    return-void

    :setlast
    sput-wide v0, Lcom/winlator/xserver/BlitProbe;->lastLog:J
    return-void
.end method
