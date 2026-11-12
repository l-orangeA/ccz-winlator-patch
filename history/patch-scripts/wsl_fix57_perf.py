NL = chr(10)
p = '/root/wine0905/dlls/ntdll/unix/virtual.c'
s = open(p, encoding='utf-8', errors='replace').read()

if 'HWBP_PERF' in s:
    print('already')
    raise SystemExit

# includes
anchor = '#include "wine/debug.h"'
assert anchor in s, 'anchor'
s = s.replace(anchor, anchor + NL + '#include <linux/perf_event.h>' + NL + '#include <linux/hw_breakpoint.h>' + NL + '#include <sys/ioctl.h>' + NL + '#include <fcntl.h>' + NL + '#include <sys/syscall.h>  /* HWBP_PERF */', 1)

# perf slot code before hwbp_update
anchor2 = 'void hwbp_update( unsigned long d0, unsigned long d1, unsigned long d2, unsigned long d3, unsigned long d7 )'
assert anchor2 in s, 'upd'
perf = NL.join([
'/* HWBP_PERF: real hardware data breakpoints through perf_event_open */',
'struct hwbp_perf_slot { int fd; pid_t tid; unsigned long addr; unsigned int len, rw; int delivered; };',
'static struct hwbp_perf_slot hwbp_perf_slots[4];',
'',
'static pid_t hwbp_perf_tid(void) { return (pid_t)syscall( SYS_gettid ); }',
'',
'static void hwbp_perf_close( int i )',
'{',
'    if (hwbp_perf_slots[i].fd >= 0) close( hwbp_perf_slots[i].fd );',
'    hwbp_perf_slots[i].fd = -1;',
'    hwbp_perf_slots[i].tid = 0;',
'    hwbp_perf_slots[i].addr = 0;',
'    hwbp_perf_slots[i].delivered = 0;',
'}',
'',
'static int hwbp_perf_open( int i, unsigned long addr, unsigned int len, unsigned int rw )',
'{',
'    struct perf_event_attr a;',
'    struct f_owner_ex own;',
'    int fd;',
'    memset( &a, 0, sizeof(a) );',
'    a.type = PERF_TYPE_BREAKPOINT;',
'    a.size = sizeof(a);',
'    a.bp_type = (rw == 3) ? HW_BREAKPOINT_RW : (rw == 2) ? HW_BREAKPOINT_W : HW_BREAKPOINT_R;',
'    a.bp_addr = addr;',
'    a.bp_len = len;',
'    a.sample_period = 1;',
'    a.wakeup_events = 1;',
'    a.exclude_kernel = 1;',
'    a.exclude_hv = 1;',
'    a.disabled = 1;',
'    fd = syscall( __NR_perf_event_open, &a, 0, -1, -1, 0 );',
'    if (fd < 0) { char b[96]; int n = snprintf(b, sizeof(b), "HWBP_PERF open fail addr=%lx errno=%d\\n", addr, errno); write(2,b,n); return -1; }',
'    own.type = F_OWNER_TID;',
'    own.pid = hwbp_perf_tid();',
'    fcntl( fd, F_SETOWN_EX, &own );',
'    fcntl( fd, F_SETSIG, SIGTRAP );',
'    fcntl( fd, F_SETFL, O_ASYNC | O_NONBLOCK );',
'    ioctl( fd, PERF_EVENT_IOC_RESET, 0 );',
'    ioctl( fd, PERF_EVENT_IOC_ENABLE, 0 );',
'    hwbp_perf_slots[i].fd = fd;',
'    hwbp_perf_slots[i].tid = hwbp_perf_tid();',
'    hwbp_perf_slots[i].addr = addr;',
'    hwbp_perf_slots[i].len = len;',
'    hwbp_perf_slots[i].rw = rw;',
'    hwbp_perf_slots[i].delivered = 0;',
'    { char b[128]; int n = snprintf(b, sizeof(b), "HWBP_PERF arm idx=%d addr=%lx len=%u rw=%u fd=%d\\n", i+1, addr, len, rw, fd); write(2,b,n); }',
'    return fd;',
'}',
'',
'/* returns 1..4 if fd belongs to a perf slot of this thread */',
'int hwbp_perf_owner( int fd )',
'{',
'    int i;',
'    pid_t tid = hwbp_perf_tid();',
'    for (i = 0; i < 4; i++)',
'        if (hwbp_perf_slots[i].fd == fd && hwbp_perf_slots[i].tid == tid) return i + 1;',
'    return 0;',
'}',
'',
'/* mark a hit; returns 1 for the first delivery, 0 for a duplicate */',
'int hwbp_perf_mark( int idx )',
'{',
'    if (idx < 1 || idx > 4) return 0;',
'    if (hwbp_perf_slots[idx-1].delivered) return 0;',
'    hwbp_perf_slots[idx-1].delivered = 1;',
'    return 1;',
'}',
'',
'static void hwbp_perf_update( unsigned long d0, unsigned long d1, unsigned long d2, unsigned long d3, unsigned long d7 )',
'{',
'    unsigned long dr[4];',
'    pid_t tid = hwbp_perf_tid();',
'    int i;',
'    dr[0] = d0; dr[1] = d1; dr[2] = d2; dr[3] = d3;',
'    for (i = 0; i < 4; i++)',
'    {',
'        unsigned int rw, len, size;',
'        int enabled = (d7 & (1u << (2*i))) || (d7 & (2u << (2*i)));',
'        if (!enabled || !dr[i]) { hwbp_perf_close( i ); continue; }',
'        rw = (unsigned int)((d7 >> (16 + 4*i)) & 3);',
'        len = (unsigned int)((d7 >> (18 + 4*i)) & 3);',
'        size = hwbp_len_bytes( len );',
'        if (rw == 0) { hwbp_perf_close( i ); continue; }',
'        if (hwbp_perf_slots[i].fd >= 0 &&',
'            (hwbp_perf_slots[i].tid != tid || hwbp_perf_slots[i].addr != dr[i] ||',
'             hwbp_perf_slots[i].len != size || hwbp_perf_slots[i].rw != rw))',
'            hwbp_perf_close( i );',
'        if (hwbp_perf_slots[i].fd < 0) hwbp_perf_open( i, dr[i], size, rw );',
'    }',
'}',
'',
anchor2])
s = s.replace(anchor2, perf, 1)

# call perf update from hwbp_update
old = NL.join([
'    hwbp_dr[0] = d0; hwbp_dr[1] = d1; hwbp_dr[2] = d2; hwbp_dr[3] = d3; hwbp_dr7 = d7;',
'    hwbp_rw_clear();'])
new = NL.join([
'    hwbp_dr[0] = d0; hwbp_dr[1] = d1; hwbp_dr[2] = d2; hwbp_dr[3] = d3; hwbp_dr7 = d7;',
'    hwbp_perf_update( d0, d1, d2, d3, d7 );   /* HWBP_PERF */',
'    if (!getenv( "WINE_HWBP_SOFT" )) return;   /* HWBP_PERF: skip page protection */',
'    hwbp_rw_clear();'])
assert old in s, 'call site'
s = s.replace(old, new, 1)

open(p, 'w', encoding='utf-8', newline=NL).write(s)
print('perf patch applied')
