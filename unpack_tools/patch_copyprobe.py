import io
p = "/root/candZ/smali/com/winlator/xserver/Drawable.smali"
s = io.open(p, encoding="utf-8").read()
marker = ".method public copyArea(SSSSSSLcom/winlator/xserver/Drawable;Lcom/winlator/xserver/GraphicsContext$Function;)V"
start = s.index(marker)
line_end = s.index("\n", start)
# insert right after the .param p8 block, before the first .line
seg = s[start:]
idx = seg.index('    .param p8, "gcFunction"')
idx_end = seg.index("\n", idx) + 1
insert = ('\n    move/from16 v0, p5\n\n    move/from16 v1, p6\n\n'
          '    invoke-static {v0, v1}, Lcom/winlator/xserver/BlitProbe;->onPut(II)V\n')
seg2 = seg[:idx_end] + insert + seg[idx_end:]
s = s[:start] + seg2
io.open(p, "w", encoding="utf-8").write(s)
print("copyArea probe patched")
