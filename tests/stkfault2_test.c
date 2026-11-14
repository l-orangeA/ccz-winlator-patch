#include <windows.h>

static volatile LONG av_count = 0;
static volatile DWORD av_addr = 0;
static volatile DWORD av_rip = 0;
static DWORD g_target = 0, g_saved_esp = 0, g_after = 0;

static LONG WINAPI veh(EXCEPTION_POINTERS *ep)
{
    if (ep->ExceptionRecord->ExceptionCode != EXCEPTION_ACCESS_VIOLATION) return EXCEPTION_CONTINUE_SEARCH;
    av_count++;
    av_addr = (DWORD)ep->ExceptionRecord->ExceptionInformation[1];
    av_rip  = ep->ContextRecord->Eip;
    {
        DWORD old2;
        VirtualProtect((void *)g_target, 0x1000, PAGE_READWRITE, &old2);
    }
    ep->ContextRecord->Esp = g_saved_esp;
    ep->ContextRecord->Eip = g_after;
    return EXCEPTION_CONTINUE_EXECUTION;
}

int main(void)
{
    HANDLE f;
    DWORD sp, old = 0, w;
    char buf[256];
    int n;
    f = CreateFileA("C:\\stkfault2.txt", GENERIC_WRITE, 0, NULL, CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
    AddVectoredExceptionHandler(1, veh);
    __asm__ __volatile__("mov %%esp,%0" : "=r"(sp));
    g_saved_esp = sp;
    g_target = (sp - 0x4000) & ~0xfff;
    g_after = (DWORD)&&after;
    n = wsprintfA(buf, "esp=%lx target=%lx vp=%ld old=%lx\r\n", sp, g_target,
                  VirtualProtect((void *)g_target, 0x1000, PAGE_NOACCESS, &old), old);
    WriteFile(f, buf, n, &w, NULL);
    __asm__ __volatile__("mov %0, %%esp\n\tpush %%eax" :: "r"(g_target + 0x400) : "memory");
after:
    {
        DWORD old2;
        VirtualProtect((void *)g_target, 0x1000, PAGE_READWRITE, &old2);
    }
    n = wsprintfA(buf, "av=%ld av_addr=%lx av_rip=%lx\r\n", av_count, av_addr, av_rip);
    WriteFile(f, buf, n, &w, NULL);
    CloseHandle(f);
    return 0;
}
