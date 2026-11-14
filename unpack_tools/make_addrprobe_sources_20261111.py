#!/usr/bin/env python3
"""Addr probe: log the first N page-fault addresses next to Dr1, so we can
tell whether perf was watching the same address the shell actually touches."""
import os
import shutil

SRC = "src/fix110b"
DST = "src/fix110b_addrprobe"
FILES = ("signal_x86_64.c", "virtual.c", "thread.c")

os.makedirs(DST, exist_ok=True)
for f in FILES:
    shutil.copy2(os.path.join(SRC, f), os.path.join(DST, f))


def sub(text, old, new, tag):
    n = text.count(old)
    if n != 1:
        raise SystemExit("FAIL %s: expected 1 occurrence, got %d" % (tag, n))
    print("  OK %s" % tag)
    return text.replace(old, new, 1)


p = os.path.join(DST, "virtual.c")
v = open(p, encoding="utf-8").read()

v = sub(v,
        "int hwbp_handle_rw_fault( char *page, const void *addr )   /* DIRECT: return matched index */\n"
        "{\n"
        "    int idx;\n"
        "    hwbp_cnt_fault++;\n"
        "    idx = hwbp_match_write( addr );\n"
        "    if (idx) hwbp_cnt_hit++;\n",
        "int hwbp_handle_rw_fault( char *page, const void *addr )   /* DIRECT: return matched index */\n"
        "{\n"
        "    int idx;\n"
        "    hwbp_cnt_fault++;\n"
        "    idx = hwbp_match_write( addr );\n"
        "    if (idx) hwbp_cnt_hit++;\n"
        "    {   /* ADDRPROBE: compare fault address with the Dr1 the shell set */\n"
        "        static int ap;\n"
        "        const char *e = getenv( \"WINE_HWBP_ADDRPROBE\" );\n"
        "        if (e && ap < 40)\n"
        "        {\n"
        "            char b[192]; int n = snprintf( b, sizeof(b),\n"
        "                \"ADDRPROBE addr=%lx dr0=%lx dr1=%lx dr2=%lx dr3=%lx dr7=%lx idx=%d page=%lx\\n\",\n"
        "                (unsigned long)addr, hwbp_dr[0], hwbp_dr[1], hwbp_dr[2], hwbp_dr[3],\n"
        "                hwbp_dr7, idx, (unsigned long)page );\n"
        "            write( 2, b, n ); ap++;\n"
        "        }\n"
        "    }\n",
        "addr probe")

open(p, "w", encoding="utf-8", newline="\n").write(v)
print("DONE: %s" % DST)
