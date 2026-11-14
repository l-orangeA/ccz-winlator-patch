import io
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = io.open(p, encoding='utf-8', errors='replace', newline='').read()
old = '''    {
        char b[192]; int n = snprintf( b, sizeof(b),
            "SEGV_ALL sig=%d code=%d rip=%lx addr=%lx err=%x rsp=%lx trap=%lx\\n",'''
new = '''    if ((ULONG_PTR)RIP_sig(ucontext) >= 0x10000000)
    {   /* locate the module of a host-side fault */
        char mb[4096]; int fd = open( "/proc/self/maps", O_RDONLY ), mn;
        if (fd >= 0)
        {
            mn = read( fd, mb, sizeof(mb) - 1 ); close( fd );
            if (mn > 0)
            {
                char *line = mb; ULONG_PTR a = (ULONG_PTR)RIP_sig(ucontext);
                mb[mn] = 0;
                while (line && *line)
                {
                    char *eol = strchr( line, '\\n' );
                    unsigned long lo, hi;
                    if (sscanf( line, "%lx-%lx", &lo, &hi ) == 2 && a >= lo && a < hi)
                    {
                        char b[256]; int n = snprintf( b, sizeof(b), "SEGV_MODULE %s\\n", line );
                        write( 2, b, n ); break;
                    }
                    line = eol ? eol + 1 : NULL;
                }
            }
        }
    }
    {
        char b[192]; int n = snprintf( b, sizeof(b),
            "SEGV_ALL sig=%d code=%d rip=%lx addr=%lx err=%x rsp=%lx trap=%lx\\n",'''
assert old in s, 'segv all'
s = s.replace(old, new, 1)
io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('module probe added')
