#include <windows.h>
#include <stdio.h>

int main(void)
{
    DWORD teb = __readfsdword(0x18);
    DWORD peb = __readfsdword(0x30);
    DWORD seh = __readfsdword(0x00);
    DWORD stackbase = __readfsdword(0x04);
    DWORD stacklimit = __readfsdword(0x08);
    DWORD version = __readfsdword(0x0C);
    DWORD heap = __readfsdword(0x20);
    DWORD ntglobal = __readfsdword(0x68);
    HANDLE ph = GetProcessHeap();
    DWORD hflags = ph ? *(DWORD *)((BYTE *)ph + 0x0C) : 0;
    printf("TEB=%08lx PEB=%08lx SEH=%08lx StackBase=%08lx StackLimit=%08lx\n",
           teb, peb, seh, stackbase, stacklimit);
    printf("PEB.BeingDebugged=%u NtGlobalFlag=%08lx Heap=%08lx HeapFlags=%08lx\n",
           peb ? *(BYTE *)(peb + 2) : 0, ntglobal, (unsigned long)ph, hflags);
    printf("fs:0x0c=%08lx GetVersion=%08lx NumProc=%lu\n",
           version, GetVersion(), GetSystemInfo ? 0ul : 0ul);
    return 0;
}
