import io
p = "/root/probe/smali/com/winlator/renderer/GLRenderer.smali"
s = io.open(p, encoding="utf-8").read()
start = s.index("    :try_start_f")
end = s.index("    invoke-static {}, Lcom/winlator/renderer/PerfStats;->onRender()V", start)
s = s[:start] + s[end:]
io.open(p, "w", encoding="utf-8").write(s)
print("removed inline frame marker")
