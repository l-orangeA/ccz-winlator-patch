#!/usr/bin/env python3
"""提取两份日志里 wine 自己的 stderr 行."""
import gzip, re

A = r"D:\ccz_winlator_patch\unpack_tools\a0_trace"
h = gzip.open(A + r"\hwbp_a0.log.gz", "rt", encoding="utf-8", errors="replace").read().splitlines()
d = open(A + r"\hang_raw.log", "rb").read().decode("utf-8", "replace").splitlines()

def wine_lines(lines, tag, limit=30):
    out = [ln for ln in lines if not re.match(r"^[A-Z][A-Z0-9_]{2,20}[ =]", ln) and ln.strip()]
    print("=== %s: %d non-sim lines ===" % (tag, len(out)))
    for ln in out[:limit]:
        print("  " + ln[:180])

wine_lines(h, "0.3.0")
wine_lines(d, "0.4.4-r113")
