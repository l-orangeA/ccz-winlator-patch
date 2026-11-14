#!/usr/bin/env python3
"""0.3.0 全程日志: 代码页(0x400000-0x600000)的 NtProtect/NtAlloc 操作时间线."""
import gzip, re

lines = gzip.open(r"D:\ccz_winlator_patch\unpack_tools\a0_trace\v030_full.log.gz", "rt", encoding="utf-8", errors="replace").read().splitlines()
print("total lines:", len(lines))
out = []
for i, ln in enumerate(lines):
    if re.search(r"(NtProtectVirtualMemory|NtAllocateVirtualMemory).*0x0{0,2}[4-5][0-9a-f]{5}", ln):
        out.append("@%d  %s" % (i, ln[:190]))
print("code-page ops:", len(out))
for ln in out[:45]:
    print("  " + ln)
# EXEC fault 检查
ef = [(i, ln) for i, ln in enumerate(lines) if re.search(r"err=8", ln) and "virtual" in ln]
print("DEP-looking faults:", len(ef))
for i, ln in ef[:6]:
    print("  @%d  %s" % (i, ln[:170]))
