#!/usr/bin/env python3
"""JIT-HWBP pass 2e: SINGLE VARIABLE = guest TF.

Baseline = jit6 (known-good, 1629 push matches via file channel, page
protection ON, shell passes). The ONLY change is that box64 sets guest TF
on a match. Wine (jit4) merely counts every TRCTRAP and changes no
behaviour.

Judgement: if jittrap > steps, setting TF inside a JIT CALL really produces
extra host SIGTRAPs reaching Wine -> the #DB delivery chain is viable.
"""
import shutil

d = "src/box64_clean030/src/dynarec/dynarec_native_functions.c"
text = open(d, encoding="utf-8").read()
start = text.index("/* JIT-HWBP pass 1 (diagnostic) -- pure file channel")
end = text.index("\n}", start) + 2
new = r'''/* JIT-HWBP pass 2e: file channel (jit6-proven) + guest TF on match.
 * Wine writes Dr1 to /home/xuser/hwbp_watch.txt whenever it changes.
 * Refresh every 256 pushes (per-push open() stalled boot in jit8/jit9). */
void native_hwbp_check(x64emu_t* emu)
{
    static uint32_t watch;
    static unsigned int attempts;
    static int called_marked;

    if (!called_marked) {
        int fd = open("/home/xuser/hwbp_jit_called.txt",
                      O_WRONLY | O_CREAT | O_TRUNC, 0644);
        called_marked = 1;
        if (fd >= 0) { write(fd, "1", 1); close(fd); }
    }
    if ((attempts++ & 0xffu) == 0) {
        int fd = open("/home/xuser/hwbp_watch.txt", O_RDONLY);
        if (fd >= 0) {
            char b[40];
            int n = read(fd, b, (int)sizeof(b) - 1);
            close(fd);
            if (n > 0) { b[n] = 0; watch = (uint32_t)strtoul(b, NULL, 16); }
        }
    }
    if (!watch || (uint32_t)R_RSP != watch) return;

    /* log the match (Wine reads this file, cheap: ~1600 times per boot) */
    {
        int fd = open("/home/xuser/hwbp_jit_hits.txt",
                      O_WRONLY | O_CREAT | O_APPEND, 0644);
        if (fd >= 0) {
            char b[32];
            int n = snprintf(b, sizeof(b), "%x\n", (uint32_t)R_RSP);
            if (n > 0) write(fd, b, n);
            close(fd);
        }
    }
    emu->eflags.x64 |= 0x100;   /* TF: box64 emits SIGTRAP(si_code=1) */
}'''
open(d, "w", encoding="utf-8", newline="\n").write(text[:start] + new + text[end:])
print("box64 native rewritten: jit6 channel + TF only")
