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
static volatile int w32 = 0x11223344;
static int g_fd = -1;
static volatile uintptr_t last_rip;
static volatile int last_code, last_fd;

static void handler(int sig, siginfo_t *si, void *uc) {
    ucontext_t *u = (ucontext_t *)uc;
    hits++;
    last_rip = (uintptr_t)u->uc_mcontext.pc;
    last_code = si->si_code; last_fd = si->si_fd;
}

static int perf_open(struct perf_event_attr *a) {
    return syscall(__NR_perf_event_open, a, 0, -1, -1, 0);
}

static int arm(int sigtrap, int size_override, int *out_fd) {
    struct perf_event_attr a;
    memset(&a, 0, sizeof(a));
    a.type = PERF_TYPE_BREAKPOINT;
    a.size = size_override ? size_override : sizeof(a);
    a.bp_type = HW_BREAKPOINT_RW;
    a.bp_addr = (unsigned long)&w32;
    a.bp_len = HW_BREAKPOINT_LEN_4;
    a.sample_period = 1;
    a.wakeup_events = 1;
    a.exclude_kernel = 1;
    a.exclude_hv = 1;
    a.disabled = 1;
    if (sigtrap) a.sigtrap = 1;
    int fd = perf_open(&a);
    int e = errno;
    fprintf(stderr, "sigtrap=%d size=%d -> fd=%d errno=%d\n", sigtrap, (int)a.size, fd, e);
    if (fd < 0) return -1;
    struct f_owner_ex own; own.type = F_OWNER_TID; own.pid = (pid_t)syscall(__NR_gettid);
    fcntl(fd, F_SETOWN_EX, &own);
    fcntl(fd, F_SETSIG, SIGTRAP);
    fcntl(fd, F_SETFL, O_ASYNC | O_NONBLOCK);
    if (!sigtrap) ioctl(fd, PERF_EVENT_IOC_ENABLE, 0);
    *out_fd = fd;
    return 0;
}

int main(void) {
    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sa.sa_sigaction = handler; sa.sa_flags = SA_SIGINFO | SA_RESTART;
    sigaction(SIGTRAP, &sa, NULL);

    int fd;
    arm(1, 128, &fd);
    arm(1, 0, &fd);
    arm(0, 0, &fd);
    if (fd < 0) return 2;
    g_fd = fd;

    for (int i = 0; i < 3; i++) {
        hits = 0;
        unsigned v;
        uintptr_t after = (uintptr_t)&&d1;
        __asm__ __volatile__("ldr %w0, [%1]" : "=r"(v) : "r"((void*)&w32) : "memory");
d1:
        usleep(120000);
        fprintf(stderr, "LDR#%d hits=%d v=%x rip=%lx after=%lx code=%d fd=%d\n",
                i, hits, v, last_rip, after, last_code, last_fd);
    }
    for (int i = 0; i < 3; i++) {
        hits = 0;
        unsigned v = 0xAA000000u + i;
        uintptr_t after = (uintptr_t)&&d2;
        __asm__ __volatile__("str %w0, [%1]" :: "r"(v), "r"((void*)&w32) : "memory");
d2:
        usleep(120000);
        fprintf(stderr, "STR#%d hits=%d rip=%lx after=%lx\n", i, hits, last_rip, after);
    }
    g_fd = -1;
    ioctl(fd, PERF_EVENT_IOC_DISABLE, 0);
    close(fd);
    return 0;
}
