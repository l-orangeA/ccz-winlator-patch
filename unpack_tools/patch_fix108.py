S = "/root/wine0905/dlls/ntdll/unix/signal_x86_64.c"
V = "/root/wine0905/dlls/ntdll/unix/virtual.c"
s = open(S, encoding="utf-8").read()
v = open(V, encoding="utf-8").read()

def rep(old, new, src):
    assert old in src, old[:90]
    return src.replace(old, new, 1)

# 1) BPT fix default-on
s = rep(r'if (bpt_fix < 0) bpt_fix = getenv( "WINE_BPT_FIX" ) != NULL;',
        r'''if (bpt_fix < 0) { const char *e = getenv( "WINE_BPT_FIX" ); bpt_fix = !(e && *e == '0'); }''',
        s)
# gate its log
s = rep(r'''                char t[96]; int n = snprintf( t, sizeof(t), "BPT_RIP_FIX rip=%lx -> %lx trap=%lx\n", rip, rip + 1, (unsigned long)TRAP_sig(ucontext) );
                write( 2, t, n );''',
        r'''                if (hwbp_verbose()) { char t[96]; int n = snprintf( t, sizeof(t), "BPT_RIP_FIX rip=%lx -> %lx trap=%lx\n", rip, rip + 1, (unsigned long)TRAP_sig(ucontext) ); write( 2, t, n ); }''',
        s)

# 2) GCTX_DBG
s = rep(r'''    { I386_CONTEXT *c0 = ctx; char b[128]; int n = snprintf( b, sizeof(b),
        "GCTX_DBG size=%lx flags=%lx eip=%lx\n", (unsigned long)size,
        (unsigned long)c0->ContextFlags, (unsigned long)c0->Eip ); write( 2, b, n ); }''',
        r'''    if (hwbp_verbose()) { I386_CONTEXT *c0 = ctx; char b[128]; int n = snprintf( b, sizeof(b),
        "GCTX_DBG size=%lx flags=%lx eip=%lx\n", (unsigned long)size,
        (unsigned long)c0->ContextFlags, (unsigned long)c0->Eip ); write( 2, b, n ); }''',
        s)

# 3) GCTX2
s = rep(r'''        if (needed_flags & CONTEXT_I386_DEBUG_REGISTERS)
    {
        char b[192]; int n = snprintf( b, sizeof(b),
            "GCTX2 dr0=%lx dr1=%lx dr2=%lx dr3=%lx dr6=%lx dr7=%lx eip=%lx\n",''',
        r'''        if (hwbp_verbose() && (needed_flags & CONTEXT_I386_DEBUG_REGISTERS))
    {
        char b[192]; int n = snprintf( b, sizeof(b),
            "GCTX2 dr0=%lx dr1=%lx dr2=%lx dr3=%lx dr6=%lx dr7=%lx eip=%lx\n",''',
        s)

# 4) SRE_AFTER
s = rep(r'''    if ((ULONG_PTR)rec->ExceptionAddress < 0x10000000)
    {
        char b[160]; int n = snprintf( b, sizeof(b),
            "SRE_AFTER code=%lx stack=%lx stackptr=%lx\n",''',
        r'''    if (hwbp_verbose() && (ULONG_PTR)rec->ExceptionAddress < 0x10000000)
    {
        char b[160]; int n = snprintf( b, sizeof(b),
            "SRE_AFTER code=%lx stack=%lx stackptr=%lx\n",''',
        s)

# 5) STKFLT_FAULT
s = rep(r'''        if ((ULONG_PTR)RIP_sig(ucontext) < 0x10000000)
        {
            char b[160]; int n = snprintf( b, sizeof(b),
                "STKFLT_FAULT addr=%lx ret=%lx\n",''',
        r'''        if (hwbp_verbose() && (ULONG_PTR)RIP_sig(ucontext) < 0x10000000)
        {
            char b[160]; int n = snprintf( b, sizeof(b),
                "STKFLT_FAULT addr=%lx ret=%lx\n",''',
        s)

# 6) STK mega block
s = rep(r'''        {
            TEB *teb = NtCurrentTeb();''',
        r'''        if (hwbp_verbose()) {
            TEB *teb = NtCurrentTeb();''',
        s)

# 7) SEGV_ENTRY
s = rep(r'''    if ((ULONG_PTR)RIP_sig(ucontext) < 0x10000000)
    {
        char b[192]; int n = snprintf( b, sizeof(b),
            "SEGV_ENTRY rip=%lx addr=%lx err=%x rsp=%lx trap=%lx\n",''',
        r'''    if (hwbp_verbose() && (ULONG_PTR)RIP_sig(ucontext) < 0x10000000)
    {
        char b[192]; int n = snprintf( b, sizeof(b),
            "SEGV_ENTRY rip=%lx addr=%lx err=%x rsp=%lx trap=%lx\n",''',
        s)

# 8) reglog
s = rep(r'''    if (n++ >= 5000) return;''',
        r'''    if (!hwbp_verbose() || n++ >= 5000) return;''',
        s)

# 9) TRAMP hit
s = rep(r'''            { char b[96]; int n = snprintf(b,sizeof(b),"TRAMP hit addr=%lx\n",(unsigned long)a); write(2,b,n); }''',
        r'''            if (hwbp_verbose()) { char b[96]; int n = snprintf(b,sizeof(b),"TRAMP hit addr=%lx\n",(unsigned long)a); write(2,b,n); }''',
        s)

# virtual.c: perf + alloc probes
v = rep(r'''            n = snprintf( b, sizeof(b), "HWBP_PERF software fd=%d errno=%d\n", sfd, e1 ); write( 2, b, n );''',
        r'''            if (hwbp_verbose()) { n = snprintf( b, sizeof(b), "HWBP_PERF software fd=%d errno=%d\n", sfd, e1 ); write( 2, b, n ); }''', v)
v = rep(r'''            n = snprintf( b, sizeof(b), "HWBP_PERF bp-noxk fd=%d errno=%d\n", bfd, e2 ); write( 2, b, n );''',
        r'''            if (hwbp_verbose()) { n = snprintf( b, sizeof(b), "HWBP_PERF bp-noxk fd=%d errno=%d\n", bfd, e2 ); write( 2, b, n ); }''', v)
v = rep(r'''    if (fd < 0) { char b[96]; int n = snprintf(b, sizeof(b), "HWBP_PERF open fail addr=%lx errno=%d\n", addr, errno); write(2,b,n); return -1; }''',
        r'''    if (fd < 0) { if (hwbp_verbose()) { char b[96]; int n = snprintf(b, sizeof(b), "HWBP_PERF open fail addr=%lx errno=%d\n", addr, errno); write(2,b,n); } return -1; }''', v)
v = rep(r'''    { char b[128]; int n = snprintf(b, sizeof(b), "HWBP_PERF arm idx=%d addr=%lx len=%u rw=%u fd=%d\n", i+1, addr, len, rw, fd); write(2,b,n); }''',
        r'''    if (hwbp_verbose()) { char b[128]; int n = snprintf(b, sizeof(b), "HWBP_PERF arm idx=%d addr=%lx len=%u rw=%u fd=%d\n", i+1, addr, len, rw, fd); write(2,b,n); }''', v)
v = rep(r'''    { char hb[128]; int hn = snprintf( hb, sizeof(hb),
      "HASL_DBG limit=%lx pvsize=%lx\n",
      (unsigned long)host_addr_space_limit, (unsigned long)pages_vprot_size ); write( 2, hb, hn ); }''',
        r'''    if (hwbp_verbose()) { char hb[128]; int hn = snprintf( hb, sizeof(hb),
      "HASL_DBG limit=%lx pvsize=%lx\n",
      (unsigned long)host_addr_space_limit, (unsigned long)pages_vprot_size ); write( 2, hb, hn ); }''', v)
v = rep(r'''        if (*size_ptr >= 0x1000)
        {
            char b[192]; int n = snprintf( b, sizeof(b),
                "ALLOCPROBE req=%lu size=%lu type=%lx prot=%lx ret=%lu rsize=%lu st=%ld\n",''',
        r'''        if (hwbp_verbose() && *size_ptr >= 0x1000)
        {
            char b[192]; int n = snprintf( b, sizeof(b),
                "ALLOCPROBE req=%lu size=%lu type=%lx prot=%lx ret=%lu rsize=%lu st=%ld\n",''', v)

open(S, "w", encoding="utf-8").write(s)
open(V, "w", encoding="utf-8").write(v)
print("cleanup patched")
