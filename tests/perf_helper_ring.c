/* M3b helper: polls a 4 KB shared-memory ring written by container-side ntdll.
 * usage: perf_helper_ring <ringfile> [daemon <logfile>]
 */
#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/syscall.h>
#include <sys/ioctl.h>
#include <sys/wait.h>
#include <signal.h>
#include <dirent.h>
#include <linux/perf_event.h>

#ifndef HW_BREAKPOINT_RW
#define HW_BREAKPOINT_RW 3
#endif
#ifndef HW_BREAKPOINT_W
#define HW_BREAKPOINT_W 2
#endif

struct entry { int tid; unsigned long addr; unsigned int len; unsigned int rw; };
struct ring {
    volatile unsigned int head;
    volatile unsigned int tail;
    unsigned int drop;
    unsigned int pad;
    struct entry e[64];
};

static int arm_one(int tid, unsigned long addr, unsigned int len, unsigned int rw)
{
    struct perf_event_attr pe;
    memset(&pe, 0, sizeof(pe));
    pe.type = PERF_TYPE_BREAKPOINT;
    pe.size = sizeof(pe);
    pe.bp_type = (rw == 3) ? HW_BREAKPOINT_RW : HW_BREAKPOINT_W;
    pe.bp_addr = addr;
    pe.bp_len = len ? len : 1;
    pe.sample_period = 1;
    pe.wakeup_events = 1;
    pe.exclude_kernel = 1;
    pe.exclude_hv = 1;
    pe.disabled = 1;
    errno = 0;
    int fd = (int)syscall(__NR_perf_event_open, &pe, tid, -1, -1, 0);
    if (fd < 0) return -1;

    /* M3c: deliver SIGIO straight to the watched thread; ntdll turns it into #DB */
    struct f_owner_ex own;
    memset(&own, 0, sizeof(own));
    own.type = F_OWNER_TID;
    own.pid = tid;
    signal(SIGIO, SIG_IGN);      /* never let a stray SIGIO kill the helper */
    signal(SIGTRAP, SIG_IGN);    /* M3c: proot forwards SIGTRAP, delivers to tracee */
    errno = 0;
    int r1 = fcntl(fd, F_SETOWN_EX, &own); int e1 = errno;
    errno = 0;
    int r2 = fcntl(fd, F_SETSIG, SIGTRAP); int e2 = errno;
    errno = 0;
    int r3 = fcntl(fd, F_SETFL, O_ASYNC | O_NONBLOCK); int e3 = errno;
    int r4 = (int)ioctl(fd, PERF_EVENT_IOC_RESET, 0);
    int r5 = (int)ioctl(fd, PERF_EVENT_IOC_ENABLE, 0);
    printf("ARMCFG fd=%d setown=%d/%d setsig=%d/%d setfl=%d/%d reset=%d enable=%d\n",
           fd, r1, e1, r2, e2, r3, e3, r4, r5);
    fflush(stdout);
    return fd;
}

int main(int argc, char **argv)
{
    const char *path = (argc > 1) ? argv[1] : "/home/xuser/hwbp_ring";

    if (argc > 3 && strcmp(argv[1], "--armx") == 0) {
        int tid = atoi(argv[2]);
        unsigned long addr = strtoul(argv[3], NULL, 0);
        struct perf_event_attr pe;
        memset(&pe, 0, sizeof(pe));
        pe.type = PERF_TYPE_BREAKPOINT;
        pe.size = sizeof(pe);
        pe.bp_type = 4;            /* HW_BREAKPOINT_X: execute */
        pe.bp_addr = addr;
        pe.bp_len = 1;
        pe.sample_period = 1;
        pe.wakeup_events = 1;
        pe.exclude_kernel = 1;
        pe.exclude_hv = 1;
        pe.disabled = 1;
        errno = 0;
        int fd = (int)syscall(__NR_perf_event_open, &pe, tid, -1, -1, 0);
        printf("ARMX tid=%d addr=0x%lx fd=%d errno=%d\n", tid, addr, fd, errno);
        fflush(stdout);
        if (fd < 0) return 1;
        signal(SIGTRAP, SIG_IGN);
        signal(SIGIO, SIG_IGN);
        ioctl(fd, PERF_EVENT_IOC_ENABLE, 0);
        for (int i = 0; i < 10; i++) {
            unsigned long long cnt = 0;
            usleep(100000);
            read(fd, &cnt, sizeof(cnt));
            printf("ARMX cnt=%llu\n", cnt);
            fflush(stdout);
        }
        return 0;
    }

    if (argc > 4 && strcmp(argv[1], "--arm") == 0) {
        int tid = atoi(argv[2]);
        unsigned long addr = strtoul(argv[3], NULL, 0);
        unsigned int len = (unsigned)atoi(argv[4]);
        int fd = arm_one(tid, addr, len, 3);
        printf("ARMONE tid=%d addr=0x%lx len=%u fd=%d errno=%d\n", tid, addr, len, fd, errno);
        fflush(stdout);
        if (fd < 0) return 1;
        for (int i = 0; i < 12; i++) {
            unsigned long long cnt = 0;
            usleep(100000);
            read(fd, &cnt, sizeof(cnt));
            printf("ARMONE cnt=%llu\n", cnt);
            fflush(stdout);
        }
        return 0;
    }

    if (argc > 1 && strcmp(argv[1], "--selftest") == 0) {
        volatile unsigned long *g = mmap(NULL, 4096, PROT_READ | PROT_WRITE,
                                         MAP_SHARED | MAP_ANONYMOUS, -1, 0);
        if (g == MAP_FAILED) { perror("mmap"); return 2; }
        pid_t c = fork();
        if (c == 0) { signal(SIGTRAP, SIG_IGN); for (;;) { (*g)++; } _exit(0); }
        usleep(100000);
        int fd = arm_one(c, (unsigned long)g, 8, 3);
        printf("SELFTEST tracee=%d addr=0x%lx fd=%d errno=%d\n",
               (int)c, (unsigned long)g, fd, errno);
        fflush(stdout);
        if (fd < 0) { kill(c, SIGKILL); return 1; }
        for (int i = 0; i < 10; i++) {
            unsigned long long cnt = 0;
            usleep(100000);
            ssize_t rn = read(fd, &cnt, sizeof(cnt));
            printf("SELFTEST cnt=%llu read=%zd errno=%d\n", cnt, rn, errno);
            fflush(stdout);
        }
        kill(c, SIGKILL);
        waitpid(c, NULL, 0);
        return 0;
    }

    if (argc > 3 && strcmp(argv[2], "daemon") == 0) {
        pid_t p = fork();
        if (p > 0) _exit(0);
        setsid();
        int dn = open("/dev/null", O_RDWR);
        if (dn >= 0) { dup2(dn, 0); dup2(dn, 1); dup2(dn, 2); }
        int lf = open(argv[3], O_WRONLY | O_CREAT | O_APPEND, 0644);
        if (lf >= 0) { dup2(lf, 1); dup2(lf, 2); }
    }

    int fd = open(path, O_RDWR | O_CREAT, 0666);
    if (fd < 0) { perror("open ring"); return 2; }
    if (ftruncate(fd, 4096) < 0) { perror("ftruncate"); return 2; }
    struct ring *r = mmap(NULL, 4096, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
    if (r == MAP_FAILED) { perror("mmap"); return 2; }
    r->head = 0;
    r->tail = 0;
    r->drop = 0;

    printf("READY_RING path=%s pid=%d\n", path, (int)getpid());
    fflush(stdout);

    unsigned int last = 0, ok = 0, fail = 0;
    int kept = -1;
    int fds[32];
    for (int k = 0; k < 32; k++) fds[k] = -1;
    struct entry prev;
    memset(&prev, 0, sizeof(prev));

    for (;;) {
        unsigned int h = r->head;
        while (last != h) {
            struct entry e = r->e[last & 63];
            if (kept < 0 || e.tid != prev.tid || e.addr != prev.addr ||
                e.len != prev.len || e.rw != prev.rw) {
                int nfd = 0, nthreads = 0;
                char tpath[64];
                for (int k = 0; k < 32; k++) { if (fds[k] >= 0) { close(fds[k]); fds[k] = -1; } }
                kept = -1;
                snprintf(tpath, sizeof(tpath), "/proc/%d/task", e.tid);
                DIR *dp = opendir(tpath);
                if (dp) {
                    struct dirent *de;
                    while ((de = readdir(dp)) && nfd < 32) {
                        if (de->d_name[0] < '0' || de->d_name[0] > '9') continue;
                        int tt = atoi(de->d_name);
                        int fd = arm_one(tt, e.addr, e.len, e.rw);
                        if (fd >= 0) fds[nfd++] = fd;
                        nthreads++;
                    }
                    closedir(dp);
                }
                if (nfd > 0) {
                    kept = fds[0];
                    prev = e;
                    ok++;
                    printf("ARM tid=%d addr=0x%lx len=%u rw=%u threads=%d armed=%d ok=%u head=%u\n",
                           e.tid, e.addr, e.len, e.rw, nthreads, nfd, ok, h);
                } else {
                    fail++;
                    printf("ARM_FAIL tid=%d addr=0x%lx threads=%d errno=%d fail=%u\n",
                           e.tid, e.addr, nthreads, errno, fail);
                }
                fflush(stdout);
            }
            last++;
            r->tail = last;
        }
        static int tick;
        if (kept >= 0 && (++tick % 100) == 0) {
            unsigned long long cnt = 0;
            errno = 0;
            ssize_t rn = read(kept, &cnt, sizeof(cnt));
            printf("COUNT fd=%d read=%zd cnt=%llu errno=%d\n", kept, rn, cnt, errno);
            fflush(stdout);
        }
        usleep(200);
    }
}
