#!/usr/bin/env python3
"""TF_FIX：修复 box64 0.4.4 把 Wine 清掉的 TF 又恢复回去。

EFL_PROBE 实证（a0_trace/efl_probe.log，36 行精确命中）：
    sig=5 old=10346(tf=1) ctx=10246(tf=0) copy=10346(tf=1) changed=1
Wine 在 handler 里通过 EFL_sig(ucontext) &= ~0x100 清掉 TF，
但 signals.c:563 的 `emu->eflags.x64 = old_eflags;` 无条件恢复 handler 前的
EFLAGS（TF=1），后续 mctx2emu/siglongjmp 分支不再纠正 → guest 永久单步。

修复：恢复 old_eflags 后，只把 TF(bit8) 从 Wine 修改后的 sigcontext 同步过来，
其它位保持 old_eflags。单点、不改变其它语义。

在 WSL 跑: python3 /mnt/d/ccz_winlator_patch/unpack_tools/patch_box64_044_tf_fix.py
"""
P = "/root/box64-0.4.4/src/libtools/signals.c"

OLD = """    emu->eflags.x64 = old_eflags;

    if(memcmp(sigcontext, &sigcontext_copy, sizeof(x64_ucontext_t))) {"""

NEW = """    emu->eflags.x64 = old_eflags;
    /* TF_FIX: Wine clears/sets TF in sigcontext via EFL_sig(); restoring
       old_eflags unconditionally drops that change and leaves the guest in
       permanent single-step (hwbp hits freeze at 25). Mirror only TF. */
    emu->eflags.x64 = (emu->eflags.x64 & ~0x100ULL)
                    | (sigcontext->uc_mcontext.gregs[X64_EFL] & 0x100ULL);

    if(memcmp(sigcontext, &sigcontext_copy, sizeof(x64_ucontext_t))) {"""


def main():
    s = open(P, encoding="utf-8", errors="replace").read()
    if "TF_FIX" in s:
        print("already patched")
        return
    n = s.count(OLD)
    if n != 1:
        raise SystemExit("anchor count=%d (want 1)" % n)
    open(P, "w", encoding="utf-8", newline="\n").write(s.replace(OLD, NEW, 1))
    print("TF_FIX inserted")


if __name__ == "__main__":
    main()
