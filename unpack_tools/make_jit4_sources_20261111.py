#!/usr/bin/env python3
"""JIT-HWBP pass 2c (diagnostic, SINGLE VARIABLE).

Only ONE thing differs from the known-good jit6 baseline: box64 sets guest
TF on a push match. Page protection stays ON, so the shell still passes via
the original page-fault path. Wine just COUNTS how many TRCTRAPs arrive
without a pending page-fault step, using the marker file written by box64.

Goal: prove that setting TF inside a JIT CALL actually produces a host
SIGTRAP that reaches Wine's trap_handler. No #DB construction, no disabling
of page protection.
"""
import os
import shutil

SRC = "src/fix110b"
DST = "src/fix110b_jit4"
os.makedirs(DST, exist_ok=True)
for f in ("signal_x86_64.c", "virtual.c", "thread.c"):
    shutil.copy2(os.path.join(SRC, f), os.path.join(DST, f))


def sub(text, old, new, tag):
    n = text.count(old)
    if n != 1:
        raise SystemExit("FAIL %s: expected 1, got %d" % (tag, n))
    print("  OK %s" % tag)
    return text.replace(old, new, 1)


p = os.path.join(DST, "virtual.c")
v = open(p, encoding="utf-8").read()

v = sub(v,
        "static void hwbp_write_stats( void )\n{",
        "unsigned int hwbp_jit_traps = 0;\n"
        "int hwbp_pending_peek(void) { return hwbp_pending; }\n"
        "\n"
        "static void hwbp_jit_publish( unsigned long addr )\n"
        "{\n"
        "    static unsigned long last;\n"
        "    int fd; char b[40]; int n;\n"
        "    if (last == addr) return;\n"
        "    last = addr;\n"
        "    fd = open( \"/home/xuser/hwbp_watch.txt\",\n"
        "               O_WRONLY | O_CREAT | O_TRUNC, 0644 );\n"
        "    if (fd < 0) return;\n"
        "    n = snprintf( b, sizeof(b), \"%lx\\n\", addr );\n"
        "    if (write( fd, b, n ) < 0) { /* ignore */ }\n"
        "    close( fd );\n"
        "}\n"
        "\n"
        "static void hwbp_write_stats( void )\n{",
        "publish + counter")

v = sub(v,
        "                  hwbp_t_first > 0 ? hwbp_elapsed() : 0.0 );\n"
        "    write( fd, b, n );",
        "                  hwbp_t_first > 0 ? hwbp_elapsed() : 0.0 );\n"
        "    if (getenv( \"WINE_HWBP_JIT\" ))\n"
        "        n += snprintf( b + n, sizeof(b) - n, \" jittrap=%u\", hwbp_jit_traps );\n"
        "    write( fd, b, n );",
        "stats jittrap")

v = sub(v,
        "        hwbp_arm_one( hwbp_dr[i], len, rw );\n"
        "    }\n"
        "    if (!d7 && (hwbp_cnt_arm || hwbp_cnt_fault)) hwbp_write_stats();",
        "        hwbp_arm_one( hwbp_dr[i], len, rw );\n"
        "        if (getenv( \"WINE_HWBP_JIT\" )) hwbp_jit_publish( hwbp_dr[i] );\n"
        "    }\n"
        "    if (!d7 && (hwbp_cnt_arm || hwbp_cnt_fault)) hwbp_write_stats();",
        "publish on arm")

open(p, "w", encoding="utf-8", newline="\n").write(v)

p = os.path.join(DST, "signal_x86_64.c")
s = open(p, encoding="utf-8").read()

s = sub(s,
        "static void trap_handler( int signal, siginfo_t *siginfo, void *sigcontext )\n"
        "{\n"
        "    EXCEPTION_RECORD rec = { 0 };\n"
        "    struct xcontext context;\n"
        "    ucontext_t *ucontext = init_handler( sigcontext );\n",
        "extern unsigned int hwbp_jit_traps;\n"
        "extern int hwbp_pending_peek(void);\n"
        "\n"
        "static void trap_handler( int signal, siginfo_t *siginfo, void *sigcontext )\n"
        "{\n"
        "    EXCEPTION_RECORD rec = { 0 };\n"
        "    struct xcontext context;\n"
        "    ucontext_t *ucontext = init_handler( sigcontext );\n"
        "\n"
        "    if (getenv( \"WINE_HWBP_JIT\" ) && TRAP_sig(ucontext) == TRAP_x86_TRCTRAP)\n"
        "        ++hwbp_jit_traps;   /* count-only, never change behaviour */\n",
        "TF trap counter")

open(p, "w", encoding="utf-8", newline="\n").write(s)
print("DONE: %s" % DST)
