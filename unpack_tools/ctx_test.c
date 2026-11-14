#include <windows.h>
#include <stdio.h>

typedef struct _PEB32 {
    BYTE Reserved1[2];
    BYTE BeingDebugged;
    BYTE Reserved2[1];
    DWORD Reserved3[2];
    DWORD Ldr;
    DWORD ProcessParameters;
    DWORD Reserved4[3];
    DWORD AtlThunkSListPtr;
    DWORD Reserved5;
    DWORD Reserved6;
    DWORD Reserved7;
    DWORD Reserved8;
    DWORD Reserved9;
    DWORD Reserved10;
    DWORD Reserved11;
    DWORD PostProcessInitRoutine;
    DWORD Reserved12[128];
    DWORD SessionId;
} PEB32;

int main(void)
{
    CONTEXT ctx;
    DWORD peb = 0, being = 0, ntglobal = 0;
    int i;
    memset(&ctx, 0, sizeof(ctx));
    ctx.ContextFlags = CONTEXT_DEBUG_REGISTERS;
    if (!GetThreadContext(GetCurrentThread(), &ctx)) {
        printf("GetThreadContext FAILED err=%lu\n", GetLastError());
        return 2;
    }
    printf("DR0=%08lx DR1=%08lx DR2=%08lx DR3=%08lx DR6=%08lx DR7=%08lx\n",
           ctx.Dr0, ctx.Dr1, ctx.Dr2, ctx.Dr3, ctx.Dr6, ctx.Dr7);
    __asm__ __volatile__("movl %%fs:0x30, %0" : "=r"(peb));
    __asm__ __volatile__("movl %%fs:0x68, %0" : "=r"(ntglobal));
    if (peb) {
        PEB32 *p = (PEB32 *)(ULONG_PTR)peb;
        being = p->BeingDebugged;
    }
    printf("PEB=%08lx BeingDebugged=%lu NtGlobalFlag=%08lx\n",
           (unsigned long)peb, (unsigned long)being, (unsigned long)ntglobal);
    printf("TEB=%08lx\n", (unsigned long)(ULONG_PTR)NtCurrentTeb());
    return 0;
}
