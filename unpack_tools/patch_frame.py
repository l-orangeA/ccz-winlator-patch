import io
base = "/root/probe/smali/com/winlator/renderer/"

# 1) onDrawFrame: inline frame_ok.txt write
p = base + "GLRenderer.smali"
s = io.open(p, encoding="utf-8").read()
old = """    invoke-static {}, Lcom/winlator/renderer/PerfStats;->onRender()V

    .line 98"""
new = """    :try_start_f
    const-string v0, "/data/data/com.winlatcn/files/frame_ok.txt"

    new-instance v1, Ljava/io/FileOutputStream;

    invoke-direct {v1, v0}, Ljava/io/FileOutputStream;-><init>(Ljava/lang/String;)V

    const-string v0, "frame"

    invoke-virtual {v0}, Ljava/lang/String;->getBytes()[B

    move-result-object v0

    invoke-virtual {v1, v0}, Ljava/io/OutputStream;->write([B)V

    invoke-virtual {v1}, Ljava/io/OutputStream;->close()V
    :try_end_f
    .catch Ljava/lang/Exception; {:try_start_f .. :try_end_f} :catch_f

    goto :after_f

    :catch_f
    move-exception v0

    :after_f
    invoke-static {}, Lcom/winlator/renderer/PerfStats;->onRender()V

    .line 98"""
assert s.count(old) == 1
io.open(p, "w", encoding="utf-8").write(s.replace(old, new))

# 2) PerfStats: drop append flag (overwrite)
p = base + "PerfStats.smali"
s = io.open(p, encoding="utf-8").read()
old = """    const-string v15, "/data/data/com.winlatcn/files/perf.txt"
    new-instance v9, Ljava/io/FileOutputStream;
    const/4 v6, 0x1
    invoke-direct {v9, v15, v6}, Ljava/io/FileOutputStream;-><init>(Ljava/lang/String;Z)V"""
new = """    const-string v15, "/data/data/com.winlatcn/files/perf.txt"
    new-instance v9, Ljava/io/FileOutputStream;
    invoke-direct {v9, v15}, Ljava/io/FileOutputStream;-><init>(Ljava/lang/String;)V"""
assert s.count(old) == 1
io.open(p, "w", encoding="utf-8").write(s.replace(old, new))
print("patched frame marker + overwrite mode")
