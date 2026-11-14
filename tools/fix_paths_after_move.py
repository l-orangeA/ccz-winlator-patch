import os
import io

root = r"D:\ccz_winlator_patch"
old1 = r"D:\ccz_winlator_patch\unpack_tools"
new1 = r"D:\ccz_winlator_patch\unpack_tools"
old2 = "D:/ccz_winlator_patch/unpack_tools"
new2 = "D:/ccz_winlator_patch/unpack_tools"
exts = {".py", ".bat", ".cmd", ".ps1", ".md", ".txt", ".json", ".sh"}

updated = 0
for dp, dn, fn in os.walk(root):
    for f in fn:
        if os.path.splitext(f)[1].lower() not in exts:
            continue
        p = os.path.join(dp, f)
        try:
            s = io.open(p, encoding="utf-8", errors="ignore").read()
        except Exception:
            continue
        o = s
        s = s.replace(old1, new1).replace(old2, new2)
        if s != o:
            io.open(p, "w", encoding="utf-8").write(s)
            updated += 1
print("files updated", updated)

p = os.path.join(root, "unpack_tools", "auto_load.py")
s = io.open(p, encoding="utf-8").read()
s = s.replace(
    r'TMP = r"D:\ccz_winlator_patch\unpack_tools\auto_state.png"',
    'BASE = os.path.dirname(os.path.abspath(__file__))\nTMP = os.path.join(BASE, "auto_state.png")',
)
s = s.replace(
    r'r"D:\ccz_winlator_patch\unpack_tools\auto_shot.png"',
    'os.path.join(BASE, "auto_shot.png")',
)
s = s.replace(
    r'r"D:\ccz_winlator_patch\unpack_tools\no1.png"',
    'os.path.join(BASE, "no1.png")',
)
s = s.replace(
    r'r"D:\ccz_winlator_patch\unpack_tools\tap8.png"',
    'os.path.join(BASE, "tap8.png")',
)
io.open(p, "w", encoding="utf-8").write(s)
print("auto_load relative ok")

# venv activation scripts (not used by our probes, but keep them consistent)
for f in ("activate.bat", "activate", "activate.ps1", "Activate.ps1"):
    vp = os.path.join(root, "unpack_tools", ".venv", "Scripts", f)
    if os.path.exists(vp):
        s = io.open(vp, encoding="utf-8", errors="ignore").read()
        o = s
        s = s.replace("D:\\unpack_tools", "D:\\ccz_winlator_patch\\unpack_tools")
        if s != o:
            io.open(vp, "w", encoding="utf-8").write(s)
            print("venv updated", f)
