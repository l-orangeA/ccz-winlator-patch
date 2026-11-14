"""fix110b: 扫描跳过 KUSD 页(0x7ffe0000, wine 自建) + 每视图地址日志.
在 fix110 树上增量修改. 在 WSL 跑: python3 patch_fix110b.py
"""
P = "/root/wine0905/dlls/ntdll/unix/virtual.c"

OLD = '''                if (sscanf( line, "%lx-%lx %7s", &lo, &hi, perms ) != 3) continue;
                if (!lo && !hi) continue;
                if (find_view_range( (void *)lo, hi - lo )) continue;  /* already tracked */
                if (strchr( perms, 'w' )) vprot |= VPROT_WRITE;
                if (strchr( perms, 'x' )) vprot |= VPROT_EXEC;
                status = create_view( &view, (void *)lo, hi - lo, vprot );
                if (!status) count++;'''

NEW = '''                if (sscanf( line, "%lx-%lx %7s", &lo, &hi, perms ) != 3) continue;
                if (!lo && !hi) continue;
                if (lo <= 0x7ffe0000 && hi >= 0x7ffe1000) continue;  /* fix110b: KUSD page, wine re-creates it */
                if (find_view_range( (void *)lo, hi - lo )) continue;  /* already tracked */
                if (strchr( perms, 'w' )) vprot |= VPROT_WRITE;
                if (strchr( perms, 'x' )) vprot |= VPROT_EXEC;
                status = create_view( &view, (void *)lo, hi - lo, vprot );
                if (!status)
                {
                    count++;
                    if (hwbp_verbose())
                    {
                        char b[128]; int n = snprintf( b, sizeof(b), "FIX110 view %lx-%lx %s\\n",
                            (unsigned long)lo, (unsigned long)hi, perms );
                        write( 2, b, n );
                    }
                }'''

src = open(P, encoding="utf-8", errors="replace").read()
if "fix110b" in src:
    print("already patched")
elif src.count(OLD) == 1:
    open(P, "w", encoding="utf-8", newline="\n").write(src.replace(OLD, NEW))
    print("patched")
else:
    raise SystemExit("anchor count=%d" % src.count(OLD))
