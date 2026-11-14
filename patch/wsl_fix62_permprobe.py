import io
p = '/root/wine0905/dlls/ntdll/unix/virtual.c'
s = io.open(p, encoding='utf-8', errors='replace', newline='').read()
if 'HWBP_MPROT' in s:
    print('already'); raise SystemExit

anchor = '''static void hwbp_arm_one( unsigned long a, unsigned int len, unsigned int rw )'''
assert anchor in s, 'arm'
helper = '''static void hwbp_log_perms( char *page )
{
    char buf[2048];
    int fd = open( "/proc/self/maps", O_RDONLY ), n, i;
    if (fd < 0) return;
    n = read( fd, buf, sizeof(buf) - 1 );
    close( fd );
    if (n <= 0) return;
    buf[n] = 0;
    {
        unsigned long pg = (unsigned long)page;
        char *line = buf;
        while (line && *line)
        {
            char *eol = strchr( line, '\\n' );
            unsigned long lo, hi;
            if (sscanf( line, "%lx-%lx", &lo, &hi ) == 2 && pg >= lo && pg < hi)
            {
                char hb[160]; int hn = snprintf( hb, sizeof(hb), "HWBP_PERMS page=%lx maps=%.40s\\n", pg, line );
                write( 2, hb, hn );
                return;
            }
            line = eol ? eol + 1 : NULL;
        }
        { char hb[96]; int hn = snprintf( hb, sizeof(hb), "HWBP_PERMS page=%lx nomap\\n", pg ); write( 2, hb, hn ); }
    }
}

'''
s = s.replace(anchor, helper + anchor, 1)

old = '''            { char hb[160]; int hn = snprintf( hb, sizeof(hb), "HWBP_ARMDBG rw3 page=%lx vprot=%x prot=%x\\n", (unsigned long)page, (unsigned)get_page_vprot(page), (unsigned)get_unix_prot(get_page_vprot(page)) ); write( 2, hb, hn ); }'''
new = old + '''
            { int rc = mprotect( page, page_size, PROT_NONE ); int e = errno; char hb[128]; int hn = snprintf( hb, sizeof(hb), "HWBP_MPROT page=%lx rc=%d errno=%d\\n", (unsigned long)page, rc, e ); write( 2, hb, hn ); }
            hwbp_log_perms( page );'''
assert old in s, 'armdbg'
s = s.replace(old, new, 1)
io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('probe added')
