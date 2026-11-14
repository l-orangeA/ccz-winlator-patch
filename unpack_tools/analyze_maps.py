#!/usr/bin/env python3
"""分析 maps_r113_full.txt: 8ae000 所在区域 / 巨区 / 低区概览."""
lines = open(r"D:\ccz_winlator_patch\unpack_tools\a0_trace\maps_r113_full.txt", encoding="utf-8", errors="replace").read().splitlines()
print("total maps lines:", len(lines))
tgt = 0x8ae000
print("=== region containing 0x8ae000 ===")
for ln in lines:
    try:
        a, b = ln.split()[0].split("-")
        if int(a, 16) <= tgt < int(b, 16):
            print("  " + ln[:170])
    except Exception:
        pass
print("=== regions overlapping the 0x70a000-0x30110000 span ===")
lo, hi = 0x70a000, 0x30110000
for ln in lines:
    try:
        a, b = ln.split()[0].split("-")
        a, b = int(a, 16), int(b, 16)
    except Exception:
        continue
    if a < hi and b > lo:
        print("  " + ln[:170])
print("=== biggest NONE regions ===")
none = []
for ln in lines:
    try:
        a, b = ln.split()[0].split("-")
        if "---p" in ln:
            none.append((int(b, 16) - int(a, 16), ln))
    except Exception:
        pass
none.sort(reverse=True)
for sz, ln in none[:5]:
    print("  %6.1f MB  %s" % (sz / 1048576, ln[:150]))
