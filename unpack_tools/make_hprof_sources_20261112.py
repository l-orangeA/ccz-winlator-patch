#!/usr/bin/env python3
"""Break the 465 us SIGSEGV handler into its internal segments.

Measured inside Wine's segv_handler (guest code):
  A = entry -> just before hwbp_handle_rw_fault   (init_handler, vprot lookup)
  B = hwbp_handle_rw_fault                        (already known ~11 us)
  C = after B -> leave_handler                    (push_delta, TF, leave)
Reported as us: init=... unlock=... tail=...
"""
import os
import shutil

SRC = "src/fix110b"
DST = "src/fix110b_hprof"
os.makedirs(DST, exist_ok=True)
for f in ("signal_x86_64.c", "virtual.c", "thread.c"):
    shutil.copy2(os.path.join(SRC, f), os.path.join(DST, f))


def sub(text, old, new, tag):
    n = text.count(old)
    if n != 1:
        raise SystemExit("FAIL %s: expected 1, got %d" % (tag, n))
    print("  OK %s" % tag)
    return text.replace(old, new, 1)


# ---- virtual.c: globals + now_ns + dump into hwbp_stats ----
p = os.path.join(DST, "virtual.c")
v = open(p, encoding="utf-8").read()

v = sub(v,
        "static void hwbp_write_stats( void )\n{",
        "unsigned long long hwbp_ns_init, hwbp_ns_unlock, hwbp_ns_tail;\n"
        "unsigned long long hwbp_ns_calls;\n"
        "double hwbp_now_ns(void)\n"
        "{\n"
        "    struct timespec ts;\n"
        "    clock_gettime( CLOCK_MONOTONIC, &ts );\n"
        "    return (double)ts.tv_sec * 1e9 + (double)ts.tv_nsec;\n"
        "}\n"
        "\n"
        "static void hwbp_write_stats( void )\n{",
        "globals + clock")

v = sub(v,
        "                  hwbp_t_first > 0 ? hwbp_elapsed() : 0.0 );\n"
        "    write( fd, b, n );",
        "                  hwbp_t_first > 0 ? hwbp_elapsed() : 0.0 );\n"
        "    if (hwbp_ns_calls)\n"
        "        n += snprintf( b + n, sizeof(b) - n,\n"
        "                      \" init=%llu unlock=%llu tail=%llu n=%llu\",\n"
        "                      hwbp_ns_init / hwbp_ns_calls,\n"
        "                      hwbp_ns_unlock / hwbp_ns_calls,\n"
        "                      hwbp_ns_tail / hwbp_ns_calls, hwbp_ns_calls );\n"
        "    write( fd, b, n );",
        "stats segments")

open(p, "w", encoding="utf-8", newline="\n").write(v)

# ---- signal_x86_64.c: instrument segv_handler ----
p = os.path.join(DST, "signal_x86_64.c")
s = open(p, encoding="utf-8").read()

s = sub(s,
        "static void segv_handler( int signal, siginfo_t *siginfo, void *sigcontext )\n"
        "{\n"
        "    EXCEPTION_RECORD rec = { 0 };\n"
        "    struct xcontext context;\n"
        "    ucontext_t *ucontext = init_handler( sigcontext );",
        "extern double hwbp_now_ns(void);\n"
        "extern unsigned long long hwbp_ns_init, hwbp_ns_unlock, hwbp_ns_tail, hwbp_ns_calls;\n"
        "\n"
        "static void segv_handler( int signal, siginfo_t *siginfo, void *sigcontext )\n"
        "{\n"
        "    EXCEPTION_RECORD rec = { 0 };\n"
        "    struct xcontext context;\n"
        "    ucontext_t *ucontext = init_handler( sigcontext );\n"
        "    double hprof_t0 = hwbp_now_ns();",
        "entry timer")

# A segment: entry -> just before hwbp_handle_rw_fault
s = sub(s,
        "                    int idx = hwbp_handle_rw_fault( rwpage, siginfo->si_addr );",
        "                    double hprof_t1 = hwbp_now_ns();\n"
        "                    int idx = hwbp_handle_rw_fault( rwpage, siginfo->si_addr );\n"
        "                    double hprof_t2 = hwbp_now_ns();",
        "segment markers")

# C segment + accumulate at the generic return (leave_handler then return)
s = sub(s,
        "                    leave_handler( ucontext );\n"
        "                    return;\n"
        "                }\n"
        "            }\n"
        "        }",
        "                    leave_handler( ucontext );\n"
        "                    {\n"
        "                        double hprof_t3 = hwbp_now_ns();\n"
        "                        hwbp_ns_init   += (unsigned long long)(hprof_t1 - hprof_t0);\n"
        "                        hwbp_ns_unlock += (unsigned long long)(hprof_t2 - hprof_t1);\n"
        "                        hwbp_ns_tail   += (unsigned long long)(hprof_t3 - hprof_t2);\n"
        "                        ++hwbp_ns_calls;\n"
        "                    }\n"
        "                    return;\n"
        "                }\n"
        "            }\n"
        "        }",
        "accumulate + return")

open(p, "w", encoding="utf-8", newline="\n").write(s)
print("DONE: %s" % DST)
