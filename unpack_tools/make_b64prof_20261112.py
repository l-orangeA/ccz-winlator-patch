#!/usr/bin/env python3
"""1) revert the 3 JIT-HWBP edits so box64 is back to pristine 0.3.0
   2) instrument signals.c to split the signal cost into P/R/F
"""
import os
import re

ROOT = "src/box64_clean030/src"

# ---------- 1a. dynarec_arm64_00.c: drop the injected CALL ----------
p = os.path.join(ROOT, "dynarec/arm64/dynarec_arm64_00.c")
s = open(p, encoding="utf-8").read()
n0 = s.count("CALL(native_hwbp_check, -1);")
s = re.sub(r"\n\s*CALL\(native_hwbp_check, -1\);[^\n]*", "", s)
open(p, "w", encoding="utf-8", newline="\n").write(s)
print("revert CALL: removed %d" % n0)

# ---------- 1b. dynarec_native_functions.h: drop declarations ----------
p = os.path.join(ROOT, "dynarec/dynarec_native_functions.h")
s = open(p, encoding="utf-8").read()
s = s.replace("void native_hwbp_check(x64emu_t* emu);   /* JIT-HWBP */\n", "")
s = s.replace("void native_hwbp_check(x64emu_t* emu);\n", "")
open(p, "w", encoding="utf-8", newline="\n").write(s)
print("revert .h done")

# ---------- 1c. dynarec_native_functions.c: drop the whole hook ----------
p = os.path.join(ROOT, "dynarec/dynarec_native_functions.c")
s = open(p, encoding="utf-8").read()
i = s.find("/* JIT-HWBP pass")
if i >= 0:
    s = s[:i].rstrip() + "\n"
s = s.replace("#include <fcntl.h>\n", "")
s = s.replace("#include <sys/mman.h>\n", "")
open(p, "w", encoding="utf-8", newline="\n").write(s)
print("revert native_functions done; hwbp refs left =", s.count("native_hwbp_check"))

# ---------- 2. instrument signals.c ----------
p = os.path.join(ROOT, "libtools/signals.c")
s = open(p, encoding="utf-8").read()
if "B64PROF" in s:
    raise SystemExit("signals.c already instrumented")

anchor = "void my_sigactionhandler_oldcode(int32_t sig, int simple, siginfo_t* info, void * ucntx, int* old_code, void* cur_db)\n{"
if anchor not in s:
    raise SystemExit("anchor missing")

helpers = r'''/* ==== B64PROF ==== */
static unsigned long long b64p_prep, b64p_run, b64p_fin, b64p_n;
static double b64p_ns(void)
{
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (double)ts.tv_sec * 1e9 + (double)ts.tv_nsec;
}
static void b64p_dump(void)
{
    char b[192]; int fd, n;
    if (!b64p_n) return;
    fd = open("/home/xuser/b64prof.txt", O_WRONLY | O_CREAT | O_TRUNC, 0644);
    if (fd < 0) return;
    n = snprintf(b, sizeof(b), "prof calls=%llu prep_avg=%llu run_avg=%llu fin_avg=%llu\n",
                 b64p_n, b64p_prep / b64p_n, b64p_run / b64p_n, b64p_fin / b64p_n);
    if (write(fd, b, n) < 0) { /* ignore */ }
    close(fd);
}
/* ==== end B64PROF ==== */

'''
s = s.replace(anchor, helpers + anchor, 1)
s = s.replace(anchor + "\n    int Locks = unlockMutex();",
              anchor + "\n    double b64p_t0 = b64p_ns();\n    int Locks = unlockMutex();", 1)

old_run = "    ret = RunFunctionHandler(&exits, dynarec, sigcontext, my_context->signals[info2->si_signo], 3, info2->si_signo, info2, sigcontext);"
if old_run not in s:
    raise SystemExit("RunFunctionHandler call missing")
new_run = ("    { double b64p_t1 = b64p_ns();\n"
           "      ret = RunFunctionHandler(&exits, dynarec, sigcontext, my_context->signals[info2->si_signo], 3, info2->si_signo, info2, sigcontext);\n"
           "      { double b64p_t2 = b64p_ns();\n"
           "        b64p_prep += (unsigned long long)(b64p_t1 - b64p_t0);\n"
           "        b64p_run  += (unsigned long long)(b64p_t2 - b64p_t1);\n"
           "        b64p_fin  += (unsigned long long)(b64p_ns() - b64p_t2);\n"
           "        ++b64p_n;\n"
           "        if (!(b64p_n & 1023)) b64p_dump();\n"
           "      }\n"
           "    }")
s = s.replace(old_run, new_run, 1)
open(p, "w", encoding="utf-8", newline="\n").write(s)
print("signals.c instrumented")
