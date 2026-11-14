/* P0-A M2 probe: helper process OUTSIDE proot.
 * Binds a Unix socket, accepts (tid, addr, len, rw), opens a HW data
 * breakpoint on that tid, returns (fd, errno).
 * Serves only as an IPC + perf capability prototype - no game, no Wine.
 */
#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
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
struct resp { int fd; int err; };

int main(int argc, char **argv)
{
    const char *path = (argc > 1) ? argv[1] : "/data/local/tmp/hwbp.sock";
    int sfd = socket(AF_UNIX, SOCK_STREAM, 0);
    if (sfd < 0) { perror("socket"); return 2; }
    unlink(path);
    struct sockaddr_un sa;
    memset(&sa, 0, sizeof(sa));
    sa.sun_family = AF_UNIX;
    strncpy(sa.sun_path, path, sizeof(sa.sun_path) - 1);
    if (bind(sfd, (struct sockaddr *)&sa, sizeof(sa)) < 0) { perror("bind"); return 2; }
    chmod(path, 0666);
    if (listen(sfd, 8) < 0) { perror("listen"); return 2; }
    printf("READY path=%s pid=%d\n", path, (int)getpid());
    fflush(stdout);

    for (;;) {
        int c = accept(sfd, NULL, NULL);
        if (c < 0) continue;
        struct req r;
        ssize_t n = read(c, &r, sizeof(r));
        struct resp rp = { -1, 0 };
        if (n == (ssize_t)sizeof(r)) {
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
            rp.fd = (int)syscall(__NR_perf_event_open, &pe, r.tid, -1, -1, 0);
            rp.err = errno;
            printf("REQ tid=%d addr=0x%lx len=%u rw=%u -> fd=%d errno=%d\n",
                   r.tid, r.addr, r.len, r.rw, rp.fd, rp.err);
            fflush(stdout);
        } else {
            rp.err = EINVAL;
        }
        if (write(c, &rp, sizeof(rp)) < 0) { /* ignore */ }
        close(c);
    }
}
