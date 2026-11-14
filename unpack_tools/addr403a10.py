#!/usr/bin/env python3
"""death_b64log.log: 追踪 0x403a10 的分配来源."""
import re

data = open(r"D:\ccz_winlator_patch\unpack_tools\a0_trace\death_b64log.log", "rb").read().decode("utf-8", "replace")
lines = data.splitlines()
# 找和 403a10 / 0x400000-0x410000 区域相关的分配
alloc = [(i, ln) for i, ln in enumerate(lines)
         if re.search(r"(ALLOCPROBE|mmap|mprotect).*0x4[0-5][0-9a-f]{5}", ln)]
print("allocations in 0x400000-0x5FFFFF region: %d" % len(alloc))
for i, ln in alloc[:20]:
    print("@%d  %s" % (i, ln[:190]))

# 找最后一个 NTCONT → 403a10 的跳转
ntcont = [(i, ln) for i, ln in enumerate(lines) if "NTCONT" in ln and "403a10" in ln]
print("\nNTCONT to 403a10: %d times" % len(ntcont))
for i, ln in ntcont[-3:]:
    print("@%d  %s" % (i, ln[:190]))
