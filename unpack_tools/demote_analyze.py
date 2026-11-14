#!/usr/bin/env python3
"""demote_full.log.gz: 代码页(0x400000-0x600000)相关的全部保护事件时间线."""
import gzip, re, collections

lines = gzip.open(r"D:\ccz_winlator_patch\unpack_tools\a0_trace\demote_full.log.gz", "rt", encoding="utf-8", errors="replace").read().splitlines()
print("total lines:", len(lines))
out = []
for i, ln in enumerate(lines):
    if re.search(r"0x0{0,2}4[0-6][0-9a-f]{4}", ln) and any(k in ln for k in (
            "mprotect", "NtProtect", "View:", "unprotect", "protectDB", "delete", "MPROT", "unmap")):
        out.append("@%d  %s" % (i, ln[:200]))
print("code-page protection events:", len(out))
for ln in out[:60]:
    print("  " + ln)
# VHF/SRE 在代码页的事件（EXEC fault）
exec_faults = [(i, ln) for i, ln in enumerate(lines) if re.search(r"addr=0?0?4[0-6][0-9a-f]{4}.*err=8", ln)]
print("EXEC faults on code pages:", len(exec_faults))
for i, ln in exec_faults[:10]:
    print("  @%d  %s" % (i, ln[:180]))
