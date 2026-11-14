import io
p = "/root/candZ/smali/com/winlator/xserver/Drawable.smali"
s = io.open(p, encoding="utf-8").read()

def block_replace(marker, old, new):
    global s
    start = s.index(marker)
    end = s.index(".end method", start)
    block = s[start:end]
    assert block.count(old) == 1, (marker, block.count(old))
    block = block.replace(old, new)
    s = s[:start] + block + s[end:]

def insert_after_param(marker, paramline, code):
    global s
    start = s.index(marker)
    seg = s[start:]
    idx = seg.index(paramline)
    idx_end = seg.index("\n", idx) + 1
    seg = seg[:idx_end] + code + seg[idx_end:]
    s = s[:start] + seg

# drawImage -> onImage
block_replace(".method public drawImage(SSSSSSBLjava/nio/ByteBuffer;SS)V",
              "Lcom/winlator/xserver/BlitProbe;->onPut(II)V",
              "Lcom/winlator/xserver/BlitProbe;->onImage(II)V")

# copyArea -> onCopy
block_replace(".method public copyArea(SSSSSSLcom/winlator/xserver/Drawable;Lcom/winlator/xserver/GraphicsContext$Function;)V",
              "Lcom/winlator/xserver/BlitProbe;->onPut(II)V",
              "Lcom/winlator/xserver/BlitProbe;->onCopy(II)V")

# fillRect
insert_after_param(".method public fillRect(IIIII)V", '    .param p5, "color"    # I',
    '\n    move/from16 v0, p3\n\n    move/from16 v1, p4\n\n'
    '    invoke-static {v0, v1}, Lcom/winlator/xserver/BlitProbe;->onFill(II)V\n')

# drawAlphaMaskedBitmap
insert_after_param(".method public drawAlphaMaskedBitmap(BBBBBBLcom/winlator/xserver/Drawable;Lcom/winlator/xserver/Drawable;)V",
    '    .param p8, "maskDrawable"    # Lcom/winlator/xserver/Drawable;',
    '\n    invoke-static {}, Lcom/winlator/xserver/BlitProbe;->onAlpha()V\n')

# drawLine
insert_after_param(".method public drawLine(IIIIII)V",
    '    .param p6, "lineWidth"    # I',
    '\n    invoke-static {}, Lcom/winlator/xserver/BlitProbe;->onLine()V\n')

io.open(p, "w", encoding="utf-8").write(s)
print("probe3 patched")
