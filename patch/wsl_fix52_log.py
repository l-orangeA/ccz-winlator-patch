NL = chr(10)
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = open(p, encoding='utf-8', errors='replace').read()
if 'HWBP_LOG_INIT' in s:
    print('already')
else:
    anchor = '#include "wine/debug.h"'
    assert anchor in s, 'anchor'
    add = NL.join([
anchor,
'#include <fcntl.h>',
'',
'/* HWBP_LOG_INIT: send this process stderr to a shared probe file */',
'__attribute__((constructor)) static void hwbp_log_init(void)',
'{',
'    int fd = open( "/home/xuser/hwbp.log", O_WRONLY|O_CREAT|O_APPEND, 0644 );',
'    if (fd >= 0) { dup2( fd, 2 ); if (fd != 2) close( fd ); }',
'    {',
'        char n[96]; int f = open( "/proc/self/comm", O_RDONLY ), l;',
'        if (f >= 0)',
'        {',
'            l = read( f, n, 63 ); close( f );',
'            if (l > 0)',
'            {',
'                int k = snprintf( n + l, sizeof(n) - l, " HWBP_INIT pid=%d\\n", getpid() );',
'                if (k > 0) write( 2, n, l + k );',
'            }',
'        }',
'    }',
'}'])
    s = s.replace(anchor, add, 1)
    open(p, 'w', encoding='utf-8', newline=NL).write(s)
    print('log init added')
