#include <windows.h>
#include <stdio.h>

static volatile LONG g_veh_bp = 0;
static volatile LONG g_uef = 0;
static volatile LONG g_traps = 0;
static volatile LONG g_other = 0;
static volatile DWORD g_eflags_at_uef = 0;
static volatile DWORD g_eip_at_uef = 0;

static LONG CALLBACK veh(EXCEPTION_POINTERS *ep)
{
    DWORD code = ep->ExceptionRecord->ExceptionCode;
    if (code == EXCEPTION_BREAKPOINT) {
        InterlockedIncrement(&g_veh_bp);
        ep->ContextRecord->EFlags |= 0x100u;
        ep->ContextRecord->Eip = (DWORD)(ULONG_PTR)ep->ExceptionRecord->ExceptionAddress + 1;
        return EXCEPTION_CONTINUE_SEARCH;
    }
    if (code == EXCEPTION_SINGLE_STEP) {
        InterlockedIncrement(&g_traps);
        ep->ContextRecord->EFlags &= ~0x100u;
        return EXCEPTION_CONTINUE_EXECUTION;
    }
    InterlockedIncrement(&g_other);
    return EXCEPTION_CONTINUE_SEARCH;
}

static LONG WINAPI uef(EXCEPTION_POINTERS *ep)
{
    if (ep->ExceptionRecord->ExceptionCode == EXCEPTION_BREAKPOINT) {
        InterlockedIncrement(&g_uef);
        g_eflags_at_uef = (DWORD)ep->ContextRecord->EFlags;
        g_eip_at_uef = (DWORD)ep->ContextRecord->Eip;
        return EXCEPTION_CONTINUE_EXECUTION;
    }
    return EXCEPTION_CONTINUE_SEARCH;
}

int main(void)
{
    FILE *f;
    AddVectoredExceptionHandler(1, veh);
    SetUnhandledExceptionFilter(uef);
    __asm__ __volatile__("int3\n\tnop\n\tnop\n\tnop\n\tnop\n\t" ::: "memory");
    printf("veh_bp=%ld uef=%ld traps=%ld other=%ld uef_eflags=%08lx uef_eip=%08lx\n",
           (long)g_veh_bp, (long)g_uef, (long)g_traps, (long)g_other,
           (unsigned long)g_eflags_at_uef, (unsigned long)g_eip_at_uef);
    fflush(stdout);
    f = fopen("C:\\tf7_result.txt", "w");
    if (!f) f = fopen("tf7_result.txt", "w");
    if (f) { fprintf(f, "veh_bp=%ld uef=%ld traps=%ld other=%ld uef_eflags=%08lx\n",
                     (long)g_veh_bp, (long)g_uef, (long)g_traps, (long)g_other,
                     (unsigned long)g_eflags_at_uef); fclose(f); }
    return (int)g_traps;
}
