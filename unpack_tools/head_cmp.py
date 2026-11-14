#!/usr/bin/env python3
"""检查 0.4.4 日志有无 HASL; 打印两份日志的前 12 个 sim 事件."""
import gzip, re

def load(path, gz=False):
    if gz:
        data = gzip.open(path, "rt", encoding="utf-8", errors="replace").read()
    else:
        data = open(path, "rb").read().decode("utf-8", "replace")
    evs = []
    for ln in data.splitlines():
        m = re.match(r"^([A-Z][A-Z0-9_]{2,20})", ln)
        if m:
            evs.append((m.group(1), ln))
    return evs

A = r"D:\ccz_winlator_patch\unpack_tools\a0_trace"
h = load(A + r"\hwbp_a0.log.gz", gz=True)
d = load(A + r"\hang_raw.log")

hasl_h = [ln for k, ln in h if "HASL" in k]
hasl_d = [ln for k, ln in d if "HASL" in k]
print("0.3.0 HASL lines:", len(hasl_h), hasl_h[:2])
print("0.4.4 HASL lines:", len(hasl_d), hasl_d[:2])
print()
print("=== 0.3.0 first 12 events ===")
for k, ln in h[:12]:
    print("  %s | %s" % (k, ln[:130]))
print("=== 0.4.4 first 12 events ===")
for k, ln in d[:12]:
    print("  %s | %s" % (k, ln[:130]))
