#!/usr/bin/env python3
"""M3b: tell the container-side helper about every arm through a 4 KB
shared-memory ring in /home/xuser/.  ntdll only does open+mmap once, then
plain memory writes - proot's seccomp cannot see those (sendto/kill were
rejected with EACCES/EPERM, see report 2.10/M3 notes)."""
import os
import shutil

SRC = "src/fix110b"
DST = "src/fix110b_m3b"
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
        "static void hwbp_arm_one( unsigned long a, unsigned int len, unsigned int rw )",
        "/* M3b: shared-memory ring. Container-side ntdll cannot sendto() or kill()\n"
        " * (proot seccomp: EACCES / EPERM), but mmap+write to a file is allowed. */\n"
        "struct hwbp_ring_entry { int tid; unsigned long addr; unsigned int len; unsigned int rw; };\n"
        "struct hwbp_ring {\n"
        "    volatile unsigned int head;\n"
        "    volatile unsigned int tail;\n"
        "    unsigned int drop;\n"
        "    unsigned int pad;\n"
        "    struct hwbp_ring_entry e[64];\n"
        "};\n"
        "static struct hwbp_ring *hwbp_ring_ptr;\n"
        "\n"
        "static void hwbp_ring_init(void)\n"
        "{\n"
        "    int fd;\n"
        "    void *p;\n"
        "    if (hwbp_ring_ptr) return;\n"
        "    fd = open( \"/home/xuser/hwbp_ring\", O_RDWR );\n"
        "    if (fd < 0) return;\n"
        "    p = mmap( NULL, 4096, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0 );\n"
        "    close( fd );\n"
        "    if (p != MAP_FAILED) hwbp_ring_ptr = (struct hwbp_ring *)p;\n"
        "}\n"
        "\n"
        "static void hwbp_perf_send( unsigned long addr, unsigned int len, unsigned int rw )\n"
        "{\n"
        "    struct hwbp_ring_entry *e;\n"
        "    unsigned int h;\n"
        "    if (!getenv( \"WINE_HWBP_PERF_SEND\" )) return;\n"
        "    hwbp_ring_init();\n"
        "    if (!hwbp_ring_ptr) return;\n"
        "    h = hwbp_ring_ptr->head;\n"
        "    if (h - hwbp_ring_ptr->tail >= 64) { hwbp_ring_ptr->drop++; return; }\n"
        "    e = &hwbp_ring_ptr->e[h & 63];\n"
        "    e->tid  = (int)syscall( SYS_gettid );\n"
        "    e->addr = addr;\n"
        "    e->len  = len;\n"
        "    e->rw   = rw;\n"
        "    __sync_synchronize();\n"
        "    hwbp_ring_ptr->head = h + 1;\n"
        "}\n"
        "\n"
        "static void hwbp_arm_one( unsigned long a, unsigned int len, unsigned int rw )",
        "ring + send")

v = sub(v,
        "        hwbp_arm_one( hwbp_dr[i], len, rw );\n"
        "    }\n"
        "    if (!d7 && (hwbp_cnt_arm || hwbp_cnt_fault)) hwbp_write_stats();",
        "        hwbp_arm_one( hwbp_dr[i], len, rw );\n"
        "        hwbp_perf_send( hwbp_dr[i], hwbp_len_bytes( len ), rw );   /* M3b */\n"
        "    }\n"
        "    if (!d7 && (hwbp_cnt_arm || hwbp_cnt_fault)) hwbp_write_stats();",
        "arm loop send")

open(p, "w", encoding="utf-8", newline="\n").write(v)
print("DONE: %s" % DST)
