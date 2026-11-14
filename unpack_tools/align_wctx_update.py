#!/usr/bin/env python3
"""对齐 WCTX（壳传给 NtSetContextThread 的原始 Dr）与 HWBP_DBG update（Wine 解析后的 Dr）。

若两者不一致 → 32 位 CONTEXT 在 box64 0.4.4 的 sigcontext 路径被写错（P2 假设）。
另输出第 25 次命中（pend=2）前后的事件上下文。
"""
import re
from pathlib import Path

P = Path(r"D:\ccz_winlator_patch\unpack_tools\a0_trace\fix110c_dr.log")
WCTX = re.compile(r"WCTX .*?dr1=([0-9a-fA-F]+) dr7=([0-9a-fA-F]+)")
UPD = re.compile(r"HWBP_DBG update dr0=\S+ dr1=([0-9a-fA-F]+) .*?dr7=([0-9a-fA-F]+)")
STEP = re.compile(r"STEP_END pend=(\d+) rip=([0-9a-fA-F]+)")
INTERESTING = ("WCTX", "HWBP_DBG update", "STEP_END", "SEGV_ALL", "HWBP_ARMDBG", "VHF_EXIT", "TRAP")

pairs = []
pending_wctx = None
hit2 = 0
freeze_line = None
events = []

with P.open("rb") as f:
    for n, raw in enumerate(f, 1):
        ln = raw.decode("utf-8", "replace").strip()
        if len(ln) < 8 or sum(1 for c in ln if c.isprintable()) / len(ln) < 0.85:
            continue
        m = STEP.search(ln)
        if m:
            if m.group(1) in ("1", "2", "3", "4"):
                hit2 += 1
                if hit2 == 25 and freeze_line is None:
                    freeze_line = n
        m = WCTX.search(ln)
        if m:
            pending_wctx = (n, m.group(1), m.group(2), ln[:150])
            continue
        m = UPD.search(ln)
        if m:
            pairs.append((pending_wctx, (n, m.group(1), m.group(2), ln[:150])))
            pending_wctx = None
        if freeze_line and abs(n - freeze_line) <= 260 and any(k in ln for k in INTERESTING):
            events.append((n, ln[:170]))

print("第 25 次命中所在行:", freeze_line, " 总 pend=1..4 次数:", hit2)
print("\n=== WCTX（壳给的 Dr）→ HWBP_DBG update（Wine 收到的 Dr），前 45 对 ===")
same = diff = 0
for i, (w, u) in enumerate(pairs[:45]):
    if w is None:
        print("  %2d  (无 WCTX)  -> dr1=%s dr7=%s" % (i, u[1], u[2]))
        continue
    ok = (w[1].lower() == u[1].lower() and w[2].lower() == u[2].lower())
    same += ok
    diff += (not ok)
    print("  %2d  @%-7d dr1=%-9s dr7=%-8s -> @%-7d dr1=%-9s dr7=%-8s %s" %
          (i, w[0], w[1], w[2], u[0], u[1], u[2], "OK" if ok else "★ MISMATCH"))
print("  前 45 对: 一致 %d / 不一致 %d" % (same, diff))

print("\n=== 全部配对中 Dr1 不一致的分布 ===")
bad = [(w, u) for w, u in pairs if w and (w[1].lower() != u[1].lower() or w[2].lower() != u[2].lower())]
print("  总配对 %d，不一致 %d" % (len(pairs), len(bad)))
for w, u in bad[:12]:
    print("  @%d dr1=%s dr7=%s  ->  @%d dr1=%s dr7=%s" % (w[0], w[1], w[2], u[0], u[1], u[2]))

print("\n=== 第 25 次命中附近事件（前后 260 行内的可打印关键行）===")
for n, ln in events[:70]:
    print("  @%-7d %s" % (n, ln))
