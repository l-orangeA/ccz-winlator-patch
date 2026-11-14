import re

V = "/root/wine0905/dlls/ntdll/unix/virtual.c"
S = "/root/wine0905/dlls/ntdll/unix/signal_x86_64.c"

# ---------- virtual.c ----------
v = open(V, encoding="utf-8").read()

# 1) 计数器
anchor = "static int hwbp_pending;  /* rw watch pages below */"
assert anchor in v
counters = anchor + """
unsigned long hwbp_cnt_arm, hwbp_cnt_fault, hwbp_cnt_step, hwbp_cnt_hit;
"""
v = v.replace(anchor, counters, 1)

# 2) step_end 支持“即将 consume 时跳过复保护”
old_se = """void hwbp_step_end( void )
{
    char *pg = hwbp_step_page;
    hwbp_step_flag = 0;
    hwbp_step_page = 0;
    if (!pg) return;
    if (!hwbp_rw_watched( pg )) return;
    set_page_vprot_bits( pg, page_size, 0, VPROT_READ | VPROT_WRITE | VPROT_WRITEWATCH );
    mprotect_range( pg, page_size, 0, 0 );
}"""
new_se = """void hwbp_step_end( void ) { hwbp_step_end_ex( 0 ); }

/* keep!=0: 这次单步马上就要 consume 了，页面保持开放，
   省掉一次「复保护 + 再解锁」的 mprotect 往返 */
void hwbp_step_end_ex( int keep )
{
    char *pg = hwbp_step_page;
    hwbp_step_flag = 0;
    hwbp_step_page = 0;
    hwbp_cnt_step++;
    if (!pg) return;
    if (!hwbp_rw_watched( pg )) return;
    if (keep) return;
    set_page_vprot_bits( pg, page_size, 0, VPROT_READ | VPROT_WRITE | VPROT_WRITEWATCH );
    mprotect_range( pg, page_size, 0, 0 );
}"""
assert old_se in v
v = v.replace(old_se, new_se, 1)

# 3) 故障计数
old_h = """int hwbp_handle_rw_fault( char *page, const void *addr )   /* DIRECT: return matched index */
{
    int idx = hwbp_match_write( addr );"""
new_h = """int hwbp_handle_rw_fault( char *page, const void *addr )   /* DIRECT: return matched index */
{
    int idx;
    hwbp_cnt_fault++;
    idx = hwbp_match_write( addr );
    if (idx) hwbp_cnt_hit++;"""
assert old_h in v
v = v.replace(old_h, new_h, 1)

# 4) 去掉 arm 里重复的 mprotect(PROT_NONE)：mprotect_range 已按 vprot=0 落下 PROT_NONE
old_a = """            { int rc = mprotect( page, page_size, PROT_NONE ); if (hwbp_verbose()) { int e = errno; char hb[128]; int hn = snprintf( hb, sizeof(hb), "HWBP_MPROT page=%lx rc=%d errno=%d\\n", (unsigned long)page, rc, e ); write( 2, hb, hn ); } }"""
new_a = """            hwbp_cnt_arm++;"""
assert old_a in v
v = v.replace(old_a, new_a, 1)

# 5) 退出时把统计写到文件
old_tail = "void hwbp_pending_restore(int v) { hwbp_pending = v; }"
new_tail = old_tail + """

__attribute__((destructor)) static void hwbp_dump_stats( void )
{
    int fd;
    char b[192];
    int n;
    if (!hwbp_cnt_arm && !hwbp_cnt_fault) return;
    fd = open( "/home/xuser/hwbp_stats.txt", O_WRONLY | O_CREAT | O_TRUNC, 0644 );
    if (fd < 0) return;
    n = snprintf( b, sizeof(b), "pid=%d arms=%lu faults=%lu steps=%lu hits=%lu\\n",
                  (int)getpid(), hwbp_cnt_arm, hwbp_cnt_fault, hwbp_cnt_step, hwbp_cnt_hit );
    write( fd, b, n );
    close( fd );
}"""
assert old_tail in v
v = v.replace(old_tail, new_tail, 1)

open(V, "w", encoding="utf-8").write(v)
print("virtual.c patched")

# ---------- signal_x86_64.c ----------
s = open(S, encoding="utf-8").read()
s = s.replace("extern void hwbp_step_end( void );",
              "extern void hwbp_step_end( void );\nextern void hwbp_step_end_ex( int keep );", 1)

old_c = """        int pend = hwbp_take_pending();
        hwbp_step_end();"""
new_c = """        int pend = hwbp_take_pending();
        hwbp_step_end_ex( pend >= 1 && pend <= 4 );"""
assert old_c in s
s = s.replace(old_c, new_c, 1)
open(S, "w", encoding="utf-8").write(s)
print("signal_x86_64.c patched")
