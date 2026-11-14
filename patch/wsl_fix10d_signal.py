import subprocess
NL = chr(10)
BS = chr(92)

subprocess.run(['tar','-xzf','/mnt/d/unpack_tools/wine-20240905.tar.gz','-C','/tmp',
                'wine-9.2-custom-a48ee6df4a77/dlls/ntdll/unix/signal_x86_64.c'], check=True)
src = '/tmp/wine-9.2-custom-a48ee6df4a77/dlls/ntdll/unix/signal_x86_64.c'
dst = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = open(src, encoding='utf-8', errors='replace').read()

# --- fix4: page-protection fallback after frame pointer is computed ---
old = '    stack = virtual_setup_exception( stack_ptr, stack_size, rec );' + NL
new = NL.join([
'    stack = virtual_setup_exception( stack_ptr, stack_size, rec );',
'    if ((ULONG_PTR)rec->ExceptionAddress < 0x10000000)',
'    {',
'        char b[160]; int n = snprintf( b, sizeof(b),',
'            "SRE_AFTER code=%lx stack=%lx stackptr=%lx' + BS + 'n",',
'            (unsigned long)rec->ExceptionCode, (unsigned long)stack,',
'            (unsigned long)stack_ptr );',
'        write( 2, b, n );',
'    }',
'    {',
'        ULONG_PTR page_size = getpagesize();',
'        ULONG_PTR page = (ULONG_PTR)stack & ~(page_size - 1);',
'        ULONG_PTR end  = ((ULONG_PTR)stack + sizeof(*stack) + page_size - 1) & ~(page_size - 1);',
'        size_t sz = end - page;',
'        if (mprotect( (void *)page, sz, PROT_READ | PROT_WRITE ) != 0)',
'        {',
'            static char tmp[16384];',
'            if (sz <= sizeof(tmp)) {',
'                static const char m2[] = "MPROT_FALLBACK' + BS + 'n";',
'                write( 2, m2, sizeof(m2)-1 );',
'                memcpy( tmp, (void *)page, sz );',
'                void *p = mmap( (void *)page, sz, PROT_READ | PROT_WRITE,',
'                                MAP_PRIVATE | MAP_ANONYMOUS | MAP_FIXED, -1, 0 );',
'                if (p != MAP_FAILED) memcpy( p, tmp, sz );',
'            }',
'        }',
'    }',
''])
assert old in s, 'call site'
s = s.replace(old, new, 1)

# --- SRE_ENTRY probe ---
old = NL.join([
'    void *stack_ptr = (void *)(RSP_sig(sigcontext) & ~15);',
'    CONTEXT *context = &xcontext->c;',
'    struct exc_stack_layout *stack;',
'    size_t stack_size;',
'    NTSTATUS status;',
'    XSTATE *src_xs;'])
new = old + NL.join([
'',
'    if ((ULONG_PTR)rec->ExceptionAddress < 0x10000000)',
'    {',
'        char b[160]; int n = snprintf( b, sizeof(b),',
'            "SRE_ENTRY code=%lx addr=%lx rsp=%lx' + BS + 'n",',
'            (unsigned long)rec->ExceptionCode, (unsigned long)rec->ExceptionAddress,',
'            (unsigned long)RSP_sig(sigcontext) );',
'        write( 2, b, n );',
'    }'])
assert old in s, 'prologue'
s = s.replace(old, new, 1)

# --- PF_ENTRY probe ---
old = NL.join([
'    case TRAP_x86_PAGEFLT:  /* Page fault */',
'        rec.NumberParameters = 2;'])
new = NL.join([
'    case TRAP_x86_PAGEFLT:  /* Page fault */',
'        if ((ULONG_PTR)RIP_sig(ucontext) < 0x10000000 || (ULONG_PTR)siginfo->si_addr < 0x10000000)',
'        {',
'            char b[192]; int n = snprintf( b, sizeof(b),',
'                "PF_ENTRY rip=%lx addr=%lx err=%x rsp=%lx trap=%lx' + BS + 'n",',
'                (unsigned long)RIP_sig(ucontext), (unsigned long)siginfo->si_addr,',
'                (unsigned)(ERROR_sig(ucontext)), (unsigned long)RSP_sig(ucontext),',
'                (unsigned long)TRAP_sig(ucontext) );',
'            write( 2, b, n );',
'        }',
'        rec.NumberParameters = 2;'])
assert old in s, 'pagefault case'
s = s.replace(old, new, 1)

open(dst, 'w', encoding='utf-8', newline=NL).write(s)
print('signal_x86_64.c rewritten with fix4 + probes')
