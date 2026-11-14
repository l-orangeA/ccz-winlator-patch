import io
p = "/root/candZ/smali/com/winlator/xserver/Drawable.smali"
s = io.open(p, encoding="utf-8").read()
old = """    .param p10, "totalHeight"    # S

    .line 86"""
new = """    .param p10, "totalHeight"    # S

    invoke-static {p5, p6}, Lcom/winlator/xserver/BlitProbe;->onPut(II)V

    .line 86"""
assert s.count(old) == 1, s.count(old)
s = s.replace(old, new)
io.open(p, "w", encoding="utf-8").write(s)
print("blit probe patched")
