#!/usr/bin/env python3
"""列出 0.3.0 maps 里所有 >16MB 的 ---p 区域."""
m30 = open(r"D:\ccz_winlator_patch\unpack_tools\a0_trace\maps_030.txt", encoding="utf-8", errors="replace").read().splitlines()
big = []
for ln in m30:
    try:
        a, b = ln.split()[0].split("-")
        a, b = int(a, 16), int(b, 16)
        if b - a >= 16 * 1024 * 1024:
            big.append((b - a, ln))
    except Exception:
        pass
big.sort(reverse=True)
for sz, ln in big[:10]:
    print("%6.1f MB  %s" % (sz / 1048576, ln[:130]))
print("(total big-none:", len(big), ")")
