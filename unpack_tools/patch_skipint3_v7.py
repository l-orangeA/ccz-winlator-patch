#!/usr/bin/env python3
"""skip-int3 v7: resume at syscall_32to64_return instead of its prolog.

v5: fixup leaves Rip = syscall_32to64 (prolog) -> NtContinue re-enters the
    syscall prolog, %eax (0x586f8c) is taken as syscall number ->
    Wow64SystemServiceEx unsupported syscall -> c0000005 at C000001C.
v6: no fixup -> NtContinue to a raw 32-bit address -> nested exception.
v7: keep the fixup (64-bit CS/Rsp/machine frame) but point Rip at the
    syscall_32to64_return label, which restores the 32-bit registers from
    the CPU context (%r13) and ljmp/iretq back to 32-bit code at Eip+1.
"""
import sys

CPU = "/root/wine1010/dlls/wow64cpu/cpu.c"
c = open(CPU, encoding="utf-8", errors="replace").read()

# 1. export the return label as a global symbol
asm_old = '                   "syscall_32to64_return:\\n\\t"\n'
if c.count(asm_old) != 1:
    sys.exit("FAIL: asm return label count=%d" % c.count(asm_old))
asm_new = ('                   ".globl " __ASM_NAME("wow64_resume_32") "\\n\\t"\n'
           '                   "wow64_resume_32:\\n\\t"\n'
           '                   "syscall_32to64_return:\\n\\t"\n')
c = c.replace(asm_old, asm_new, 1)

# 2. declare it next to syscall_32to64
decl = "extern void WINAPI syscall_32to64(void);"
if c.count(decl) != 1:
    sys.exit("FAIL: syscall_32to64 decl count=%d" % c.count(decl))
c = c.replace(decl, decl + "\nextern void wow64_resume_32(void);", 1)

# 3. v6 early-return block -> set a flag, keep the normal fixup
old = """    /* SKIP-INT3 v6: step over the one-byte int3 probe in 32-bit guest code,
     * and keep the 32-bit context so NtContinue() can restore it directly. */
    if (ptrs->ExceptionRecord->ExceptionCode == EXCEPTION_BREAKPOINT &&
        (ULONG_PTR)ptrs->ExceptionRecord->ExceptionAddress < 0x10000000)
    {
        ERR( "SKIP-INT3 cpu v6: Eip %lx -> %lx (no fixup)\\n",
             wow_context.Eip, wow_context.Eip + 1 );
        wow_context.Eip += 1;
        context->Rip += 1;
        BTCpuSetContext( GetCurrentThread(), GetCurrentProcess(), NULL, &wow_context );
        return STATUS_SUCCESS;
    }
"""
if c.count(old) != 1:
    sys.exit("FAIL: v6 block count=%d" % c.count(old))
new = """    /* SKIP-INT3 v7: step over the one-byte int3 probe; the fixup below
     * will resume at wow64_resume_32 (syscall_32to64_return) instead. */
    int skip_int3 = 0;
    if (ptrs->ExceptionRecord->ExceptionCode == EXCEPTION_BREAKPOINT &&
        (ULONG_PTR)ptrs->ExceptionRecord->ExceptionAddress < 0x10000000)
    {
        ERR( "SKIP-INT3 cpu v7: Eip %lx -> %lx (resume at return label)\\n",
             wow_context.Eip, wow_context.Eip + 1 );
        wow_context.Eip += 1;
        skip_int3 = 1;
    }
"""
c = c.replace(old, new, 1)

fix = "    context->Rip = (ULONG64)syscall_32to64;"
if c.count(fix) != 1:
    sys.exit("FAIL: fixup Rip count=%d" % c.count(fix))
c = c.replace(fix, "    context->Rip = (ULONG64)(skip_int3 ? wow64_resume_32 : syscall_32to64);", 1)

open(CPU, "w", encoding="utf-8", newline="\n").write(c)
print("OK v7 applied to wow64cpu/cpu.c")
