import io
p = "/root/probe/smali/com/winlator/renderer/PerfStats.smali"
s = io.open(p, encoding="utf-8").read()
old = """    const-string v15, "/data/data/com.winlatcn/files/perf.txt"
    new-instance v9, Ljava/io/FileOutputStream;
    const/4 v6, 0x1
    invoke-direct {v9, v15, v6}, Ljava/io/FileOutputStream;-><init>(Ljava/lang/String;Z)V"""
new = """    const-string v15, "/data/data/com.winlatcn/files/perf.txt"
    new-instance v9, Ljava/io/FileOutputStream;
    invoke-direct {v9, v15}, Ljava/io/FileOutputStream;-><init>(Ljava/lang/String;)V"""
assert s.count(old) == 1, s.count(old)
io.open(p, "w", encoding="utf-8").write(s.replace(old, new))
print("overwrite mode patched")
