import io
p = "/root/candZ/smali/com/winlator/xserver/XServer.smali"
s = io.open(p, encoding="utf-8").read()
old = """    .param p2, "dy"    # I

    .line 151"""
new = """    .param p2, "dy"    # I

    or-int v0, p1, p2
    if-nez v0, :nonzero
    return-void

    :nonzero
    .line 151"""
assert s.count(old) == 1, s.count(old)
s = s.replace(old, new)
io.open(p, "w", encoding="utf-8").write(s)
print("zero-delta early return added")
