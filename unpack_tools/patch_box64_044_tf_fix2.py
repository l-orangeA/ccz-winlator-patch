#!/usr/bin/env python3
"""TF_FIX2：修复外层 my_sigactionhandler_oldcode 二次恢复 EFLAGS 冲掉 TF。

EFL_PROBE 实证：Wine 清了 TF（ctx tf=0），内层 my_sigactionhandler_oldcode_64
也会把 emu EFLAGS 同步成 sigcontext（:632 / mctx2emu）。但外层在调用返回后：
    #define GO(A) emu->A = old_##A
    GO(eflags);            <-- 无条件恢复成外层进入时的 EFLAGS（TF=1）
于是 guest 永久单步。E7（只改内层）无效，正是被这里冲掉。

修复：外层恢复前先记下内层结果的 TF 位，恢复后只把 TF 位写回。
同一逻辑的两处改动，算一个变量。

在 WSL 跑: python3 /mnt/d/ccz_winlator_patch/unpack_tools/patch_box64_044_tf_fix2.py
"""
P = "/root/box64-0.4.4/src/libtools/signals.c"

A1_OLD = """    direct_ret = my_sigactionhandler_oldcode_64(emu, sig, simple, info, ucntx, old_code, cur_db);
    if(direct_ret)
        return;"""
A1_NEW = """    direct_ret = my_sigactionhandler_oldcode_64(emu, sig, simple, info, ucntx, old_code, cur_db);
    if(direct_ret)
        return;
    uint64_t inner_tf = emu->eflags.x64 & 0x100ULL;  /* TF_FIX2: TF decided by inner handler / Wine */"""

A2_OLD = """    #define GO(A) emu->A = old_##A
    GO(eflags);
    GO(df);"""
A2_NEW = """    #define GO(A) emu->A = old_##A
    GO(eflags);
    emu->eflags.x64 = (emu->eflags.x64 & ~0x100ULL) | inner_tf;  /* TF_FIX2 */
    GO(df);"""


def main():
    s = open(P, encoding="utf-8", errors="replace").read()
    if "TF_FIX2" in s:
        print("already patched")
        return
    for i, (old, new) in enumerate(((A1_OLD, A1_NEW), (A2_OLD, A2_NEW)), 1):
        n = s.count(old)
        if n != 1:
            raise SystemExit("anchor #%d count=%d (want 1)" % (i, n))
        s = s.replace(old, new, 1)
        print("patched #%d" % i)
    open(P, "w", encoding="utf-8", newline="\n").write(s)
    print("TF_FIX2 applied")


if __name__ == "__main__":
    main()
