#include <windows.h>
#include <stdio.h>

int main(void)
{
    STARTUPINFOA si;
    PROCESS_INFORMATION pi;
    FILE *f;
    char cmd[] = "D:\\ccz22\\Ekd5.exe";
    memset(&si, 0, sizeof(si));
    si.cb = sizeof(si);
    f = fopen("C:\\launcher.log", "w");
    if (f) { fprintf(f, "start\n"); fflush(f); }
    if (CreateProcessA(NULL, cmd, NULL, NULL, FALSE, 0, NULL, "D:\\ccz22", &si, &pi)) {
        if (f) { fprintf(f, "pid=%lu\n", (unsigned long)pi.dwProcessId); fflush(f); }
        WaitForSingleObject(pi.hProcess, INFINITE);
        if (f) { DWORD code = 0; GetExitCodeProcess(pi.hProcess, &code); fprintf(f, "exit=%lu\n", (unsigned long)code); fclose(f); }
    } else {
        if (f) { fprintf(f, "CreateProcess failed %lu\n", GetLastError()); fclose(f); }
    }
    return 0;
}
