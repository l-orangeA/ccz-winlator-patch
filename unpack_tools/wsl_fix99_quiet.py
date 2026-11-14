def patch(path, edits, helper_anchor, helper_text):
    s = open(path, encoding='utf-8').read()
    i = s.index(helper_anchor)
    s = s[:i] + helper_text + s[i:]
    for old, new in edits:
        assert old in s, 'MISSING: ' + old[:80]
        s = s.replace(old, new, 1)
    open(path, 'w', encoding='utf-8').write(s)
    print('patched', path)

HELPER = '''static int hwbp_verbose(void)
{
    static int v = -1;
    if (v < 0) v = getenv( "WINE_HWBP_LOG" ) != NULL;
    return v;
}

'''

VC = '/root/wine0905/dlls/ntdll/unix/virtual.c'
v_edits = [
 ('            hwbp_log_perms( page );',
  '            if (hwbp_verbose()) hwbp_log_perms( page );'),
 ('            { char hb[160]; int hn = snprintf( hb, sizeof(hb), "HWBP_ARMDBG rw3 page=%lx vprot=%x prot=%x\\n", (unsigned long)page, (unsigned)get_page_vprot(page), (unsigned)get_unix_prot(get_page_vprot(page)) ); write( 2, hb, hn ); }',
  '            if (hwbp_verbose()) { char hb[160]; int hn = snprintf( hb, sizeof(hb), "HWBP_ARMDBG rw3 page=%lx vprot=%x prot=%x\\n", (unsigned long)page, (unsigned)get_page_vprot(page), (unsigned)get_unix_prot(get_page_vprot(page)) ); write( 2, hb, hn ); }'),
 ('            { int rc = mprotect( page, page_size, PROT_NONE ); int e = errno; char hb[128]; int hn = snprintf( hb, sizeof(hb), "HWBP_MPROT page=%lx rc=%d errno=%d\\n", (unsigned long)page, rc, e ); write( 2, hb, hn ); }',
  '            { int rc = mprotect( page, page_size, PROT_NONE ); if (hwbp_verbose()) { int e = errno; char hb[128]; int hn = snprintf( hb, sizeof(hb), "HWBP_MPROT page=%lx rc=%d errno=%d\\n", (unsigned long)page, rc, e ); write( 2, hb, hn ); } }'),
 ('    { char hb[192]; int hn = snprintf( hb, sizeof(hb), "HWBP_DBG update dr0=%lx dr1=%lx dr2=%lx dr3=%lx dr7=%lx\\n", d0, d1, d2, d3, d7 ); write( 2, hb, hn ); }',
  '    if (hwbp_verbose()) { char hb[192]; int hn = snprintf( hb, sizeof(hb), "HWBP_DBG update dr0=%lx dr1=%lx dr2=%lx dr3=%lx dr7=%lx\\n", d0, d1, d2, d3, d7 ); write( 2, hb, hn ); }'),
 ('            { char hb[160]; int hn = snprintf( hb, sizeof(hb), "HWBP_DBG writewatch page=%lx addr=%lx match=%d\\n", (unsigned long)page, (unsigned long)addr, hwbp_match_write( addr ) ); write( 2, hb, hn ); }',
  '            if (hwbp_verbose()) { char hb[160]; int hn = snprintf( hb, sizeof(hb), "HWBP_DBG writewatch page=%lx addr=%lx match=%d\\n", (unsigned long)page, (unsigned long)addr, hwbp_match_write( addr ) ); write( 2, hb, hn ); }'),
]
patch(VC, v_edits, 'static void hwbp_log_perms( char *page )', HELPER)

SC = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'

# gate our own per-event step logs
s = open(SC, encoding='utf-8').read()
needle = '{ static int sc; if (sc++ < 4000) {'
n = 0
out = []
pos = 0
while True:
    p = s.find(needle, pos)
    if p < 0:
        out.append(s[pos:]); break
    out.append(s[pos:p])
    out.append('if (hwbp_verbose()) ' + needle)
    pos = p + len(needle)
    n += 1
s = ''.join(out)
print('gated step logs:', n)
open(SC, 'w', encoding='utf-8').write(s)

s_edits = [
 ('    if ((ULONG_PTR)rec->ExceptionAddress < 0x10000000)\n    {\n        char b[224]; int n = snprintf( b, sizeof(b),\n            "SRE_ENTRY',
  '    if (hwbp_verbose() && (ULONG_PTR)rec->ExceptionAddress < 0x10000000)\n    {\n        char b[224]; int n = snprintf( b, sizeof(b),\n            "SRE_ENTRY'),
 ('    {\n        char b[192]; int n = snprintf( b, sizeof(b),\n            "SEGV_ALL',
  '    if (hwbp_verbose()) {\n        char b[192]; int n = snprintf( b, sizeof(b),\n            "SEGV_ALL'),
 ('    { char b[112]; int n = snprintf(b,sizeof(b),"TRAP sig=%d',
  '    if (hwbp_verbose()) { char b[112]; int n = snprintf(b,sizeof(b),"TRAP sig=%d'),
]
patch(SC, s_edits, 'static int hwbp_push_delta( const unsigned char *ip )', HELPER)
