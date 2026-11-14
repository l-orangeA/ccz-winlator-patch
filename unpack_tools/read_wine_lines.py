#!/usr/bin/env python3
"""读 wine_lines.txt,过滤可打印行,写纯文本."""
data = open(r"C:\Users\MECHREV\AppData\Local\Temp\zcode\wine_lines.txt", "rb").read().decode("utf-8", "replace")
out = []
for ln in data.splitlines():
    printable = sum(1 for c in ln if c.isprintable() or c in "\t")
    if len(ln) > 0 and printable / len(ln) > 0.9:
        out.append(ln)
open(r"C:\Users\MECHREV\AppData\Local\Temp\zcode\wine_lines_clean.txt", "w", encoding="utf-8").write("\n".join(out))
print("kept %d / %d lines" % (len(out), len(data.splitlines())))
