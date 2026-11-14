#!/usr/bin/env python3
"""box64's SIGSEGV si_addr carries a host address (guest 0x31f0xx mapped with
high bits). Compare only the low 32 bits, and dump 3 samples to prove it."""
p = "/tmp/b64prof-build/src/libtools/signals.c"
s = open(p).read()
old = "(b64a & ~(uintptr_t)0xfff) == 0x31f000UL"
new = "((uint32_t)(b64a & 0xfffff000UL)) == 0x31f000UL"
if old not in s:
    raise SystemExit("pattern missing")
s = s.replace(old, new, 1)

# dump 3 samples of si_addr into the same file (separate marker line)
anchor = "            ++b64p_segv;"
if anchor not in s:
    raise SystemExit("anchor missing")
sample = ("            if (b64p_segv <= 3) {\n"
          "                char sb[96]; int sfd, sn;\n"
          "                sfd = open(\"/home/xuser/b64segv.txt\", O_WRONLY|O_CREAT|O_APPEND, 0644);\n"
          "                if (sfd >= 0) { sn = snprintf(sb, sizeof(sb), \"si_addr=%llx low32=%x\\n\",\n"
          "                                  (unsigned long long)b64a, (unsigned)(uint32_t)b64a);\n"
          "                  if (sn > 0) { if (write(sfd, sb, sn) < 0) {} } close(sfd); }\n"
          "            }\n")
s = s.replace(anchor, sample + anchor, 1)
open(p, "w").write(s)
print("patched: low32 compare + sample dump")
