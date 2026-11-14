#!/usr/bin/env python3
"""JIT-HWBP pass 2b: end-to-end replacement of page protection.

box64 push hook, on a match:
  - sets guest TF (0x100) in emu->eflags  -> box64 emits a real host SIGTRAP
    with a full ucontext (unlike emit_signal, which passes NULL and crashes
    init_handler, jit7b).
  - writes /home/xuser/hwbp_jit_trap.txt as a marker.

Wine ntdll:
  - hwbp_arm_one skips VPROT_WRITEWATCH / mprotect when WINE_HWBP_JIT is set
    => faults should drop to 0.
  - trap_handler: on TRCTRAP with no pending page-fault state, if the marker
    exists, raise EXCEPTION_SINGLE_STEP with dr6 = B1 (Dr1).
  - hwbp_write_stats reports jitdb= (number of #DB constructed).

Single causal chain, three edits; cannot be A/B'd separately.
"""
import os
import shutil

SRC = "src/fix110b"
DST = "src/fix110b_jit3"
os.makedirs(DST, exist_ok=True)
for f in ("signal_x86_64.c", "virtual.c", "thread.c"):
    shutil.copy2(os.path.join(SRC, f), os.path.join(DST, f))


def sub(text, old, new, tag):
    n = text.count(old)
    if n != 1:
        raise SystemExit("FAIL %s: expected 1, got %d" % (tag, n))
    print("  OK %s" % tag)
    return text.replace(old, new, 1)


# ---------------- virtual.c ----------------
p = os.path.join(DST, "virtual.c")
v = open(p, encoding="utf-8").read()

v = sub(v,
        "static void hwbp_write_stats( void )\n{",
        "/* JIT-HWBP pass 2b */\n"
        "unsigned int hwbp_jit_db = 0;\n"
        "int hwbp_pending_peek(void) { return hwbp_pending; }\n"
        "\n"
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
        "publish + counter")

v = sub(v,
        "                  hwbp_t_first > 0 ? hwbp_elapsed() : 0.0 );\n"
        "    write( fd, b, n );",
        "                  hwbp_t_first > 0 ? hwbp_elapsed() : 0.0 );\n"
        "    if (getenv( \"WINE_HWBP_JIT\" ))\n"
        "        n += snprintf( b + n, sizeof(b) - n, \" jitdb=%u\", hwbp_jit_db );\n"
        "    write( fd, b, n );",
        "stats jitdb field")

v = sub(v,
        "        if (rw == 3)\n"
        "        {\n"
        "            if (hwbp_suppress || hwbp_hold_flag)\n",
        "        if (rw == 3)\n"
        "        {\n"
        "            if (getenv( \"WINE_HWBP_JIT\" ))\n"
        "            {   /* JIT-HWBP: no page protection, box64 hook handles it */\n"
        "                if (!hwbp_cnt_arm) hwbp_t_first = hwbp_now();\n"
        "                hwbp_cnt_arm++;\n"
        "                if (!(hwbp_cnt_arm & 63)) hwbp_write_stats();\n"
        "                page += page_size;\n"
        "                continue;\n"
        "            }\n"
        "            if (hwbp_suppress || hwbp_hold_flag)\n",
        "skip page protection")

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

# ---------------- signal_x86_64.c ----------------
p = os.path.join(DST, "signal_x86_64.c")
s = open(p, encoding="utf-8").read()

s = sub(s,
        "static void trap_handler( int signal, siginfo_t *siginfo, void *sigcontext )\n"
        "{\n"
        "    EXCEPTION_RECORD rec = { 0 };\n"
        "    struct xcontext context;\n"
        "    ucontext_t *ucontext = init_handler( sigcontext );\n",
        "extern unsigned int hwbp_jit_db;\n"
        "extern int hwbp_pending_peek(void);\n"
        "\n"
        "static void trap_handler( int signal, siginfo_t *siginfo, void *sigcontext )\n"
        "{\n"
        "    EXCEPTION_RECORD rec = { 0 };\n"
        "    struct xcontext context;\n"
        "    ucontext_t *ucontext = init_handler( sigcontext );\n"
        "\n"
        "    if (getenv( \"WINE_HWBP_JIT\" ) && TRAP_sig(ucontext) == TRAP_x86_TRCTRAP\n"
        "        && !hwbp_step_active() && !hwbp_pending_peek())\n"
        "    {   /* JIT-HWBP pass 2b: TF raised by box64 after a push match */\n"
        "        if (access( \"/home/xuser/hwbp_jit_trap.txt\", F_OK ) == 0)\n"
        "        {\n"
        "            unlink( \"/home/xuser/hwbp_jit_trap.txt\" );\n"
        "            ++hwbp_jit_db;\n"
        "            EFL_sig(ucontext) &= ~0x100;\n"
        "            amd64_thread_data()->dr6 = 0xffff0ff0u | (1u << 1);  /* B1 = Dr1 */\n"
        "            rec.ExceptionCode = EXCEPTION_SINGLE_STEP;\n"
        "            rec.ExceptionAddress = (void *)RIP_sig(ucontext);\n"
        "            save_context( &context, ucontext );\n"
        "            setup_raise_exception( ucontext, &rec, &context );\n"
        "            return;\n"
        "        }\n"
        "    }\n",
        "JIT #DB branch")

open(p, "w", encoding="utf-8", newline="\n").write(s)
print("DONE: %s" % DST)
