NL = chr(10)
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = open(p, encoding='utf-8', errors='replace').read()
old = NL.join([
'                hwbp_handle_rw_fault( rwpage, siginfo->si_addr );',
'                EFL_sig(ucontext) |= 0x100;',
'                leave_handler( ucontext );',
'                return;'])
new = NL.join([
'                hwbp_handle_rw_fault( rwpage, siginfo->si_addr );',
'                {   /* set TF in the 32-bit context so only guest code traps */',
'                    I386_CONTEXT *wow = get_cpu_area( IMAGE_FILE_MACHINE_I386 );',
'                    if (wow) wow->EFlags |= 0x100;',
'                    else EFL_sig(ucontext) |= 0x100;',
'                }',
'                leave_handler( ucontext );',
'                return;'])
if new in s:
    print('already')
else:
    assert old in s, 'rw hook'
    s = s.replace(old, new, 1)
    open(p, 'w', encoding='utf-8', newline=NL).write(s)
    print('patched TF->cpu area')
