/* P0-A final permission gap: does proot (as ptrace tracer) forward an
 * externally-generated SIGTRAP to its tracee?
 *
 * Fully self-contained: run this binary UNDER libtinyproot.so so that it
 * becomes a proot tracee.  It forks a helper that sends SIGTRAP to the
 * tracee; the tracee installs a handler and reports the outcome.
 * No Wine / no game / no running container involved.
 */
#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>
#include <fcntl.h>
#include <signal.h>
#include <sys/mman.h>
#include <sys/wait.h>

static volatile int got = 0;
static void on_trap(int s, siginfo_t *si, void *u)
{
    (void)s; (void)si; (void)u;
    got = 1;
}

int main(int argc, char **argv)
{
    const char *out = (argc > 1) ? argv[1] : "/data/local/tmp/proot_sig_result.txt";
    int fd = open(out, O_WRONLY | O_CREAT | O_TRUNC, 0644);
    if (fd < 0) { perror("open result"); return 2; }

    pid_t *shared = mmap(NULL, 4096, PROT_READ | PROT_WRITE,
                         MAP_SHARED | MAP_ANONYMOUS, -1, 0);
    if (shared == MAP_FAILED) { perror("mmap"); return 2; }
    *shared = getpid();

    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sa.sa_sigaction = on_trap;
    sa.sa_flags = SA_SIGINFO;
    if (sigaction(SIGTRAP, &sa, NULL) < 0) perror("sigaction");

    pid_t helper = fork();
    if (helper == 0) {
        usleep(400000);
        pid_t target = *shared;
        int rc = kill(target, SIGTRAP);
        char b[128];
        int n = snprintf(b, sizeof(b), "helper: kill(%d,SIGTRAP) rc=%d errno=%d\n",
                         (int)target, rc, errno);
        if (write(fd, b, n) < 0) { /* ignore */ }
        _exit(0);
    }

    for (int i = 0; i < 60 && !got; i++) usleep(100000);
    char b[256];
    int n = snprintf(b, sizeof(b),
                     "TRACE_PID=%d TRACER_PID=%d HELPER_PID=%d SIGTRAP_RECEIVED=%d RESULT=%s\n",
                     (int)getpid(), (int)getppid(), (int)helper, got,
                     got ? "PASS" : "FAIL");
    if (write(fd, b, n) < 0) { /* ignore */ }
    printf("%s", b);
    waitpid(helper, NULL, 0);
    close(fd);
    return got ? 0 : 1;
}
