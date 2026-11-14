#!/usr/bin/env python3
"""对齐 0.3.0 健康解包与 0.4.4 死亡的 sim 事件序列, 找壳行为的第一分歧.
sim 事件 = WCTX/ALLOCPROBE/NTCONT/SEGV_ALL/SRE_ENTRY/HWBP_DBG 等 (壳活动的指纹)."""
import gzip, re, collections

def events(path, gz):
    if gz:
        data = gzip.open(path, "rt", encoding="utf-8", errors="replace").read()
    else:
        data = open(path, "rb").read().decode("utf-8", "replace")
    evs = []
    for ln in data.splitlines():
        m = re.match(r"^(WCTX|ALLOCPROBE|NTCONT|SEGV_ALL|SRE_ENTRY|GCTX_DBG|GCTX2|HWBP_DBG|HWBP_PERMS|HWBP_ARMDBG|DRPROBE|HASL_DBG)\s?(.*)", ln)
        if m:
            evs.append((m.group(1), m.group(2)[:120]))
    return evs

h = events(r"D:\ccz_winlator_patch\unpack_tools\a0_trace\v030_full.log.gz", True)
d = events(r"D:\ccz_winlator_patch\unpack_tools\a0_trace\hang_raw.log", False)
print("0.3.0 sim events: %d   0.4.4 events: %d" % (len(h), len(d)))

# 类型序列对比
th = [e[0] for e in h]
td = [e[0] for e in d]
div = None
for i in range(min(len(th), len(td))):
    if th[i] != td[i]:
        div = i
        break
print("first type divergence at event #%d" % div if div is not None else "types identical for %d" % min(len(th), len(td)))
if div is not None:
    print("--- 0.3.0 around divergence ---")
    for k, detail in h[max(0, div-2):div+5]:
        print("  %s | %s" % (k, detail[:110]))
    print("--- 0.4.4 around divergence ---")
    for k, detail in d[max(0, div-2):div+5]:
        print("  %s | %s" % (k, detail[:110]))
else:
    # 类型相同但 0.4.4 更短——看 0.4.4 的最后事件
    print("--- 0.4.4 last 10 events ---")
    for k, detail in d[-10:]:
        print("  %s | %s" % (k, detail[:110]))
    print("--- 0.3.0 events at the same count ---")
    for k, detail in h[len(d)-10:len(d)+4]:
        print("  %s | %s" % (k, detail[:110]))
