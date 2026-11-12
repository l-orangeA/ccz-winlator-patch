NL = chr(10)
BS = chr(92)

# ---------- virtual.c ----------
p = '/root/wine0905/dlls/ntdll/unix/virtual.c'
s = open(p, encoding='utf-8', errors='replace').read()
if 'HWBP_SOFT' in s:
    print('virtual already')
else:
    anchor = NL.join([
'/***********************************************************************',
' *           virtual_handle_fault',
' */'])
    insert = NL.join([
'/* HWBP_SOFT: software emulation of x86 hardware breakpoints (ARM hosts) */',
'static unsigned long hwbp_dr[4];',
'static unsigned long hwbp_dr7;',
'static int hwbp_step_flag;',
'',
'static unsigned int hwbp_len_bytes( unsigned int len )',
'{',
'    switch (len) { case 0: return 1; case 1: return 2; case 2: return 4; default: return 4; }',
'}',
'',
'static void hwbp_arm_one( unsigned long a, unsigned int len )',
'{',
'    char *page, *end;',
'    unsigned int size = hwbp_len_bytes( len );',
'    if (!a) return;',
'    page = (char *)(a & ~(unsigned long)page_mask);',
'    end = (char *)((a + size - 1) & ~(unsigned long)page_mask);',
'    while (page <= end)',
'    {',
'        set_page_vprot_bits( page, page_size, VPROT_WRITEWATCH, 0 );',
'        mprotect_range( page, page_size, 0, 0 );',
'        page += page_size;',
'    }',
'}',
'',
'void hwbp_update( unsigned long d0, unsigned long d1, unsigned long d2, unsigned long d3, unsigned long d7 )',
'{',
'    int i;',
'    hwbp_dr[0] = d0; hwbp_dr[1] = d1; hwbp_dr[2] = d2; hwbp_dr[3] = d3; hwbp_dr7 = d7;',
'    if (!(d7 & 0xff)) return;  /* no enabled breakpoints */',
'    for (i = 0; i < 4; i++)',
'    {',
'        unsigned int rw, len;',
'        if (!(d7 & (1u << (2 * i))) && !(d7 & (2u << (2 * i)))) continue;',
'        if (!hwbp_dr[i]) continue;',
'        rw = (unsigned int)((d7 >> (16 + 4 * i)) & 3);',
'        len = (unsigned int)((d7 >> (18 + 4 * i)) & 3);',
'        if (rw == 0) continue;  /* execute breakpoints not emulated here */',
'        hwbp_arm_one( hwbp_dr[i], len );',
'    }',
'}',
'',
'int hwbp_step_pending(void) { int v = hwbp_step_flag; hwbp_step_flag = 0; return v; }',
'',
'/* returns 1 if a write to addr hits an enabled data breakpoint */',
'static int hwbp_match_write( const void *addr )',
'{',
'    int i;',
'    unsigned long a = (unsigned long)addr;',
'    if (!(hwbp_dr7 & 0xff)) return 0;',
'    for (i = 0; i < 4; i++)',
'    {',
'        unsigned int rw, len, size;',
'        if (!(hwbp_dr7 & (1u << (2 * i))) && !(hwbp_dr7 & (2u << (2 * i)))) continue;',
'        rw = (unsigned int)((hwbp_dr7 >> (16 + 4 * i)) & 3);',
'        len = (unsigned int)((hwbp_dr7 >> (18 + 4 * i)) & 3);',
'        if (rw == 0) continue;',
'        size = hwbp_len_bytes( len );',
'        if (hwbp_dr[i] && a >= hwbp_dr[i] && a < hwbp_dr[i] + size) return 1;',
'    }',
'    return 0;',
'}',
'',
'void hwbp_rearm(void)',
'{',
'    int i;',
'    if (!(hwbp_dr7 & 0xff)) return;',
'    for (i = 0; i < 4; i++)',
'    {',
'        unsigned int rw, len;',
'        if (!(hwbp_dr7 & (1u << (2 * i))) && !(hwbp_dr7 & (2u << (2 * i)))) continue;',
'        rw = (unsigned int)((hwbp_dr7 >> (16 + 4 * i)) & 3);',
'        len = (unsigned int)((hwbp_dr7 >> (18 + 4 * i)) & 3);',
'        if (rw == 0) continue;',
'        hwbp_arm_one( hwbp_dr[i], len );',
'    }',
'}',
'',
'/***********************************************************************',
' *           virtual_handle_fault',
' */'])
    assert anchor in s, 'virtual anchor'
    s = s.replace(anchor, insert, 1)

    # hook into the write-fault / write-watch branch
    old2 = NL.join([
'    else if (err & EXCEPTION_WRITE_FAULT)',
'    {',
'        if (vprot & VPROT_WRITEWATCH)',
'        {',
'            set_page_vprot_bits( page, page_size, 0, VPROT_WRITEWATCH );',
'            mprotect_range( page, page_size, 0, 0 );',
'        }'])
    new2 = NL.join([
'    else if (err & EXCEPTION_WRITE_FAULT)',
'    {',
'        if (vprot & VPROT_WRITEWATCH)',
'        {',
'            set_page_vprot_bits( page, page_size, 0, VPROT_WRITEWATCH );',
'            mprotect_range( page, page_size, 0, 0 );',
'            if (hwbp_match_write( addr ))',
'            {',
'                hwbp_step_flag = 1;   /* HWBP_SOFT: trap after this store */',
'            }',
'        }'])
    assert old2 in s, 'writewatch branch'
    s = s.replace(old2, new2, 1)
    open(p, 'w', encoding='utf-8', newline=NL).write(s)
    print('virtual patched')

# ---------- signal_x86_64.c ----------
p2 = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s2 = open(p2, encoding='utf-8', errors='replace').read()
if 'HWBP_SOFT_SIG' in s2:
    print('signal already')
else:
    # externs
    old = 'NTSTATUS WINAPI NtSetContextThread('
    if old not in s2:
        old = 'void *get_native_context('
    assert old in s2, 'extern anchor'
    s2 = s2.replace(old, NL.join([
'/* HWBP_SOFT_SIG */',
'extern void hwbp_update( unsigned long d0, unsigned long d1, unsigned long d2, unsigned long d3, unsigned long d7 );',
'extern int hwbp_step_pending(void);',
'extern void hwbp_rearm(void);',
'',
old]), 1)

    # arm on 32-bit context set: after Dr stored in set_thread_wow64_context
    old = NL.join([
'        if (flags & CONTEXT_I386_DEBUG_REGISTERS)',
'        {',
'            amd64_thread_data()->dr0 = context->Dr0;',
'            amd64_thread_data()->dr1 = context->Dr1;',
'            amd64_thread_data()->dr2 = context->Dr2;',
'            amd64_thread_data()->dr3 = context->Dr3;',
'            amd64_thread_data()->dr6 = context->Dr6;',
'            amd64_thread_data()->dr7 = context->Dr7;',
'        }'])
    new = NL.join([
'        if (flags & CONTEXT_I386_DEBUG_REGISTERS)',
'        {',
'            amd64_thread_data()->dr0 = context->Dr0;',
'            amd64_thread_data()->dr1 = context->Dr1;',
'            amd64_thread_data()->dr2 = context->Dr2;',
'            amd64_thread_data()->dr3 = context->Dr3;',
'            amd64_thread_data()->dr6 = context->Dr6;',
'            amd64_thread_data()->dr7 = context->Dr7;',
'            hwbp_update( (unsigned long)context->Dr0, (unsigned long)context->Dr1,',
'                         (unsigned long)context->Dr2, (unsigned long)context->Dr3,',
'                         (unsigned long)context->Dr7 );',
'        }'])
    assert old in s2, 'wow64 dr store'
    s2 = s2.replace(old, new, 1)

    # after handled page fault, set TF if requested
    old = NL.join([
'        rec.ExceptionCode = virtual_handle_fault( siginfo->si_addr, rec.ExceptionInformation[0],',
'                                                  (void *)RSP_sig(ucontext) );',
'        if (!rec.ExceptionCode)',
'        {',
'            leave_handler( ucontext );',
'            return;',
'        }'])
    new = NL.join([
'        rec.ExceptionCode = virtual_handle_fault( siginfo->si_addr, rec.ExceptionInformation[0],',
'                                                  (void *)RSP_sig(ucontext) );',
'        if (!rec.ExceptionCode)',
'        {',
'            if (hwbp_step_pending()) EFL_sig(ucontext) |= 0x100;',
'            leave_handler( ucontext );',
'            return;',
'        }'])
    assert old in s2, 'pagefault handled'
    s2 = s2.replace(old, new, 1)

    # re-arm on SIGTRAP delivery (trap_handler)
    old = NL.join([
'    rec.ExceptionAddress = (void *)RIP_sig(ucontext);',
'    save_context( &context, ucontext );',
'',
'    switch (TRAP_sig(ucontext))',
'    {',
'    case TRAP_x86_TRCTRAP:'])
    new = NL.join([
'    rec.ExceptionAddress = (void *)RIP_sig(ucontext);',
'    save_context( &context, ucontext );',
'    hwbp_rearm();',
'',
'    switch (TRAP_sig(ucontext))',
'    {',
'    case TRAP_x86_TRCTRAP:'])
    assert old in s2, 'trap handler'
    s2 = s2.replace(old, new, 1)

    open(p2, 'w', encoding='utf-8', newline=NL).write(s2)
    print('signal patched')
