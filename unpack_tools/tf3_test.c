#include <windows.h>
#include <stdio.h>

static volatile LONG db_count = 0;
static volatile LONG other_count = 0;
static DWORD step_eips[16];
static int step_n = 0;
static DWORD other_code = 0;
static DWORD other_eip = 0;

static LONG WINAPI veh(EXCEPTION_POINTERS *ep)
{
    EXCEPTION_RECORD *r = ep->ExceptionRecord;
    CONTEXT *c = ep->ContextRecord;
    if (r->ExceptionCode == EXCEPTION_SINGLE_STEP) {
        if (step_n < 16) step_eips[step_n++] = c->Eip;
        db_count++;
        if (db_count >= 3) c->EFlags &= ~0x100u;
        return EXCEPTION_CONTINUE_EXECUTION;
    }
    other_count++;
    other_code = r->ExceptionCode;
    other_eip = c->Eip;
    return EXCEPTION_CONTINUE_SEARCH;
}

int main(void)
{
    HANDLE f;
    DWORD w;
    char buf[512];
    int n, i;
    volatile int sink = 0;

    f = CreateFileA("C:\\tftest.txt", GENERIC_WRITE, 0, NULL, CREATE_ALWAYS,
                    FILE_ATTRIBUTE_NORMAL, NULL);
    AddVectoredExceptionHandler(1, veh);

    /* set TF, run 4 instructions, clear TF */
    __asm__ __volatile__(
        "pushfl\n\t"
        "orl $0x100, (%%esp)\n\t"
        "popfl\n\t"
        "movl $11, %0\n\t"
        "movl $22, %1\n\t"
        "addl $5, %0\n\t"
        "addl $6, %1\n\t"
        "pushfl\n\t"
        "andl $-257, (%%esp)\n\t"
        "popfl\n\t"
        : "=m"(sink), "=m"(i)
        :
        : "memory", "cc");

    if (f != INVALID_HANDLE_VALUE) {
        n = wsprintfA(buf, "db=%ld other=%ld ocode=%lx oeip=%lx sink=%d i=%d\r\n",
                      db_count, other_count, other_code, other_eip, sink, i);
        for (i = 0; i < step_n; i++) {
            n += wsprintfA(buf + n, "trap%d eip=%lx\r\n", i, step_eips[i]);
        }
        WriteFile(f, buf, n, &w, NULL);
        CloseHandle(f);
    }
    return 0;
}
