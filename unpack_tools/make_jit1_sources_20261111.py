#!/usr/bin/env python3
"""JIT-HWBP pass 1 (diagnostic), pure file channel.

Wine ntdll publishes the current Dr1 VALUE to /home/xuser/hwbp_watch.txt
whenever it changes (on each arm). box64's push hook reads that file and
appends matches to /home/xuser/hwbp_jit_hits.txt.

No shared globals, no cross-layer pointers: writing Wine's .bss from box64
stalled the boot in jit4/jit5. Page protection stays ON; this pass only
proves that the JIT comparison can see the shell's watch address.
"""
import os
import shutil

SRC = "src/fix110b"
DST = "src/fix110b_jit1"
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
        "/* JIT-HWBP pass 1: publish Dr1 as a hex VALUE through a file.\n"
        " * box64 reads it from the same proot namespace. */\n"
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
        "publish helper")

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
print("DONE: %s" % DST)
