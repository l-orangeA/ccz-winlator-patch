#!/usr/bin/env python3
"""P0-B behaviour package: arm WRITE-ONLY watch instead of PROT_NONE.

Base = src/fix110b_probe (already carries rfault/wfault counters).
Three edits, all asserted:
  1. add hwbp_w_step_page + hwbp_w_step_begin()
  2. hwbp_step_end_ex() re-arms WRITEWATCH for that page after the single-step
  3. hwbp_arm_one() rw==3 branch arms write-watch (keeps arm counter/timer)
  4. virtual_handle_fault() WRITEWATCH branch counts, matches, single-steps
"""
import os
import shutil

SRC = "src/fix110b_probe"
DST = "src/fix110b_wonly"
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
        "static char *hwbp_step_page;\n",
        "static char *hwbp_step_page;\n"
        "static char *hwbp_w_step_page;   /* WRITEONLY: page to re-arm after single-step */\n"
        "static unsigned long hwbp_w_page[8];   /* WRITEONLY: pages armed write-watch only */\n\n"
        "static int hwbp_w_watched( char *page )\n"
        "{\n"
        "    int i;\n"
        "    for (i = 0; i < 8; i++) if (hwbp_w_page[i] == (unsigned long)page) return 1;\n"
        "    return 0;\n"
        "}\n\n"
        "static void hwbp_w_clear( void )\n"
        "{\n"
        "    int i;\n"
        "    for (i = 0; i < 8; i++)\n"
        "    {\n"
        "        char *pg = (char *)hwbp_w_page[i];\n"
        "        if (!pg) continue;\n"
        "        hwbp_w_page[i] = 0;\n"
        "        set_page_vprot_bits( pg, page_size, VPROT_READ | VPROT_WRITE, VPROT_WRITEWATCH );\n"
        "        mprotect_range( pg, page_size, 0, 0 );\n"
        "    }\n"
        "}\n",
        "w_page array + helpers")

v = sub(v,
        "int hwbp_handle_rw_fault( char *page, const void *addr, int is_write )",
        "void hwbp_w_step_begin( char *page )\n"
        "{\n"
        "    hwbp_w_step_page = page;\n"
        "    hwbp_step_flag = 1;\n"
        "}\n\n"
        "int hwbp_handle_rw_fault( char *page, const void *addr, int is_write )",
        "w_step_begin fn")

v = sub(v,
        "void hwbp_step_end_ex( int keep )\n"
        "{\n"
        "    char *pg = hwbp_step_page;\n"
        "    hwbp_step_flag = 0;\n"
        "    hwbp_step_page = 0;\n"
        "    hwbp_cnt_step++;\n"
        "    if (!pg) return;",
        "void hwbp_step_end_ex( int keep )\n"
        "{\n"
        "    char *pg = hwbp_step_page;\n"
        "    char *wp = hwbp_w_step_page;\n"
        "    hwbp_step_flag = 0;\n"
        "    hwbp_step_page = 0;\n"
        "    hwbp_w_step_page = 0;\n"
        "    hwbp_cnt_step++;\n"
        "    if (wp)\n"
        "    {\n"
        "        set_page_vprot_bits( wp, page_size, VPROT_WRITEWATCH, 0 );\n"
        "        mprotect_range( wp, page_size, 0, 0 );\n"
        "    }\n"
        "    if (!pg) return;",
        "step_end re-arm")

v = sub(v,
        "        if (rw == 3)\n"
        "        {\n"
        "            if (hwbp_suppress || hwbp_hold_flag)\n"
        "                set_page_vprot_bits( page, page_size, VPROT_READ | VPROT_WRITE, VPROT_WRITEWATCH );\n"
        "            else\n"
        "                set_page_vprot_bits( page, page_size, 0, VPROT_READ | VPROT_WRITE | VPROT_WRITEWATCH );\n"
        "            mprotect_range( page, page_size, 0, 0 );",
        "        if (rw == 3)\n"
        "        {\n"
        "            /* WRITEONLY A/B: watch writes only, reads pass through */\n"
        "            set_page_vprot_bits( page, page_size, VPROT_WRITEWATCH, 0 );\n"
        "            mprotect_range( page, page_size, 0, 0 );",
        "arm_one write-only")

v = sub(v,
        "            if (!hwbp_rw_watched( page )) for (i = 0; i < 8; i++) if (!hwbp_rw_page[i]) { hwbp_rw_page[i] = (unsigned long)page; break; }",
        "            if (!hwbp_w_watched( page )) for (i = 0; i < 8; i++) if (!hwbp_w_page[i]) { hwbp_w_page[i] = (unsigned long)page; break; }",
        "arm_one register w_page")

v = sub(v,
        "    hwbp_rw_clear();\n    if (!(d7 & 0xff)) return;  /* no enabled breakpoints */",
        "    hwbp_rw_clear();\n    hwbp_w_clear();\n    if (!(d7 & 0xff)) return;  /* no enabled breakpoints */",
        "hwbp_update w_clear")

v = sub(v,
        "            if (hwbp_page_watched( page ))\n"
        "            {\n"
        "                { int idx = hwbp_match_write( addr ); hwbp_pending = idx ? idx : 9; }\n"
        "                hwbp_step_flag = 1;   /* HWBP_V2: trap after this store */\n"
        "            }",
        "            if (hwbp_page_watched( page ))\n"
        "            {\n"
        "                int idx = hwbp_match_write( addr );\n"
        "                if (idx) hwbp_cnt_hit++;\n"
        "                hwbp_pending = idx ? idx : 9;\n"
        "                hwbp_w_step_begin( page );   /* WRITEONLY: trap after store, then re-arm */\n"
        "            }",
        "fault branch re-arm + hit")

open(p, "w", encoding="utf-8", newline="\n").write(v)
print("DONE: %s" % DST)
