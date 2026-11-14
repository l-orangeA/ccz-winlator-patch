#include <windows.h>

/* wfm.exe replacement: the container's default shell.
 * Starts the game in its own directory and stays alive while it runs. */

int main(void)
{
    STARTUPINFOA si;
    PROCESS_INFORMATION pi;
    HANDLE log;
    char buf[256];
    DWORD n;

    memset(&si, 0, sizeof(si));
    si.cb = sizeof(si);

    SetCurrentDirectoryA("D:\\Download\\ccz22");

    log = CreateFileA("C:\\wfm_launcher.log", GENERIC_WRITE, FILE_SHARE_READ,
                      NULL, CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);

    if (!CreateProcessA(NULL, "Ekd5.exe", NULL, NULL, FALSE, 0, NULL,
                        "D:\\Download\\ccz22", &si, &pi))
    {
        if (log != INVALID_HANDLE_VALUE)
        {
            n = (DWORD)wsprintfA(buf, "CreateProcess failed %lu\r\n", GetLastError());
            WriteFile(log, buf, n, &n, NULL);
            CloseHandle(log);
        }
        return 1;
    }

    if (log != INVALID_HANDLE_VALUE)
    {
        n = (DWORD)wsprintfA(buf, "started pid=%lu\r\n", (unsigned long)pi.dwProcessId);
        WriteFile(log, buf, n, &n, NULL);
        CloseHandle(log);
    }

    WaitForSingleObject(pi.hProcess, INFINITE);
    return 0;
}
