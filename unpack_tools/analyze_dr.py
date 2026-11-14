#!/usr/bin/env python3
"""分析 fix110c_dr.log: Dr 匹配失败的模式.
提取 WCTX(H壳设的监视地址), SEGV_ALL(故障地址), MPROT(保护变化), 匹配结果.
"""
import re, sys

path = r"D:\ccz_winlator_patch\unpack_tools\a0_trace\fix110c_dr.log"
# 只分析前 3000 行（解包期的关键段）
wctx = []       # 壳设的 Dr 值
segs = []       # SEGV 故障
dbg = []        # HWBP_DBG (sim 的 Dr 跟踪)
match_fail = 0
match_ok = 0
count = 0
with open(path, "rb") as f:
    for raw in f:
        count += 1
        if count > 50000: break
        try:
            ln = raw.decode("utf-8", "replace")
        except Exception:
            continue
        ln = ln.strip()
        if "WCTX" in ln:
            m = re.search(r"dr1=([0-9a-f]+).*dr7=([0-9a-f]+)", ln)
            if m:
                wctx.append((count, m.group(1), m.group(2)))
        elif "SEGV_ALL" in ln:
            m = re.search(r"rip=(\S+) addr=(\S+) err=(\S+)", ln)
            if m:
                segs.append((count, m.group(1), m.group(2), m.group(3)))
        elif "HWBP_DBG update" in ln:
            m = re.search(r"dr1=([0-9a-f]+).*dr7=([0-9a-f]+)", ln)
            if m:
                dbg.append((count, m.group(1), m.group(2)))

print(f"analyzed {count} lines")
print(f"WCTX (shell sets Dr): {len(wctx)}")
print(f"SEGV_ALL (faults): {len(segs)}")
print(f"HWBP_DBG (sim tracks): {len(dbg)}")

if wctx:
    print("\n=== WCTX: shell sets Dr1/Dr7 ===")
    for c, d1, d7 in wctx[:15]:
        print(f"  line={c} dr1={d1} dr7={d7}")
    # 去重看不同的 Dr1 值
    seen = set()
    print("  unique Dr1 values:", end=" ")
    for c, d1, d7 in wctx:
        if d1 not in seen:
            print(d1, end=" ")
            seen.add(d1)
    print()

if segs:
    print("\n=== SEGV faults (first 20) ===")
    for c, rip, addr, err in segs[:20]:
        print(f"  line={c} rip={rip} addr={addr} err={err}")

# 看 SEGV 的 fault addr 是否匹配 Dr1
if wctx and segs:
    last_dr1 = wctx[-1][1] if wctx else "0"
    print(f"\n=== last Dr1 = {last_dr1} ===")
    # 检查最近的 SEGV addr 是否匹配
    match = sum(1 for c, rip, addr, err in segs if addr == last_dr1)
    print(f"faults matching last Dr1: {match}/{len(segs)}")
