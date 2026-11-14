"""box64 0.4.4 补丁（r112c）：摘除信号返回路径的 no_tf 写点，恢复硬件忠实的 1:1 TF 单步。
保留 POPF/IRET 的延迟语义（x64run.c 的写点不动）。
在 WSL 跑: python3 patch_box64_044_notf.py <源树> <源树>
"""
import sys, os, shutil

SRC_REF = "/root/box64-0.4.4"
DST = "/root/box64-0.4.4ntf"

PAIRS = [
    ("/src/libtools/signals.c",
     "            if (ACCESS_FLAG(F_TF) && skip == 1) emu->flags.no_tf = 1;",
     "            /* r112c: removed no_tf=1 — hardware-faithful 1:1 TF stepping (VProtect needs it) */"),
    ("/src/libtools/signals.c",
     "    if (ACCESS_FLAG(F_TF)) emu->flags.no_tf = 1;",
     "    /* r112c: removed no_tf=1 */"),
    ("/src/libtools/signal32.c",
     "            if (ACCESS_FLAG(F_TF) && skip == 1) emu->flags.no_tf = 1;",
     "            /* r112c: removed no_tf=1 */"),
    ("/src/libtools/signal32.c",
     "    if(ACCESS_FLAG(F_TF)) emu->flags.no_tf = 1;",
     "    /* r112c: removed no_tf=1 */"),
]


def main():
    if not os.path.exists(DST):
        shutil.copytree(SRC_REF, DST, symlinks=True)
        print("copied tree ->", DST)
    else:
        print("tree exists:", DST)
    for rel, old, new in PAIRS:
        p = DST + rel
        src = open(p, encoding="utf-8", errors="replace").read()
        if new in src:
            print("already:", rel, repr(old[:40]))
            continue
        n = src.count(old)
        if n != 1:
            raise SystemExit("anchor in %s count=%d: %r" % (rel, n, old[:60]))
        open(p, "w", encoding="utf-8", newline="\n").write(src.replace(old, new))
        print("patched:", rel, repr(old[:50]))
    print("DONE")


if __name__ == "__main__":
    main()
