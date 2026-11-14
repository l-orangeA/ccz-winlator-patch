#!/usr/bin/env python3
"""M3c: let the container-side helper's perf breakpoint take over.
- hwbp_arm_one: in WINE_HWBP_PERF mode, do NOT set PROT_NONE / do NOT
  register the page (perf owns the watch); keep arm counter + timer.
- hwbp_perf_owner: helper-owned fd is unknown to ntdll -> fall back to Dr1.
- hwbp_perf_mark: deliver every hit (no one-shot 'delivered' latch).
"""
import os
import shutil

SRC = "src/fix110b_m3b"
DST = "src/fix110b_m3c"
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
        "        if (rw == 3)\n"
        "        {\n"
        "            if (hwbp_suppress || hwbp_hold_flag)\n"
        "                set_page_vprot_bits( page, page_size, VPROT_READ | VPROT_WRITE, VPROT_WRITEWATCH );\n"
        "            else\n"
        "                set_page_vprot_bits( page, page_size, 0, VPROT_READ | VPROT_WRITE | VPROT_WRITEWATCH );\n"
        "            mprotect_range( page, page_size, 0, 0 );",
        "        if (rw == 3)\n"
        "        {\n"
        "            if (!getenv( \"WINE_HWBP_PERF\" ))   /* M3c: perf helper owns the watch */\n"
        "            {\n"
        "                if (hwbp_suppress || hwbp_hold_flag)\n"
        "                    set_page_vprot_bits( page, page_size, VPROT_READ | VPROT_WRITE, VPROT_WRITEWATCH );\n"
        "                else\n"
        "                    set_page_vprot_bits( page, page_size, 0, VPROT_READ | VPROT_WRITE | VPROT_WRITEWATCH );\n"
        "                mprotect_range( page, page_size, 0, 0 );\n"
        "            }",
        "arm_one skip guard")

v = sub(v,
        "            if (!hwbp_rw_watched( page )) for (i = 0; i < 8; i++) if (!hwbp_rw_page[i]) { hwbp_rw_page[i] = (unsigned long)page; break; }",
        "            if (!getenv( \"WINE_HWBP_PERF\" ) && !hwbp_rw_watched( page )) for (i = 0; i < 8; i++) if (!hwbp_rw_page[i]) { hwbp_rw_page[i] = (unsigned long)page; break; }",
        "arm_one skip register")

v = sub(v,
        "int hwbp_perf_owner( int fd )\n"
        "{\n"
        "    int i;\n"
        "    pid_t tid = hwbp_perf_tid();\n"
        "    for (i = 0; i < 4; i++)\n"
        "        if (hwbp_perf_slots[i].fd == fd && hwbp_perf_slots[i].tid == tid) return i + 1;\n"
        "    return 0;\n"
        "}",
        "int hwbp_perf_owner( int fd )\n"
        "{\n"
        "    int i;\n"
        "    pid_t tid = hwbp_perf_tid();\n"
        "    for (i = 0; i < 4; i++)\n"
        "        if (hwbp_perf_slots[i].fd == fd && hwbp_perf_slots[i].tid == tid) return i + 1;\n"
        "    if (getenv( \"WINE_HWBP_PERF\" )) return 1;   /* M3c: helper-owned, shell only uses Dr1 */\n"
        "    return 0;\n"
        "}",
        "perf_owner fallback")

v = sub(v,
        "int hwbp_perf_mark( int idx )\n"
        "{\n"
        "    if (idx < 1 || idx > 4) return 0;\n"
        "    if (hwbp_perf_slots[idx-1].delivered) return 0;\n"
        "    hwbp_perf_slots[idx-1].delivered = 1;\n"
        "    return 1;\n"
        "}",
        "int hwbp_perf_mark( int idx )\n"
        "{\n"
        "    if (idx < 1 || idx > 4) return 0;\n"
        "    if (getenv( \"WINE_HWBP_PERF\" )) return 1;   /* M3c: deliver every hit */\n"
        "    if (hwbp_perf_slots[idx-1].delivered) return 0;\n"
        "    hwbp_perf_slots[idx-1].delivered = 1;\n"
        "    return 1;\n"
        "}",
        "perf_mark every-hit")

open(p, "w", encoding="utf-8", newline="\n").write(v)

sp = os.path.join(DST, "signal_x86_64.c")
s = open(sp, encoding="utf-8").read()
s = sub(s,
        "            if (hwbp_perf_mark( idx ) && CS_sig(ucontext) == 0x23)\n"
        "            {\n"
        "                amd64_thread_data()->dr6 = 0xffff0ff0u | (1u << idx);\n"
        "                rec.ExceptionAddress = (void *)RIP_sig(ucontext);",
        "            if (hwbp_perf_mark( idx ) && CS_sig(ucontext) == 0x23)\n"
        "            {\n"
        "                amd64_thread_data()->dr6 = 0xffff0ff0u | (1u << idx);\n"
        "                rec.ExceptionCode = EXCEPTION_SINGLE_STEP;   /* M3c: was missing (rec={0} -> code 0) */\n"
        "                rec.ExceptionAddress = (void *)RIP_sig(ucontext);",
        "sigio ExceptionCode")
open(sp, "w", encoding="utf-8", newline="\n").write(s)

print("DONE: %s" % DST)
