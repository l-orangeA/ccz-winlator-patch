#!/usr/bin/env python3
"""路线 A 的累积式重构补丁：a1 / a2 / a3，每级只比上一级多一个变量。

每次都从官方无补丁源码出发，由 build_reconstruct.sh 调用：
  python3 apply_reconstruct.py a1|a2|a3

A1 删内层双层 old_eflags 中的内层（538/566）
A2 再删外层保存/恢复（682/703/747）
A3 再补「memcmp 之前统一同步 TF 到 emu->eflags 与宿主 ucontext 的 xFlags 槽」
"""
import sys

P = "/root/box64-0.4.4/src/libtools/signals.c"

A1 = [
    (
        """    uint64_t old_eflags = emu->eflags.x64;
    emu->eflags.x64 = 0x202; // default flags for inside the signal handler""",
        """    /* A1: no pre-handler EFLAGS save; sigcontext is the single source */
    emu->eflags.x64 = 0x202; // default flags for inside the signal handler""",
    ),
    (
        """    emu->eflags.x64 = old_eflags;

    if(memcmp(sigcontext, &sigcontext_copy, sizeof(x64_ucontext_t))) {""",
        """    /* A1: no unconditional old_eflags restore */

    if(memcmp(sigcontext, &sigcontext_copy, sizeof(x64_ucontext_t))) {""",
    ),
]

A2 = [
    (
        """    x64flags_t old_eflags;
    deferred_flags_t old_df;""",
        """    /* A2: no outer EFLAGS save */
    deferred_flags_t old_df;""",
    ),
    (
        """    #define GO(A) old_##A = emu->A
    GO(eflags);
    GO(df);""",
        """    #define GO(A) old_##A = emu->A
    /* A2: no outer EFLAGS save */
    GO(df);""",
    ),
    (
        """    #define GO(A) emu->A = old_##A
    GO(eflags);
    GO(df);""",
        """    #define GO(A) emu->A = old_##A
    /* A2: no outer EFLAGS restore */
    GO(df);""",
    ),
]

A3 = [
    (
        """    /* A1: no unconditional old_eflags restore */

    if(memcmp(sigcontext, &sigcontext_copy, sizeof(x64_ucontext_t))) {""",
        """    /* A3: single source of truth — mirror only the TF bit from Wine's
       sigcontext into both emu->eflags and the host ARM64 xFlags slot (regs[26]).
       Other EFLAGS bits (notably RF) are left alone to avoid side effects. */
    {
        uint64_t tf = sigcontext->uc_mcontext.gregs[X64_EFL] & 0x100ULL;
        emu->eflags.x64 = (emu->eflags.x64 & ~0x100ULL) | tf;
        if (ucntx) {
            ucontext_t *hp = (ucontext_t *)ucntx;
            CONTEXT_REG(hp, xFlags) = (CONTEXT_REG(hp, xFlags) & ~0x100ULL) | tf;
        }
    }

    if(memcmp(sigcontext, &sigcontext_copy, sizeof(x64_ucontext_t))) {""",
    ),
]

PROBE_DEF = """static void a3_probe(const char *tag, int sig, x64emu_t *emu, x64_ucontext_t *sc, void *ucntx, void *db, void *jmpbuf)
{
    static int n = 0;
    FILE *fp;
    uint64_t emu_f, sc_f = 0, host_f = 0;
    if (n++ >= 120) return;
    emu_f = emu ? emu->eflags.x64 : 0;
    if (sc) sc_f = sc->uc_mcontext.gregs[X64_EFL];
    if (ucntx) host_f = CONTEXT_REG((ucontext_t *)ucntx, xFlags);
    fp = fopen("/home/xuser/a3probe.log", "a");
    if (!fp) return;
    fprintf(fp, "%s n=%d sig=%d emu=%lx(tf=%d) sc=%lx(tf=%d) host=%lx(tf=%d) db=%d jmp=%d\\n",
            tag, n, sig, (unsigned long)emu_f, (int)((emu_f >> 8) & 1),
            (unsigned long)sc_f, (int)((sc_f >> 8) & 1),
            (unsigned long)host_f, (int)((host_f >> 8) & 1), db != 0, jmpbuf != 0);
    fclose(fp);
}

"""

PROBE_POINTS = [
    (
        "static __thread int signal_jmpbuf_active = 0;",
        "static __thread int signal_jmpbuf_active = 0;\n\n/* A3_PROBE: read-only, first 120 events */\n" + PROBE_DEF.rstrip(),
    ),
    (
        """    sig = signal_to_x64(sig);
    x64emu_t* emu = thread_get_emu_no_create();""",
        """    sig = signal_to_x64(sig);
    x64emu_t* emu = thread_get_emu_no_create();
    a3_probe("h_sigaction", sig, emu, NULL, ucntx, NULL, NULL);""",
    ),
    (
        """void my_box64signalhandler(int32_t sig, siginfo_t* info, void * ucntx)
{""",
        """void my_box64signalhandler(int32_t sig, siginfo_t* info, void * ucntx)
{
    a3_probe("h_box64", sig, thread_get_emu_no_create(), NULL, ucntx, NULL, NULL);""",
    ),
    (
        """void my_sigactionhandler_oldcode(x64emu_t* emu, int32_t sig, int simple, siginfo_t* info, void * ucntx, int* old_code, void* cur_db, uintptr_t x64pc)
{
    #define GO(A) uintptr_t old_##A = R_##A;""",
        """void my_sigactionhandler_oldcode(x64emu_t* emu, int32_t sig, int simple, siginfo_t* info, void * ucntx, int* old_code, void* cur_db, uintptr_t x64pc)
{
    a3_probe("oldcode", sig, emu, NULL, ucntx, cur_db, emu ? emu->jmpbuf : NULL);
    #define GO(A) uintptr_t old_##A = R_##A;""",
    ),
    (
        """int my_sigactionhandler_oldcode_64(x64emu_t* emu, int32_t sig, int simple, siginfo_t* info, void * ucntx, int* old_code, void* cur_db)
{
    int Locks = unlockMutex();""",
        """int my_sigactionhandler_oldcode_64(x64emu_t* emu, int32_t sig, int simple, siginfo_t* info, void * ucntx, int* old_code, void* cur_db)
{
    a3_probe("old64_in", sig, emu, NULL, ucntx, cur_db, emu ? emu->jmpbuf : NULL);
    int Locks = unlockMutex();""",
    ),
    (
        """    emu->deferred_signal_pending[signum] = 1;
    return 1;
}""",
        """    emu->deferred_signal_pending[signum] = 1;
    a3_probe("defer_hit", signum, emu, NULL, NULL, NULL, NULL);
    return 1;
}""",
    ),
    (
        """            CONTEXT_REG(hp, xFlags) = (CONTEXT_REG(hp, xFlags) & ~0x100ULL) | tf;
        }
    }""",
        """            CONTEXT_REG(hp, xFlags) = (CONTEXT_REG(hp, xFlags) & ~0x100ULL) | tf;
        }
    }
    a3_probe("a3_sync", sig, emu, sigcontext, ucntx, cur_db, emu ? emu->jmpbuf : NULL);""",
    ),
    (
        """            if (ACCESS_FLAG(F_TF) && skip == 1) emu->flags.no_tf = 1;
            printf_log((sig==10)?LOG_DEBUG:log_minimum, "Context has been changed in Sigactionhanlder, doing siglongjmp""",
        """            if (ACCESS_FLAG(F_TF) && skip == 1) emu->flags.no_tf = 1;
            a3_probe("jmp", sig, emu, sigcontext, ucntx, cur_db, emu ? emu->jmpbuf : NULL);
            printf_log((sig==10)?LOG_DEBUG:log_minimum, "Context has been changed in Sigactionhanlder, doing siglongjmp""",
    ),
    (
        """    emu->eflags.x64=sigcontext->uc_mcontext.gregs[X64_EFL];
    if (ACCESS_FLAG(F_TF)) emu->flags.no_tf = 1;""",
        """    emu->eflags.x64=sigcontext->uc_mcontext.gregs[X64_EFL];
    if (ACCESS_FLAG(F_TF)) emu->flags.no_tf = 1;
    a3_probe("tail", sig, emu, sigcontext, ucntx, cur_db, emu ? emu->jmpbuf : NULL);""",
    ),
]

A3B = [
    (
        """        if(emu->jmpbuf) {
            #ifndef DYNAREC
            mctx2emu(emu, &sigcontext->uc_mcontext);""",
        """        if(0 && emu->jmpbuf) {   /* A3B: bypass siglongjmp so the kernel restores the ucontext we patched (xFlags TF) */
            #ifndef DYNAREC
            mctx2emu(emu, &sigcontext->uc_mcontext);""",
    ),
]

A3C = [
    (
        """            #ifdef ANDROID
            siglongjmp(*emu->jmpbuf, skip);
            #else
            siglongjmp(emu->jmpbuf, skip);
            #endif""",
        """            /* A3C: full context sync instead of siglongjmp.
               siglongjmp restores callee-saved x26 (= guest EFLAGS) to its
               setjmp-time value, undoing Wine's TF clear. Sync the whole
               patched context to the host ucontext and return to the kernel;
               PC goes to arm64_epilog, which writes the registers back to emu. */
            mctx2emu(emu, &sigcontext->uc_mcontext);
            copyEmu2USignalCTXreg(p, emu, native_epilog);
            return 0;""",
    ),
]

A3D = [
    (
        """            #ifdef ANDROID
            siglongjmp(*emu->jmpbuf, skip);
            #else
            siglongjmp(emu->jmpbuf, skip);
            #endif""",
        """            /* A3D: no siglongjmp, no epilog jump. Sync all guest registers and
               EFLAGS into the host ucontext, keep the original host PC, then
               return to the kernel so it restores the patched context. */
            {
                uintptr_t saved_pc = CONTEXT_PC(p);
                mctx2emu(emu, &sigcontext->uc_mcontext);
                copyEmu2USignalCTXreg(p, emu, saved_pc);
                return 0;
            }""",
    ),
]

STAGES = {
    "a1": A1,
    "a2": A1 + A2,
    "a3": A1 + A2 + A3,
    "a3probe": A1 + A2 + A3 + PROBE_POINTS,
    "a3b": A1 + A2 + A3 + A3B,
    "a3c": A1 + A2 + A3 + A3C,
    "a3d": A1 + A2 + A3 + A3D,
}


def main():
    stage = (sys.argv[1] if len(sys.argv) > 1 else "").lower()
    if stage not in STAGES:
        raise SystemExit("usage: apply_reconstruct.py a1|a2|a3")
    s = open(P, encoding="utf-8", errors="replace").read()
    if "/* A1:" in s or "/* A2:" in s or "/* A3:" in s:
        raise SystemExit("源码已有 A 系列补丁，请先复原官方树")
    for i, (old, new) in enumerate(STAGES[stage], 1):
        n = s.count(old)
        if n != 1:
            raise SystemExit("stage=%s anchor #%d count=%d (want 1)" % (stage, i, n))
        s = s.replace(old, new, 1)
        print("  patched #%d" % i)
    open(P, "w", encoding="utf-8", newline="\n").write(s)
    print("stage %s applied (%d anchors)" % (stage, len(STAGES[stage])))


if __name__ == "__main__":
    main()
