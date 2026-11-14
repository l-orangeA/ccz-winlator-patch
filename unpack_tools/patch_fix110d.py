#!/usr/bin/env python3
"""fix110d: 扫描跳过低地址区域（0x10000-0x40000000）——壳的工作区。
只在 0x40000000（=1GB）以上注册视图，以下留给壳和 wine。
在 WSL 跑: python3 patch_fix110d.py
"""
P = "/root/wine0905/dlls/ntdll/unix/virtual.c"

OLD = '''                if (lo <= 0x7ffe0000 && hi >= 0x7ffe1000) continue;  /* fix110b: KUSD page, wine re-creates it */'''

NEW = '''                if (lo <= 0x7ffe0000 && hi >= 0x7ffe1000) continue;  /* fix110b: KUSD page, wine re-creates it */
                if (hi <= 0x40000000) continue;  /* fix110d: low addresses = packer workspace, don't track */'''

src = open(P, encoding="utf-8", errors="replace").read()
if "fix110d" in src:
    print("already patched")
elif src.count(OLD) == 1:
    open(P, "w", encoding="utf-8", newline="\n").write(src.replace(OLD, NEW))
    print("patched")
else:
    raise SystemExit("anchor count=%d" % src.count(OLD))
