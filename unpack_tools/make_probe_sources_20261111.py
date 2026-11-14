#!/usr/bin/env python3
"""Build a read/write-split counter variant of fix110b (P0-B, behaviour unchanged).

Copies src/fix110b -> src/fix110b_probe and injects two counters into
hwbp_stats.txt.  Every replacement is asserted so a source drift fails loudly.
"""
import os
import shutil

SRC = "src/fix110b"
DST = "src/fix110b_probe"
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


vpath = os.path.join(DST, "virtual.c")
v = open(vpath, encoding="utf-8").read()

v = sub(v,
        "unsigned long hwbp_cnt_arm, hwbp_cnt_fault, hwbp_cnt_step, hwbp_cnt_hit;",
        "unsigned long hwbp_cnt_arm, hwbp_cnt_fault, hwbp_cnt_step, hwbp_cnt_hit;\n"
        "unsigned long hwbp_cnt_fault_r, hwbp_cnt_fault_w;  /* PROBE: read/write split */",
        "counter decl")

v = sub(v,
        '    n = snprintf( b, sizeof(b), "pid=%d arms=%lu faults=%lu steps=%lu hits=%lu t=%.1f\\n",\n'
        '                  (int)getpid(), hwbp_cnt_arm, hwbp_cnt_fault, hwbp_cnt_step, hwbp_cnt_hit,\n',
        '    n = snprintf( b, sizeof(b), "pid=%d arms=%lu faults=%lu rfault=%lu wfault=%lu steps=%lu hits=%lu t=%.1f\\n",\n'
        '                  (int)getpid(), hwbp_cnt_arm, hwbp_cnt_fault, hwbp_cnt_fault_r, hwbp_cnt_fault_w, hwbp_cnt_step, hwbp_cnt_hit,\n',
        "stats format")

v = sub(v,
        "int hwbp_handle_rw_fault( char *page, const void *addr )   /* DIRECT: return matched index */",
        "int hwbp_handle_rw_fault( char *page, const void *addr, int is_write )   /* PROBE: read/write split */",
        "handler signature")

v = sub(v,
        "    hwbp_cnt_fault++;\n    idx = hwbp_match_write( addr );",
        "    hwbp_cnt_fault++;\n"
        "    if (is_write) hwbp_cnt_fault_w++; else hwbp_cnt_fault_r++;  /* PROBE */\n"
        "    idx = hwbp_match_write( addr );",
        "handler counter")

v = sub(v,
        "        if (vprot & VPROT_WRITEWATCH)\n        {\n"
        "            set_page_vprot_bits( page, page_size, 0, VPROT_WRITEWATCH );",
        "        if (vprot & VPROT_WRITEWATCH)\n        {\n"
        "            hwbp_cnt_fault++; hwbp_cnt_fault_w++;  /* PROBE: write-only path */\n"
        "            set_page_vprot_bits( page, page_size, 0, VPROT_WRITEWATCH );",
        "writewatch counter")

open(vpath, "w", encoding="utf-8", newline="\n").write(v)

spath = os.path.join(DST, "signal_x86_64.c")
s = open(spath, encoding="utf-8").read()
s = sub(s,
        "extern int hwbp_handle_rw_fault( char *page, const void *addr );  /* HWBP_V2_SIG */",
        "extern int hwbp_handle_rw_fault( char *page, const void *addr, int is_write );  /* PROBE */",
        "extern decl")
s = sub(s,
        "                    int idx = hwbp_handle_rw_fault( rwpage, siginfo->si_addr );",
        "                    WORD pf_err = ERROR_sig(ucontext);  /* PROBE: bit1 = write */\n"
        "                    int idx = hwbp_handle_rw_fault( rwpage, siginfo->si_addr, (pf_err & 2) != 0 );",
        "call site")
open(spath, "w", encoding="utf-8", newline="\n").write(s)

print("DONE: %s (thread.c copied verbatim)" % DST)
