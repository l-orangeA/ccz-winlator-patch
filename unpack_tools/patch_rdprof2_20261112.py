#!/usr/bin/env python3
"""Lower RDPROF dump threshold to 256 and add an entry counter to prove
whether NtReadFile is even the source of the 79k reads."""
P = "/root/wine0905/dlls/ntdll/unix/file.c"
s = open(P, encoding="utf-8", errors="surrogateescape").read()

if "rdprof_enter" in s:
    print("already patched")
    raise SystemExit(0)

s = s.replace("if (!(++rdprof_total & 4095)) rdprof_dump();",
              "if (!(++rdprof_total & 255)) rdprof_dump();", 1)
s = s.replace("static unsigned long long rdprof_total;",
              "static unsigned long long rdprof_total, rdprof_enter;", 1)

old_fmt = 'n += snprintf( b + n, sizeof(b) - n, "total=%llu\\n", rdprof_total );'
new_fmt = 'n += snprintf( b + n, sizeof(b) - n, "total=%llu enter=%llu\\n", rdprof_total, rdprof_enter );'
if old_fmt not in s:
    raise SystemExit("format line not found")
s = s.replace(old_fmt, new_fmt, 1)

old_tr = '    TRACE( "(%p,%p,%p,%p,%p,%p,0x%08x,%p,%p)\\n",'
new_tr = '    ++rdprof_enter;\n    TRACE( "(%p,%p,%p,%p,%p,%p,0x%08x,%p,%p)\\n",'
if old_tr not in s:
    raise SystemExit("TRACE anchor not found")
s = s.replace(old_tr, new_tr, 1)

open(P, "w", encoding="utf-8", errors="surrogateescape", newline="\n").write(s)
print("patched: threshold=256, enter counter on")
