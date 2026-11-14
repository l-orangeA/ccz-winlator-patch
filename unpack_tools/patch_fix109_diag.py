"""fix109-diag v2: 在 MPROT_FAIL 写行后插入诊断（VMA dump + 替代 mprotect 序列）。
在 WSL 跑: python3 patch_fix109_diag.py
"""
P = "/root/wine0905/dlls/ntdll/unix/signal_x86_64.c"

ANCHOR = "(unsigned long)stack_ptr, (unsigned long)context->Rip ); write( 2, b, n ); } }"

INSERT = ANCHOR + '''
                    /* fix109-diag: dump the VMA line(s) covering [page, page+sz) */
                    {
                        FILE *mf = fopen( "/proc/self/maps", "r" );
                        if (mf) {
                            char ml[512];
                            int dumped = 0;
                            while (fgets( ml, sizeof(ml), mf )) {
                                unsigned long va = 0, vb = 0;
                                if (sscanf( ml, "%lx-%lx", &va, &vb ) == 2 && va < page + sz && vb > page) {
                                    write( 2, "MPROT_VMA ", 10 );
                                    write( 2, ml, strlen( ml ) );
                                    dumped++;
                                }
                            }
                            fclose( mf );
                            if (!dumped) { static const char m3[] = "MPROT_VMA none\\n"; write( 2, m3, sizeof(m3)-1 ); }
                        }
                    }
                    /* fix109-diag: alternative mprotect sequences with per-try errno */
                    {
                        int e1, e2, e3, e4;
                        e1 = mprotect( (void *)page, sz, PROT_READ );
                        e1 = e1 ? errno : 0;
                        e2 = mprotect( (void *)page, sz, PROT_NONE );
                        e2 = e2 ? errno : 0;
                        e3 = mprotect( (void *)page, sz, PROT_READ | PROT_WRITE );
                        e3 = e3 ? errno : 0;
                        e4 = mprotect( (void *)page, sz, PROT_READ | PROT_WRITE | PROT_EXEC );
                        e4 = e4 ? errno : 0;
                        { char b2[200]; int n2 = snprintf( b2, sizeof(b2),
                            "MPROT_ALT R=%d NONE=%d RW=%d RWX=%d\\n", e1, e2, e3, e4 ); write( 2, b2, n2 ); }
                    }'''

src = open(P, encoding="utf-8", errors="replace").read()
if "fix109-diag" in src:
    print("already patched")
elif src.count(ANCHOR) == 1:
    open(P, "w", encoding="utf-8", newline="\n").write(src.replace(ANCHOR, INSERT))
    print("patched")
else:
    raise SystemExit("anchor count=%d" % src.count(ANCHOR))
