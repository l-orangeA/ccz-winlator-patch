import io
NL = chr(10)

# ---------- virtual.c ----------
p = '/root/wine0905/dlls/ntdll/unix/virtual.c'
s = io.open(p, encoding='utf-8', errors='replace', newline='').read()

# add suppress variable next to hold flag
old = 'static int hwbp_hold_flag;'
assert old in s, 'hold var'
if 'hwbp_suppress' not in s:
    s = s.replace(old, old + NL + 'static int hwbp_suppress;   /* HWBP_SUPPRESS: no arming during synthetic #DB dispatch */', 1)
    s = s.replace('void hwbp_hold_begin( void )', 'void hwbp_suppress_arm( int v ) { hwbp_suppress = v; }' + NL + NL + 'void hwbp_hold_begin( void )', 1)

# in hwbp_update: clear suppress when the handler cleared Dr
old2 = '''    hwbp_dr[0] = d0; hwbp_dr[1] = d1; hwbp_dr[2] = d2; hwbp_dr[3] = d3; hwbp_dr7 = d7;
    hwbp_perf_update( d0, d1, d2, d3, d7 );   /* HWBP_PERF */'''
new2 = '''    hwbp_dr[0] = d0; hwbp_dr[1] = d1; hwbp_dr[2] = d2; hwbp_dr[3] = d3; hwbp_dr7 = d7;
    if (!(d7 & 0xff)) hwbp_suppress = 0;   /* HWBP_SUPPRESS: packer's handler cleared Dr */
    hwbp_perf_update( d0, d1, d2, d3, d7 );   /* HWBP_PERF */'''
assert old2 in s, 'update body'
s = s.replace(old2, new2, 1)

# arm honours suppress
old3 = '''            if (hwbp_hold_flag)
                set_page_vprot_bits( page, page_size, VPROT_READ | VPROT_WRITE, VPROT_WRITEWATCH );'''
new3 = '''            if (hwbp_suppress || hwbp_hold_flag)
                set_page_vprot_bits( page, page_size, VPROT_READ | VPROT_WRITE, VPROT_WRITEWATCH );'''
assert old3 in s, 'arm hold'
s = s.replace(old3, new3, 1)
io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('virtual.c: suppress added')

# ---------- signal_x86_64.c ----------
p2 = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s2 = io.open(p2, encoding='utf-8', errors='replace', newline='').read()

# remove hold_begin from setup_raise_exception
old4 = NL + '    hwbp_hold_begin();   /* HWBP_TRAMP_SIG: let the guest handler edit its context */'
if old4 in s2:
    s2 = s2.replace(old4, '', 1)
    print('removed hold from setup_raise_exception')

# remove consume call
old5 = '''                        hwbp_consume();   /* HWBP_CONSUME: watch fired, page can stay open */'''
if old5 in s2:
    s2 = s2.replace(old5, '', 1)
    print('removed consume')

# extern
old6 = 'extern void hwbp_reprotect_all( void );'
assert old6 in s2
if 'hwbp_suppress_arm' not in s2:
    s2 = s2.replace(old6, 'extern void hwbp_suppress_arm( int v );   /* HWBP_SUPPRESS */' + NL + old6, 1)

# suppress during synthetic #DB delivery
old7 = '''                        amd64_thread_data()->dr6 = 0xffff0ff0u | (1u << idx);
                        {   /* deliver with our own ExceptionAddress (after the insn) */
                            struct xcontext xctx;'''
new7 = '''                        amd64_thread_data()->dr6 = 0xffff0ff0u | (1u << idx);
                        hwbp_suppress_arm( 1 );   /* HWBP_SUPPRESS: dispatch runs on the watched page */
                        {   /* deliver with our own ExceptionAddress (after the insn) */
                            struct xcontext xctx;'''
assert old7 in s2, 'deliver'
s2 = s2.replace(old7, new7, 1)
io.open(p2, 'w', encoding='utf-8', newline='').write(s2)
print('signal_x86_64.c: suppress wired')
