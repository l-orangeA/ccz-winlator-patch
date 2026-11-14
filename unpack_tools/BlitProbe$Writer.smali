.class Lcom/winlator/xserver/BlitProbe$Writer;
.super Ljava/lang/Object;
.source "BlitProbe.java"

.implements Ljava/lang/Runnable;

.method public constructor <init>()V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public run()V
    .locals 3

    :loop
    const-wide/16 v0, 0x3e8

    :try_start_0
    invoke-static {v0, v1}, Ljava/lang/Thread;->sleep(J)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0
    goto :after

    :catch_0
    move-exception v2

    :after
    invoke-static {}, Lcom/winlator/xserver/BlitProbe;->dump()V
    goto :loop
.end method
