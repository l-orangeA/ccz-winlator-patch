import re, subprocess

def sub(path, old, new, tag):
    s = open(path, encoding='utf-8', errors='replace').read()
    assert old in s, 'pattern missing: ' + tag
    s = s.replace(old, new, 1)
    open(path, 'w', encoding='utf-8', newline='\n').write(s)
    print('patched', tag)

nl = chr(10)
sig = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
vir = '/root/wine0905/dlls/ntdll/unix/virtual.c'

# 1) segv_handler page fault entry probe (32-bit addr only)
old = nl.join([
'    case TRAP_x86_PAGEFLT:  /* Page fault */',
'        rec.NumberParameters = 2;'])
new = nl.join([
'    case TRAP_x86_PAGEFLT:  /* Page fault */',
'        if ((ULONG_PTR)RIP_sig(ucontext) < 0x10000000 || (ULONG_PTR)siginfo->si_addr < 0x10000000)',
'        {',
'            char b[192]; int n = snprintf( b, sizeof(b),',
'                "PF_ENTRY rip=%lx addr=%lx err=%x rsp=%lx trap=%lx' + chr(92) + 'n",',
'                (unsigned long)RIP_sig(ucontext), (unsigned long)siginfo->si_addr,',
'                (unsigned)(ERROR_sig(ucontext)), (unsigned long)RSP_sig(ucontext),',
'                (unsigned long)TRAP_sig(ucontext) );',
'            write( 2, b, n );',
'        }',
'        rec.NumberParameters = 2;'])
sub(sig, old, new, 'PF_ENTRY')

# 2) setup_raise_exception entry + after virtual_setup_exception
old = nl.join([
'    void *stack_ptr = (void *)(RSP_sig(sigcontext) & ~15);',
'    CONTEXT *context = &xcontext->c;',
'    struct exc_stack_layout *stack;',
'    size_t stack_size;',
'    NTSTATUS status;',
'    XSTATE *src_xs;'])
new = nl.join([
'    void *stack_ptr = (void *)(RSP_sig(sigcontext) & ~15);',
'    CONTEXT *context = &xcontext->c;',
'    struct exc_stack_layout *stack;',
'    size_t stack_size;',
'    NTSTATUS status;',
'    XSTATE *src_xs;',
'    if ((ULONG_PTR)rec->ExceptionAddress < 0x10000000)',
'    {',
'        char b[160]; int n = snprintf( b, sizeof(b),',
'            "SRE_ENTRY code=%lx addr=%lx rsp=%lx' + chr(92) + 'n",',
'            (unsigned long)rec->ExceptionCode, (unsigned long)rec->ExceptionAddress,',
'            (unsigned long)RSP_sig(sigcontext) );',
'        write( 2, b, n );',
'    }'])
sub(sig, old, new, 'SRE_ENTRY')

old = nl.join([
'    stack = virtual_setup_exception( stack_ptr, stack_size, rec );',
'    stack->rec               = *rec;'])
new = nl.join([
'    {',
'        void *vp = virtual_setup_exception( stack_ptr, stack_size, rec );',
'        if ((ULONG_PTR)rec->ExceptionAddress < 0x10000000)',
'        {',
'            char b[160]; int n = snprintf( b, sizeof(b),',
'                "SRE_AFTER code=%lx stack=%lx stackptr=%lx' + chr(92) + 'n",',
'                (unsigned long)rec->ExceptionCode, (unsigned long)vp,',
'                (unsigned long)stack_ptr );',
'            write( 2, b, n );',
'        }',
'        stack = vp;',
'    }',
'    stack->rec               = *rec;'])
sub(sig, old, new, 'SRE_AFTER')

# 3) virtual_handle_fault entry (after vprot) and exit
old = nl.join([
'    mutex_lock( &virtual_mutex );  /* no need for signal masking inside signal handler */',
'    vprot = get_page_vprot( page );',
'',
'#ifdef __APPLE__'])
new = nl.join([
'    mutex_lock( &virtual_mutex );  /* no need for signal masking inside signal handler */',
'    vprot = get_page_vprot( page );',
'    if ((ULONG_PTR)addr < 0x10000000)',
'    {',
'        char b[192]; int n = snprintf( b, sizeof(b),',
'            "VHF_ENTRY addr=%lx err=%x stack=%lx page=%lx vprot=%x' + chr(92) + 'n",',
'            (unsigned long)addr, err, (unsigned long)stack, (unsigned long)page, vprot );',
'        write( 2, b, n );',
'    }',
'',
'#ifdef __APPLE__'])
sub(vir, old, new, 'VHF_ENTRY')

old = nl.join([
'    mutex_unlock( &virtual_mutex );',
'    return ret;',
'}'])
new = nl.join([
'    if ((ULONG_PTR)addr < 0x10000000)',
'    {',
'        char b[128]; int n = snprintf( b, sizeof(b),',
'            "VHF_EXIT addr=%lx ret=%lx' + chr(92) + 'n",',
'            (unsigned long)addr, (unsigned long)ret );',
'        write( 2, b, n );',
'    }',
'    mutex_unlock( &virtual_mutex );',
'    return ret;',
'}'])
# this pattern may occur multiple times; target the virtual_handle_fault one (last occurrence before virtual_setup_exception)
s = open(vir, encoding='utf-8', errors='replace').read()
idx = s.index('virtual_handle_fault( void *addr, DWORD err, void *stack )')
end = s.index('virtual_setup_exception', idx)
seg = s[idx:end]
assert old in seg, 'vhf exit missing'
seg2 = seg.replace(old, new, 1)
s = s[:idx] + seg2 + s[end:]
open(vir, 'w', encoding='utf-8', newline='\n').write(s)
print('patched VHF_EXIT')
