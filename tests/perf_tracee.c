/* Tracee for "does perf work on a proot tracee?" - writes pid/tid/addr then spins. */
#define _GNU_SOURCE
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/syscall.h>

volatile unsigned long g;

int main(void)
{
    int fd = open("/data/local/tmp/tracee.info", O_WRONLY | O_CREAT | O_TRUNC, 0666);
    if (fd >= 0) {
        char b[128];
        int n = snprintf(b, sizeof(b), "pid=%d tid=%d addr=0x%lx\n",
                         (int)getpid(), (int)syscall(SYS_gettid), (unsigned long)&g);
        if (write(fd, b, n) < 0) { /* ignore */ }
        close(fd);
    }
    for (;;) { g++; }
    return 0;
}
