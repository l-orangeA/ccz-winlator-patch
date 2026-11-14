#include <windows.h>
#include <stdio.h>

static volatile LONG g_traps = 0;
static volatile LONG g_bp = 0;
static volatile LONG g_other = 0;
static volatile DWORD g_bp_eip = 0;
static volatile DWORD g_bp_addr = 0;

static LONG CALLBACK veh(EXCEPTION_POINTERS *ep)
{
    DWORD code = ep->ExceptionRecord->ExceptionCode;
    if (code == EXCEPTION_BREAKPOINT) {
        InterlockedIncrement(&g_bp);
        g_bp_eip = (DWORD)ep->ContextRecord->Eip;
        g_bp_addr = (DWORD)ep->ExceptionRecord->ExceptionAddress;
        ep->ContextRecord->Eip = (DWORD)ep->ExceptionRecord->ExceptionAddress + 1;
        ep->ContextRecord->EFlags |= 0x100u;
        return EXCEPTION_CONTINUE_EXECUTION;
    }
    if (code == EXCEPTION_SINGLE_STEP) {
        InterlockedIncrement(&g_traps);
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
    __asm__ __volatile__("int3\n\tnop\n\tnop\n\tnop\n\tnop\n\t" ::: "memory");
    printf("bp=%ld traps=%ld other=%ld bp_eip=%08lx bp_addr=%08lx\n",
           (long)g_bp, (long)g_traps, (long)g_other,
           (unsigned long)g_bp_eip, (unsigned long)g_bp_addr);
    fflush(stdout);
    f = fopen("C:\tf2_result.txt", "w");
    if (!f) f = fopen("tf2_result.txt", "w");
    if (f) {
        fprintf(f, "bp=%ld traps=%ld bp_eip=%08lx bp_addr=%08lx\n",
                (long)g_bp, (long)g_traps, (unsigned long)g_bp_eip, (unsigned long)g_bp_addr);
        fclose(f);
    }
    return (int)g_traps;
}
