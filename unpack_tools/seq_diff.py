#!/usr/bin/env python3
"""对比 0.3.0 健康日志与 0.4.4-r113 死亡日志的事件序列,找第一个分歧.
sim 事件 = 行首大写标记 (WCTX/NTCONT/ALLOCPROBE/...)."""
import gzip, re, sys

def load(path, gz=False, limit=None):
    if gz:
        data = gzip.open(path, "rt", encoding="utf-8", errors="replace").read()
    else:
        data = open(path, "rb").read().decode("utf-8", "replace")
    evs = []
    for ln in data.splitlines():
        m = re.match(r"^([A-Z][A-Z0-9_]{2,20})", ln)
        if m:
            evs.append((m.group(1), ln))
    return evs[:limit] if limit else evs

A = r"D:\ccz_winlator_patch\unpack_tools\a0_trace"
h = load(A + r"\hwbp_a0.log.gz", gz=True)
d = load(A + r"\hang_raw.log")
print("0.3.0 events: %d   0.4.4-r113 events: %d" % (len(h), len(d)))

def sig(evs, n=60):
    """事件签名序列(前n个)"""
    return [e[0] for e in evs[:n]]

sh_, sd = sig(h, 120), sig(d, 120)
div = None
for i in range(min(len(sh_), len(sd))):
    if sh_[i] != sd[i]:
        div = i
        break
print("first divergence at event #%d" % div if div is not None else "first 120 events IDENTICAL")
if div is not None:
    print("--- 0.3.0 context ---")
    for k, ln in h[max(0, div-3):div+6]:
        print("  %s | %s" % (k, ln[:150]))
    print("--- 0.4.4-r113 context ---")
    for k, ln in d[max(0, div-3):div+6]:
        print("  %s | %s" % (k, ln[:150]))
else:
    print("--- 0.3.0 events 100-140 ---")
    for k, ln in h[100:140]:
        print("  %s | %s" % (k, ln[:150]))
    print("--- 0.4.4 events 100-end ---")
    for k, ln in d[100:]:
        print("  %s | %s" % (k, ln[:150]))
