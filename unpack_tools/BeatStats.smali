.class public Lcom/winlator/renderer/BeatStats;
.super Ljava/lang/Object;
.source "BeatStats.java"

# debug heartbeat: append one byte to files/beat.txt every 60 drawFrame calls

.field public static renders:I
.field public static updates:I

.method public constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static beat()V
    .locals 4

    sget v0, Lcom/winlator/renderer/BeatStats;->renders:I
    add-int/lit8 v0, v0, 0x1
    sput v0, Lcom/winlator/renderer/BeatStats;->renders:I

    rem-int/lit8 v1, v0, 0x3c
    if-nez v1, :do

    return-void

    :do
    :try_start_0
    const-string v1, "/data/data/com.winlatcn/files/beat.txt"
    new-instance v2, Ljava/io/FileOutputStream;
    const/4 v3, 0x1
    invoke-direct {v2, v1, v3}, Ljava/io/FileOutputStream;-><init>(Ljava/lang/String;Z)V
    const-string v1, "b\n"
    invoke-virtual {v1}, Ljava/lang/String;->getBytes()[B
    move-result-object v1
    invoke-virtual {v2, v1}, Ljava/io/FileOutputStream;->write([B)V
    invoke-virtual {v2}, Ljava/io/FileOutputStream;->close()V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    return-void

    :catch_0
    return-void
.end method

.method public static beatU()V
    .locals 4

    sget v0, Lcom/winlator/renderer/BeatStats;->updates:I
    add-int/lit8 v0, v0, 0x1
    sput v0, Lcom/winlator/renderer/BeatStats;->updates:I

    rem-int/lit8 v1, v0, 0xa
    if-nez v1, :do

    return-void

    :do
    :try_start_0
    const-string v1, "/data/data/com.winlatcn/files/beatu.txt"
    new-instance v2, Ljava/io/FileOutputStream;
    const/4 v3, 0x1
    invoke-direct {v2, v1, v3}, Ljava/io/FileOutputStream;-><init>(Ljava/lang/String;Z)V
    const-string v1, "u\n"
    invoke-virtual {v1}, Ljava/lang/String;->getBytes()[B
    move-result-object v1
    invoke-virtual {v2, v1}, Ljava/io/FileOutputStream;->write([B)V
    invoke-virtual {v2}, Ljava/io/FileOutputStream;->close()V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    return-void

    :catch_0
    return-void
.end method
