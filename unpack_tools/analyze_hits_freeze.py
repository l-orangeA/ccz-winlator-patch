#!/usr/bin/env python3
"""定位 0.4.4 下 hits 冻结的根因：全量分析 fix110c_dr.log 的事件序列。

关键公式（src/fix110b/virtual.c）：
  hits = hwbp_cnt_hit，只在 hwbp_handle_rw_fault() 里 hwbp_match_write(addr) 非 0 时 +1；
  STEP_END 的 pend=hwbp_take_pending()，1..4 才 consume，9 = unmatched。
日志混杂二进制噪声，只取可打印行。
"""
import collections
import re
from pathlib import Path

P = Path(r"D:\ccz_winlator_patch\unpack_tools\a0_trace\fix110c_dr.log")

RE = {
    "step_end": re.compile(r"STEP_END pend=(\d+) rip=([0-9a-fA-F]+) cs=([0-9a-fA-F]+)"),
    "step_begin": re.compile(r"STEP_BEGIN idx=(\d+) rip=([0-9a-fA-F]+) fa=([0-9a-fA-F]+)"),
    "segv": re.compile(r"SEGV_ALL sig=\d+ code=\d+ rip=([0-9a-fA-F]+) addr=([0-9a-fA-F]+) err=(\d+)"),
    "update": re.compile(r"HWBP_DBG update dr0=([0-9a-fA-F]+) dr1=([0-9a-fA-F]+) dr2=\S+ dr3=\S+ dr7=([0-9a-fA-F]+)"),
    "arm": re.compile(r"HWBP_ARMDBG rw\d+ page=([0-9a-fA-F]+) vprot=([0-9a-fA-F]+) prot=([0-9a-fA-F]+)"),
    "perms": re.compile(r"HWBP_PERMS page=([0-9a-fA-F]+) maps=([0-9a-fA-F]+)-([0-9a-fA-F]+) (\S+)"),
    "trap": re.compile(r"TRAP sig=\d+ code=\d+ rip=([0-9a-fA-F]+) trap=(\d+) tf=(\d+)"),
    "vhf": re.compile(r"VHF_EXIT addr=([0-9a-fA-F]+) ret=([0-9a-fA-F]+)"),
}

pend = collections.Counter()
segv_rip = collections.Counter()
segv_addr = collections.Counter()
dr1 = collections.Counter()
dr7 = collections.Counter()
arm_page = collections.Counter()
trap_rip = collections.Counter()
vhf_ret = collections.Counter()
first_40_step = []
matched_lines = 0
n = 0

with P.open("rb") as f:
    for raw in f:
        n += 1
        ln = raw.decode("utf-8", "replace").strip()
        if len(ln) < 8:
            continue
        if sum(1 for c in ln if c.isprintable()) / len(ln) < 0.85:
            continue
        matched_lines += 1
        m = RE["step_end"].search(ln)
        if m:
            pend[m.group(1)] += 1
            if len(first_40_step) < 40:
                first_40_step.append((n, m.group(1), m.group(2)))
        m = RE["segv"].search(ln)
        if m:
            segv_rip[m.group(1)] += 1
            segv_addr[m.group(2)] += 1
        m = RE["update"].search(ln)
        if m:
            dr1[m.group(2)] += 1
            dr7[m.group(3)] += 1
        m = RE["arm"].search(ln)
        if m:
            arm_page[m.group(1)] += 1
        m = RE["trap"].search(ln)
        if m:
            trap_rip[m.group(1)] += 1
        m = RE["vhf"].search(ln)
        if m:
            vhf_ret[m.group(2)] += 1

print("总行数 %d，可打印行 %d" % (n, matched_lines))
print("\n=== STEP_END pend 分布（1..4 才 consume/hit）===")
for k, v in pend.most_common():
    print("  pend=%-3s %6d" % (k, v))
print("  前 40 个 STEP_END: " + " ".join("%s:%s" % (p, r[:6]) for _, p, r in first_40_step))
print("\n=== SEGV rip top10 ===")
for k, v in segv_rip.most_common(10):
    print("  %-10s %6d" % (k, v))
print("\n=== SEGV addr top10 ===")
for k, v in segv_addr.most_common(10):
    print("  %-10s %6d" % (k, v))
print("\n=== HWBP_DBG dr1 top10 / dr7 top10 ===")
for k, v in dr1.most_common(10):
    print("  dr1=%-10s %6d" % (k, v))
for k, v in dr7.most_common(10):
    print("  dr7=%-10s %6d" % (k, v))
print("\n=== HWBP_ARMDBG page top10 ===")
for k, v in arm_page.most_common(10):
    print("  page=%-10s %6d" % (k, v))
print("\n=== VHF_EXIT ret 分布 ===")
for k, v in vhf_ret.most_common(10):
    print("  ret=%-10s %6d" % (k, v))
print("\n=== TRAP rip top10 ===")
for k, v in trap_rip.most_common(10):
    print("  %-10s %6d" % (k, v))
