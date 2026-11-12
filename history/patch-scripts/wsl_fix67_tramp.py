import io
p = '/root/wine0905/dlls/ntdll/unix/virtual.c'
s = io.open(p, encoding='utf-8', errors='replace', newline='').read()
if 'HWBP_TRAMPOLINE' in s:
    print('already'); raise SystemExit

# keep page in list on fault (was removing)
old = '''    hwbp_pending = idx ? idx : 9;
    hwbp_step_flag = 0;   /* DIRECT */
    hwbp_step_page = page;
    hwbp_rw_unwatch( page );
    set_page_vprot_bits( page, page_size, VPROT_READ | VPROT_WRITE, VPROT_WRITEWATCH );'''
new = '''    hwbp_pending = idx ? idx : 9;
    hwbp_step_flag = 0;   /* DIRECT */
    hwbp_step_page = page;
    set_page_vprot_bits( page, page_size, VPROT_READ | VPROT_WRITE, VPROT_WRITEWATCH );'''
if old in s:
    s = s.replace(old, new, 1)
else:
    print('fault body already in keep form')

# add hold + trampoline helpers before hwbp_arm_one
anchor = '''static void hwbp_arm_one( unsigned long a, unsigned int len, unsigned int rw )'''
assert anchor in s, 'arm anchor'
helper = '''/* HWBP_TRAMPOLINE: temp int3 + hold during host context application */
static int hwbp_hold_flag;
static struct { unsigned long addr; unsigned char orig; int used; } hwbp_tempbp[8];

void hwbp_hold_begin( void ) { hwbp_hold_flag = 1; }
void hwbp_reprotect_all( void )
{
    int i;
    for (i = 0; i < 8; i++)
    {
        char *pg = (char *)hwbp_rw_page[i];
        if (!pg) continue;
        set_page_vprot_bits( pg, page_size, 0, VPROT_READ | VPROT_WRITE | VPROT_WRITEWATCH );
        mprotect_range( pg, page_size, 0, 0 );
    }
}
void hwbp_hold_end( void ) { hwbp_hold_flag = 0; hwbp_reprotect_all(); }

void hwbp_tempbp_clear_all( void )
{
    int i;
    for (i = 0; i < 8; i++) if (hwbp_tempbp[i].used)
    {
        unsigned long addr = hwbp_tempbp[i].addr, pg = addr & ~page_mask;
        int old = get_unix_prot( get_page_vprot( (void *)pg ) );
        if (!mprotect( (void *)pg, page_size, PROT_READ | PROT_WRITE | PROT_EXEC ))
        {
            *(unsigned char *)addr = hwbp_tempbp[i].orig;
            mprotect( (void *)pg, page_size, old );
        }
        hwbp_tempbp[i].used = 0;
    }
}

int hwbp_tempbp_set( unsigned long addr )
{
    int i;
    for (i = 0; i < 8; i++) if (!hwbp_tempbp[i].used) break;
    if (i == 8)  return 0;
    {
        unsigned long pg = addr & ~page_mask;
        int old = get_unix_prot( get_page_vprot( (void *)pg ) );
        if (mprotect( (void *)pg, page_size, PROT_READ | PROT_WRITE | PROT_EXEC )) return 0;
        hwbp_tempbp[i].orig = *(unsigned char *)addr;
        *(unsigned char *)addr = 0xcc;
        mprotect( (void *)pg, page_size, old );
        hwbp_tempbp[i].addr = addr;
        hwbp_tempbp[i].used = 1;
    }
    return 1;
}

int hwbp_tempbp_check( unsigned long addr )
{
    int i;
    for (i = 0; i < 8; i++) if (hwbp_tempbp[i].used && hwbp_tempbp[i].addr == addr)
    {
        unsigned long pg = addr & ~page_mask;
        int old = get_unix_prot( get_page_vprot( (void *)pg ) );
        if (!mprotect( (void *)pg, page_size, PROT_READ | PROT_WRITE | PROT_EXEC ))
        {
            *(unsigned char *)addr = hwbp_tempbp[i].orig;
            mprotect( (void *)pg, page_size, old );
        }
        hwbp_tempbp[i].used = 0;
        return 1;
    }
    return 0;
}

/* x86-32 instruction length (common subset), 0 = unknown */
int hwbp_insn_len( const unsigned char *p )
{
    int i = 0;
    unsigned char op;
    int has66 = 0, has67 = 0, opsz, adsz;
    for (;;)
    {
        unsigned char c = p[i];
        if (c == 0x66) { has66 = 1; i++; continue; }
        if (c == 0x67) { has67 = 1; i++; continue; }
        if (c == 0xf0 || c == 0xf2 || c == 0xf3 || c == 0x2e || c == 0x36 || c == 0x3e ||
            c == 0x26 || c == 0x64 || c == 0x65) { i++; continue; }
        break;
    }
    opsz = has66 ? 2 : 4;
    adsz = has67 ? 2 : 4;
    op = p[i++];
    if (op >= 0x40 && op <= 0x5f) return i;                 /* inc/dec/push/pop reg */
    if (op == 0x60 || op == 0x61 || op == 0x62 || op == 0x63) return i;
    if (op == 0x68) return i + opsz;                        /* push imm */
    if (op == 0x6a) return i + 1;                           /* push imm8 */
    if (op >= 0x70 && op <= 0x7f) return i + 1;             /* jcc rel8 */
    if (op >= 0xb0 && op <= 0xb7) return i + 1;             /* mov r8,imm8 */
    if (op >= 0xb8 && op <= 0xbf) return i + opsz;          /* mov r,imm */
    if (op >= 0x90 && op <= 0x9f) return i;                 /* nop/xchg/pushfd/... */
    if (op == 0xc2 || op == 0xca) return i + 2;
    if (op == 0xc3 || op == 0xcb || op == 0xc9 || op == 0xcc || op == 0xcd) return (op == 0xcd) ? i + 1 : i;
    if (op == 0xc8) return i + 3;
    if (op == 0xa0 || op == 0xa1 || op == 0xa2 || op == 0xa3) return i + adsz;
    if (op == 0xa4 || op == 0xa5 || op == 0xa6 || op == 0xa7 ||
        op == 0xaa || op == 0xab || op == 0xac || op == 0xad) return i;
    if (op == 0xe8 || op == 0xe9) return i + 4;
    if (op == 0xeb) return i + 1;
    if (op == 0xc6) { /* mov r/m8, imm8 */
        int m = p[i], mod = m >> 6, rm = m & 7, len = i + 1;
        if (mod != 3 && rm == 4) len++;
        if (mod == 0 && rm == 5) len += 4; else if (mod == 1) len += 1; else if (mod == 2) len += 4;
        return len + 1;
    }
    if (op == 0xc7) { /* mov r/m32, imm32 */
        int m = p[i], mod = m >> 6, rm = m & 7, len = i + 1;
        if (mod != 3 && rm == 4) len++;
        if (mod == 0 && rm == 5) len += 4; else if (mod == 1) len += 1; else if (mod == 2) len += 4;
        return len + opsz;
    }
    if (op == 0xf6 || op == 0xf7) { /* group3 */
        int m = p[i], mod = m >> 6, rm = m & 7, reg = (m >> 3) & 7, len = i + 1;
        if (mod != 3 && rm == 4) len++;
        if (mod == 0 && rm == 5) len += 4; else if (mod == 1) len += 1; else if (mod == 2) len += 4;
        if (reg == 0 || reg == 1) len += (op == 0xf6) ? 1 : opsz;
        return len;
    }
    if (op == 0xfe || op == 0xff) { /* group4/5 */
        int m = p[i], mod = m >> 6, rm = m & 7, len = i + 1;
        if (mod != 3 && rm == 4) len++;
        if (mod == 0 && rm == 5) len += 4; else if (mod == 1) len += 1; else if (mod == 2) len += 4;
        return len;
    }
    if (op == 0x0f) { /* two-byte */
        unsigned char op2 = p[i++];
        if (op2 >= 0x80 && op2 <= 0x8f) return i + 4;
        if (op2 >= 0x40 && op2 <= 0x4f) { int m = p[i], mod = m >> 6, rm = m & 7, len = i + 1;
            if (mod != 3 && rm == 4) len++;
            if (mod == 0 && rm == 5) len += 4; else if (mod == 1) len += 1; else if (mod == 2) len += 4;
            return len; }
        { int m = p[i], mod = m >> 6, rm = m & 7, len = i + 1;
          if (mod != 3 && rm == 4) len++;
          if (mod == 0 && rm == 5) len += 4; else if (mod == 1) len += 1; else if (mod == 2) len += 4;
          return len; }
    }
    /* common ALU/mov forms with modrm */
    switch (op)
    {
    case 0x00: case 0x01: case 0x02: case 0x03: case 0x08: case 0x09: case 0x0a: case 0x0b:
    case 0x10: case 0x11: case 0x12: case 0x13: case 0x18: case 0x19: case 0x1a: case 0x1b:
    case 0x20: case 0x21: case 0x22: case 0x23: case 0x28: case 0x29: case 0x2a: case 0x2b:
    case 0x30: case 0x31: case 0x32: case 0x33: case 0x38: case 0x39: case 0x3a: case 0x3b:
    case 0x84: case 0x85: case 0x86: case 0x87: case 0x88: case 0x89: case 0x8a: case 0x8b:
    case 0x8d: case 0x8f: case 0xd1: case 0xd3: case 0xc1: case 0xd0: case 0xd2:
    {
        int m = p[i], mod = m >> 6, rm = m & 7, len = i + 1;
        if (mod != 3 && rm == 4) len++;
        if (mod == 0 && rm == 5) len += 4; else if (mod == 1) len += 1; else if (mod == 2) len += 4;
        if (op == 0xc1) len += 1;
        return len;
    }
    case 0x80: case 0x81: case 0x83:
    {
        int m = p[i], mod = m >> 6, rm = m & 7, len = i + 1;
        if (mod != 3 && rm == 4) len++;
        if (mod == 0 && rm == 5) len += 4; else if (mod == 1) len += 1; else if (mod == 2) len += 4;
        len += (op == 0x81) ? opsz : 1;
        return len;
    }
    default: break;
    }
    return 0;
}

'''
s = s.replace(anchor, helper + anchor, 1)

# in hwbp_arm_one: honour hold flag and clear trampolines
old2 = '''            set_page_vprot_bits( page, page_size, 0, VPROT_READ | VPROT_WRITE | VPROT_WRITEWATCH );
            mprotect_range( page, page_size, 0, 0 );'''
new2 = '''            if (hwbp_hold_flag)
                set_page_vprot_bits( page, page_size, VPROT_READ | VPROT_WRITE, VPROT_WRITEWATCH );
            else
                set_page_vprot_bits( page, page_size, 0, VPROT_READ | VPROT_WRITE | VPROT_WRITEWATCH );
            mprotect_range( page, page_size, 0, 0 );'''
assert old2 in s, 'arm hold'
s = s.replace(old2, new2, 1)

io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('trampoline helpers added')
