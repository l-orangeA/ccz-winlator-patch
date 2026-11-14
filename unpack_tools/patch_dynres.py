#!/usr/bin/env python3
"""Dynamic resolution: screen height follows the app window, 16:9, min 450.

Why this shape:
  * 16:9 is a hard requirement -- 20:9 screenSizes crash the shell at 92-96%
    of unpack (tested 858x386, 1000x450).
  * ViewTransformation.update() already letterboxes (keeps aspect), so the
    picture never stretches.
  * Screen height = max(windowHeight, 450) makes the window touch top/bottom,
    left/right black bars shrink automatically as the window grows.

Changes (3 files):
  1. ScreenInfo.smali      : width/height lose `final`
  2. GLRenderer.onUpdateWindowGeometry : compute + apply new size on resize
  3. GLRenderer.onDrawFrame            : refresh viewTransformation each frame
"""
import sys

SN = "/root/wn_gl60/smali/com/winlator/xserver/ScreenInfo.smali"
GR = "/root/wn_gl60/smali/com/winlator/renderer/GLRenderer.smali"

# ---------- 1. ScreenInfo ----------
s = open(SN, encoding="utf-8").read()
for a, b in ((".field public final height:S", ".field public height:S"),
             (".field public final width:S", ".field public width:S")):
    if s.count(a) != 1:
        sys.exit("FAIL ScreenInfo field %s count=%d" % (a, s.count(a)))
    s = s.replace(a, b, 1)
open(SN, "w", encoding="utf-8", newline="\n").write(s)
print("OK ScreenInfo: final removed")

# ---------- 2. onUpdateWindowGeometry ----------
g = open(GR, encoding="utf-8").read()

head2 = """.method public onUpdateWindowGeometry(Lcom/winlator/xserver/Window;Z)V
    .locals 2
    .param p1, "window"    # Lcom/winlator/xserver/Window;
    .param p2, "resized"    # Z
"""
if g.count(head2) != 1:
    sys.exit("FAIL onUpdateWindowGeometry head count=%d" % g.count(head2))

new2 = """.method public onUpdateWindowGeometry(Lcom/winlator/xserver/Window;Z)V
    .locals 6
    .param p1, "window"    # Lcom/winlator/xserver/Window;
    .param p2, "resized"    # Z

    # DYNRES: screen follows window height, 16:9, min 450
    if-eqz p2, :dynres_skip
    invoke-virtual {p1}, Lcom/winlator/xserver/Window;->isApplicationWindow()Z
    move-result v0
    if-eqz v0, :dynres_skip
    invoke-virtual {p1}, Lcom/winlator/xserver/Window;->getHeight()S
    move-result v0
    const/16 v1, 0x1c2
    if-ge v0, v1, :dynres_h_ok
    move v0, v1
    :dynres_h_ok
    iget-object v1, p0, Lcom/winlator/renderer/GLRenderer;->xServer:Lcom/winlator/xserver/XServer;
    iget-object v1, v1, Lcom/winlator/xserver/XServer;->screenInfo:Lcom/winlator/xserver/ScreenInfo;
    iget-short v2, v1, Lcom/winlator/xserver/ScreenInfo;->height:S
    sub-int/2addr v2, v0
    invoke-static {v2}, Ljava/lang/Math;->abs(I)I
    move-result v2
    const/16 v3, 0x10
    if-lt v2, v3, :dynres_skip
    iput-short v0, v1, Lcom/winlator/xserver/ScreenInfo;->height:S
    mul-int/lit8 v2, v0, 0x10
    div-int/lit8 v2, v2, 0x9
    iput-short v2, v1, Lcom/winlator/xserver/ScreenInfo;->width:S
    iget-object v3, p0, Lcom/winlator/renderer/GLRenderer;->viewTransformation:Lcom/winlator/renderer/ViewTransformation;
    iget-short v4, p0, Lcom/winlator/renderer/GLRenderer;->surfaceWidth:S
    iget-short v5, p0, Lcom/winlator/renderer/GLRenderer;->surfaceHeight:S
    invoke-virtual {v3, v4, v5, v2, v0}, Lcom/winlator/renderer/ViewTransformation;->update(IIII)V
    :dynres_skip
"""
g = g.replace(head2, new2, 1)

# ---------- 3. onDrawFrame ----------
head3 = """.method public onDrawFrame(Ljavax/microedition/khronos/opengles/GL10;)V
    .locals 2
    .param p1, "gl"    # Ljavax/microedition/khronos/opengles/GL10;
"""
if g.count(head3) != 1:
    sys.exit("FAIL onDrawFrame head count=%d" % g.count(head3))

new3 = """.method public onDrawFrame(Ljavax/microedition/khronos/opengles/GL10;)V
    .locals 6
    .param p1, "gl"    # Ljavax/microedition/khronos/opengles/GL10;

    iget-object v0, p0, Lcom/winlator/renderer/GLRenderer;->viewTransformation:Lcom/winlator/renderer/ViewTransformation;
    iget-short v1, p0, Lcom/winlator/renderer/GLRenderer;->surfaceWidth:S
    iget-short v2, p0, Lcom/winlator/renderer/GLRenderer;->surfaceHeight:S
    iget-object v3, p0, Lcom/winlator/renderer/GLRenderer;->xServer:Lcom/winlator/xserver/XServer;
    iget-object v3, v3, Lcom/winlator/xserver/XServer;->screenInfo:Lcom/winlator/xserver/ScreenInfo;
    iget-short v4, v3, Lcom/winlator/xserver/ScreenInfo;->width:S
    iget-short v5, v3, Lcom/winlator/xserver/ScreenInfo;->height:S
    invoke-virtual {v0, v1, v2, v4, v5}, Lcom/winlator/renderer/ViewTransformation;->update(IIII)V
"""
g = g.replace(head3, new3, 1)

open(GR, "w", encoding="utf-8", newline="\n").write(g)
print("OK GLRenderer: dynres inserted, markers=%d" % g.count("DYNRES"))
