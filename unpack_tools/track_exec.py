#!/usr/bin/env python3
"""demote_full.log.gz: 追踪 image 视图中 0x401000-0x409fff 的保护变化时间线."""
import gzip, re

lines = gzip.open(r"D:\ccz_winlator_patch\unpack_tools\a0_trace\demote_full.log.gz", "rt", encoding="utf-8", errors="replace").read().splitlines()
print("total lines:", len(lines))
# 每次 dump_view 中 0x401000-0x409fff 或覆盖它的子行的保护
timeline = []
i = 0
last_prot = None
cur_line_no = None
while i < len(lines):
    ln = lines[i]
    if "dump_view" in ln and "View: 0x400000" in ln:
        # 进入 image 视图的 dump 块
        j = i + 1
        found = None
        while j < len(lines) and j < i + 12:
            m = re.match(r"\s+(0x[0-9a-f]+) - (0x[0-9a-f]+) ([crw-]{4})", lines[j])
            if m:
                a, b = int(m.group(1), 16), int(m.group(2), 16)
                if a <= 0x403a10 < b:
                    found = (m.group(1), m.group(2), m.group(3))
                    break
            j += 1
        if found and found[2] != last_prot:
            timeline.append("@%d: 0x403a10 in %s-%s prot=%s" % (i, found[0], found[1], found[2]))
            last_prot = found[2]
        i = j if j > i else i + 1
    else:
        i += 1
print("=== 0x403a10 保护变化时间线 (%d 次变化) ===" % len(timeline))
for t in timeline[:20]:
    print("  " + t)
# 附: NtProtect 调用中目标在 0x401000-0x40a000 的
print("=== NtProtect calls targeting 0x401000-0x40a000 ===")
cnt = 0
for i, ln in enumerate(lines):
    if "NtProtectVirtualMemory" in ln:
        m = re.search(r"(0x0{0,2}4[0-9a-f]{5}) ([0-9a-f]{8}) ([0-9a-f]{8})$", ln)
        if m:
            a = int(m.group(1), 16)
            if 0x401000 <= a < 0x40a000:
                cnt += 1
                if cnt <= 8:
                    print("  @%d  %s" % (i, ln[:150]))
print("  (total %d)" % cnt)
