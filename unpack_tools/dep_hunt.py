#!/usr/bin/env python3
"""在 fix110b 死亡日志里找 0x403a10 相关的映射/保护信息."""
data = open(r"D:\ccz_winlator_patch\unpack_tools\a0_trace\hang_raw.log", "rb").read().decode("utf-8", "replace")
lines = data.splitlines()
out = []
for i, ln in enumerate(lines):
    if any(k in ln for k in ("403a10", "403000", "VHF_", "SRE_", "HWBP_PERMS", "DEP", "execute")):
        pr = sum(1 for c in ln if c.isprintable())
        if len(ln) == 0 or pr / max(1, len(ln)) > 0.8:
            out.append("@%d  %s" % (i, ln[:200]))
open(r"C:\Users\MECHREV\AppData\Local\Temp\zcode\dep_hunt.txt", "w", encoding="utf-8").write("\n".join(out))
print("kept", len(out))
