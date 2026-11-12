/* Cost breakdown probe: where do the 450 us per page fault come from?
 * A) pure SIGSEGV round trip (siglongjmp back to loop head)
 * B) mprotect 4KB pair (unlock + re-protect)
 * C) empty syscall baseline (getpid)
 * Run once outside proot (run-as) and once inside proot, compare.
 */
#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <signal.h>
#include <setjmp.h>
#include <sys/mman.h>
#include <unistd.h>
#include <time.h>

static sigjmp_buf jb;
static volatile unsigned long cnt;
static void h(int s) { (void)s; ++cnt; siglongjmp(jb, 1); }

static double now_us(void)
{
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (double)ts.tv_sec * 1e6 + (double)ts.tv_nsec / 1e3;
}

int main(void)
{
    const int N = 20000;
    const int NS = 5000;
    void *p;
    double t0, t1;
    int i;
    struct sigaction sa;

    printf("=== COST PROBE ===\n");
    printf("uid=%d\n", getuid());

    t0 = now_us();
    for (i = 0; i < N; i++) (void)getpid();
    t1 = now_us();
    printf("C getpid            : %8.2f us/call\n", (t1 - t0) / N);

    p = mmap(NULL, 4096, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
    if (p == MAP_FAILED) { printf("mmap B failed\n"); return 1; }
    t0 = now_us();
    for (i = 0; i < N; i++) {
        mprotect(p, 4096, PROT_READ);
        mprotect(p, 4096, PROT_READ | PROT_WRITE);
    }
    t1 = now_us();
    printf("B mprotect 4KB pair : %8.2f us/pair (%8.2f us/call)\n", (t1 - t0) / N, (t1 - t0) / N / 2);

    p = mmap(NULL, 4096, PROT_NONE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
    if (p == MAP_FAILED) { printf("mmap A failed\n"); return 1; }
    sa.sa_handler = h;
    sigemptyset(&sa.sa_mask);
    sa.sa_flags = SA_NODEFER;
    sigaction(SIGSEGV, &sa, NULL);

    cnt = 0;
    t0 = now_us();
    for (i = 0; i < NS; i++) {
        if (sigsetjmp(jb, 1) == 0) *(volatile char *)p = 1;
    }
    t1 = now_us();
    printf("A SIGSEGV trip      : %8.2f us/trip (n=%lu)\n", (t1 - t0) / NS, cnt);
    printf("=== DONE ===\n");
    return 0;
}
