#!/usr/bin/env python3
"""M3a: ntdll tells the container-side helper about every arm, over a Unix
datagram socket. Page protection is left untouched, so this is purely
additive and cannot break startup if the helper is absent."""
import os
import shutil

SRC = "src/fix110b"
DST = "src/fix110b_m3a"
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
        "#include <sys/socket.h>\n",
        "#include <sys/socket.h>\n#include <sys/un.h>\n#include <stddef.h>\n",
        "include sys/un.h")

v = sub(v,
        "static void hwbp_arm_one( unsigned long a, unsigned int len, unsigned int rw )",
        "struct hwbp_perf_req { int tid; unsigned long addr; unsigned int len; unsigned int rw; };\n"
        "\n"
        "/* M3a: fire-and-forget notification to the container-side helper.\n"
        " * Datagram socket => no connect, no blocking, safe from context-setup paths. */\n"
        "static void hwbp_perf_send( unsigned long addr, unsigned int len, unsigned int rw )\n"
        "{\n"
        "    struct sockaddr_un sa;\n"
        "    struct hwbp_perf_req rq;\n"
        "    int fd;\n"
        "    static int dbg_cnt;\n"
        "    if (!getenv( \"WINE_HWBP_PERF_SEND\" ))\n"
        "    {\n"
        "        if (hwbp_verbose() && dbg_cnt++ < 3) { char b[80]; int n = snprintf(b,sizeof(b),\"HWBP_SEND disabled\\n\"); write(2,b,n); }\n"
        "        return;\n"
        "    }\n"
        "    fd = socket( AF_UNIX, SOCK_DGRAM, 0 );\n"
        "    if (fd < 0)\n"
        "    {\n"
        "        if (hwbp_verbose() && dbg_cnt++ < 20) { char b[96]; int n = snprintf(b,sizeof(b),\"HWBP_SEND socket errno=%d\\n\", errno); write(2,b,n); }\n"
        "        return;\n"
        "    }\n"
        "    memset( &sa, 0, sizeof(sa) );\n"
        "    sa.sun_family = AF_UNIX;\n"
        "    sa.sun_path[0] = '\\0';\n"
        "    strcpy( sa.sun_path + 1, \"winlator_hwbp\" );\n"
        "    memset( &rq, 0, sizeof(rq) );\n"
        "    rq.tid  = (int)syscall( SYS_gettid );\n"
        "    rq.addr = addr;\n"
        "    rq.len  = len;\n"
        "    rq.rw   = rw;\n"
        "    { ssize_t sn = sendto( fd, &rq, sizeof(rq), 0, (struct sockaddr *)&sa,\n"
        "            (socklen_t)(offsetof( struct sockaddr_un, sun_path ) + 1 + strlen( \"winlator_hwbp\" )) );\n"
        "      int se = errno;\n"
        "      if (hwbp_verbose() && dbg_cnt++ < 20) { char b[128]; int n = snprintf(b,sizeof(b),\"HWBP_SEND addr=%lx tid=%d ret=%ld errno=%d\\n\", addr, rq.tid, (long)sn, se); write(2,b,n); } }\n"
        "    close( fd );\n"
        "}\n"
        "\n"
        "static void hwbp_arm_one( unsigned long a, unsigned int len, unsigned int rw )",
        "send helper")

v = sub(v,
        "        hwbp_arm_one( hwbp_dr[i], len, rw );\n"
        "    }\n"
        "    if (!d7 && (hwbp_cnt_arm || hwbp_cnt_fault)) hwbp_write_stats();",
        "        hwbp_arm_one( hwbp_dr[i], len, rw );\n"
        "        hwbp_perf_send( hwbp_dr[i], hwbp_len_bytes( len ), rw );   /* M3a */\n"
        "    }\n"
        "    if (!d7 && (hwbp_cnt_arm || hwbp_cnt_fault)) hwbp_write_stats();",
        "arm loop send")

open(p, "w", encoding="utf-8", newline="\n").write(v)
print("DONE: %s" % DST)
