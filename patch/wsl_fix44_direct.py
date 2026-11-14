NL = chr(10)
BS = chr(92)

# virtual.c: return the matched index from hwbp_handle_rw_fault
p = '/root/wine0905/dlls/ntdll/unix/virtual.c'
s = open(p, encoding='utf-8', errors='replace').read()
old = NL.join([
'void hwbp_handle_rw_fault( char *page, const void *addr )',
'{',
'    { int idx = hwbp_match_write( addr ); hwbp_pending = idx ? idx : 9; }',
'    hwbp_step_flag = 1;',
'    set_page_vprot_bits( page, page_size, VPROT_READ | VPROT_WRITE, 0 );',
'    mprotect_range( page, page_size, 0, 0 );',
'}'])
new = NL.join([
'int hwbp_handle_rw_fault( char *page, const void *addr )   /* DIRECT: return matched index */',
'{',
'    int idx = hwbp_match_write( addr );',
'    hwbp_pending = idx ? idx : 9;',
'    hwbp_step_flag = 0;',
'    set_page_vprot_bits( page, page_size, VPROT_READ | VPROT_WRITE, 0 );',
'    mprotect_range( page, page_size, 0, 0 );',
'    return idx;',
'}'])
if new in s:
    print('virt already')
else:
    assert old in s, 'rw fn body'
    s = s.replace(old, new, 1)
    open(p, 'w', encoding='utf-8', newline=NL).write(s)
    print('virt patched DIRECT')

# signal: deliver SINGLE_STEP synchronously
p2 = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s2 = open(p2, encoding='utf-8', errors='replace').read()
old2 = NL.join([
'            char *rwpage = hwbp_rw_page_for( siginfo->si_addr );',
'            if (rwpage)',
'            {',
'                hwbp_handle_rw_fault( rwpage, siginfo->si_addr );',
'                {   /* set TF in the 32-bit context so only guest code traps */',
'                    I386_CONTEXT *wow = get_cpu_area( IMAGE_FILE_MACHINE_I386 );',
'                    if (wow) wow->EFlags |= 0x100;',
'                    else EFL_sig(ucontext) |= 0x100;',
'                }',
'                leave_handler( ucontext );',
'                return;',
'            }'])
new2 = NL.join([
'            char *rwpage = hwbp_rw_page_for( siginfo->si_addr );',
'            if (rwpage)',
'            {',
'                int idx = hwbp_handle_rw_fault( rwpage, siginfo->si_addr );  /* DIRECT */',
'                if (idx >= 1 && idx <= 4)',
'                {',
'                    EXCEPTION_RECORD srec = { 0 };',
'                    srec.ExceptionCode = EXCEPTION_SINGLE_STEP;',
'                    srec.ExceptionAddress = (void *)RIP_sig(ucontext);',
'                    amd64_thread_data()->dr6 = 0xffff0ff0u | (1u << idx);',
'                    setup_exception( ucontext, &srec );',
'                    return;',
'                }',
'                leave_handler( ucontext );',
'                return;',
'            }'])
if new2 in s2:
    print('signal already')
else:
    assert old2 in s2, 'rw branch'
    s2 = s2.replace(old2, new2, 1)
    open(p2, 'w', encoding='utf-8', newline=NL).write(s2)
    print('signal patched DIRECT')
