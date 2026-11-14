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
static int g_fd = -1;

static void handler(int sig, siginfo_t *si, void *uc) {
    hits++;
}

static int perf_open(struct perf_event_attr *a) {
    return syscall(__NR_perf_event_open, a, 0, -1, -1, 0);
}

static int setup(int bp_len, int sample, int wake, int refresh) {
    struct perf_event_attr a;
    int fd;
    memset(&a, 0, sizeof(a));
    a.type = PERF_TYPE_BREAKPOINT;
    a.size = sizeof(a);
    a.bp_type = HW_BREAKPOINT_RW;
    a.bp_addr = (unsigned long)&watched;
    a.bp_len = bp_len;
    a.sample_period = sample;
    a.wakeup_events = wake;
    a.exclude_kernel = 1;
    a.exclude_hv = 1;
    a.disabled = 1;
    fd = perf_open(&a);
    if (fd < 0) return -1;
    struct f_owner_ex own; own.type = F_OWNER_TID; own.pid = (pid_t)syscall(__NR_gettid);
    fcntl(fd, F_SETOWN_EX, &own);
    fcntl(fd, F_SETSIG, SIGTRAP);
    fcntl(fd, F_SETFL, O_ASYNC | O_NONBLOCK);
    if (refresh) ioctl(fd, PERF_EVENT_IOC_REFRESH, 1);
    else ioctl(fd, PERF_EVENT_IOC_ENABLE, 0);
    g_fd = fd;
    return fd;
}

static void run(const char *name, int bp_len, int sample, int wake, int refresh) {
    int fd = setup(bp_len, sample, wake, refresh);
    if (fd < 0) { fprintf(stderr, "%s: open failed errno=%d\n", name, errno); return; }
    hits = 0;
    volatile long v = watched;
    usleep(150000);
    int h1 = hits;
    hits = 0;
    watched = 0x22334455;
    usleep(150000);
    int h2 = hits;
    fprintf(stderr, "%s len=%d sample=%d wake=%d refresh=%d -> read_hits=%d write_hits=%d\n",
            name, bp_len, sample, wake, refresh, h1, h2);
    ioctl(fd, PERF_EVENT_IOC_DISABLE, 0);
    close(fd);
    g_fd = -1;
    (void)v;
}

int main(void) {
    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sa.sa_sigaction = handler; sa.sa_flags = SA_SIGINFO | SA_RESTART;
    sigaction(SIGTRAP, &sa, NULL);
    run("A", HW_BREAKPOINT_LEN_4, 1, 1, 0);
    run("B", HW_BREAKPOINT_LEN_4, 1, 0, 0);
    run("C", HW_BREAKPOINT_LEN_4, 1, 1, 1);
    run("D", HW_BREAKPOINT_LEN_8, 1, 1, 0);
    return 0;
}
