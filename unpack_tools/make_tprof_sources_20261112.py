#!/usr/bin/env python3
"""Measure how much of the 450 us/fault is spent INSIDE Wine.

Instruments hwbp_handle_rw_fault (the only Wine-side function on the fault
path that does real work: match + unlock mprotect) with clock_gettime and
reports the average via hwbp_stats.

Interpretation:
  - avg < 10 us   -> Wine's own work is negligible; 450 us is proot+box64
  - avg 50-200 us -> Wine is a big chunk; moving the fast path into box64
                     would pay off
  - avg > 200 us  -> Wine dominates; refactoring box64's signal path is the
                     only lever with real money in it
"""
import os
import shutil

SRC = "src/fix110b"
DST = "src/fix110b_tprof"
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
        "/* timing probe: total ns inside hwbp_handle_rw_fault + call count */\n"
        "static unsigned long long hwbp_ns_total;\n"
        "static unsigned long long hwbp_ns_calls;\n"
        "static double hwbp_now_ns(void)\n"
        "{\n"
        "    struct timespec ts;\n"
        "    clock_gettime( CLOCK_MONOTONIC, &ts );\n"
        "    return (double)ts.tv_sec * 1e9 + (double)ts.tv_nsec;\n"
        "}\n"
        "\n"
        "static void hwbp_write_stats( void )\n{",
        "timing decl")

v = sub(v,
        "    int idx;\n"
        "    hwbp_cnt_fault++;\n"
        "    idx = hwbp_match_write( addr );\n"
        "    if (idx) hwbp_cnt_hit++;\n"
        "    hwbp_pending = idx ? idx : 9;\n"
        "    hwbp_step_flag = 0;   /* DIRECT */\n"
        "    hwbp_step_page = page;\n"
        "    set_page_vprot_bits( page, page_size, VPROT_READ | VPROT_WRITE, VPROT_WRITEWATCH );\n"
        "    mprotect_range( page, page_size, 0, 0 );\n"
        "    return idx;",
        "    int idx;\n"
        "    double t0 = hwbp_now_ns();\n"
        "    hwbp_cnt_fault++;\n"
        "    idx = hwbp_match_write( addr );\n"
        "    if (idx) hwbp_cnt_hit++;\n"
        "    hwbp_pending = idx ? idx : 9;\n"
        "    hwbp_step_flag = 0;   /* DIRECT */\n"
        "    hwbp_step_page = page;\n"
        "    set_page_vprot_bits( page, page_size, VPROT_READ | VPROT_WRITE, VPROT_WRITEWATCH );\n"
        "    mprotect_range( page, page_size, 0, 0 );\n"
        "    hwbp_ns_total += (unsigned long long)(hwbp_now_ns() - t0);\n"
        "    ++hwbp_ns_calls;\n"
        "    return idx;",
        "timing in fault handler")

v = sub(v,
        "                  hwbp_t_first > 0 ? hwbp_elapsed() : 0.0 );\n"
        "    write( fd, b, n );",
        "                  hwbp_t_first > 0 ? hwbp_elapsed() : 0.0 );\n"
        "    if (hwbp_ns_calls)\n"
        "        n += snprintf( b + n, sizeof(b) - n, \" wns=%llu wcalls=%llu\",\n"
        "                      hwbp_ns_total / hwbp_ns_calls, hwbp_ns_calls );\n"
        "    write( fd, b, n );",
        "stats timing")

open(p, "w", encoding="utf-8", newline="\n").write(v)
print("DONE: %s" % DST)
