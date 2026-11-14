#!/usr/bin/env python3
"""DYNRES part 3: keep rootWindow in sync with the new screen size.

Without this, child windows keep their old coordinate frame (rootWindow is
still 1280x720), so after shrinking the screen the window content lands
off-screen -> the blank client area we saw.
"""
import sys

GR = "/root/wn_gl60/smali/com/winlator/renderer/GLRenderer.smali"
g = open(GR, encoding="utf-8").read()

# insert right after the width store in BOTH DYNRES blocks
anchor_a = """    iput-short v2, v1, Lcom/winlator/xserver/ScreenInfo;->width:S
    iget-object v3, p0, Lcom/winlator/renderer/GLRenderer;->viewTransformation:Lcom/winlator/renderer/ViewTransformation;
    iget-short v4, p0, Lcom/winlator/renderer/GLRenderer;->surfaceWidth:S
    iget-short v5, p0, Lcom/winlator/renderer/GLRenderer;->surfaceHeight:S
    invoke-virtual {v3, v4, v5, v2, v0}, Lcom/winlator/renderer/ViewTransformation;->update(IIII)V"""

count = g.count(anchor_a)
if count != 2:
    sys.exit("FAIL anchor count=%d (expected 2)" % count)

new_a = """    iput-short v2, v1, Lcom/winlator/xserver/ScreenInfo;->width:S
    # DYNRES: keep rootWindow in sync, otherwise child windows stay off-screen
    iget-object v3, p0, Lcom/winlator/renderer/GLRenderer;->xServer:Lcom/winlator/xserver/XServer;
    iget-object v3, v3, Lcom/winlator/xserver/XServer;->windowManager:Lcom/winlator/xserver/WindowManager;
    iget-object v3, v3, Lcom/winlator/xserver/WindowManager;->rootWindow:Lcom/winlator/xserver/Window;
    invoke-virtual {v3, v2}, Lcom/winlator/xserver/Window;->setWidth(S)V
    invoke-virtual {v3, v0}, Lcom/winlator/xserver/Window;->setHeight(S)V
    iget-object v3, p0, Lcom/winlator/renderer/GLRenderer;->viewTransformation:Lcom/winlator/renderer/ViewTransformation;
    iget-short v4, p0, Lcom/winlator/renderer/GLRenderer;->surfaceWidth:S
    iget-short v5, p0, Lcom/winlator/renderer/GLRenderer;->surfaceHeight:S
    invoke-virtual {v3, v4, v5, v2, v0}, Lcom/winlator/renderer/ViewTransformation;->update(IIII)V"""

g = g.replace(anchor_a, new_a)
open(GR, "w", encoding="utf-8", newline="\n").write(g)
print("OK rootWindow sync inserted in %d blocks" % count)
