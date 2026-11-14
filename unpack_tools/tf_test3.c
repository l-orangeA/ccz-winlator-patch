#include <windows.h>
#include <stdio.h>

static volatile LONG g_traps = 0;
static volatile LONG g_bp = 0;
static volatile DWORD g_bp_eip = 0;

typedef struct { struct { void *Prev; void *Handler; } *unused; } DUMMY;

typedef struct _REG {
    struct _REG *Prev;
    void *Handler;
} REG;

static DWORD CDECL handler(EXCEPTION_RECORD *rec, void *frame, CONTEXT *ctx, void *disp)
{
    (void)frame; (void)disp;
    if (rec->ExceptionCode == EXCEPTION_BREAKPOINT) {
        InterlockedIncrement(&g_bp);
        g_bp_eip = (DWORD)ctx->Eip;
        ctx->Eip = (DWORD)(ULONG_PTR)rec->ExceptionAddress + 1;
        ctx->EFlags |= 0x100u;
        return 1; /* ExceptionContinueExecution */
    }
    if (rec->ExceptionCode == EXCEPTION_SINGLE_STEP) {
        InterlockedIncrement(&g_traps);
        ctx->EFlags &= ~0x100u;
        return 1;
    }
    return 0; /* ExceptionContinueSearch */
}

int main(void)
{
    FILE *f;
    REG reg;
    DWORD prev;
    reg.Prev = (REG *)__readfsdword(0);
    reg.Handler = (void *)handler;
    prev = __readfsdword(0);
    printf("before int3
"); fflush(stdout);
    __writefsdword(0, (DWORD)(ULONG_PTR)&reg);
    __asm__ __volatile__("int3\n\tnop\n\tnop\n\tnop\n\tnop\n\t" ::: "memory");
    __writefsdword(0, prev);
    printf("bp=%ld traps=%ld bp_eip=%08lx\n", (long)g_bp, (long)g_traps, (unsigned long)g_bp_eip);
    fflush(stdout);
    f = fopen("C:\\tf3_result.txt", "w");
    if (!f) f = fopen("tf3_result.txt", "w");
    if (f) { fprintf(f, "bp=%ld traps=%ld bp_eip=%08lx\n", (long)g_bp, (long)g_traps, (unsigned long)g_bp_eip); fclose(f); }
    return (int)g_traps;
}
