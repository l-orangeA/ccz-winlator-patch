#!/usr/bin/env python3
"""A0：固化并校验 0.4.4 作者版构建配置 + 产物 md5。

每次重构编译前跑一次；任一不符即停，避免 §7.2 的配置坑重现。
用 Windows venv python 跑（通过 UNC 读 WSL 文件）。
"""
import hashlib
import re
import sys
from pathlib import Path

W = Path(r"\\wsl$\Ubuntu2004x64\root\box64-0.4.4")
CACHE = W / "build" / "CMakeCache.txt"
BIN = W / "build" / "box64"
SRC = W / "src" / "libtools" / "signals.c"
WANT_MD5 = "a7360b1e71ef26e515ce78dc40bc97e6"
WANT = {
    "ARM64:BOOL": "1",
    "ARM_DYNAREC:BOOL": "ON",
    "BOX32:BOOL": "OFF",
    "CMAKE_BUILD_TYPE:STRING": "Release",
}
WANT_LINKER = "/data/data/com.winlatcn/files/imagefs/lib/ld-linux-aarch64.so.1"


def main():
    ok = True

    def chk(cond, msg):
        nonlocal ok
        print(("  OK   " if cond else "  FAIL ") + msg)
        if not cond:
            ok = False

    print("=== A0 构建配置校验 ===")
    if not CACHE.is_file():
        raise SystemExit("CMakeCache 不存在: " + str(CACHE))
    c = CACHE.read_text(encoding="utf-8", errors="replace")
    for key, want in WANT.items():
        m = re.search(r"^" + re.escape(key) + r"=(.*)$", c, re.M)
        got = m.group(1).strip() if m else "(缺失)"
        chk(got == want, "%-28s got=%s want=%s" % (key, got, want))
    m = re.search(r"^CMAKE_EXE_LINKER_FLAGS:STRING=(.*)$", c, re.M)
    linker = m.group(1).strip() if m else ""
    chk(WANT_LINKER in linker, "linker 指向 imagefs: " + (linker or "(空)"))

    if BIN.is_file():
        md5 = hashlib.md5(BIN.read_bytes()).hexdigest()
        chk(md5 == WANT_MD5, "build/box64 md5=%s want=%s" % (md5[:12], WANT_MD5[:12]))
    else:
        chk(False, "build/box64 缺失")

    s = SRC.read_text(encoding="utf-8", errors="replace")
    markers = [t for t in ("A1:", "A2:", "A3:", "TF_FIX", "EFL_PROBE", "NOTF_E10") if t in s]
    chk(not markers, "官方源码无残留补丁标记" + (" (发现 %s)" % markers if markers else ""))

    print("\n" + ("PASS：A0 通过，可进行重构编译" if ok else "FAIL：先修正构建配置"))
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
