#!/usr/bin/env python3
"""Let SIGTRAP handlers run under DynaCall instead of the interpreter.

Root cause found in RunFunctionHandler():
    #ifndef USE_CUSTOM_MEM
        dynarec = 0;   // kills JIT for ALL signal handlers
    #endif
The caller already sets dynarec=1 for sig != SIGSEGV, so this line throws
that away. We keep it for SIGSEGV (handlers may run inside malloc) but allow
it for SIGTRAP (TF single-step -> Wine trap_handler).

Also splits run time into SEGV vs TRAP so the effect is visible.
"""
p = "/tmp/b64prof-build/src/libtools/signals.c"
s = open(p).read()

# 1) global switch + per-signal accumulators
s = s.replace(
    "static unsigned long long b64p_prep, b64p_run, b64p_fin, b64p_n, b64p_segv;",
    "static unsigned long long b64p_prep, b64p_run, b64p_fin, b64p_n, b64p_segv;\n"
    "static unsigned long long b64p_run_segv, b64p_n_segv, b64p_run_trap, b64p_n_trap;\n"
    "static int b64p_allow_dynarec;", 1)

# 2) keep dynarec for SIGTRAP only
old = "#ifndef USE_CUSTOM_MEM\n    // because a signal can interupt a malloc-like function\n    // Dynarec cannot be used in signal handling unless custom malloc is used\n    dynarec = 0;\n#endif"
if old not in s:
    raise SystemExit("USE_CUSTOM_MEM block not found")
new = ("#ifndef USE_CUSTOM_MEM\n"
       "    /* B64PROF: keep interpreter for SIGSEGV (handler may run inside\n"
       "     * malloc), but allow DynaCall for SIGTRAP. */\n"
       "    if (!b64p_allow_dynarec) dynarec = 0;\n"
       "#endif")
s = s.replace(old, new, 1)

# 3) set the switch per signal, right where we already branch on sig
old2 = "    if (sig == SIGSEGV) {"
if old2 not in s:
    raise SystemExit("sig split not found")
new2 = "    b64p_allow_dynarec = (sig == SIGTRAP);\n    if (sig == SIGSEGV) {"
s = s.replace(old2, new2, 1)

# 4) split run accumulation by signal
old3 = ("        b64p_prep += (unsigned long long)(b64p_t1 - b64p_t0);\n"
        "        b64p_run  += (unsigned long long)(b64p_t2 - b64p_t1);")
if old3 not in s:
    raise SystemExit("run accum not found")
new3 = ("        b64p_prep += (unsigned long long)(b64p_t1 - b64p_t0);\n"
        "        b64p_run  += (unsigned long long)(b64p_t2 - b64p_t1);\n"
        "        if (sig == SIGSEGV) { b64p_run_segv += (unsigned long long)(b64p_t2 - b64p_t1); ++b64p_n_segv; }\n"
        "        else if (sig == SIGTRAP) { b64p_run_trap += (unsigned long long)(b64p_t2 - b64p_t1); ++b64p_n_trap; }")
s = s.replace(old3, new3, 1)

# 5) report the split
old4 = '                 b64p_n, b64p_prep / b64p_n, b64p_run / b64p_n, b64p_fin / b64p_n, b64p_segv);'
if old4 not in s:
    raise SystemExit("dump args not found")
new4 = ('                 b64p_n, b64p_prep / b64p_n, b64p_run / b64p_n, b64p_fin / b64p_n, b64p_segv);\n'
        '    n += snprintf(b + n, sizeof(b) - n, " segv_n=%llu segv_run=%llu trap_n=%llu trap_run=%llu\\n",\n'
        '                  b64p_n_segv, b64p_n_segv ? b64p_run_segv / b64p_n_segv : 0,\n'
        '                  b64p_n_trap, b64p_n_trap ? b64p_run_trap / b64p_n_trap : 0);')
s = s.replace(old4, new4, 1)

open(p, "w").write(s)
print("patched: SIGTRAP allowed under DynaCall + per-signal split")
