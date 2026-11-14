#!/usr/bin/env python3
"""DYNRES part 2: also apply on window map.

Window creation calls createWindow(), which does NOT go through
changeWindowGeometry -> onUpdateWindowGeometry. So the first window never
triggered the dynamic sizing. onMapWindow is guaranteed to fire when the
window is shown, so apply the same rule there.
"""
import sys

GR = "/root/wn_gl60/smali/com/winlator/renderer/GLRenderer.smali"
g = open(GR, encoding="utf-8").read()

head = """.method public onMapWindow(Lcom/winlator/xserver/Window;)V
    .locals 2
    .param p1, "window"    # Lcom/winlator/xserver/Window;
"""
if g.count(head) != 1:
    sys.exit("FAIL onMapWindow head count=%d" % g.count(head))

new = """.method public onMapWindow(Lcom/winlator/xserver/Window;)V
    .locals 6
    .param p1, "window"    # Lcom/winlator/xserver/Window;

    # DYNRES-MAP: apply dynamic screen size when the window is shown
    invoke-virtual {p1}, Lcom/winlator/xserver/Window;->isApplicationWindow()Z
    move-result v0
    if-eqz v0, :dynmap_skip
    invoke-virtual {p1}, Lcom/winlator/xserver/Window;->getHeight()S
    move-result v0
    const/16 v1, 0x1c2
    if-ge v0, v1, :dynmap_h_ok
    move v0, v1
    :dynmap_h_ok
    iget-object v1, p0, Lcom/winlator/renderer/GLRenderer;->xServer:Lcom/winlator/xserver/XServer;
    iget-object v1, v1, Lcom/winlator/xserver/XServer;->screenInfo:Lcom/winlator/xserver/ScreenInfo;
    iget-short v2, v1, Lcom/winlator/xserver/ScreenInfo;->height:S
    sub-int/2addr v2, v0
    invoke-static {v2}, Ljava/lang/Math;->abs(I)I
    move-result v2
    const/16 v3, 0x10
    if-lt v2, v3, :dynmap_skip
    iput-short v0, v1, Lcom/winlator/xserver/ScreenInfo;->height:S
    mul-int/lit8 v2, v0, 0x10
    div-int/lit8 v2, v2, 0x9
    iput-short v2, v1, Lcom/winlator/xserver/ScreenInfo;->width:S
    iget-object v3, p0, Lcom/winlator/renderer/GLRenderer;->viewTransformation:Lcom/winlator/renderer/ViewTransformation;
    iget-short v4, p0, Lcom/winlator/renderer/GLRenderer;->surfaceWidth:S
    iget-short v5, p0, Lcom/winlator/renderer/GLRenderer;->surfaceHeight:S
    invoke-virtual {v3, v4, v5, v2, v0}, Lcom/winlator/renderer/ViewTransformation;->update(IIII)V
    :dynmap_skip
"""
g = g.replace(head, new, 1)
open(GR, "w", encoding="utf-8", newline="\n").write(g)
print("OK onMapWindow dynres inserted, markers=%d" % g.count("DYNRES"))
