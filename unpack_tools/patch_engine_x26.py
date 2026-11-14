#!/usr/bin/env python3
"""X26_SYNC 引擎补丁：siglongjmp 回主循环后，强制 x26 = emu->eflags。

背景（可行性研究 §11–§13）：Wine 在信号 handler 里清了 TF，写回宿主 ucontext 也生效，
但 siglongjmp 恢复 callee-saved x26（= guest EFLAGS）时把它覆盖回 setjmp 时的 TF=1。
x64emu_t 布局：regs[16] 占 0..127，eflags 在 128；arm64_prolog 的 ldp x26,x27,[x0,128]
说明 x26 就是 guest EFLAGS 槽。

改动：
  X1 新建 src/dynarec/arm64/arm64_set_flags.S  (mov x26,x0; ret)
  X2 CMakeLists.txt 的 DYNAREC_ASM 加入该文件
  X3 dynarec.c 的 EmuRun setjmp 恢复分支调用它，并 clobber x26

在 WSL 跑: python3 /mnt/d/ccz_winlator_patch/unpack_tools/patch_engine_x26.py
"""
import os

ROOT = "/root/box64-0.4.4"
ASM = os.path.join(ROOT, "src/dynarec/arm64/arm64_set_flags.S")
CMAKE = os.path.join(ROOT, "CMakeLists.txt")
DYN = os.path.join(ROOT, "src/dynarec/dynarec.c")

ASM_SRC = """// X26_SYNC: force host x26 (= guest EFLAGS) from emu->eflags
.text
.align 4
.global arm64_set_flags
arm64_set_flags:
    mov x26, x0
    ret
"""

CMAKE_OLD = '        "${BOX64_ROOT}/src/dynarec/arm64/arm64_prolog.S"\n'
CMAKE_NEW = CMAKE_OLD + '        "${BOX64_ROOT}/src/dynarec/arm64/arm64_set_flags.S"\n'

DYN_OLD = """            #ifdef ANDROID
            if ((skip = SigSetJmp(*(JUMPBUFF*)emu->jmpbuf, 1)))
            #else
            if ((skip = SigSetJmp(emu->jmpbuf, 1)))
            #endif
            {
                dynarec_log(LOG_DEBUG, "Setjmp EmuRun, fs=0x%x will %sskip dynarec next\\n", emu->segs[_FS], (skip==3)?"not ":"");
"""

DYN_NEW = DYN_OLD + """                /* X26_SYNC: siglongjmp restored callee-saved x26 (= guest EFLAGS)
                   to its setjmp-time value, undoing TF clears done in the signal
                   handler. Re-load it from emu->eflags (layout: regs[16]@0, eflags@128). */
                extern void arm64_set_flags(uint64_t);
                arm64_set_flags(emu->eflags.x64);
                __asm__ __volatile__("" ::: "x26");
"""


def main():
    # X1
    if os.path.exists(ASM):
        print("X1 exists")
    else:
        open(ASM, "w", encoding="utf-8", newline="\n").write(ASM_SRC)
        print("X1 created", ASM)

    # X2
    c = open(CMAKE, encoding="utf-8", errors="replace").read()
    if "arm64_set_flags.S" in c:
        print("X2 already patched")
    else:
        n = c.count(CMAKE_OLD)
        if n != 1:
            raise SystemExit("X2 anchor count=%d" % n)
        open(CMAKE, "w", encoding="utf-8", newline="\n").write(c.replace(CMAKE_OLD, CMAKE_NEW, 1))
        print("X2 patched")

    # X3
    d = open(DYN, encoding="utf-8", errors="replace").read()
    if "X26_SYNC" in d:
        print("X3 already patched")
    else:
        n = d.count(DYN_OLD)
        if n != 1:
            raise SystemExit("X3 anchor count=%d" % n)
        open(DYN, "w", encoding="utf-8", newline="\n").write(d.replace(DYN_OLD, DYN_NEW, 1))
        print("X3 patched")


if __name__ == "__main__":
    main()
