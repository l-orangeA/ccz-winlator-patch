#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <signal.h>
#include <unistd.h>
#include <stdint.h>
#include <sys/auxv.h>
#include <sys/mman.h>
#include <sys/prctl.h>

#ifndef HWCAP2_MTE
#define HWCAP2_MTE (1UL << 18)
#endif
#ifndef PROT_MTE
#define PROT_MTE 0x20
#endif
#ifndef PR_MTE_TCF_SYNC
#define PR_MTE_TCF_SYNC (1UL << 1)
#define PR_MTE_TAG_SHIFT 3
#define PR_TAGGED_ADDR_ENABLE (1UL << 0)
#endif

static volatile int hits = 0;
static void handler(int sig, siginfo_t *si, void *uc) {
    ucontext_t *u = (ucontext_t *)uc;
    hits++;
    fprintf(stderr, "SIGSEGV code=%d addr=%p pc=%llx\n", si->si_code, si->si_addr,
            (unsigned long long)u->uc_mcontext.pc);
}

int main(void) {
    unsigned long hw = getauxval(AT_HWCAP2);
    fprintf(stderr, "HWCAP2=%lx MTE=%d\n", hw, !!(hw & HWCAP2_MTE));
    unsigned long ctl = prctl(PR_GET_TAGGED_ADDR_CTRL, 0, 0, 0, 0);
    fprintf(stderr, "tagged_addr_ctrl=%lx errno=%d\n", ctl, errno);
    unsigned long want = PR_TAGGED_ADDR_ENABLE | PR_MTE_TCF_SYNC | (0xfffeUL << PR_MTE_TAG_SHIFT);
    long r = prctl(PR_SET_TAGGED_ADDR_CTRL, want, 0, 0, 0);
    fprintf(stderr, "set tagged ctrl=%ld errno=%d\n", r, errno);
    ctl = prctl(PR_GET_TAGGED_ADDR_CTRL, 0, 0, 0, 0);
    fprintf(stderr, "now ctrl=%lx\n", ctl);

    void *p = mmap(NULL, 4096, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
    if (p == MAP_FAILED) { perror("mmap"); return 1; }
    long rr = mprotect(p, 4096, PROT_READ | PROT_WRITE | PROT_MTE);
    fprintf(stderr, "mprotect MTE=%ld errno=%d (%s) p=%p\n", rr, errno, strerror(errno), p);
    if (rr != 0) return 2;

    /* set tag 5 on the first 16-byte granule using stg (address with tag 5 in top byte) */
    uintptr_t tagged = ((uintptr_t)p & 0x00ffffffffffffffULL) | ((uintptr_t)5 << 56);
    unsigned long val = 0x1122334455667788UL;
    __asm__ __volatile__("stg %0, [%1]" :: "r"(val), "r"(tagged) : "memory");
    fprintf(stderr, "tag set; now read untagged\n");

    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sa.sa_sigaction = handler; sa.sa_flags = SA_SIGINFO | SA_RESTART;
    sigaction(SIGSEGV, &sa, NULL);

    volatile unsigned long *q = (volatile unsigned long *)p;
    unsigned long got = *q;   /* should fault if MTE works */
    fprintf(stderr, "read ok got=%lx hits=%d\n", got, hits);

    /* clear the tag and read again */
    __asm__ __volatile__("stzg %0, [%1]" :: "r"(0UL), "r"(tagged) : "memory");
    got = *q;
    fprintf(stderr, "after untag read got=%lx hits=%d\n", got, hits);
    return 0;
}
