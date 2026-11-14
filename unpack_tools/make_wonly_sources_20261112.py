#!/usr/bin/env python3
"""P0-B: real "write-only" page watch (the one never actually tested).

Background: the 2026-11-10 probe only measured the `err` bit, which box64
misreports for push (a push store is reported as a write fault even when the
underlying access is a read). So "only 400 of 44106 are writes" was never
proven either way.

`get_unix_prot()` already implements the switch:
    if (vprot & VPROT_WRITEWATCH) prot &= ~PROT_WRITE;
i.e. VPROT_READ | VPROT_WRITEWATCH  =>  PROT_READ (reads pass, writes trap).
Current `hwbp_arm_one` instead clears everything => PROT_NONE (both trap).

This package switches to the write-only mode behind WINE_HWBP_WONLY=1 and
changes nothing else. Judgement:
  - faults drop a lot AND hits stays 400 / arms=1536  -> P0-B works, big win
  - faults unchanged                                  -> most faults are writes, P0-B dead
  - hits drops below 400 / arms stalls                -> shell also reads Dr1, unsafe
"""
import os
import shutil

SRC = "src/fix110b"
DST = "src/fix110b_wonly"
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

# 1) cached switch helper, inserted before hwbp_write_stats
v = sub(v,
        "static void hwbp_write_stats( void )\n{",
        "/* P0-B: write-only watch. Cached because it is read from a signal path. */\n"
        "static int hwbp_wonly(void)\n"
        "{\n"
        "    static int v = -1;\n"
        "    if (v < 0) { const char *e = getenv( \"WINE_HWBP_WONLY\" ); v = (e && *e && *e != '0'); }\n"
        "    return v;\n"
        "}\n"
        "\n"
        "static void hwbp_write_stats( void )\n{",
        "wonly helper")

# 2) arm: write-only instead of PROT_NONE
v = sub(v,
        "            if (hwbp_suppress || hwbp_hold_flag)\n"
        "                set_page_vprot_bits( page, page_size, VPROT_READ | VPROT_WRITE, VPROT_WRITEWATCH );\n"
        "            else\n"
        "                set_page_vprot_bits( page, page_size, 0, VPROT_READ | VPROT_WRITE | VPROT_WRITEWATCH );",
        "            if (hwbp_suppress || hwbp_hold_flag)\n"
        "                set_page_vprot_bits( page, page_size, VPROT_READ | VPROT_WRITE, VPROT_WRITEWATCH );\n"
        "            else if (hwbp_wonly())\n"
        "                /* reads allowed, writes trap: get_unix_prot drops PROT_WRITE */\n"
        "                set_page_vprot_bits( page, page_size, VPROT_READ | VPROT_WRITEWATCH, VPROT_WRITE );\n"
        "            else\n"
        "                set_page_vprot_bits( page, page_size, 0, VPROT_READ | VPROT_WRITE | VPROT_WRITEWATCH );",
        "arm write-only")

# 3) re-protect after a false positive: same write-only mode
v = sub(v,
        "    if (keep) return;\n"
        "    set_page_vprot_bits( pg, page_size, 0, VPROT_READ | VPROT_WRITE | VPROT_WRITEWATCH );\n"
        "    mprotect_range( pg, page_size, 0, 0 );",
        "    if (keep) return;\n"
        "    if (hwbp_wonly())\n"
        "        set_page_vprot_bits( pg, page_size, VPROT_READ | VPROT_WRITEWATCH, VPROT_WRITE );\n"
        "    else\n"
        "        set_page_vprot_bits( pg, page_size, 0, VPROT_READ | VPROT_WRITE | VPROT_WRITEWATCH );\n"
        "    mprotect_range( pg, page_size, 0, 0 );",
        "re-protect write-only")

# 4) count read vs write faults separately would need err bit (untrusted) -> instead
#    expose the mode in stats so we can prove it took effect
v = sub(v,
        "                  hwbp_t_first > 0 ? hwbp_elapsed() : 0.0 );\n"
        "    write( fd, b, n );",
        "                  hwbp_t_first > 0 ? hwbp_elapsed() : 0.0 );\n"
        "    if (hwbp_wonly()) n += snprintf( b + n, sizeof(b) - n, \" wonly=1\" );\n"
        "    write( fd, b, n );",
        "stats wonly flag")

open(p, "w", encoding="utf-8", newline="\n").write(v)
print("DONE: %s" % DST)
