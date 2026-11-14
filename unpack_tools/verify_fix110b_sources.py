#!/usr/bin/env python3
"""fix110b 源码快照 / 部署产物自检。Windows 与 WSL 均可跑（路径按脚本位置推导）。

用法:
  Windows: <venv>\\python.exe unpack_tools\\verify_fix110b_sources.py
  WSL    : python3 /mnt/d/ccz_winlator_patch/unpack_tools/verify_fix110b_sources.py
"""
import hashlib
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src", "fix110b")
SO = os.path.join(ROOT, "build", "ntdll_fix110b.so")

# 源码快照 md5（生成方式见 make_fix110b_snapshot.py）
SRC_MD5 = {
    "signal_x86_64.c": "b25f30c9281db5972f1b7f0a236a4ddd",
    "virtual.c": "34e21b514cab04d34dcba81ef4649018",
    "thread.c": "a0cb5648dcfa1ccfaec9a7b2a3d99e27",
}
SO_MD5 = "eddb74af96587b15589d430b1199a07b"

# 这些串必须存在于 fix110b.so（实证：build/ntdll_fix110b.so 二进制）
SO_MARKERS = (b"FIX110 view", b"FIX110 pre-existing", b"MPROT_VMA", b"MPROT_ALT", b"BPT_RIP_FIX")
# fix110c/d 的源码标记不能出现在快照里
FORBIDDEN = (b"fix110c", b"fix110d")


def md5(path):
    return hashlib.md5(open(path, "rb").read()).hexdigest()


def main():
    ok = True

    def chk(cond, msg):
        nonlocal ok
        print(("  OK   " if cond else "  FAIL ") + msg)
        if not cond:
            ok = False

    print("=== src/fix110b 源码快照 ===")
    for name, want in SRC_MD5.items():
        p = os.path.join(SRC, name)
        if not os.path.isfile(p):
            chk(False, name + " 缺失")
            continue
        chk(md5(p) == want, "%-20s %s" % (name, md5(p)))

    v = open(os.path.join(SRC, "virtual.c"), "rb").read()
    s = open(os.path.join(SRC, "signal_x86_64.c"), "rb").read()
    t = open(os.path.join(SRC, "thread.c"), "rb").read()
    chk(b"fix110b" in v and b"FIX110 view" in v, "virtual.c 含 fix110b / FIX110 view")
    chk(all(x not in v for x in FORBIDDEN), "virtual.c 不含 fix110c/d")
    chk(b"fix109-diag" in s and b"MPROT_VMA" in s, "signal_x86_64.c 含 fix109-diag（与 fix110b.so 一致）")
    chk(b"0xe00" in t, "thread.c WINE_SS 默认 0xe00")
    chk(b"WINE_HWBP_SOFT" in v, "virtual.c 含 HWBP 软模拟开关")

    print("=== build/ntdll_fix110b.so（当前部署版）===")
    if not os.path.isfile(SO):
        chk(False, "产物缺失")
    else:
        chk(md5(SO) == SO_MD5, "md5 %s" % md5(SO))
        data = open(SO, "rb").read()
        for m in SO_MARKERS:
            chk(m in data, "含 " + m.decode())

    print("\n" + ("PASS" if ok else "FAIL"))
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
