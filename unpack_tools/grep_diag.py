#!/usr/bin/env python3
"""提取 diag109.log 的 MPROT_FAIL/VMA/ALT 序列."""
data = open(r"D:\ccz_winlator_patch\unpack_tools\a0_trace\diag109.log", "rb").read().decode("utf-8", "replace")
out = []
for ln in data.splitlines():
    if any(k in ln for k in ("MPROT_FAIL", "MPROT_VMA", "MPROT_ALT", "MPROT_FALLBACK", "SRE_ENTRY", "VHF_ENTRY", "VHF_EXIT")):
        printable = sum(1 for c in ln if c.isprintable())
        if len(ln) == 0 or printable / max(1, len(ln)) > 0.85:
            out.append(ln[:200])
open(r"C:\Users\MECHREV\AppData\Local\Temp\zcode\diag_mprot.txt", "w", encoding="utf-8").write("\n".join(out))
print("kept", len(out), "lines")
