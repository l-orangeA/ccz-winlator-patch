/* M2 client: send (tid, addr, len, rw) to the helper and print its reply. */
#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>
#include <sys/socket.h>
#include <sys/un.h>

struct req { int tid; unsigned long addr; unsigned int len; unsigned int rw; };
struct resp { int fd; int err; };

int main(int argc, char **argv)
{
    if (argc < 4) { fprintf(stderr, "usage: %s <sock> <tid> <addr_hex>\n", argv[0]); return 2; }
    const char *path = argv[1];
    struct req r;
    memset(&r, 0, sizeof(r));
    r.tid = atoi(argv[2]);
    r.addr = strtoul(argv[3], NULL, 0);
    r.len = (argc > 4) ? (unsigned)atoi(argv[4]) : 1;
    r.rw = (argc > 5) ? (unsigned)atoi(argv[5]) : 3;

    int s = socket(AF_UNIX, SOCK_STREAM, 0);
    if (s < 0) { perror("socket"); return 2; }
    struct sockaddr_un sa;
    memset(&sa, 0, sizeof(sa));
    sa.sun_family = AF_UNIX;
    strncpy(sa.sun_path, path, sizeof(sa.sun_path) - 1);
    errno = 0;
    if (connect(s, (struct sockaddr *)&sa, sizeof(sa)) < 0) {
        printf("CONNECT_FAIL errno=%d (%s)\n", errno, strerror(errno));
        return 3;
    }
    if (write(s, &r, sizeof(r)) != (ssize_t)sizeof(r)) { perror("write"); return 3; }
    struct resp rp;
    if (read(s, &rp, sizeof(rp)) != (ssize_t)sizeof(rp)) { perror("read"); return 3; }
    printf("HELPER_RESP tid=%d addr=0x%lx fd=%d errno=%d RESULT=%s\n",
           r.tid, r.addr, rp.fd, rp.err, rp.fd >= 0 ? "PASS" : "FAIL");
    close(s);
    return rp.fd >= 0 ? 0 : 1;
}
