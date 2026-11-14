/* M3a helper: Unix DATAGRAM socket, container-side, app uid.
 * Receives (tid, addr, len, rw) and keeps ONE HW data breakpoint armed.
 *
 * usage: perf_helper_dgram <sockpath> [daemon <logfile>]
 */
#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stddef.h>
#include <errno.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <sys/stat.h>
#include <sys/syscall.h>
#include <linux/perf_event.h>

#ifndef HW_BREAKPOINT_RW
#define HW_BREAKPOINT_RW 3
#endif
#ifndef HW_BREAKPOINT_W
#define HW_BREAKPOINT_W 2
#endif

struct req { int tid; unsigned long addr; unsigned int len; unsigned int rw; };

int main(int argc, char **argv)
{
    const char *path = (argc > 1) ? argv[1] : "/tmp/hwbp.sock";

    if (argc > 3 && strcmp(argv[2], "daemon") == 0) {
        pid_t p = fork();
        if (p > 0) _exit(0);
        setsid();
        int devnull = open("/dev/null", O_RDWR);
        if (devnull >= 0) { dup2(devnull, 0); dup2(devnull, 1); dup2(devnull, 2); }
        int lf = open(argv[3], O_WRONLY | O_CREAT | O_APPEND, 0644);
        if (lf >= 0) { dup2(lf, 1); dup2(lf, 2); }
    }

    int sfd = socket(AF_UNIX, SOCK_DGRAM, 0);
    if (sfd < 0) { perror("socket"); return 2; }
    struct sockaddr_un sa;
    socklen_t salen;
    memset(&sa, 0, sizeof(sa));
    sa.sun_family = AF_UNIX;
    if (path[0] == '@') {
        /* abstract namespace: no filesystem path, proot cannot rewrite it */
        strncpy(sa.sun_path + 1, path + 1, sizeof(sa.sun_path) - 2);
        salen = (socklen_t)(offsetof(struct sockaddr_un, sun_path) + 1 + strlen(path + 1));
    } else {
        unlink(path);
        strncpy(sa.sun_path, path, sizeof(sa.sun_path) - 1);
        salen = sizeof(sa);
    }
    if (bind(sfd, (struct sockaddr *)&sa, salen) < 0) { perror("bind"); return 2; }
    if (path[0] != '@') chmod(path, 0666);
    printf("READY_DGRAM path=%s pid=%d\n", path, (int)getpid());
    fflush(stdout);

    int kept = -1, total = 0, ok = 0;
    struct req last;
    memset(&last, 0, sizeof(last));

    for (;;) {
        struct req r;
        ssize_t n = recvfrom(sfd, &r, sizeof(r), 0, NULL, NULL);
        if (n != (ssize_t)sizeof(r)) continue;
        total++;

        if (kept >= 0 && r.tid == last.tid && r.addr == last.addr &&
            r.len == last.len && r.rw == last.rw) {
            continue;
        }

        struct perf_event_attr pe;
        memset(&pe, 0, sizeof(pe));
        pe.type = PERF_TYPE_BREAKPOINT;
        pe.size = sizeof(pe);
        pe.bp_type = (r.rw == 3) ? HW_BREAKPOINT_RW : HW_BREAKPOINT_W;
        pe.bp_addr = r.addr;
        pe.bp_len = r.len ? r.len : 1;
        pe.sample_period = 1;
        pe.wakeup_events = 1;
        pe.exclude_kernel = 1;
        pe.exclude_hv = 1;
        pe.disabled = 1;
        errno = 0;
        int fd = (int)syscall(__NR_perf_event_open, &pe, r.tid, -1, -1, 0);
        int err = errno;
        if (fd >= 0) {
            if (kept >= 0) close(kept);
            kept = fd;
            last = r;
            ok++;
            printf("ARM tid=%d addr=0x%lx len=%u rw=%u fd=%d total=%d ok=%d\n",
                   r.tid, r.addr, r.len, r.rw, fd, total, ok);
        } else {
            printf("ARM_FAIL tid=%d addr=0x%lx len=%u rw=%u errno=%d total=%d\n",
                   r.tid, r.addr, r.len, r.rw, err, total);
        }
        fflush(stdout);
    }
}
