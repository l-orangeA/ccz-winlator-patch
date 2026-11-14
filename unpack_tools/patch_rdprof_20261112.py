#!/usr/bin/env python3
"""RDPROF: count NtReadFile calls per file path, to find what generates the
48k tiny reads (110 B each) during the load phase.

Injected into /root/wine0905/dlls/ntdll/unix/file.c:
  - helper block before NtReadFile
  - one call at the `done:` label (unix_handle still open there)
Output: /home/xuser/rdprof.txt, rewritten every 4096 reads.
"""
P = "/root/wine0905/dlls/ntdll/unix/file.c"
s = open(P, encoding="utf-8", errors="surrogateescape").read()

if "RDPROF" in s:
    raise SystemExit("already instrumented")

HELPERS = r'''
/* ==== RDPROF: per-path read counter ==== */
#define RDPROF_SLOTS 64
struct rdprof_slot { int fd; unsigned long long cnt, bytes; char path[96]; };
static struct rdprof_slot rdprof_slots[RDPROF_SLOTS];
static unsigned long long rdprof_total;
static int rdprof_init_done;

static void rdprof_dump(void)
{
    char b[4096]; int n = 0, i, fd;
    fd = open( "/home/xuser/rdprof.txt", O_WRONLY | O_CREAT | O_TRUNC, 0644 );
    if (fd < 0) return;
    n += snprintf( b + n, sizeof(b) - n, "total=%llu\n", rdprof_total );
    for (i = 0; i < RDPROF_SLOTS && n < (int)sizeof(b) - 220; i++)
        if (rdprof_slots[i].cnt)
            n += snprintf( b + n, sizeof(b) - n, "%8llu %12llu %s\n",
                           rdprof_slots[i].cnt, rdprof_slots[i].bytes, rdprof_slots[i].path );
    if (write( fd, b, n ) < 0) { /* ignore */ }
    close( fd );
}

static void rdprof_note( int fd, long long n )
{
    int i, slot;
    if (fd < 0 || n <= 0) return;
    if (!rdprof_init_done) {
        for (i = 0; i < RDPROF_SLOTS; i++) rdprof_slots[i].fd = -1;
        rdprof_init_done = 1;
    }
    for (i = 0; i < RDPROF_SLOTS; i++)
        if (rdprof_slots[i].fd == fd) {
            rdprof_slots[i].cnt++;
            rdprof_slots[i].bytes += (unsigned long long)n;
            goto out;
        }
    slot = (unsigned)fd % RDPROF_SLOTS;
    {
        char p[64], buf[128]; int len;
        snprintf( p, sizeof(p), "/proc/self/fd/%d", fd );
        len = readlink( p, buf, sizeof(buf) - 1 );
        rdprof_slots[slot].fd = fd;
        rdprof_slots[slot].cnt = 1;
        rdprof_slots[slot].bytes = (unsigned long long)n;
        if (len > 0) { buf[len] = 0; strncpy( rdprof_slots[slot].path, buf, 95 ); rdprof_slots[slot].path[95] = 0; }
        else rdprof_slots[slot].path[0] = 0;
    }
out:
    if (!(++rdprof_total & 4095)) rdprof_dump();
}
/* ==== end RDPROF ==== */

'''

anchor = "NTSTATUS WINAPI NtReadFile( HANDLE handle, HANDLE event, PIO_APC_ROUTINE apc, void *apc_user,"
if anchor not in s:
    raise SystemExit("NtReadFile anchor missing")
s = s.replace(anchor, HELPERS + anchor, 1)

# call at done: (unix_handle still open, total already accumulated)
done_anchor = "done:\n    send_completion = cvalue != 0;"
if done_anchor not in s:
    raise SystemExit("done anchor missing")
s = s.replace(done_anchor,
              "done:\n    rdprof_note( unix_handle, (long long)total );\n    send_completion = cvalue != 0;", 1)

open(P, "w", encoding="utf-8", errors="surrogateescape", newline="\n").write(s)
print("RDPROF injected into file.c")
