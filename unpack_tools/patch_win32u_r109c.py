"""r109c: 摘除 pragma O3（二分定位崩溃），保留 memset_32/16 的 libc 改写。
在 WSL 跑: python3 patch_win32u_r109c.py
"""
PATH = "/root/wine0905/dlls/win32u/dibdrv/primitives.c"

OLD = '''/* r109: 本文件热循环自动向量化（box64 对 SSE2 1:1 映射 NEON） */
#pragma GCC optimize("O3", "no-strict-aliasing")'''

NEW = '/* r109c: pragma O3 已摘除——r109/r109b 在标题 GUI 初始化期崩溃，二分定位中 */'

src = open(PATH, encoding="utf-8").read()
if NEW in src:
    print("already r109c")
elif OLD in src:
    src = src.replace(OLD, NEW)
    open(PATH, "w", encoding="utf-8", newline="\n").write(src)
    print("pragma removed -> r109c")
else:
    raise SystemExit("anchor not found; pragma count=%d" % src.count(OLD))
