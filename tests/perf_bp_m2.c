/* M2 relay probe: when a helper process opens a HW data breakpoint on
 * ANOTHER thread, can the SIGTRAP be delivered DIRECTLY to that thread
 * (F_SETOWN_EX -> target tid)?  No game / no Wine involved.
 */
#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>
#include <fcntl.h>
#include <signal.h>
#include <poll.h>
#include <sys/mman.h>
#include <sys/syscall.h>
#include <sys/ioctl.h>
#include <sys/wait.h>
#include <linux/perf_event.h>

#ifndef HW_BREAKPOINT_RW
#define HW_BREAKPOINT_RW 3
#endif

static int ackfd = -1;
static volatile int self_got = 0;

static void on_trap(int s, siginfo_t *si, void *u)
{
    (void)s; (void)si; (void)u;
    if (ackfd >= 0) {
        char c = 'X';
        if (write(ackfd, &c, 1) < 0) { /* ignore */ }
        _exit(0);
    }
    self_got = 1;
}

int main(int argc, char **argv)
{
    int self_mode = (argc > 1 && strcmp(argv[1], "self") == 0);
    int nosetsig = 0;
    for (int i = 1; i < argc; i++) if (!strcmp(argv[i], "nosetsig")) nosetsig = 1;
    int addrp[2], ackp[2];
    if (pipe(addrp) || pipe(ackp)) { perror("pipe"); return 2; }
    volatile unsigned long *shared = mmap(NULL, 4096, PROT_READ | PROT_WRITE,
                                          MAP_SHARED | MAP_ANONYMOUS, -1, 0);
    if (shared == MAP_FAILED) { perror("mmap"); return 2; }
    *shared = 0;

    pid_t c = fork();
    if (c == 0) {
        close(addrp[0]); close(ackp[0]);
        ackfd = ackp[1];
        struct sigaction sa;
        memset(&sa, 0, sizeof(sa));
        sa.sa_sigaction = on_trap;
        sa.sa_flags = SA_SIGINFO;
        sigaction(SIGTRAP, &sa, NULL);
        unsigned long a = (unsigned long)shared;
        if (write(addrp[1], &a, sizeof(a)) < 0) { /* ignore */ }
        for (;;) { (*shared)++; (*shared)--; }
        _exit(0);
    }
    close(addrp[1]); close(ackp[1]);
    unsigned long addr = 0;
    if (read(addrp[0], &addr, sizeof(addr)) != sizeof(addr)) { perror("read"); return 2; }
    usleep(200000);
    printf("target_tid=%d watch_addr=0x%lx\n", (int)c, addr);

    struct perf_event_attr pe;
    memset(&pe, 0, sizeof(pe));
    pe.type = PERF_TYPE_BREAKPOINT;
    pe.size = sizeof(pe);
    pe.bp_type = HW_BREAKPOINT_RW;
    pe.bp_addr = addr;
    pe.bp_len = 1;
    pe.sample_period = 1;
    pe.wakeup_events = 1;
    pe.exclude_kernel = 1;
    pe.exclude_hv = 1;
    pe.disabled = 1;
    errno = 0;
    long fd = syscall(__NR_perf_event_open, &pe, c, -1, -1, 0);
    printf("PERF_OPEN fd=%ld errno=%d (%s)\n", fd, errno, strerror(errno));
    if (fd < 0) { kill(c, SIGKILL); return 1; }

    struct f_owner_ex own;
    memset(&own, 0, sizeof(own));
    own.type = self_mode ? F_OWNER_TID : F_OWNER_TID;
    own.pid = self_mode ? getpid() : c;
    if (self_mode || nosetsig) {
        struct sigaction sa;
        memset(&sa, 0, sizeof(sa));
        sa.sa_sigaction = on_trap;
        sa.sa_flags = SA_SIGINFO;
        sigaction(nosetsig ? SIGIO : SIGTRAP, &sa, NULL);
    }
    errno = 0;
    int rc = fcntl((int)fd, F_SETOWN_EX, &own);
    printf("OWNER=%s tid=%d  F_SETOWN_EX rc=%d errno=%d\n",
           self_mode ? "self" : "target", (int)own.pid, rc, errno);
    if (nosetsig) {
        printf("F_SETSIG skipped (default SIGIO)\n");
    } else {
        errno = 0;
        rc = fcntl((int)fd, F_SETSIG, SIGTRAP);
        printf("F_SETSIG(SIGTRAP) rc=%d errno=%d\n", rc, errno);
    }
    errno = 0;
    rc = fcntl((int)fd, F_SETFL, O_ASYNC | O_NONBLOCK);
    printf("F_SETFL rc=%d errno=%d\n", rc, errno);
    printf("RESET rc=%d  ENABLE rc=%d\n",
           (int)ioctl((int)fd, PERF_EVENT_IOC_RESET, 0),
           (int)ioctl((int)fd, PERF_EVENT_IOC_ENABLE, 0));

    int got = 0;
    if (self_mode) {
        for (int i = 0; i < 40 && !self_got; i++) usleep(100000);
        got = self_got;
    } else {
        struct pollfd pfd = { ackp[0], POLLIN, 0 };
        int pr = poll(&pfd, 1, 4000);
        got = (pr > 0);
        char ch = 0;
        if (got && read(ackp[0], &ch, 1) < 0) { /* ignore */ }
    }
    printf("OWNER_GOT_SIGTRAP=%d  RESULT=%s\n", got, got ? "PASS" : "FAIL");
    kill(c, SIGKILL);
    waitpid(c, NULL, 0);
    close((int)fd);
    return got ? 0 : 1;
}
