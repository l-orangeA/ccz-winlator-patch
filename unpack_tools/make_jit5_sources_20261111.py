#!/usr/bin/env python3
"""JIT-HWBP pass 2d (diagnostic, single variable).

Zero-syscall cross-layer channel via a MAP_SHARED file:
  word[0] = Wine writes Dr1 on every arm (box64 reads on every push)
  word[1] = box64 hit marker
  word[2] = box64 hit counter
  word[3] = last matching ESP

Wine: page protection stays ON; trap_handler only COUNTS every TRCTRAP
and reports word[1..3] in hwbp_stats. No #DB construction, no behaviour
change. Goal: prove that setting TF inside a JIT CALL produces a real host
SIGTRAP that reaches Wine's trap_handler.
"""
import os
import shutil

SRC = "src/fix110b"
DST = "src/fix110b_jit5"
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
        "\n"
        "/* MAP_SHARED file shared with box64: [0]=Dr1 [1]=hit marker\n"
        " * [2]=hit count [3]=last matching ESP. */\n"
        "static volatile unsigned int *hwbp_shared;\n"
        "\n"
        "static volatile unsigned int *hwbp_shared_map( void )\n"
        "{\n"
        "    int fd; void *p;\n"
        "    if (hwbp_shared) return hwbp_shared;\n"
        "    fd = open( \"/home/xuser/hwbp_shared.bin\", O_RDWR | O_CREAT, 0644 );\n"
        "    if (fd < 0) return NULL;\n"
        "    ftruncate( fd, 4096 );\n"
        "    p = mmap( NULL, 4096, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0 );\n"
        "    close( fd );\n"
        "    if (p == MAP_FAILED) return NULL;\n"
        "    hwbp_shared = (volatile unsigned int *)p;\n"
        "    return hwbp_shared;\n"
        "}\n"
        "\n"
        "static void hwbp_jit_publish( unsigned long addr )\n"
        "{\n"
        "    volatile unsigned int *s = hwbp_shared_map();\n"
        "    if (s) s[0] = (unsigned int)addr;\n"
        "}\n"
        "\n"
        "static void hwbp_write_stats( void )\n{",
        "shared map + publish")

v = sub(v,
        "                  hwbp_t_first > 0 ? hwbp_elapsed() : 0.0 );\n"
        "    write( fd, b, n );",
        "                  hwbp_t_first > 0 ? hwbp_elapsed() : 0.0 );\n"
        "    if (getenv( \"WINE_HWBP_JIT\" ))\n"
        "    {\n"
        "        volatile unsigned int *s = hwbp_shared_map();\n"
        "        n += snprintf( b + n, sizeof(b) - n,\n"
        "                      \" jittrap=%u jithit=%u esp=%x dr1=%x seen=%x calls=%u lastesp=%x\",\n"
        "                      hwbp_jit_traps, s ? s[2] : 0, s ? s[3] : 0, s ? s[0] : 0,\n"
        "                      s ? s[4] : 0, s ? s[5] : 0, s ? s[6] : 0 );\n"
        "    }\n"
        "    write( fd, b, n );",
        "stats shared fields")

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
