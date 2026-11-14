#!/usr/bin/env python3
"""NOTF_E10：在作者版配置上删除 signals.c 的两处 no_tf 写点（单变量验证）。

依据（报告 §10.3）：no_tf 是 0.4.4 新增机制，历史 r112c 只在 ntf 树
（错误构建配置）编过，从未在作者版配置上单独验证。
作者版 BOX32=OFF，signal32.c 不参与构建，故只需改 signals.c 两处。

在 WSL 跑: python3 /mnt/d/ccz_winlator_patch/unpack_tools/patch_box64_044_notf_author.py
"""
P = "/root/box64-0.4.4/src/libtools/signals.c"

PAIRS = [
    (
        "            if (ACCESS_FLAG(F_TF) && skip == 1) emu->flags.no_tf = 1;",
        "            /* NOTF_E10: removed no_tf=1 (siglongjmp path) */",
    ),
    (
        "    if (ACCESS_FLAG(F_TF)) emu->flags.no_tf = 1;",
        "    /* NOTF_E10: removed no_tf=1 (normal return path) */",
    ),
]


def main():
    s = open(P, encoding="utf-8", errors="replace").read()
    if "NOTF_E10" in s:
        print("already patched")
        return
    for i, (old, new) in enumerate(PAIRS, 1):
        n = s.count(old)
        if n != 1:
            raise SystemExit("anchor #%d count=%d (want 1)" % (i, n))
        s = s.replace(old, new, 1)
        print("patched #%d" % i)
    open(P, "w", encoding="utf-8", newline="\n").write(s)
    print("NOTF_E10 applied")


if __name__ == "__main__":
    main()
