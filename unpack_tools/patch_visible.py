import io
p = "/root/probe/smali/com/winlator/renderer/GLRenderer.smali"
s = io.open(p, encoding="utf-8").read()
old = """    .line 86
    const/4 v0, 0x0

    invoke-static {v0, v0, v0, v0}, Landroid/opengl/GLES20;->glClearColor(FFFF)V

    .line 87
    return-void"""
new = """    .line 86
    const/4 v0, 0x0

    const/high16 v1, 0x3f800000

    invoke-static {v1, v0, v1, v1}, Landroid/opengl/GLES20;->glClearColor(FFFF)V

    :try_start_s
    const-string v0, "/data/data/com.winlatcn/files/surface_ok.txt"

    new-instance v1, Ljava/io/FileOutputStream;

    invoke-direct {v1, v0}, Ljava/io/FileOutputStream;-><init>(Ljava/lang/String;)V

    const-string v0, "surface"

    invoke-virtual {v0}, Ljava/lang/String;->getBytes()[B

    move-result-object v0

    invoke-virtual {v1, v0}, Ljava/io/OutputStream;->write([B)V

    invoke-virtual {v1}, Ljava/io/OutputStream;->close()V
    :try_end_s
    .catch Ljava/lang/Exception; {:try_start_s .. :try_end_s} :catch_s

    goto :after_s

    :catch_s
    move-exception v0

    :after_s

    .line 87
    return-void"""
assert s.count(old) == 1, s.count(old)
s = s.replace(old, new)
io.open(p, "w", encoding="utf-8").write(s)
print("patched visible marker")
