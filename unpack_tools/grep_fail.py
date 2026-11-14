#!/usr/bin/env python3
"""提取 hang_raw.log 里的 MPROT_FAIL / SRE / VHF 行."""
data = open(r"D:\ccz_winlator_patch\unpack_tools\a0_trace\hang_raw.log", "rb").read().decode("utf-8", "replace")
out = []
for ln in data.splitlines():
    if any(k in ln for k in ("MPROT_FAIL", "SRE_ENTRY", "VHF_", "MPROT_FALLBACK", "SEGV_ALL")):
        printable = sum(1 for c in ln if c.isprintable())
        if len(ln) == 0 or printable / max(1, len(ln)) > 0.85:
            out.append(ln)
open(r"C:\Users\MECHREV\AppData\Local\Temp\zcode\mprot_fails.txt", "w", encoding="utf-8").write("\n".join(out))
print("kept", len(out), "lines")
