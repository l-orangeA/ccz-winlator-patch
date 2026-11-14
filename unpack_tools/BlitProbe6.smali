.class public Lcom/winlator/xserver/BlitProbe;
.super Ljava/lang/Object;
.source "BlitProbe.java"

.field public static opCounts:[I
.field public static shmCounts:[I
.field public static presCounts:[I
.field public static seen:I
.field public static imgN:I
.field public static imgPx:I
.field public static imgW:I
.field public static imgH:I

.method static constructor <clinit>()V
    .locals 2
    const/16 v0, 0x100
    new-array v1, v0, [I
    sput-object v1, Lcom/winlator/xserver/BlitProbe;->opCounts:[I
    new-array v1, v0, [I
    sput-object v1, Lcom/winlator/xserver/BlitProbe;->shmCounts:[I
    new-array v1, v0, [I
    sput-object v1, Lcom/winlator/xserver/BlitProbe;->presCounts:[I

    new-instance v0, Lcom/winlator/xserver/BlitProbe$Writer;
    invoke-direct {v0}, Lcom/winlator/xserver/BlitProbe$Writer;-><init>()V
    new-instance v1, Ljava/lang/Thread;
    invoke-direct {v1, v0}, Ljava/lang/Thread;-><init>(Ljava/lang/Runnable;)V
    invoke-virtual {v1}, Ljava/lang/Thread;->start()V
    return-void
.end method

.method public constructor <init>()V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public static onOpcode(Lcom/winlator/xserver/XClient;I)V
    .locals 6

    and-int/lit16 v0, p1, 0xff

    sget-object v1, Lcom/winlator/xserver/BlitProbe;->opCounts:[I
    aget v2, v1, v0
    add-int/lit8 v2, v2, 0x1
    aput v2, v1, v0

    invoke-virtual {p0}, Lcom/winlator/xserver/XClient;->getRequestData()B
    move-result v1
    and-int/lit16 v1, v1, 0xff

    const/16 v2, 0x9b
    if-ne v0, v2, :nshm
    sget-object v3, Lcom/winlator/xserver/BlitProbe;->shmCounts:[I
    aget v4, v3, v1
    add-int/lit8 v4, v4, 0x1
    aput v4, v3, v1

    :nshm
    const/16 v2, 0x99
    if-ne v0, v2, :done
    sget-object v3, Lcom/winlator/xserver/BlitProbe;->presCounts:[I
    aget v4, v3, v1
    add-int/lit8 v4, v4, 0x1
    aput v4, v3, v1

    :done
    sget v0, Lcom/winlator/xserver/BlitProbe;->seen:I
    if-nez v0, :ret
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
    goto :ret

    :catch_m
    move-exception v0

    :ret
    return-void
.end method

.method private static dumpArr([ILjava/io/DataOutputStream;)V
    .locals 4
    :try_start_0
    const/4 v0, 0x0
    :loop
    const/16 v1, 0x100
    if-ge v0, v1, :after
    aget v1, p0, v0
    invoke-virtual {p1, v1}, Ljava/io/DataOutputStream;->writeInt(I)V
    add-int/lit8 v0, v0, 0x1
    goto :loop
    :after
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0
    :catch_0
    return-void
.end method

.method private static zeroArr([I)V
    .locals 3
    const/4 v0, 0x0
    :loop
    const/16 v1, 0x100
    if-ge v0, v1, :done
    const/4 v2, 0x0
    aput v2, p0, v0
    add-int/lit8 v0, v0, 0x1
    goto :loop
    :done
    return-void
.end method

.method public static declared-synchronized dump()V
    .locals 4
    :try_start_0
    const-string v0, "/data/data/com.winlatcn/files/ops.bin"
    new-instance v1, Ljava/io/FileOutputStream;
    invoke-direct {v1, v0}, Ljava/io/FileOutputStream;-><init>(Ljava/lang/String;)V
    new-instance v2, Ljava/io/DataOutputStream;
    invoke-direct {v2, v1}, Ljava/io/DataOutputStream;-><init>(Ljava/io/OutputStream;)V

    sget-object v3, Lcom/winlator/xserver/BlitProbe;->opCounts:[I
    invoke-static {v3, v2}, Lcom/winlator/xserver/BlitProbe;->dumpArr([ILjava/io/DataOutputStream;)V
    sget-object v3, Lcom/winlator/xserver/BlitProbe;->shmCounts:[I
    invoke-static {v3, v2}, Lcom/winlator/xserver/BlitProbe;->dumpArr([ILjava/io/DataOutputStream;)V
    sget-object v3, Lcom/winlator/xserver/BlitProbe;->presCounts:[I
    invoke-static {v3, v2}, Lcom/winlator/xserver/BlitProbe;->dumpArr([ILjava/io/DataOutputStream;)V

    invoke-virtual {v2}, Ljava/io/DataOutputStream;->close()V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0
    goto :reset

    :catch_0
    move-exception v0

    :reset
    :try_start_i
    const-string v0, "/data/data/com.winlatcn/files/img.bin"
    new-instance v1, Ljava/io/FileOutputStream;
    invoke-direct {v1, v0}, Ljava/io/FileOutputStream;-><init>(Ljava/lang/String;)V
    new-instance v2, Ljava/io/DataOutputStream;
    invoke-direct {v2, v1}, Ljava/io/DataOutputStream;-><init>(Ljava/io/OutputStream;)V
    sget v3, Lcom/winlator/xserver/BlitProbe;->imgN:I
    invoke-virtual {v2, v3}, Ljava/io/DataOutputStream;->writeInt(I)V
    sget v3, Lcom/winlator/xserver/BlitProbe;->imgPx:I
    invoke-virtual {v2, v3}, Ljava/io/DataOutputStream;->writeInt(I)V
    sget v3, Lcom/winlator/xserver/BlitProbe;->imgW:I
    invoke-virtual {v2, v3}, Ljava/io/DataOutputStream;->writeInt(I)V
    sget v3, Lcom/winlator/xserver/BlitProbe;->imgH:I
    invoke-virtual {v2, v3}, Ljava/io/DataOutputStream;->writeInt(I)V
    invoke-virtual {v2}, Ljava/io/DataOutputStream;->close()V
    :try_end_i
    .catch Ljava/lang/Exception; {:try_start_i .. :try_end_i} :catch_i
    goto :after_i
    :catch_i
    move-exception v0
    :after_i
    const/4 v0, 0x0
    sput v0, Lcom/winlator/xserver/BlitProbe;->imgN:I
    sput v0, Lcom/winlator/xserver/BlitProbe;->imgPx:I
    sput v0, Lcom/winlator/xserver/BlitProbe;->imgW:I
    sput v0, Lcom/winlator/xserver/BlitProbe;->imgH:I

    sget-object v0, Lcom/winlator/xserver/BlitProbe;->opCounts:[I
    invoke-static {v0}, Lcom/winlator/xserver/BlitProbe;->zeroArr([I)V
    sget-object v0, Lcom/winlator/xserver/BlitProbe;->shmCounts:[I
    invoke-static {v0}, Lcom/winlator/xserver/BlitProbe;->zeroArr([I)V
    sget-object v0, Lcom/winlator/xserver/BlitProbe;->presCounts:[I
    invoke-static {v0}, Lcom/winlator/xserver/BlitProbe;->zeroArr([I)V
    return-void
.end method

.method public static onImage(II)V
    .locals 3

    sget v0, Lcom/winlator/xserver/BlitProbe;->imgN:I
    add-int/lit8 v0, v0, 0x1
    sput v0, Lcom/winlator/xserver/BlitProbe;->imgN:I

    sget v0, Lcom/winlator/xserver/BlitProbe;->imgPx:I
    mul-int v1, p0, p1
    add-int/2addr v0, v1
    sput v0, Lcom/winlator/xserver/BlitProbe;->imgPx:I

    sget v0, Lcom/winlator/xserver/BlitProbe;->imgW:I
    if-le v0, p0, :now
    sput p0, Lcom/winlator/xserver/BlitProbe;->imgW:I

    :now
    sget v0, Lcom/winlator/xserver/BlitProbe;->imgH:I
    if-le v0, p1, :done
    sput p1, Lcom/winlator/xserver/BlitProbe;->imgH:I

    :done
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
