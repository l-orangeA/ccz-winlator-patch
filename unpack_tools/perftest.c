#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <signal.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/syscall.h>
#include <linux/perf_event.h>
#include <linux/hw_breakpoint.h>

static volatile int hits = 0;
static volatile long watched = 0x11223344;

static void handler(int sig, siginfo_t *si, void *uc) {
    hits++;
    fprintf(stderr, "  SIGTRAP code=%d addr=%p fd=%d\n", si->si_code, si->si_addr, si->si_fd);
}

static int perf_open(struct perf_event_attr *a, pid_t pid, int cpu, int grp, unsigned long flags) {
    return syscall(__NR_perf_event_open, a, pid, cpu, grp, flags);
}

static int try(int idx, int bp_type, int len, int sigtrap, int exk, int asyncsig) {
    struct perf_event_attr a;
    int fd;
    memset(&a, 0, sizeof(a));
    a.type = PERF_TYPE_BREAKPOINT;
    a.size = sizeof(a);
    a.bp_type = bp_type;
    a.bp_addr = (unsigned long)&watched;
    a.bp_len = len;
    a.sample_period = 1;
    a.wakeup_events = 1;
    a.exclude_kernel = exk;
    a.exclude_hv = 1;
    if (sigtrap) a.sigtrap = 1;
    errno = 0;
    fd = perf_open(&a, 0, -1, -1, 0);
    fprintf(stderr, "case %d: type=%d len=%d sigtrap=%d exk=%d -> fd=%d errno=%d\n",
            idx, bp_type, len, sigtrap, exk, fd, errno);
    if (fd < 0) return -1;
    if (asyncsig) {
        fcntl(fd, F_SETFL, O_ASYNC | O_NONBLOCK);
        fcntl(fd, F_SETSIG, SIGTRAP);
        fcntl(fd, F_SETOWN, getpid());
    }
    ioctl(fd, PERF_EVENT_IOC_RESET, 0);
    ioctl(fd, PERF_EVENT_IOC_ENABLE, 0);
    hits = 0;
    volatile long x = watched;
    fprintf(stderr, "  read x=%lx hits=%d\n", x, hits);
    watched = 42;
    fprintf(stderr, "  write hits=%d\n", hits);
    ioctl(fd, PERF_EVENT_IOC_DISABLE, 0);
    close(fd);
    return hits;
}

int main(void) {
    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sa.sa_sigaction = handler; sa.sa_flags = SA_SIGINFO | SA_RESTART;
    sigaction(SIGTRAP, &sa, NULL);
    try(1, HW_BREAKPOINT_RW, HW_BREAKPOINT_LEN_4, 1, 1, 0);
    try(2, HW_BREAKPOINT_RW, HW_BREAKPOINT_LEN_4, 0, 1, 1);
    try(3, HW_BREAKPOINT_W,  HW_BREAKPOINT_LEN_4, 1, 1, 0);
    try(4, HW_BREAKPOINT_RW, HW_BREAKPOINT_LEN_4, 1, 0, 0);
    try(5, HW_BREAKPOINT_W,  HW_BREAKPOINT_LEN_1, 1, 1, 0);
    return 0;
}
