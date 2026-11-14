import io
p = "/root/candU/smali/com/winlator/renderer/Texture.smali"
s = io.open(p, encoding="utf-8").read()
if ".field private static skip:I" not in s:
    s = s.replace(".field protected boolean needsUpdate = true",
                  ".field protected boolean needsUpdate = true\n.field private static skip:I")
old = """    iget-boolean v0, p0, Lcom/winlator/renderer/Texture;->needsUpdate:Z

    if-eqz v0, :cond_37

    .line 112"""
new = """    iget-boolean v0, p0, Lcom/winlator/renderer/Texture;->needsUpdate:Z

    if-eqz v0, :cond_37

    sget v11, Lcom/winlator/renderer/Texture;->skip:I
    xor-int/lit8 v11, v11, 0x1
    sput v11, Lcom/winlator/renderer/Texture;->skip:I
    if-eqz v11, :do_upload
    return-void

    :do_upload
    .line 112"""
assert s.count(old) == 1, s.count(old)
s = s.replace(old, new)
io.open(p, "w", encoding="utf-8").write(s)
print("toggle-upload patch applied")
