#!/usr/bin/env python3
"""EFL_PROBE：只读探针，定位 0.4.4 下 TF 清不掉的具体分支。

插在 signals.c 的 `emu->eflags.x64 = old_eflags;` 之后（Wine handler 返回后、
memcmp 分支之前），只记录前 100 个信号，写 /home/xuser/efl_probe.log。
不改任何行为、不改 env、不走日志通道。

在 WSL 跑: python3 /mnt/d/ccz_winlator_patch/unpack_tools/patch_box64_044_efl_probe.py
"""
P = "/root/box64-0.4.4/src/libtools/signals.c"

OLD = """    emu->eflags.x64 = old_eflags;

    if(memcmp(sigcontext, &sigcontext_copy, sizeof(x64_ucontext_t))) {"""

NEW = """    emu->eflags.x64 = old_eflags;

    /* EFL_PROBE: read-only, first 100 signals */
    {
        static int efl_probe_n = 0;
        if (efl_probe_n++ < 100) {
            FILE *fp = fopen("/home/xuser/efl_probe.log", "a");
            if (fp) {
                uint64_t ce = sigcontext->uc_mcontext.gregs[X64_EFL];
                uint64_t co = sigcontext_copy.uc_mcontext.gregs[X64_EFL];
                fprintf(fp, "n=%d sig=%d old=%lx(tf=%d) ctx=%lx(tf=%d) copy=%lx(tf=%d) curdb=%d changed=%d\\n",
                        efl_probe_n, sig, old_eflags, (int)((old_eflags >> 8) & 1),
                        ce, (int)((ce >> 8) & 1), co, (int)((co >> 8) & 1),
                        (int)(cur_db != 0),
                        memcmp(sigcontext, &sigcontext_copy, sizeof(x64_ucontext_t)) != 0);
                fclose(fp);
            }
        }
    }

    if(memcmp(sigcontext, &sigcontext_copy, sizeof(x64_ucontext_t))) {"""


def main():
    s = open(P, encoding="utf-8", errors="replace").read()
    if "EFL_PROBE" in s:
        print("already patched")
        return
    n = s.count(OLD)
    if n != 1:
        raise SystemExit("anchor count=%d (want 1)" % n)
    open(P, "w", encoding="utf-8", newline="\n").write(s.replace(OLD, NEW, 1))
    print("EFL_PROBE inserted")


if __name__ == "__main__":
    main()
