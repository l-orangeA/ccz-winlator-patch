#!/usr/bin/env python3
"""打印 hang_raw.log 尾部 N 行,UTF-8 安全."""
import sys
lines = open(r"D:\ccz_winlator_patch\unpack_tools\a0_trace\hang_raw.log", "rb").read().decode("utf-8", "replace").splitlines()
n = int(sys.argv[1]) if len(sys.argv) > 1 else 40
out = []
for ln in lines[-n:]:
    try:
        out.append(ln.encode("gbk", "replace").decode("gbk"))
    except Exception:
        out.append(ln.encode("ascii", "backslashreplace").decode("ascii"))
sys.stdout.buffer.write(("\n".join(out) + "\n").encode("utf-8"))
