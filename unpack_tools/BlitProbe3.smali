.class public Lcom/winlator/xserver/BlitProbe;
.super Ljava/lang/Object;
.source "BlitProbe.java"

.field public static nImg:I
.field public static nCopy:I
.field public static nFill:I
.field public static nAlpha:I
.field public static nLine:I
.field public static nSet:I
.field public static pixels:J
.field public static lastLog:J

.method public constructor <init>()V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public static add(III)V
    .locals 6

    packed-switch p0, :sw
    goto :count
    :sw
    .packed-switch 0x0
        :img
        :copy
        :fill
        :alpha
        :line
        :set
    .end packed-switch

    :img
    sget v0, Lcom/winlator/xserver/BlitProbe;->nImg:I
    add-int/lit8 v0, v0, 0x1
    sput v0, Lcom/winlator/xserver/BlitProbe;->nImg:I
    goto :count

    :copy
    sget v0, Lcom/winlator/xserver/BlitProbe;->nCopy:I
    add-int/lit8 v0, v0, 0x1
    sput v0, Lcom/winlator/xserver/BlitProbe;->nCopy:I
    goto :count

    :fill
    sget v0, Lcom/winlator/xserver/BlitProbe;->nFill:I
    add-int/lit8 v0, v0, 0x1
    sput v0, Lcom/winlator/xserver/BlitProbe;->nFill:I
    goto :count

    :alpha
    sget v0, Lcom/winlator/xserver/BlitProbe;->nAlpha:I
    add-int/lit8 v0, v0, 0x1
    sput v0, Lcom/winlator/xserver/BlitProbe;->nAlpha:I
    goto :count

    :line
    sget v0, Lcom/winlator/xserver/BlitProbe;->nLine:I
    add-int/lit8 v0, v0, 0x1
    sput v0, Lcom/winlator/xserver/BlitProbe;->nLine:I
    goto :count

    :set
    sget v0, Lcom/winlator/xserver/BlitProbe;->nSet:I
    add-int/lit8 v0, v0, 0x1
    sput v0, Lcom/winlator/xserver/BlitProbe;->nSet:I

    :count
    sget-wide v2, Lcom/winlator/xserver/BlitProbe;->pixels:J
    int-to-long v4, p1
    int-to-long v0, p2
    mul-long/2addr v4, v0
    add-long/2addr v2, v4
    sput-wide v2, Lcom/winlator/xserver/BlitProbe;->pixels:J

    invoke-static {}, Lcom/winlator/xserver/BlitProbe;->logIfDue()V
    return-void
.end method

.method public static onImage(II)V
    .locals 1
    const/4 v0, 0x0
    invoke-static {v0, p0, p1}, Lcom/winlator/xserver/BlitProbe;->add(III)V
    return-void
.end method

.method public static onCopy(II)V
    .locals 1
    const/4 v0, 0x1
    invoke-static {v0, p0, p1}, Lcom/winlator/xserver/BlitProbe;->add(III)V
    return-void
.end method

.method public static onFill(II)V
    .locals 1
    const/4 v0, 0x2
    invoke-static {v0, p0, p1}, Lcom/winlator/xserver/BlitProbe;->add(III)V
    return-void
.end method

.method public static onAlpha()V
    .locals 2
    const/4 v0, 0x3
    const/4 v1, 0x0
    invoke-static {v0, v1, v1}, Lcom/winlator/xserver/BlitProbe;->add(III)V
    return-void
.end method

.method public static onLine()V
    .locals 2
    const/4 v0, 0x4
    const/4 v1, 0x0
    invoke-static {v0, v1, v1}, Lcom/winlator/xserver/BlitProbe;->add(III)V
    return-void
.end method

.method public static onSet()V
    .locals 2
    const/4 v0, 0x5
    const/4 v1, 0x0
    invoke-static {v0, v1, v1}, Lcom/winlator/xserver/BlitProbe;->add(III)V
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

    const-string v9, "img="
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    sget v9, Lcom/winlator/xserver/BlitProbe;->nImg:I
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v9, " cp="
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    sget v9, Lcom/winlator/xserver/BlitProbe;->nCopy:I
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v9, " fill="
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    sget v9, Lcom/winlator/xserver/BlitProbe;->nFill:I
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v9, " alpha="
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    sget v9, Lcom/winlator/xserver/BlitProbe;->nAlpha:I
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v9, " line="
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    sget v9, Lcom/winlator/xserver/BlitProbe;->nLine:I
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v9, " set="
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    sget v9, Lcom/winlator/xserver/BlitProbe;->nSet:I
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    const-string v9, " px="
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    sget-wide v10, Lcom/winlator/xserver/BlitProbe;->pixels:J
    invoke-virtual {v8, v10, v11}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;

    const-string v9, "\n"
    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v8}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v9

    :try_start_0
    const-string v0, "/data/data/com.winlatcn/files/blit.txt"
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

    :reset
    const/4 v6, 0x0
    sput v6, Lcom/winlator/xserver/BlitProbe;->nImg:I
    sput v6, Lcom/winlator/xserver/BlitProbe;->nCopy:I
    sput v6, Lcom/winlator/xserver/BlitProbe;->nFill:I
    sput v6, Lcom/winlator/xserver/BlitProbe;->nAlpha:I
    sput v6, Lcom/winlator/xserver/BlitProbe;->nLine:I
    sput v6, Lcom/winlator/xserver/BlitProbe;->nSet:I

    const-wide/16 v6, 0x0
    sput-wide v6, Lcom/winlator/xserver/BlitProbe;->pixels:J

    :done
    return-void

    :setlast
    sput-wide v0, Lcom/winlator/xserver/BlitProbe;->lastLog:J
    return-void
.end method
