"""wineserver 钉核 r112b：从 /home/xuser/.container 读 cpuListWoW64 自钉。
env 不穿透到 wineserver（environ 取证），改读容器配置文件（proot 内路径 /home/xuser/.container）。
在 WSL 跑: python3 patch_wineserver_affinity2.py /root/wine0905 && cd /root/wine0905 && make server/wineserver
"""
import sys, os

ROOT = sys.argv[1] if len(sys.argv) > 1 else "/root/wine0905"
M = os.path.join(ROOT, "server", "main.c")

OLD = '''    /* r112: env WINE_WS_AFFINITY=<hex mask> (e.g. f0) pins wineserver to those cores */
    {
        const char *aff = getenv( "WINE_WS_AFFINITY" );
        if (aff && *aff)
        {
            unsigned long mask = strtoul( aff, NULL, 16 );
            if (mask && sched_setaffinity( 0, sizeof(mask), (cpu_set_t *)&mask ) == 0)
                fprintf( stderr, "wineserver: affinity set to %s\\n", aff );
        }
    }'''

NEW = '''    /* r112b: pin wineserver to cpuListWoW64 from the container config.
       (container envVars do not reach wineserver, but its rootfs carries the config file)
       /home/xuser/.container holds "cpuListWoW64":"4,5,6,7" — follow it so the
       server shares the big cores with the game instead of floating on 0-7. */
    {
        FILE *f = fopen( "/home/xuser/.container", "r" );
        if (f)
        {
            char buf[4096];
            size_t n = fread( buf, 1, sizeof(buf) - 1, f );
            fclose( f );
            buf[n] = 0;
            const char *key = strstr( buf, "\\"cpuListWoW64\\"" );
            if (key)
            {
                const char *v = strchr( key + 15, '"' );
                if (v)
                {
                    cpu_set_t set;
                    int cpu, any = 0;
                    CPU_ZERO( &set );
                    v++;
                    while (*v && *v != '"')
                    {
                        if (*v >= '0' && *v <= '9')
                        {
                            cpu = 0;
                            while (*v >= '0' && *v <= '9') { cpu = cpu * 10 + (*v - '0'); v++; }
                            if (cpu < CPU_SETSIZE) { CPU_SET( cpu, &set ); any = 1; }
                        }
                        else v++;
                    }
                    if (any && sched_setaffinity( 0, sizeof(set), &set ) == 0)
                        fprintf( stderr, "wineserver: pinned to cpuListWoW64 cores\\n" );
                }
            }
        }
    }'''

src = open(M, encoding="utf-8", errors="replace").read()
if "r112b" in src:
    print("already r112b")
elif OLD in src:
    src = src.replace(OLD, NEW)
    open(M, "w", encoding="utf-8", newline="\n").write(src)
    print("patched to r112b")
else:
    raise SystemExit("r112 block not found (count=%d)" % src.count(OLD))
