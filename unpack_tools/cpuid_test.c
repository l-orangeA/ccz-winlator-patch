#include <windows.h>
#include <stdio.h>

static void cpuid(unsigned leaf, unsigned *a, unsigned *b, unsigned *c, unsigned *d)
{
    __asm__ __volatile__("cpuid" : "=a"(*a), "=b"(*b), "=c"(*c), "=d"(*d) : "a"(leaf), "c"(0));
}

int main(void)
{
    unsigned a, b, c, d;
    char vendor[13];
    cpuid(0, &a, &b, &c, &d);
    *(unsigned *)&vendor[0] = b; *(unsigned *)&vendor[4] = d; *(unsigned *)&vendor[8] = c;
    vendor[12] = 0;
    printf("maxleaf=%08x vendor=%s\n", a, vendor);
    cpuid(1, &a, &b, &c, &d);
    printf("leaf1 eax=%08x ebx=%08x ecx=%08x edx=%08x hyp=%u\n", a, b, c, d, (c >> 31) & 1);
    cpuid(0x40000000u, &a, &b, &c, &d);
    if (a >= 0x40000000u) {
        char hv[13];
        *(unsigned *)&hv[0] = b; *(unsigned *)&hv[4] = c; *(unsigned *)&hv[8] = d;
        hv[12] = 0;
        printf("hypervisor leaf=%08x name=%s\n", a, hv);
    } else {
        printf("no hypervisor leaf (max=%08x)\n", a);
    }
    return 0;
}
