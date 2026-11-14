V = "/root/wine0905/dlls/ntdll/unix/virtual.c"
v = open(V, encoding="utf-8").read()

# 把统计写文件抽成普通函数，析构器复用
old = """__attribute__((destructor)) static void hwbp_dump_stats( void )
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
new = """static void hwbp_write_stats( void )
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
}

__attribute__((destructor)) static void hwbp_dump_stats( void ) { hwbp_write_stats(); }"""
assert old in v
v = v.replace(old, new, 1)

# 在 hwbp_update 完全卸载时落盘
old_u = """void hwbp_update( unsigned long d0, unsigned long d1, unsigned long d2, unsigned long d3, unsigned long d7 )"""
assert old_u in v
i = v.index(old_u)
# 找到函数体的收尾（下一个 "\n}\n"），在其前插入
j = v.index("\n}\n", i)
body_insert = """
    if (!(d0 | d1 | d2 | d3 | d7) && (hwbp_cnt_arm || hwbp_cnt_fault)) hwbp_write_stats();"""
v = v[:j] + body_insert + v[j:]
open(V, "w", encoding="utf-8").write(v)
print("patched")
