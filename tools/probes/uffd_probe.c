/* Minimal userfaultfd capability probe (aarch64 static).
 * Prints exactly which step fails so the result is actionable. */
#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <fcntl.h>
#include <sys/syscall.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <linux/userfaultfd.h>

int main(void) {
    int fd, r;
    struct uffdio_api api;
    struct uffdio_register reg;
    void *page;

    printf("=== UFFD PROBE ===\n");
    printf("uid=%d euid=%d\n", getuid(), geteuid());

    errno = 0;
    fd = (int)syscall(SYS_userfaultfd, O_CLOEXEC | O_NONBLOCK);
    printf("STEP1 userfaultfd() = %d errno=%d (%s)\n", fd, errno, strerror(errno));
    if (fd < 0) { printf("RESULT: FAIL at STEP1\n"); return 1; }

    memset(&api, 0, sizeof(api));
    api.api = UFFD_API;
    errno = 0;
    r = ioctl(fd, UFFDIO_API, &api);
    printf("STEP2 UFFDIO_API = %d errno=%d (%s) ioctls=0x%llx features=0x%llx\n",
           r, errno, strerror(errno),
           (unsigned long long)api.ioctls, (unsigned long long)api.features);
    if (r < 0) { printf("RESULT: FAIL at STEP2\n"); close(fd); return 2; }

    page = mmap(NULL, 4096, PROT_READ | PROT_WRITE,
                MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
    if (page == MAP_FAILED) { printf("mmap failed errno=%d\n", errno); close(fd); return 3; }

    memset(&reg, 0, sizeof(reg));
    reg.range.start = (unsigned long)page;
    reg.range.len = 4096;
    reg.mode = UFFDIO_REGISTER_MODE_MISSING;
    errno = 0;
    r = ioctl(fd, UFFDIO_REGISTER, &reg);
    printf("STEP3 UFFDIO_REGISTER(MISSING) = %d errno=%d (%s) ioctls=0x%llx\n",
           r, errno, strerror(errno), (unsigned long long)reg.ioctls);
    if (r < 0) { printf("RESULT: FAIL at STEP3\n"); close(fd); return 4; }

    printf("RESULT: PASS (all 3 steps)\n");
    close(fd);
    return 0;
}
