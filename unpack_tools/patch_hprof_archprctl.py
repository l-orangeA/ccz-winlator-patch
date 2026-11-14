#!/usr/bin/env python3
"""Time arch_prctl(ARCH_SET_FS) inside init_handler (the only syscall there).
Reported as archprctl=<avg ns> in hwbp_stats."""
p = "src/fix110b_hprof/signal_x86_64.c"
s = open(p, encoding="utf-8").read()

# 1) extern counter
s = s.replace("extern double hwbp_now_ns(void);",
              "extern double hwbp_now_ns(void);\n"
              "extern unsigned long long hwbp_ns_archprctl, hwbp_n_archprctl;", 1)

# 2) wrap the call
old = ("        arch_prctl( ARCH_SET_FS, ((struct amd64_thread_data *)thread_data->cpu_data)->pthread_teb );")
if old not in s:
    raise SystemExit("arch_prctl call missing")
new = ("        { double ap_t0 = hwbp_now_ns();\n"
       "          arch_prctl( ARCH_SET_FS, ((struct amd64_thread_data *)thread_data->cpu_data)->pthread_teb );\n"
       "          hwbp_ns_archprctl += (unsigned long long)(hwbp_now_ns() - ap_t0); ++hwbp_n_archprctl; }")
s = s.replace(old, new, 1)
open(p, "w").write(s)

# 3) globals + stats line
p2 = "src/fix110b_hprof/virtual.c"
v = open(p2).read()
v = v.replace("unsigned long long hwbp_ns_calls;",
              "unsigned long long hwbp_ns_calls;\n"
              "unsigned long long hwbp_ns_archprctl, hwbp_n_archprctl;", 1)
v = v.replace('" init=%llu unlock=%llu tail=%llu n=%llu",',
              '" init=%llu unlock=%llu tail=%llu n=%llu archprctl=%llu apn=%llu",', 1)
v = v.replace("                      hwbp_ns_tail / hwbp_ns_calls, hwbp_ns_calls );",
              "                      hwbp_ns_tail / hwbp_ns_calls, hwbp_ns_calls,\n"
              "                      hwbp_n_archprctl ? hwbp_ns_archprctl / hwbp_n_archprctl : 0,\n"
              "                      hwbp_n_archprctl );", 1)
open(p2, "w").write(v)
print("patched arch_prctl timing")
