#include <windows.h>
#include <stdio.h>

static volatile LONG g_traps = 0;
static volatile LONG g_bp = 0;
static volatile DWORD g_val[8];
static volatile DWORD g_seg[4];
static volatile DWORD g_eip = 0;
static volatile DWORD g_efl = 0;

static LONG CALLBACK veh(EXCEPTION_POINTERS *ep)
{
    DWORD code = ep->ExceptionRecord->ExceptionCode;
    if (code == EXCEPTION_BREAKPOINT) {
        InterlockedIncrement(&g_bp);
        g_eip = (DWORD)ep->ContextRecord->Eip;
        g_efl = (DWORD)ep->ContextRecord->EFlags;
        g_val[0] = (DWORD)ep->ContextRecord->Dr0;
        g_val[1] = (DWORD)ep->ContextRecord->Dr1;
        g_val[2] = (DWORD)ep->ContextRecord->Dr2;
        g_val[3] = (DWORD)ep->ContextRecord->Dr3;
        g_val[4] = (DWORD)ep->ContextRecord->Dr6;
        g_val[5] = (DWORD)ep->ContextRecord->Dr7;
        g_seg[0] = ep->ContextRecord->SegCs;
        g_seg[1] = ep->ContextRecord->SegDs;
        g_seg[2] = ep->ContextRecord->SegFs;
        g_seg[3] = ep->ContextRecord->SegSs;
        ep->ContextRecord->Eip = (DWORD)ep->ExceptionRecord->ExceptionAddress + 1;
        ep->ContextRecord->EFlags |= 0x100u;
        return EXCEPTION_CONTINUE_EXECUTION;
    }
    if (code == EXCEPTION_SINGLE_STEP) {
        InterlockedIncrement(&g_traps);
        ep->ContextRecord->EFlags &= ~0x100u;
        return EXCEPTION_CONTINUE_EXECUTION;
    }
    return EXCEPTION_CONTINUE_SEARCH;
}

int main(void)
{
    FILE *f;
    AddVectoredExceptionHandler(1, veh);
    __asm__ __volatile__("int3\n\tnop\n\tnop\n\tnop\n\tnop\n\t" ::: "memory");
    printf("bp=%ld traps=%ld eip=%08lx efl=%08lx\n", (long)g_bp, (long)g_traps, (unsigned long)g_eip, (unsigned long)g_efl);
    printf("dr0=%08lx dr1=%08lx dr2=%08lx dr3=%08lx dr6=%08lx dr7=%08lx\n",
           (unsigned long)g_val[0], (unsigned long)g_val[1], (unsigned long)g_val[2],
           (unsigned long)g_val[3], (unsigned long)g_val[4], (unsigned long)g_val[5]);
    printf("cs=%lx ds=%lx fs=%lx ss=%lx\n", (unsigned long)g_seg[0], (unsigned long)g_seg[1],
           (unsigned long)g_seg[2], (unsigned long)g_seg[3]);
    fflush(stdout);
    f = fopen("C:\\tf6_result.txt", "w");
    if (!f) f = fopen("tf6_result.txt", "w");
    if (f) {
        fprintf(f, "bp=%ld traps=%ld eip=%08lx efl=%08lx\n", (long)g_bp, (long)g_traps, (unsigned long)g_eip, (unsigned long)g_efl);
        fprintf(f, "dr0=%08lx dr1=%08lx dr2=%08lx dr3=%08lx dr6=%08lx dr7=%08lx\n",
                (unsigned long)g_val[0], (unsigned long)g_val[1], (unsigned long)g_val[2],
                (unsigned long)g_val[3], (unsigned long)g_val[4], (unsigned long)g_val[5]);
        fprintf(f, "cs=%lx ds=%lx fs=%lx ss=%lx\n", (unsigned long)g_seg[0], (unsigned long)g_seg[1],
                (unsigned long)g_seg[2], (unsigned long)g_seg[3]);
        fclose(f);
    }
    return (int)g_traps;
}
