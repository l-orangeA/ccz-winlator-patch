import io
p = "/root/candZ/smali/com/winlator/winhandler/WinHandler.smali"
s = io.open(p, encoding="utf-8").read()
s = s.replace('    invoke-static {p1, p2}, Lcom/winlator/winhandler/AffProbe;->log(II)V\n\n', '')
marker = '.method private synthetic lambda$setProcessAffinity$4'
start = s.index(marker)
end = s.index('.end method', start)
block = s[start:end]
old = '    invoke-direct {p0, v0}, Lcom/winlator/winhandler/WinHandler;->sendPacket(I)Z\n'
new = ('    invoke-direct {p0, v0}, Lcom/winlator/winhandler/WinHandler;->sendPacket(I)Z\n\n'
       '    move-result v0\n\n'
       '    invoke-static {p1, p2, v0}, Lcom/winlator/winhandler/AffProbe;->log3(III)V\n')
assert block.count(old) == 1, block.count(old)
block = block.replace(old, new)
s = s[:start] + block + s[end:]
io.open(p, "w", encoding="utf-8").write(s)
print("patched")
