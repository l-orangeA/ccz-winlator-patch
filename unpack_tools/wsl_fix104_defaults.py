T = "/root/wine0905/dlls/ntdll/unix/thread.c"
V = "/root/wine0905/dlls/ntdll/unix/virtual.c"

t = open(T, encoding="utf-8").read()
old_t = '{ unsigned long sh = 0xe14; const char *e = getenv("WINE_SS"); if (e) sh = strtoul( e, NULL, 0 ); wow_teb->Tib.StackBase = PtrToUlong( stack.StackBase ) - sh; }  /* STACKSHIFT */'
new_t = '{ unsigned long sh = 0xe00; const char *e = getenv("WINE_SS"); if (e) sh = strtoul( e, NULL, 0 ); wow_teb->Tib.StackBase = PtrToUlong( stack.StackBase ) - sh; }  /* STACKSHIFT */'
assert old_t in t
t = t.replace(old_t, new_t, 1)
open(T, "w", encoding="utf-8").write(t)
print("thread.c default 0xe00")

v = open(V, encoding="utf-8").read()
old_v = '    if (!getenv( "WINE_HWBP_SOFT" )) return;   /* HWBP_PERF: skip page protection */'
new_v = ('    { const char *e = getenv( "WINE_HWBP_SOFT" ); if (e && *e == \'0\') return; }'
         '   /* 默认开启；设 WINE_HWBP_SOFT=0 可关 */')
assert old_v in v
v = v.replace(old_v, new_v, 1)
open(V, "w", encoding="utf-8").write(v)
print("virtual.c HWBP default on")
