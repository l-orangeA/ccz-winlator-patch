#!/usr/bin/env python3
"""从 WSL 源码树生成 fix110b 源码快照（取证脚本，可重复运行）。

依据（2026-11-08 实读）:
  * WSL 发行版 Ubuntu2004x64 的 /root/wine0905 就是产出 build/*.so 的那棵树；
    树当前停在 fix110d，其 dlls/ntdll/ntdll.so 与 build/ntdll_fix110d.so 同 md5。
  * patch_fix110c.py / patch_fix110d.py 只改 virtual.c，且各自有精确的 OLD/NEW 锚点。
  * 因此 signal_x86_64.c / thread.c 当前字节即 fix110b 时的字节；
    virtual.c 先回退 110d、再回退 110c 即 fix110b 时的字节。

输出: src/fix110b/{signal_x86_64.c,virtual.c,thread.c}
"""
import ast
import difflib
import hashlib
import os
import sys

WSL = r"\\wsl$\Ubuntu2004x64\root\wine0905"
ROOT = r"D:\ccz_winlator_patch"
TOOLS = os.path.join(ROOT, "unpack_tools")
DST = os.path.join(ROOT, "src", "fix110b")
NAMES = ("signal_x86_64.c", "virtual.c", "thread.c")


def md5(b):
    return hashlib.md5(b).hexdigest()


def load_pair(path):
    """从补丁脚本里静态取 OLD/NEW 常量，不执行脚本（脚本会就地改 WSL 树）。"""
    tree = ast.parse(open(path, encoding="utf-8").read())
    out = {}
    for node in tree.body:
        if isinstance(node, ast.Assign) and isinstance(node.targets[0], ast.Name):
            name = node.targets[0].id
            if name in ("OLD", "NEW"):
                out[name] = ast.literal_eval(node.value)
    if set(out) != {"OLD", "NEW"}:
        raise RuntimeError("missing OLD/NEW in " + path)
    return out["OLD"].encode(), out["NEW"].encode()


def revert(data, old, new, tag):
    n = data.count(new)
    if n != 1:
        raise RuntimeError("%s: NEW anchor count=%d (want 1)" % (tag, n))
    print("  %-10s revert OK (removed %d bytes)" % (tag, len(new) - len(old)))
    return data.replace(new, old, 1)


def main():
    if not os.path.isdir(os.path.join(WSL, "dlls", "ntdll", "unix")):
        sys.exit("WSL tree not reachable: " + WSL)

    # 证据 1: WSL 树产物 == build/ntdll_fix110d.so
    a = open(os.path.join(WSL, "dlls", "ntdll", "ntdll.so"), "rb").read()
    b = open(os.path.join(ROOT, "build", "ntdll_fix110d.so"), "rb").read()
    print("★ WSL tree ntdll.so   :", md5(a))
    print("  build/ntdll_fix110d.so:", md5(b), "=>", "SAME" if md5(a) == md5(b) else "DIFF")
    if md5(a) != md5(b):
        sys.exit("WSL tree is NOT the tree that produced build/*.so; abort")

    src = {}
    for name in NAMES:
        src[name] = open(os.path.join(WSL, "dlls", "ntdll", "unix", name), "rb").read()

    # 证据 2: c/d 只动过 virtual.c
    for name in ("signal_x86_64.c", "thread.c"):
        for tag in (b"fix110c", b"fix110d"):
            if tag in src[name]:
                sys.exit("%s unexpectedly contains %s" % (name, tag.decode()))
    print("★ signal_x86_64.c / thread.c: no fix110c/d markers (unchanged since fix110b)")

    # 回退 virtual.c: 先 110d 再 110c（110d 的行插在 KUSD 行与 110c 注释之间）
    v = src["virtual.c"]
    for tag in ("fix110d", "fix110c"):
        old, new = load_pair(os.path.join(TOOLS, "patch_%s.py" % tag))
        v = revert(v, old, new, tag)
    for tag in (b"fix110c", b"fix110d"):
        if tag in v:
            sys.exit("revert incomplete: %s still present" % tag.decode())
    for tag in (b"fix110b", b"FIX110 view", b"FIX110 pre-existing"):
        if tag not in v:
            sys.exit("fix110b marker missing: " + tag.decode())
    print("★ virtual.c reverted to fix110b (fix110b/FIX110 markers intact)")
    src["virtual.c"] = v

    os.makedirs(DST, exist_ok=True)
    print("\n=== src/fix110b snapshot ===")
    for name in NAMES:
        data = src[name]
        path = os.path.join(DST, name)
        open(path, "wb").write(data)
        old = open(os.path.join(ROOT, "src", name), "rb").read().decode("utf-8", "replace").splitlines()
        new = data.decode("utf-8", "replace").splitlines()
        delta = sum(1 for ln in difflib.unified_diff(old, new, n=0) if ln.startswith(("+", "-")) and not ln.startswith(("+++", "---")))
        print("  %-20s %s  %6d lines  diff-vs-src(11-02): %d lines" % (name, md5(data), len(new), delta))

    print("\nNOTE: 这三个文件是源码快照，不是编译产物。")
    print("      重建请用 unpack_tools/build_ntdll_fix110b.sh，产物 md5 应为 eddb74af96587b15589d430b1199a07b。")


if __name__ == "__main__":
    main()
