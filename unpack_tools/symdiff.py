#!/usr/bin/env python3
"""对比原版与 r109c win32u.so 的动态符号表（导出/导入），找加载期致命差异。
在 WSL 跑: python3 symdiff.py
"""
import subprocess, sys

ORIG = "/mnt/d/ccz_winlator_patch/device_backup/win32u_orig_20261106/win32u.so"
NEW = "/mnt/d/ccz_winlator_patch/build/win32u_r109c.so"


def defined(path):
    out = subprocess.run(["readelf", "--dyn-syms", "-W", path],
                         capture_output=True, text=True).stdout
    syms = set()
    for line in out.splitlines():
        if " UND " in line or "FUNC" not in line and "OBJECT" not in line and "IFUNC" not in line:
            parts = line.split()
            if len(parts) >= 8 and parts[6] != "UND":
                syms.add(parts[7].split("@")[0])
    return syms


def undefined(path):
    out = subprocess.run(["readelf", "--dyn-syms", "-W", path],
                         capture_output=True, text=True).stdout
    syms = set()
    for line in out.splitlines():
        parts = line.split()
        if len(parts) >= 8 and parts[6] == "UND":
            syms.add(parts[7].split("@")[0])
    return syms


o, n = defined(ORIG), defined(NEW)
uo, un = undefined(ORIG), undefined(NEW)
print("defined: orig=%d new=%d" % (len(o), len(n)))
print("undefined: orig=%d new=%d" % (len(uo), len(un)))
only_o = sorted(o - n)
only_n = sorted(n - o)
print("\n== only in ORIG exports (%d) ==" % len(only_o))
for s in only_o[:40]:
    print("  " + s)
print("\n== only in NEW exports (%d) ==" % len(only_n))
for s in only_n[:40]:
    print("  " + s)
print("\n== UND only in NEW (%d) ==" % len(un - uo))
for s in sorted(un - uo)[:40]:
    print("  " + s)
print("\n== UND only in ORIG (%d) ==" % len(uo - un))
for s in sorted(uo - un)[:40]:
    print("  " + s)
# 关键符号点名
for key in ("__wine_syscall_dispatcher", "wine_syscall_dispatcher", "Wow64Transition"):
    print("key '%s': orig=%s new=%s" % (key, key in o, key in n))
