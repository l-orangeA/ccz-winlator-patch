#!/usr/bin/env python3
"""打印 0.3.0 日志 @1283-1300 的原始行（壳的 protect 舞步 + dump_view 子行）."""
import gzip

lines = gzip.open(r"D:\ccz_winlator_patch\unpack_tools\a0_trace\v030_full.log.gz", "rt", encoding="utf-8", errors="replace").read().splitlines()
out = []
for i in range(1280, 1305):
    if i < len(lines):
        out.append("@%d  %s" % (i, lines[i][:170]))
open(r"C:\Users\MECHREV\AppData\Local\Temp\zcode\raw_dump.txt", "w", encoding="utf-8").write("\n".join(out))
print("written", len(out))
