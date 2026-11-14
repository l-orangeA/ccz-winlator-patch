#include <windows.h>
#include <stdio.h>

static volatile LONG g_hits = 0;
static volatile LONG g_other = 0;
static volatile DWORD g_hit_addr = 0;

static LONG CALLBACK veh(EXCEPTION_POINTERS *ep)
{
    if (ep->ExceptionRecord->ExceptionCode == EXCEPTION_SINGLE_STEP) {
        InterlockedIncrement(&g_hits);
        g_hit_addr = (DWORD)ep->ExceptionRecord->ExceptionAddress;
        /* disable Dr0 */
        ep->ContextRecord->Dr7 = 0;
        ep->ContextRecord->Dr0 = 0;
        return EXCEPTION_CONTINUE_EXECUTION;
    }
    InterlockedIncrement(&g_other);
    return EXCEPTION_CONTINUE_SEARCH;
}

__attribute__((noinline)) static void target(void)
{
    volatile int x = 1;
    x = x + 1;
    (void)x;
}

int main(void)
{
    CONTEXT ctx;
    FILE *f;
    AddVectoredExceptionHandler(1, veh);
    memset(&ctx, 0, sizeof(ctx));
    ctx.ContextFlags = CONTEXT_DEBUG_REGISTERS;
    if (!GetThreadContext(GetCurrentThread(), &ctx)) {
        printf("GetThreadContext failed %lu\n", GetLastError());
        return 3;
    }
    printf("before: dr0=%08lx dr7=%08lx target=%08lx\n", ctx.Dr0, ctx.Dr7, (unsigned long)(ULONG_PTR)target);
    ctx.Dr0 = (DWORD)(ULONG_PTR)target;
    ctx.Dr7 = 0x00000001; /* L0=1, R/W0=00 (execute), LEN0=00 */
    if (!SetThreadContext(GetCurrentThread(), &ctx)) {
        printf("SetThreadContext failed %lu\n", GetLastError());
        return 4;
    }
    target();
    memset(&ctx, 0, sizeof(ctx));
    ctx.ContextFlags = CONTEXT_DEBUG_REGISTERS;
    GetThreadContext(GetCurrentThread(), &ctx);
    printf("after: dr0=%08lx dr7=%08lx hits=%ld other=%ld hit_addr=%08lx\n",
           ctx.Dr0, ctx.Dr7, (long)g_hits, (long)g_other, (unsigned long)g_hit_addr);
    f = fopen("C:\\hwbp_result.txt", "w");
    if (!f) f = fopen("hwbp_result.txt", "w");
    if (f) { fprintf(f, "hits=%ld other=%ld hit_addr=%08lx\n", (long)g_hits, (long)g_other, (unsigned long)g_hit_addr); fclose(f); }
    return (int)g_hits;
}
