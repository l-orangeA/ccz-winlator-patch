#!/usr/bin/env python3
import re

data = open(r"D:\ccz_winlator_patch\unpack_tools\a0_trace\hang_raw.log", "rb").read().decode("utf-8", "replace")
lines = data.splitlines()
out = []
out.append("total lines: %d" % len(lines))
inits = [ln for ln in lines if "HWBP_INIT" in ln]
out.append("HWBP_INIT count: %d" % len(inits))
for ln in inits[-8:]:
    out.append("  " + ln[:80])
non = [ln for ln in lines if not re.match(r"^[A-Z][A-Z0-9_]{2,20}[ =]", ln) and ln.strip()]
out.append("non-sim lines: %d" % len(non))
for ln in non[-15:]:
    pr = sum(1 for c in ln if c.isprintable())
    if len(ln) == 0 or pr / max(1, len(ln)) > 0.8:
        out.append("  " + ln[:170])
open(r"C:\Users\MECHREV\AppData\Local\Temp\zcode\an044.txt", "w", encoding="utf-8").write("\n".join(out))
print("written")
