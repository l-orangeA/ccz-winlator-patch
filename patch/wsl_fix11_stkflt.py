NL = chr(10)
BS = chr(92)

def sub(path, old, new, tag):
    s = open(path, encoding='utf-8', errors='replace').read()
    if new in s:
        print('already', tag); return
    assert old in s, 'pattern missing: ' + tag
    s = s.replace(old, new, 1)
    open(path, 'w', encoding='utf-8', newline='\n').write(s)
    print('patched', tag)

sig = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'

# 1) segv_handler entry probe
old = NL.join([
'    rec.ExceptionAddress = (void *)RIP_sig(ucontext);',
'    save_context( &context, ucontext );',
'',
'    switch(TRAP_sig(ucontext))'])
new = NL.join([
'    rec.ExceptionAddress = (void *)RIP_sig(ucontext);',
'    save_context( &context, ucontext );',
'',
'    if ((ULONG_PTR)RIP_sig(ucontext) < 0x10000000)',
'    {',
'        char b[192]; int n = snprintf( b, sizeof(b),',
'            "SEGV_ENTRY rip=%lx addr=%lx err=%x rsp=%lx trap=%lx' + BS + 'n",',
'            (unsigned long)RIP_sig(ucontext), (unsigned long)siginfo->si_addr,',
'            (unsigned)(ERROR_sig(ucontext)), (unsigned long)RSP_sig(ucontext),',
'            (unsigned long)TRAP_sig(ucontext) );',
'        write( 2, b, n );',
'    }',
'',
'    switch(TRAP_sig(ucontext))'])
sub(sig, old, new, 'SEGV_ENTRY')

# 2) route #SS (stack fault) through page-fault handling
old = NL.join([
'    case TRAP_x86_STKFLT:  /* Stack fault */',
'        rec.ExceptionCode = EXCEPTION_STACK_OVERFLOW;',
'        break;'])
new = NL.join([
'    case TRAP_x86_STKFLT:  /* Stack fault */',
'        /* box64 reports faults on 32-bit stack accesses as #SS;',
'         * route them through the page fault path so guard pages and',
'         * read-only stack pages get Windows-correct semantics */',
'        rec.NumberParameters = 2;',
'        rec.ExceptionInformation[0] = (ERROR_sig(ucontext) >> 1) & 0x09;',
'        rec.ExceptionInformation[1] = (ULONG_PTR)siginfo->si_addr;',
'        rec.ExceptionCode = virtual_handle_fault( siginfo->si_addr, rec.ExceptionInformation[0],',
'                                                  (void *)RSP_sig(ucontext) );',
'        if ((ULONG_PTR)RIP_sig(ucontext) < 0x10000000)',
'        {',
'            char b[160]; int n = snprintf( b, sizeof(b),',
'                "STKFLT_FAULT addr=%lx ret=%lx' + BS + 'n",',
'                (unsigned long)siginfo->si_addr, (unsigned long)rec.ExceptionCode );',
'            write( 2, b, n );',
'        }',
'        if (!rec.ExceptionCode)',
'        {',
'            leave_handler( ucontext );',
'            return;',
'        }',
'        break;'])
sub(sig, old, new, 'STKFLT_ROUTE')
