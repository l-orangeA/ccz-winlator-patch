#!/usr/bin/env python3
"""skip-int3 v8: fix the 32-bit stack top before resuming at syscall_32to64.

Formula (wow64cpu/cpu.c, syscall_32to64 prolog):
    xchgq %r14,%rsp
    movl (%r14),%edx
    movl %edx,0xb8(%r13)      /* context->Eip <- 32-bit stack top */
    ...
    call Wow64SystemServiceEx
    movl %eax,0xb0(%r13)      /* context->Eax <- NTSTATUS */
  syscall_32to64_return:
    movl 0xb8(%r13),%edx      /* Eip */
    ljmp *(%r14)              /* back to 32-bit code */

v5 proved this path resumes execution, but the stack top contained 0xC000001C
(the NTSTATUS from an unsupported syscall) so Eip became C000001C -> crash.
v8 writes Eip+1 into that stack slot before NtContinue, and restores the
prolog entry (not the return label).
"""
import sys

CPU = "/root/wine1010/dlls/wow64cpu/cpu.c"
c = open(CPU, encoding="utf-8", errors="replace").read()

old_rip = "    context->Rip = (ULONG64)(skip_int3 ? wow64_resume_32 : syscall_32to64);"
if c.count(old_rip) != 1:
    sys.exit("FAIL: v7 Rip line count=%d" % c.count(old_rip))
c = c.replace(old_rip, "    context->Rip = (ULONG64)syscall_32to64;", 1)

anchor = "    machine_frame->rsp = context->Rsp;"
if c.count(anchor) != 1:
    sys.exit("FAIL: machine_frame rsp count=%d" % c.count(anchor))
add = (anchor + "\n"
       "    /* SKIP-INT3 v8: syscall_32to64 prolog reads the 32-bit return\n"
       "     * address from the top of the 32-bit stack; make it Eip+1. */\n"
       "    if (skip_int3)\n"
       "    {\n"
       "        *(DWORD *)(ULONG_PTR)context->R14 = (DWORD)wow_context.Eip;\n"
       "        ERR( \"SKIP-INT3 v8: stack top set to %lx\\n\", (ULONG_PTR)wow_context.Eip );\n"
       "    }")
c = c.replace(anchor, add, 1)
open(CPU, "w", encoding="utf-8", newline="\n").write(c)
print("OK v8 applied to wow64cpu/cpu.c")
