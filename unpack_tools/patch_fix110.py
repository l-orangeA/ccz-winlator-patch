"""fix110: virtual_init 末尾注册预存在映射（box64 下无 wine preloader，
nls/客二进制/宿主保留区对 wine 视图不可见 → 分配器把壳的栈放进 nls 只读映射）。
复用 create_view 的重叠自愈 + VPROT_SYSTEM（删除时不 unmap）。
在 WSL 跑: python3 patch_fix110.py
改动: /root/wine0905/dlls/ntdll/unix/virtual.c virtual_init 末尾
"""
P = "/root/wine0905/dlls/ntdll/unix/virtual.c"

ANCHOR = '''        TRACE( "virtual memory max size: %ld\\n", vmem_max_size );
    }
}'''

NEW = '''        TRACE( "virtual memory max size: %ld\\n", vmem_max_size );
    }

    /* fix110: register pre-existing mappings. Under box64 there is no wine
       preloader, so wine_main_preload_info is NULL and mappings established
       before ntdll init (nls files, the guest binary, host reservations) are
       invisible to the virtual view. The allocator would then hand out
       addresses that collide with them. create_view() self-heals overlaps
       (deleting stale system views without unmapping), so re-scanning here
       is safe even for ranges wine will use later. */
    {
        FILE *mf = fopen( "/proc/self/maps", "r" );
        if (mf)
        {
            char line[512];
            unsigned int count = 0;
            while (fgets( line, sizeof(line), mf ))
            {
                unsigned long lo = 0, hi = 0;
                char perms[8] = {0};
                struct file_view *view;
                BYTE vprot = VPROT_COMMITTED | VPROT_SYSTEM | VPROT_READ;
                NTSTATUS status;
                if (sscanf( line, "%lx-%lx %7s", &lo, &hi, perms ) != 3) continue;
                if (!lo && !hi) continue;
                if (find_view_range( (void *)lo, hi - lo )) continue;  /* already tracked */
                if (strchr( perms, 'w' )) vprot |= VPROT_WRITE;
                if (strchr( perms, 'x' )) vprot |= VPROT_EXEC;
                status = create_view( &view, (void *)lo, hi - lo, vprot );
                if (!status) count++;
            }
            fclose( mf );
            if (hwbp_verbose())
            {
                char b[96]; int n = snprintf( b, sizeof(b), "FIX110 pre-existing views=%u\\n", count );
                write( 2, b, n );
            }
        }
    }
}'''

src = open(P, encoding="utf-8", errors="replace").read()
if "fix110" in src:
    print("already patched")
elif src.count(ANCHOR) == 1:
    open(P, "w", encoding="utf-8", newline="\n").write(src.replace(ANCHOR, NEW))
    print("patched")
else:
    raise SystemExit("anchor count=%d" % src.count(ANCHOR))
