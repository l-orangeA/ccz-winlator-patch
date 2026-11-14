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
#include <sys/ptrace.h>
#include <sys/wait.h>
#include <sys/syscall.h>
#include <sys/mman.h>
#include <linux/perf_event.h>
#include <linux/hw_breakpoint.h>

static volatile int w32 = 0x11223344;

static int do_perf(void) {
    struct perf_event_attr a;
    memset(&a, 0, sizeof(a));
    a.type = PERF_TYPE_BREAKPOINT;
    a.size = sizeof(a);
    a.bp_type = HW_BREAKPOINT_RW;
    a.bp_addr = (unsigned long)&w32;
    a.bp_len = HW_BREAKPOINT_LEN_4;
    a.sample_period = 1;
    a.wakeup_events = 1;
    a.exclude_kernel = 1;
    a.exclude_hv = 1;
    a.disabled = 1;
    int fd = syscall(__NR_perf_event_open, &a, 0, -1, -1, 0);
    int e = errno;
    fprintf(stderr, "perf_event_open -> fd=%d errno=%d (%s)\n", fd, e, strerror(e));
    if (fd >= 0) close(fd);
    return fd;
}

int main(int argc, char **argv) {
    int traced = (argc > 1 && argv[1][0] == 't');
    if (traced) {
        pid_t pid = fork();
        if (pid == 0) {
            ptrace(PTRACE_TRACEME, 0, 0, 0);
            raise(SIGSTOP);
            do_perf();
            _exit(0);
        }
        int st;
        waitpid(pid, &st, 0);
        fprintf(stderr, "parent: attached, continuing (stop=%x)\n", st);
        ptrace(PTRACE_CONT, pid, 0, 0);
        waitpid(pid, &st, 0);
        fprintf(stderr, "parent: child exited\n");
        return 0;
    }
    do_perf();
    return 0;
}
