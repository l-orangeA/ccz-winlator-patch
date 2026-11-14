#!/usr/bin/env python3
"""DYNRES part 4: center the app window + frame-rate hint 120.

Why centering by moving the window (not by shifting the render):
  touch -> X screen coords -> window coords uses the SAME x/y as rendering,
  so moving the window keeps touch and picture consistent. Shifting only the
  render would desync them.

Also bumps Surface.setFrameRate(60f) -> 120f (experiment; the game only
produces ~20 fps of content, so expect no smoothness gain, just SF cost).
"""
import sys

GR = "/root/wn_gl60/smali/com/winlator/renderer/GLRenderer.smali"
SV = "/root/wn_gl60/smali/com/winlator/widget/XServerView.smali"

g = open(GR, encoding="utf-8").read()

# --- widen locals for v6..v9 in the two DYNRES methods ---
for head_old in (
    """.method public onMapWindow(Lcom/winlator/xserver/Window;)V
    .locals 6""",
    """.method public onUpdateWindowGeometry(Lcom/winlator/xserver/Window;Z)V
    .locals 6""",
):
    if g.count(head_old) != 1:
        sys.exit("FAIL locals head count=%d for %s" % (g.count(head_old), head_old[:40]))
    g = g.replace(head_old, head_old.replace(".locals 6", ".locals 10"), 1)

anchor = """    invoke-virtual {v3, v4, v5, v2, v0}, Lcom/winlator/renderer/ViewTransformation;->update(IIII)V"""
if g.count(anchor) != 2:
    sys.exit("FAIL update anchor count=%d" % g.count(anchor))

center = anchor + """
    # DYNRES-CENTER: move window to screen center (touch uses same coords)
    invoke-virtual {p1}, Lcom/winlator/xserver/Window;->getWidth()S
    move-result v6
    invoke-virtual {p1}, Lcom/winlator/xserver/Window;->getHeight()S
    move-result v7
    sub-int v8, v2, v6
    div-int/lit8 v8, v8, 0x2
    sub-int v9, v0, v7
    div-int/lit8 v9, v9, 0x2
    int-to-short v8, v8
    int-to-short v9, v9
    invoke-virtual {p1, v8}, Lcom/winlator/xserver/Window;->setX(S)V
    invoke-virtual {p1, v9}, Lcom/winlator/xserver/Window;->setY(S)V"""

g = g.replace(anchor, center)
open(GR, "w", encoding="utf-8", newline="\n").write(g)
print("OK center inserted, markers=%d" % g.count("DYNRES-CENTER"))

# --- frame rate 60 -> 120 ---
s = open(SV, encoding="utf-8").read()
old = """    const/high16 v1, 0x42700000    # 60.0f

    const/4 v2, 0x1

    invoke-virtual {v0, v1, v2}, Landroid/view/Surface;->setFrameRate(FI)V"""
if s.count(old) != 1:
    sys.exit("FAIL setFrameRate block count=%d" % s.count(old))
s = s.replace(old, old.replace("0x42700000    # 60.0f", "0x42f00000    # 120.0f"), 1)
open(SV, "w", encoding="utf-8", newline="\n").write(s)
print("OK frame rate -> 120f")
