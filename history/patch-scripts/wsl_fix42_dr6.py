NL = chr(10)
BS = chr(92)

# ---- virtual.c: match returns bp index; pending encodes index or 9 ----
p = '/root/wine0905/dlls/ntdll/unix/virtual.c'
s = open(p, encoding='utf-8', errors='replace').read()
if 'HWBP_IDX' in s:
    print('virt already')
else:
    old = 'static int hwbp_match_write( const void *addr );'
    new = 'static int hwbp_match_write( const void *addr );  /* HWBP_IDX: returns bp number 1..4 or 0 */'
    assert old in s
    s = s.replace(old, new, 1)

    # rewrite match body to return index
    start = s.index('static int hwbp_match_write( const void *addr )' + NL + '{')
    end = s.index('\n}\n', start) + 3
    fn = NL.join([
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
'        if (hwbp_dr[i] && a >= hwbp_dr[i] && a < hwbp_dr[i] + size) return i + 1;',
'    }',
'    return 0;',
'}',
''])
    s = s[:start] + fn + s[end:]

    # pending assignment: index or 9
    s = s.replace('hwbp_pending = hwbp_match_write( addr ) ? 1 : 2;',
                  '{ int idx = hwbp_match_write( addr ); hwbp_pending = idx ? idx : 9; }')
    s = s.replace('hwbp_pending = hwbp_match_write( addr ) ? 1 : 2;', '{ int idx = hwbp_match_write( addr ); hwbp_pending = idx ? idx : 9; }')
    # the writewatch branch used ternary assignment form
    s = s.replace('hwbp_pending = hwbp_match_write( addr ) ? 1 : 2;', '{ int idx = hwbp_match_write( addr ); hwbp_pending = idx ? idx : 9; }')
    open(p, 'w', encoding='utf-8', newline=NL).write(s)
    print('virt patched HWBP_IDX')

# ---- signal_x86_64.c: pending semantics + dr6 ----
p2 = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s2 = open(p2, encoding='utf-8', errors='replace').read()
if 'HWBP_IDX_SIG' in s2:
    print('signal already')
else:
    old = NL.join([
'    if (TRAP_sig(ucontext) == TRAP_x86_TRCTRAP)',
'    {',
'        int pend = hwbp_take_pending();',
'        if (pend == 2)          /* HWBP_V2: our own step, no match -> swallow */',
'        {',
'            hwbp_rearm();',
'            leave_handler( ucontext );',
'            return;',
'        }',
'        if (pend == 1) hwbp_rearm();   /* deliver the emulated breakpoint */',
'    }'])
    new = NL.join([
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
    assert old in s2, 'trap pend block'
    s2 = s2.replace(old, new, 1)
    open(p2, 'w', encoding='utf-8', newline=NL).write(s2)
    print('signal patched HWBP_IDX')
