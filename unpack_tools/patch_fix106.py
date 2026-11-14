S = "/root/wine0905/dlls/ntdll/unix/signal_x86_64.c"
s = open(S, encoding="utf-8").read()

old = '''        if (mprotect( (void *)page, sz, PROT_READ | PROT_WRITE ) != 0)
        {
            static char tmp[16384];
            if (sz <= sizeof(tmp)) {
                static const char m2[] = "MPROT_FALLBACK\\n";
                write( 2, m2, sizeof(m2)-1 );
                memcpy( tmp, (void *)page, sz );
                void *p = mmap( (void *)page, sz, PROT_READ | PROT_WRITE,
                                MAP_PRIVATE | MAP_ANONYMOUS | MAP_FIXED, -1, 0 );
                if (p != MAP_FAILED) memcpy( p, tmp, sz );
            }
        }'''

new = '''        if (mprotect( (void *)page, sz, PROT_READ | PROT_WRITE ) != 0)
        {
            int mpe = errno;
            static char tmp[16384];
            if (sz <= sizeof(tmp)) {
                ULONG_PTR off;
                static const char m2[] = "MPROT_FALLBACK\\n";
                write( 2, m2, sizeof(m2)-1 );
                { static int mp; if (mp++ < 64) { char b[160]; int n = snprintf( b, sizeof(b),
                    "MPROT_FAIL page=%lx sz=%lx errno=%d sp=%lx rip=%lx\\n",
                    (unsigned long)page, (unsigned long)sz, mpe,
                    (unsigned long)stack_ptr, (unsigned long)context->Rip ); write( 2, b, n ); } }
                /* the page(s) may be unmapped: read only what is mapped, zero the rest */
                memset( tmp, 0, sz );
                for (off = 0; off < sz; off += page_size) {
                    struct iovec li, ri;
                    li.iov_base = tmp + off;
                    li.iov_len  = page_size;
                    ri.iov_base = (void *)(page + off);
                    ri.iov_len  = page_size;
                    process_vm_readv( getpid(), &li, 1, &ri, 1, 0 );
                }
                void *p = mmap( (void *)page, sz, PROT_READ | PROT_WRITE,
                                MAP_PRIVATE | MAP_ANONYMOUS | MAP_FIXED, -1, 0 );
                if (p != MAP_FAILED) memcpy( p, tmp, sz );
            }
        }'''

assert old in s and "MPROT_FAIL" not in s
s = s.replace(old, new, 1)

inc = '#include <sys/uio.h>'
if inc not in s:
    anchor = '#include <sys/mman.h>'
    assert anchor in s
    s = s.replace(anchor, anchor + '\n' + inc, 1)

open(S, "w", encoding="utf-8").write(s)
print("patched signal_x86_64.c")
