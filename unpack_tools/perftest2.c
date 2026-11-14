#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <signal.h>
#include <unistd.h>
#include <fcntl.h>
#include <stdint.h>
#include <sys/ioctl.h>
#include <sys/syscall.h>
#include <linux/perf_event.h>
#include <linux/hw_breakpoint.h>

static volatile int hits = 0;
static volatile long watched = 0x11223344;
static volatile uintptr_t last_rip;
static volatile unsigned long last_fd;
static int g_fd = -1;

static void handler(int sig, siginfo_t *si, void *uc) {
    ucontext_t *u = (ucontext_t *)uc;
    hits++;
    last_rip = (uintptr_t)u->uc_mcontext.pc;
    last_fd = si->si_fd;
}

static int perf_open(struct perf_event_attr *a, pid_t pid, int cpu, int grp, unsigned long flags) {
    return syscall(__NR_perf_event_open, a, pid, cpu, grp, flags);
}

int main(void) {
    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sa.sa_sigaction = handler; sa.sa_flags = SA_SIGINFO | SA_RESTART;
    sigaction(SIGTRAP, &sa, NULL);

    struct perf_event_attr a;
    memset(&a, 0, sizeof(a));
    a.type = PERF_TYPE_BREAKPOINT;
    a.size = sizeof(a);
    a.bp_type = HW_BREAKPOINT_RW;
    a.bp_addr = (unsigned long)&watched;
    a.bp_len = HW_BREAKPOINT_LEN_4;
    a.sample_period = 1;
    a.wakeup_events = 1;
    a.exclude_kernel = 1;
    a.exclude_hv = 1;
    a.disabled = 1;
    int fd = perf_open(&a, 0, -1, -1, 0);
    fprintf(stderr, "fd=%d errno=%d\n", fd, errno);
    if (fd < 0) return 2;
    g_fd = fd;
    struct f_owner_ex own; own.type = F_OWNER_TID; own.pid = (pid_t)syscall(__NR_gettid);
    int r1 = fcntl(fd, F_SETOWN_EX, &own);
    int r2 = fcntl(fd, F_SETSIG, SIGTRAP);
    int r3 = fcntl(fd, F_SETFL, O_ASYNC | O_NONBLOCK);
    fprintf(stderr, "fcntl own=%d sig=%d fl=%d errno=%d\n", r1, r2, r3, errno);
    ioctl(fd, PERF_EVENT_IOC_ENABLE, 0);

    volatile long v;
    hits = 0;
    v = watched; __asm__ __volatile__("" ::: "memory");
    uintptr_t after_read = (uintptr_t)&&read_done;
read_done:
    usleep(200000);
    fprintf(stderr, "READ hits=%d v=%lx last_rip=%lx (after_read=%lx) fd=%lu\n",
            hits, v, last_rip, after_read, last_fd);

    hits = 0;
    watched = 0x55667788; __asm__ __volatile__("" ::: "memory");
    uintptr_t after_write = (uintptr_t)&&write_done;
write_done:
    usleep(200000);
    fprintf(stderr, "WRITE hits=%d last_rip=%lx (after_write=%lx)\n", hits, last_rip, after_write);

    hits = 0;
    v = 12345; usleep(200000);
    fprintf(stderr, "OTHER hits=%d\n", hits);

    hits = 0;
    v = watched; usleep(200000);
    fprintf(stderr, "READ2 hits=%d v=%lx last_rip=%lx\n", hits, v, last_rip);

    ioctl(fd, PERF_EVENT_IOC_DISABLE, 0);
    close(fd);
    return 0;
}
