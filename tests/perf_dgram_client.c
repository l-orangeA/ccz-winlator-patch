/* Minimal datagram client: send one M3a request to the helper. */
#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>
#include <sys/socket.h>
#include <sys/un.h>

struct req { int tid; unsigned long addr; unsigned int len; unsigned int rw; };

int main(int argc, char **argv)
{
    if (argc < 4) { fprintf(stderr, "usage: %s <sock> <tid> <addr_hex>\n", argv[0]); return 2; }
    int fd = socket(AF_UNIX, SOCK_DGRAM, 0);
    if (fd < 0) { perror("socket"); return 2; }
    struct sockaddr_un sa;
    memset(&sa, 0, sizeof(sa));
    sa.sun_family = AF_UNIX;
    strncpy(sa.sun_path, argv[1], sizeof(sa.sun_path) - 1);
    struct req r;
    memset(&r, 0, sizeof(r));
    r.tid = atoi(argv[2]);
    r.addr = strtoul(argv[3], NULL, 0);
    r.len = 1;
    r.rw = 3;
    errno = 0;
    ssize_t n = sendto(fd, &r, sizeof(r), 0, (struct sockaddr *)&sa, sizeof(sa));
    printf("SENDTO ret=%zd errno=%d (%s) path=%s\n", n, errno, strerror(errno), sa.sun_path);
    close(fd);
    return n == (ssize_t)sizeof(r) ? 0 : 1;
}
