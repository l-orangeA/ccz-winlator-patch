NL = chr(10)
BS = chr(92)

# virtual.c: hwbp_handle_rw_fault sets step flag again (unified step approach)
p = '/root/wine0905/dlls/ntdll/unix/virtual.c'
s = open(p, encoding='utf-8', errors='replace').read()
old = NL.join([
'    int idx = hwbp_match_write( addr );',
'    hwbp_pending = idx ? idx : 9;',
'    hwbp_step_flag = 0;'])
new = NL.join([
'    int idx = hwbp_match_write( addr );',
'    hwbp_pending = idx ? idx : 9;',
'    hwbp_step_flag = 1;   /* STEP: host TF first, moved to guest in trap handler */'])
if new in s:
    print('virt already')
else:
    assert old in s
    s = s.replace(old, new, 1)
    open(p, 'w', encoding='utf-8', newline=NL).write(s)
    print('virt patched STEP')

# signal: set host TF, and handle host-side trap by moving TF to guest
p2 = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s2 = open(p2, encoding='utf-8', errors='replace').read()
old = NL.join([
'                int idx = hwbp_handle_rw_fault( rwpage, siginfo->si_addr );  /* DIRECT */',
'                { char hb[160]; int hn = snprintf( hb, sizeof(hb), "HWBP_RWDBG addr=%lx idx=%d' + BS + 'n", (unsigned long)siginfo->si_addr, idx ); write( 2, hb, hn ); }  /* HWBP_RWDBG */',
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
'                return;'])
new = NL.join([
'                hwbp_handle_rw_fault( rwpage, siginfo->si_addr );',
'                EFL_sig(ucontext) |= 0x100;   /* STEP: host TF, moved to guest on first trap */',
'                leave_handler( ucontext );',
'                return;'])
if new in s2:
    print('signal already')
else:
    assert old in s2, 'rw branch direct'
    s2 = s2.replace(old, new, 1)
    # trap handler: handle host-address trap by moving TF to guest
    old2 = NL.join([
'    if (TRAP_sig(ucontext) == TRAP_x86_TRCTRAP)  /* HWBP_IDX_SIG */',
'    {',
'        int pend = hwbp_take_pending();',
'        if (pend == 9)          /* our own step, no match -> swallow */',
'        {',
'            hwbp_rearm();',
'            leave_handler( ucontext );',
'            return;',
'        }',
'        if (pend >= 1 && pend <= 4)',
'        {',
'            amd64_thread_data()->dr6 = 0xffff0ff0u | (1u << pend);',
'            hwbp_rearm();',
'        }',
'    }'])
    new2 = NL.join([
'    if (TRAP_sig(ucontext) == TRAP_x86_TRCTRAP)  /* HWBP_IDX_SIG */',
'    {',
'        int pend = hwbp_take_pending();',
'        if (pend && (ULONG_PTR)RIP_sig(ucontext) >= 0x10000000)',
'        {   /* host-side trap from our TF: move TF to the guest context */',
'            I386_CONTEXT *wow = get_cpu_area( IMAGE_FILE_MACHINE_I386 );',
'            EFL_sig(ucontext) &= ~0x100;',
'            if (wow) wow->EFlags |= 0x100;',
'            if (pend == 9) hwbp_pending_restore(9); else hwbp_pending_restore(pend);',
'            leave_handler( ucontext );',
'            return;',
'        }',
'        if (pend == 9)          /* guest step with no match -> swallow */',
'        {',
'            hwbp_rearm();',
'            leave_handler( ucontext );',
'            return;',
'        }',
'        if (pend >= 1 && pend <= 4)',
'        {',
'            amd64_thread_data()->dr6 = 0xffff0ff0u | (1u << pend);',
'            hwbp_rearm();',
'        }',
'    }'])
    assert old2 in s2, 'trap pend'
    s2 = s2.replace(old2, new2, 1)
    old3 = 'extern int hwbp_take_pending(void);'
    s2 = s2.replace(old3, NL.join([old3, 'extern void hwbp_pending_restore(int v);']), 1)
    open(p2, 'w', encoding='utf-8', newline=NL).write(s2)
    print('signal patched STEP')

# virtual.c: pending restore helper
s = open(p, encoding='utf-8', errors='replace').read()
if 'hwbp_pending_restore' in s:
    print('restore already')
else:
    old = 'int hwbp_take_pending(void) { int v = hwbp_pending; hwbp_pending = 0; return v; }'
    s = s.replace(old, old + NL + 'void hwbp_pending_restore(int v) { hwbp_pending = v; }', 1)
    open(p, 'w', encoding='utf-8', newline=NL).write(s)
    print('pending restore added')
