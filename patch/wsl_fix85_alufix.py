import io
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = io.open(p, encoding='utf-8', errors='replace', newline='').read()

# 1) replace the broken ALU condition
start = s.find('    /* ALU forms: r/m,reg')
assert start > 0
line_end = s.find('\n', s.find('op == 0x84 || op == 0x85 || op == 0x86 || op == 0x87)', start))
assert line_end > start
old_cond = s[start:line_end+1]
new_cond = '''    /* ALU forms: r/m,reg (0x00..0x3D with low3<=5) and reg,r/m; test/xchg */
    if (((op <= 0x3d) && ((op & 7) <= 5)) || op == 0x84 || op == 0x85 || op == 0x86 || op == 0x87)
'''
s = s[:start] + new_cond + s[line_end+1:]

# 2) reg_form detection: (op & 6) == 2 covers 0x02/0x03, 0x0a/0x0b, ... 0x3a/0x3b
s = s.replace('        int reg_form = ((op & 0xc6) == 0x02);        /* e.g. 0x03 add r32,r/m32 */',
              '        int reg_form = ((op & 6) == 2);              /* 0x02/0x03, 0x0a/0x0b ... 0x3a/0x3b */', 1)

# 3) AL/EAX,imm forms (op&7 == 4/5) cannot fault -> return 0
s = s.replace('''        n = hwbp_modrm_len( ip + i + 1 );
        if (((ip[i+1] >> 6) & 3) == 3) return 0;      /* register form cannot fault */
        mask = (width == 1)''',
              '''        if ((op & 7) >= 4) return 0;                  /* AL,imm / EAX,imm - cannot fault */
        n = hwbp_modrm_len( ip + i + 1 );
        if (((ip[i+1] >> 6) & 3) == 3) return 0;      /* register form cannot fault */
        mask = (width == 1)''', 1)
io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('ALU condition fixed')
