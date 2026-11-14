#!/usr/bin/env python3
"""fix110c: 扫描跳过 PROT_NONE 映射（box64 保留区，wine 需要在里面分配）.
在 fix110b 树上增量. 在 WSL 跑: python3 patch_fix110c.py
"""
P = "/root/wine0905/dlls/ntdll/unix/virtual.c"

OLD = '''                if (lo <= 0x7ffe0000 && hi >= 0x7ffe1000) continue;  /* fix110b: KUSD page, wine re-creates it */
                if (find_view_range( (void *)lo, hi - lo )) continue;  /* already tracked */'''

NEW = '''                if (lo <= 0x7ffe0000 && hi >= 0x7ffe1000) continue;  /* fix110b: KUSD page, wine re-creates it */
                /* fix110c: skip PROT_NONE reservations — these are box64's guest-space
                   infrastructure; wine must be able to allocate within them */
                if (perms[0] == '-' && perms[1] == '-' && perms[2] == '-') continue;
                if (find_view_range( (void *)lo, hi - lo )) continue;  /* already tracked */'''

src = open(P, encoding="utf-8", errors="replace").read()
if "fix110c" in src:
    print("already patched")
elif src.count(OLD) == 1:
    open(P, "w", encoding="utf-8", newline="\n").write(src.replace(OLD, NEW))
    print("patched")
else:
    raise SystemExit("anchor count=%d" % src.count(OLD))
