/* M1 permission probe: can a process OUTSIDE proot open a hardware data
 * breakpoint on ANOTHER tid?  Static aarch64 ELF, no bionic dependency.
 *
 * Self-contained: fork a child, child publishes &g watch address, then
 * hammers it; parent opens PERF_TYPE_BREAKPOINT on child tid, enables it,
 * and waits for a SIGTRAP/SIGIO.  No game / no Wine involved.
 */
#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>
#include <fcntl.h>
#include <signal.h>
#include <sys/syscall.h>
#include <sys/ioctl.h>
#include <sys/wait.h>
#include <linux/perf_event.h>

#ifndef HW_BREAKPOINT_RW
#define HW_BREAKPOINT_RW 3
#endif

static volatile unsigned long g = 0x11223344;
static volatile int got = 0;

static void on_sig(int s) { (void)s; got = 1; }

int main(int argc, char **argv)
{
    int pipefd[2];
    pid_t target = -1;
    int use_pid = 0;

    if (argc > 1) { target = (pid_t)atoi(argv[1]); use_pid = 1; }

    if (!use_pid) {
        if (pipe(pipefd)) { perror("pipe"); return 2; }
        pid_t c = fork();
        if (c == 0) {
            close(pipefd[0]);
            unsigned long a = (unsigned long)&g;
            write(pipefd[1], &a, sizeof(a));
            for (;;) { g++; g--; }
            _exit(0);
        }
        close(pipefd[1]);
        target = c;
        unsigned long a = 0;
        if (read(pipefd[0], &a, sizeof(a)) != sizeof(a)) { perror("read"); return 2; }
        close(pipefd[0]);
        usleep(200000);
        printf("child_tid=%d watch_addr=0x%lx (child self-reported)\n", (int)target, a);
    }

    struct perf_event_attr pe;
    memset(&pe, 0, sizeof(pe));
    pe.type = PERF_TYPE_BREAKPOINT;
    pe.size = sizeof(pe);
    pe.bp_type = HW_BREAKPOINT_RW;
    pe.bp_len = 1;
    pe.sample_period = 1;
    pe.wakeup_events = 1;
    pe.exclude_kernel = 1;
    pe.exclude_hv = 1;
    pe.disabled = 1;
    if (!use_pid) pe.bp_addr = (unsigned long)&g;
    else if (argc > 2) pe.bp_addr = strtoul(argv[2], NULL, 0);
    else {
        printf("HINT: pass target virtual address as argv[2]; using 0 (expect EINVAL)\n");
        pe.bp_addr = 0;
    }

    errno = 0;
    long fd = syscall(__NR_perf_event_open, &pe, target, -1, -1, 0);
    printf("PERF_OPEN pid=%d fd=%ld errno=%d (%s)\n",
           (int)target, fd, errno, strerror(errno));
    if (fd < 0) {
        /* also probe a plain software event on the same tid */
        memset(&pe, 0, sizeof(pe));
        pe.type = PERF_TYPE_SOFTWARE;
        pe.size = sizeof(pe);
        pe.config = PERF_COUNT_SW_CPU_CLOCK;
        pe.sample_period = 1;
        pe.disabled = 1;
        errno = 0;
        long sfd = syscall(__NR_perf_event_open, &pe, target, -1, -1, 0);
        printf("PERF_SOFT fd=%ld errno=%d (%s)\n", sfd, errno, strerror(errno));
        if (sfd >= 0) close((int)sfd);
        if (!use_pid) kill(target, SIGKILL);
        return 1;
    }

    if (!use_pid) {
        struct f_owner_ex own;
        memset(&own, 0, sizeof(own));
        own.type = F_OWNER_TID;
        own.pid = getpid();
        int rc = fcntl((int)fd, F_SETOWN_EX, &own);
        printf("F_SETOWN_EX rc=%d errno=%d\n", rc, errno);
        signal(SIGIO, on_sig);
        rc = fcntl((int)fd, F_SETFL, O_ASYNC | O_NONBLOCK);
        printf("F_SETFL rc=%d errno=%d\n", rc, errno);
    }
    int rc = ioctl((int)fd, PERF_EVENT_IOC_RESET, 0);
    printf("RESET rc=%d errno=%d\n", rc, errno);
    rc = ioctl((int)fd, PERF_EVENT_IOC_ENABLE, 0);
    printf("ENABLE rc=%d errno=%d\n", rc, errno);

    if (!use_pid) {
        for (int i = 0; i < 30 && !got; i++) usleep(100000);
        printf("SIGIO_RECEIVED=%d\n", got);
        kill(target, SIGKILL);
        waitpid(target, NULL, 0);
    }
    close((int)fd);
    printf("RESULT=%s\n", (use_pid || got) ? "PASS" : "NO_SIGNAL");
    return 0;
}
