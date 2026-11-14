#!/usr/bin/env python3
"""JIT-HWBP pass 2f: real HOST SIGTRAP delivery.

box64 push hook calls raise(SIGTRAP) on match and writes a marker file.
Wine's trap_handler counts how many times it sees that marker (and removes
it), then swallows the signal. Page protection stays ON; no behaviour
change. Goal: prove a real host signal raised inside a JIT CALL reaches
Wine with a usable ucontext.
"""
import os
import shutil

SRC = "src/fix110b"
DST = "src/fix110b_jit6"
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
        "unsigned int hwbp_jit_marks = 0;\n"
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
        "        n += snprintf( b + n, sizeof(b) - n, \" jitmark=%u\", hwbp_jit_marks );\n"
        "    write( fd, b, n );",
        "stats jitmark")

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
        "extern unsigned int hwbp_jit_marks;\n"
        "\n"
        "static void trap_handler( int signal, siginfo_t *siginfo, void *sigcontext )\n"
        "{\n"
        "    EXCEPTION_RECORD rec = { 0 };\n"
        "    struct xcontext context;\n"
        "    ucontext_t *ucontext = init_handler( sigcontext );\n"
        "\n"
        "    if (getenv( \"WINE_HWBP_JIT\" )\n"
        "        && access( \"/home/xuser/hwbp_jit_trap.txt\", F_OK ) == 0)\n"
        "    {   /* real host SIGTRAP from box64's raise() */\n"
        "        unlink( \"/home/xuser/hwbp_jit_trap.txt\" );\n"
        "        ++hwbp_jit_marks;\n"
        "        leave_handler( ucontext );\n"
        "        return;\n"
        "    }\n",
        "marker branch")

open(p, "w", encoding="utf-8", newline="\n").write(s)
print("DONE: %s" % DST)
