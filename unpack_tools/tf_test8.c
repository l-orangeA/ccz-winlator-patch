#include <windows.h>
#include <stdio.h>

static volatile LONG g_traps = 0;

static LONG CALLBACK veh(EXCEPTION_POINTERS *ep)
{
    if (ep->ExceptionRecord->ExceptionCode == EXCEPTION_SINGLE_STEP) {
        InterlockedIncrement(&g_traps);
        ep->ContextRecord->EFlags &= ~0x100u;
        return EXCEPTION_CONTINUE_EXECUTION;
    }
    return EXCEPTION_CONTINUE_SEARCH;
}

int main(void)
{
    CONTEXT ctx;
    FILE *f;
    AddVectoredExceptionHandler(1, veh);
    memset(&ctx, 0, sizeof(ctx));
    ctx.ContextFlags = CONTEXT_CONTROL;
    if (!GetThreadContext(GetCurrentThread(), &ctx)) { printf("getctx fail %lu\n", GetLastError()); return 3; }
    printf("before: eflags=%08lx eip=%08lx\n", (unsigned long)ctx.EFlags, (unsigned long)ctx.Eip);
    ctx.EFlags |= 0x100u;
    if (!SetThreadContext(GetCurrentThread(), &ctx)) { printf("setctx fail %lu\n", GetLastError()); return 4; }
    __asm__ __volatile__("nop\n\tnop\n\tnop\n\tnop\n\t" ::: "memory");
    printf("after: traps=%ld\n", (long)g_traps);
    fflush(stdout);
    f = fopen("C:\\tf8_result.txt", "w");
    if (!f) f = fopen("tf8_result.txt", "w");
    if (f) { fprintf(f, "traps=%ld\n", (long)g_traps); fclose(f); }
    return (int)g_traps;
}
