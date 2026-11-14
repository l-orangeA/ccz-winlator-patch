/* Raw no-libc tracee: spins on a fixed low address so perf can watch it.
 * Built with -nostdlib -static -Wl,-Ttext=0x1000000 -Wl,-Tbss=0x2000000
 * so `g` lands at a 39-bit-friendly low address (proot-safe, no glibc).
 */
static volatile unsigned long g;

void _start(void)
{
    for (;;) { g++; }
}
