.class public Lcom/winlator/xserver/BlitProbe;
.super Ljava/lang/Object;
.source "BlitProbe.java"

.field public static puts:I
.field public static pixels:J
.field public static maxw:I
.field public static maxh:I
.field public static lastLog:J

.method public constructor <init>()V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public static onPut(II)V
    .locals 6

    sget v0, Lcom/winlator/xserver/BlitProbe;->puts:I
    add-int/lit8 v0, v0, 0x1
    sput v0, Lcom/winlator/xserver/BlitProbe;->puts:I

    sget-wide v2, Lcom/winlator/xserver/BlitProbe;->pixels:J
    int-to-long v4, p0
    int-to-long v0, p1
    mul-long/2addr v4, v0
    add-long/2addr v2, v4
    sput-wide v2, Lcom/winlator/xserver/BlitProbe;->pixels:J

    sget v0, Lcom/winlator/xserver/BlitProbe;->maxw:I
    if-le v0, p0, :now
    sput p0, Lcom/winlator/xserver/BlitProbe;->maxw:I

    :now
    sget v0, Lcom/winlator/xserver/BlitProbe;->maxh:I
    if-le v0, p1, :log
    sput p1, Lcom/winlator/xserver/BlitProbe;->maxh:I

    :log
    invoke-static {}, Lcom/winlator/xserver/BlitProbe;->logIfDue()V
    return-void
.end method

.method public static declared-synchronized logIfDue()V
    .locals 14

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

    new-instance v8, Ljava/lang/StringBuilder;
    invoke-direct {v8}, Ljava/lang/StringBuilder;-><init>()V

    const-string v9, "puts="
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    sget v9, Lcom/winlator/xserver/BlitProbe;->puts:I
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v9, " px="
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    sget-wide v10, Lcom/winlator/xserver/BlitProbe;->pixels:J
    invoke-virtual {v8, v10, v11}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;

    const-string v9, " max="
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    sget v9, Lcom/winlator/xserver/BlitProbe;->maxw:I
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v9, "x"
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    sget v9, Lcom/winlator/xserver/BlitProbe;->maxh:I
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v9, "\n"
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v8}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v9

    :try_start_0
    const-string v0, "/data/data/com.winlatcn/files/blit.txt"
    new-instance v1, Ljava/io/FileOutputStream;
    const/4 v2, 0x1
    invoke-direct {v1, v0, v2}, Ljava/io/FileOutputStream;-><init>(Ljava/lang/String;Z)V
    invoke-virtual {v9}, Ljava/lang/String;->getBytes()[B
    move-result-object v0
    invoke-virtual {v1, v0}, Ljava/io/OutputStream;->write([B)V
    invoke-virtual {v1}, Ljava/io/OutputStream;->close()V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0
    goto :reset

    :catch_0
    move-exception v0

    :reset
    const/4 v6, 0x0
    sput v6, Lcom/winlator/xserver/BlitProbe;->puts:I
    sput v6, Lcom/winlator/xserver/BlitProbe;->maxw:I
    sput v6, Lcom/winlator/xserver/BlitProbe;->maxh:I

    const-wide/16 v6, 0x0
    sput-wide v6, Lcom/winlator/xserver/BlitProbe;->pixels:J

    :done
    return-void

    :setlast
    sput-wide v0, Lcom/winlator/xserver/BlitProbe;->lastLog:J
    return-void
.end method
