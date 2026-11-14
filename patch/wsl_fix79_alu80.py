import io
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = io.open(p, encoding='utf-8', errors='replace', newline='').read()
old = '''        if (op == 0x83) { imm = (int)(signed char)ip[i+1+n]; }
        else if (op == 0x81) { imm = (int)(ip[i+1+n] | (ip[i+2+n] << 8) | (ip[i+3+n] << 16) | ((unsigned)ip[i+4+n] << 24)); }
        else { imm = (int)(signed char)ip[i+1+n]; }
        {
            unsigned int cur = (opsz == 2) ? *(unsigned short *)(ULONG_PTR)ea : *(unsigned int *)(ULONG_PTR)ea;'''
new = '''        if (op == 0x83) { imm = (int)(signed char)ip[i+1+n]; }
        else if (op == 0x81) { imm = (int)(ip[i+1+n] | (ip[i+2+n] << 8) | (ip[i+3+n] << 16) | ((unsigned)ip[i+4+n] << 24)); }
        else { imm = (int)(signed char)ip[i+1+n]; }
        if (op == 0x80)   /* group1 r/m8, imm8 */
        {
            unsigned int cur = *(unsigned char *)(ULONG_PTR)ea;
            unsigned int res;
            switch (reg)
            {
            case 0: res = (cur + imm) & 0xff; break;
            case 1: res = (cur | imm) & 0xff; break;
            case 4: res = (cur & imm) & 0xff; break;
            case 5: res = (cur - imm) & 0xff; break;
            case 6: res = (cur ^ imm) & 0xff; break;
            default: return 0;
            }
            *(unsigned char *)(ULONG_PTR)ea = (unsigned char)res;
            RIP_sig(uc) += i + 1 + n + 1;
            return 1;
        }
        {
            unsigned int cur = (opsz == 2) ? *(unsigned short *)(ULONG_PTR)ea : *(unsigned int *)(ULONG_PTR)ea;'''
assert old in s, 'alu block'
s = s.replace(old, new, 1)
io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('0x80 fixed')
