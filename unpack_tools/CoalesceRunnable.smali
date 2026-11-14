.class public final Lcom/winlator/renderer/CoalesceRunnable;
.super Ljava/lang/Object;
.implements Ljava/lang/Runnable;

.field private final renderer:Lcom/winlator/renderer/GLRendererCoalesce;

.method public constructor <init>(Lcom/winlator/renderer/GLRendererCoalesce;)V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Lcom/winlator/renderer/CoalesceRunnable;->renderer:Lcom/winlator/renderer/GLRendererCoalesce;

    return-void
.end method

.method public run()V
    .locals 2

    iget-object v0, p0, Lcom/winlator/renderer/CoalesceRunnable;->renderer:Lcom/winlator/renderer/GLRendererCoalesce;

    const-wide/16 v1, 0x0

    iput-wide v1, v0, Lcom/winlator/renderer/GLRendererCoalesce;->lastContentUpdate:J

    iget-object v0, v0, Lcom/winlator/renderer/GLRenderer;->xServerView:Lcom/winlator/widget/XServerView;

    invoke-virtual {v0}, Landroid/opengl/GLSurfaceView;->requestRender()V

    return-void
.end method
