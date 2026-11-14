#include <windows.h>
#include <stdio.h>

static volatile LONG g_traps = 0;
static volatile LONG g_other = 0;
static volatile DWORD g_last_eip = 0;

static LONG CALLBACK veh(EXCEPTION_POINTERS *ep)
{
    if (ep->ExceptionRecord->ExceptionCode == EXCEPTION_SINGLE_STEP) {
        InterlockedIncrement(&g_traps);
        g_last_eip = (DWORD)ep->ExceptionRecord->ExceptionAddress;
        ep->ContextRecord->EFlags &= ~0x100u;
        return EXCEPTION_CONTINUE_EXECUTION;
    }
    InterlockedIncrement(&g_other);
    return EXCEPTION_CONTINUE_SEARCH;
}

int main(void)
{
    FILE *f;
    AddVectoredExceptionHandler(1, veh);

    __asm__ __volatile__("pushfl\n\torl $0x100, (%%esp)\n\tpopfl\n\t" ::: "memory", "cc");
    __asm__ __volatile__("nop\n\tnop\n\tnop\n\t" ::: "memory");
    __asm__ __volatile__("pushfl\n\tandl $0xfffffeff, (%%esp)\n\tpopfl\n\t" ::: "memory", "cc");

    f = fopen("C:\\tf_result.txt", "w");
    if (!f) f = fopen("tf_result.txt", "w");
    if (f) {
        fprintf(f, "traps=%ld other=%ld last_eip=%08lx\n",
                (long)g_traps, (long)g_other, (unsigned long)g_last_eip);
        fclose(f);
    }
    return (int)g_traps;
}
