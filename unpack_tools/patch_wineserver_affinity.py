"""wineserver 亲和补丁（r112）：env WINE_WS_AFFINITY=<hex 掩码> 钉核。
在 WSL 跑: python3 patch_wineserver_affinity.py /root/wine0905
之后: cd /root/wine0905 && make server/wineserver
"""
import sys, os

ROOT = sys.argv[1] if len(sys.argv) > 1 else "/root/wine0905"
M = os.path.join(ROOT, "server", "main.c")

INC_OLD = '''#include <stdio.h>
#include <stdlib.h>'''
INC_NEW = '''#include <stdio.h>
#include <stdlib.h>
#include <sched.h>'''

MAIN_OLD = '''    signal( SIGABRT, sigterm_handler );

    sock_init();'''
MAIN_NEW = '''    signal( SIGABRT, sigterm_handler );

    /* r112: env WINE_WS_AFFINITY=<hex mask> (e.g. f0) pins wineserver to those cores */
    {
        const char *aff = getenv( "WINE_WS_AFFINITY" );
        if (aff && *aff)
        {
            unsigned long mask = strtoul( aff, NULL, 16 );
            if (mask && sched_setaffinity( 0, sizeof(mask), (cpu_set_t *)&mask ) == 0)
                fprintf( stderr, "wineserver: affinity set to %s\\n", aff );
        }
    }

    sock_init();'''

src = open(M, encoding="utf-8", errors="replace").read()
changed = False
for name, old, new in [("include sched.h", INC_OLD, INC_NEW), ("affinity block", MAIN_OLD, MAIN_NEW)]:
    if new in src:
        continue
    if src.count(old) != 1:
        raise SystemExit("anchor %s count=%d" % (name, src.count(old)))
    src = src.replace(old, new)
    changed = True
    print("patched:", name)
if changed:
    open(M, "w", encoding="utf-8", newline="\n").write(src)
    print("main.c written")
else:
    print("already patched")
