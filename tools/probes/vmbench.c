/* Dense integer loop + memory traffic, mimics a VM interpreter hot loop.
 * Build for both x86_64 (run under box64) and aarch64 (native baseline). */
#include <stdio.h>

static volatile unsigned long sink;

int main(void)
{
    unsigned long x = 1, i;
    unsigned long mem[256];
    int k;

    for (k = 0; k < 256; k++) mem[k] = (unsigned long)k * 2654435761UL;

    for (i = 0; i < 30000000UL; i++) {
        x = x * 1103515245UL + 12345UL;
        x ^= (x >> 13);
        mem[x & 255] += x;
        x += mem[(x >> 8) & 255];
    }
    sink = x + mem[0];
    printf("done %lu\n", (unsigned long)sink);
    return 0;
}
