#include <windows.h>

static volatile LONG av_count = 0;
static volatile DWORD av_addr = 0;
static volatile DWORD av_rip = 0;
static void *g_target = 0;
static DWORD g_old = 0;
static void *g_after = 0;

static LONG WINAPI veh(EXCEPTION_POINTERS *ep)
{
    char buf[256];
    DWORD w;
    if (ep->ExceptionRecord->ExceptionCode != EXCEPTION_ACCESS_VIOLATION) return EXCEPTION_CONTINUE_SEARCH;
    av_count++;
    av_addr = (DWORD)ep->ExceptionRecord->ExceptionInformation[1];
    av_rip  = ep->ContextRecord->Eip;
    if (av_count == 1)
    {
        DWORD old2;
        VirtualProtect(g_target, 0x1000, g_old, &old2);
        ep->ContextRecord->Eip = (DWORD)g_after;
        return EXCEPTION_CONTINUE_EXECUTION;
    }
    return EXCEPTION_CONTINUE_EXECUTION;
}

int main(void)
{
    HANDLE f;
    DWORD sp, page, old = 0, w;
    char buf[256];
    int n;
    f = CreateFileA("C:\\stkfault.txt", GENERIC_WRITE, 0, NULL, CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
    AddVectoredExceptionHandler(1, veh);
    __asm__ __volatile__("mov %%esp,%0" : "=r"(sp));
    page = (sp - 0x1000) & ~0xfff;
    g_target = (void *)page;
    g_old = 0;
    n = wsprintfA(buf, "sp=%lx target=%lx vp=%ld old=%lx\r\n", sp, page,
                  VirtualProtect((void *)page, 0x1000, PAGE_NOACCESS, &old), old);
    WriteFile(f, buf, n, &w, NULL);
    g_old = old;
    g_after = &&after;
    __asm__ __volatile__("movl $0x12345678, 0x40(%%eax)" :: "a"((void *)page) : "memory");
after:
    {
        DWORD old2;
        VirtualProtect((void *)page, 0x1000, old, &old2);
    }
    n = wsprintfA(buf, "av=%ld av_addr=%lx av_rip=%lx\r\n", av_count, av_addr, av_rip);
    WriteFile(f, buf, n, &w, NULL);
    CloseHandle(f);
    return 0;
}
