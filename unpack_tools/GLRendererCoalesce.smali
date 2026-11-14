.class public final Lcom/winlator/renderer/GLRendererCoalesce;
.super Lcom/winlator/renderer/GLRenderer;

# coalesces window-content updates so one game frame is presented atomically

.field public lastContentUpdate:J

.field private final coalesceRunnable:Ljava/lang/Runnable;

.method public constructor <init>(Lcom/winlator/widget/XServerView;Lcom/winlator/xserver/XServer;)V
    .locals 1

    invoke-direct {p0, p1, p2}, Lcom/winlator/renderer/GLRenderer;-><init>(Lcom/winlator/widget/XServerView;Lcom/winlator/xserver/XServer;)V

    new-instance v0, Lcom/winlator/renderer/CoalesceRunnable;

    invoke-direct {v0, p0}, Lcom/winlator/renderer/CoalesceRunnable;-><init>(Lcom/winlator/renderer/GLRendererCoalesce;)V

    iput-object v0, p0, Lcom/winlator/renderer/GLRendererCoalesce;->coalesceRunnable:Ljava/lang/Runnable;

    return-void
.end method

.method public onUpdateWindowContent(Lcom/winlator/xserver/Window;)V
    .locals 6

    invoke-static {}, Landroid/os/SystemClock;->uptimeMillis()J

    move-result-wide v0

    iget-wide v2, p0, Lcom/winlator/renderer/GLRendererCoalesce;->lastContentUpdate:J

    const-wide/16 v4, 0x0

    cmp-long v4, v2, v4

    if-nez v4, :cond_0

    iput-wide v0, p0, Lcom/winlator/renderer/GLRendererCoalesce;->lastContentUpdate:J

    :cond_0
    iget-wide v2, p0, Lcom/winlator/renderer/GLRendererCoalesce;->lastContentUpdate:J

    sub-long v2, v0, v2

    const-wide/16 v4, 0x8

    cmp-long v4, v2, v4

    if-ltz v4, :cond_delay

    const-wide/16 v2, 0x0

    iput-wide v2, p0, Lcom/winlator/renderer/GLRendererCoalesce;->lastContentUpdate:J

    iget-object v2, p0, Lcom/winlator/renderer/GLRenderer;->xServerView:Lcom/winlator/widget/XServerView;

    invoke-virtual {v2}, Landroid/opengl/GLSurfaceView;->requestRender()V

    return-void

    :cond_delay
    iget-object v2, p0, Lcom/winlator/renderer/GLRenderer;->xServerView:Lcom/winlator/widget/XServerView;

    iget-object v3, p0, Lcom/winlator/renderer/GLRendererCoalesce;->coalesceRunnable:Ljava/lang/Runnable;

    invoke-virtual {v2, v3}, Landroid/view/View;->removeCallbacks(Ljava/lang/Runnable;)Z

    move-result v4

    iget-object v2, p0, Lcom/winlator/renderer/GLRenderer;->xServerView:Lcom/winlator/widget/XServerView;

    iget-object v3, p0, Lcom/winlator/renderer/GLRendererCoalesce;->coalesceRunnable:Ljava/lang/Runnable;

    const-wide/16 v4, 0x5

    invoke-virtual {v2, v3, v4, v5}, Landroid/view/View;->postDelayed(Ljava/lang/Runnable;J)Z

    move-result v4

    return-void
.end method
