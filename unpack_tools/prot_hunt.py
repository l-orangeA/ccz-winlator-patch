#!/usr/bin/env python3
"""f110bv_virtual.log: NtProtectVirtualMemory 调用与死亡序列."""
data = open(r"D:\ccz_winlator_patch\unpack_tools\a0_trace\f110bv_virtual.log", "rb").read().decode("utf-8", "replace")
lines = data.splitlines()
out = []
# 死亡序列（VHF/SRE/wine Unhandled）
for i, ln in enumerate(lines):
    if any(k in ln for k in ("Unhandled", "VHF_ENTRY", "VHF_EXIT", "SRE_ENTRY", "MPROT_FAIL", "MPROT_ALT")):
        pr = sum(1 for c in ln if c.isprintable())
        if len(ln) == 0 or pr / max(1, len(ln)) > 0.8:
            out.append("@%d  %s" % (i, ln[:200]))
# 死亡点附近的 NtProtect（最后 60 行内的）
tail_start = max(0, len(lines) - 60)
for i in range(tail_start, len(lines)):
    if "NtProtectVirtualMemory" in lines[i]:
        out.append("@%d  %s" % (i, lines[i][:200]))
open(r"C:\Users\MECHREV\AppData\Local\Temp\zcode\prot_hunt.txt", "w", encoding="utf-8").write("\n".join(out))
print("total lines:", len(lines), " kept:", len(out))
