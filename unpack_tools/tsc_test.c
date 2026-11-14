#include <windows.h>
#include <stdio.h>

static unsigned __int64 rdtsc(void)
{
    unsigned lo, hi;
    __asm__ __volatile__("rdtsc" : "=a"(lo), "=d"(hi));
    return ((unsigned __int64)hi << 32) | lo;
}

int main(void)
{
    unsigned __int64 t0, t1, t2;
    DWORD g0, g1;
    volatile int i, x = 0;
    t0 = rdtsc();
    for (i = 0; i < 100000; i++) x += i;
    t1 = rdtsc();
    for (i = 0; i < 100000; i++) x += i;
    t2 = rdtsc();
    g0 = GetTickCount();
    for (i = 0; i < 100000; i++) x += i;
    g1 = GetTickCount();
    printf("d1=%llu d2=%llu tickms=%lu\n", t1 - t0, t2 - t1, g1 - g0);
    return 0;
}
