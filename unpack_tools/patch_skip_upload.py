import io
p = "/root/candU/smali/com/winlator/renderer/Texture.smali"
s = io.open(p, encoding="utf-8").read()
old = "    invoke-static/range {v0 .. v8}, Landroid/opengl/GLES20;->glTexSubImage2D(IIIIIIIILjava/nio/Buffer;)V\n"
assert s.count(old) == 1, s.count(old)
s = s.replace(old, "    # upload skipped for CPU-cost experiment\n")
io.open(p, "w", encoding="utf-8").write(s)
print("upload skipped")
