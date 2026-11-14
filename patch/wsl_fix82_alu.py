import io
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = io.open(p, encoding='utf-8', errors='replace', newline='').read()
if '/* ALU r/m forms */' in s:
    print('already'); raise SystemExit

anchor = '''        RIP_sig(uc) += i + 1 + n;
        return 1;
    }
    return 0;
}
'''
assert anchor in s, 'anchor'
alu = '''        RIP_sig(uc) += i + 1 + n;
        return 1;
    }
    /* ALU r/m forms: 0x00..0x3B (add/or/adc/sbb/and/sub/xor/cmp) and test/xchg */
    if ((op <= 0x3b) && ((op & 0xc6) == 0x00 || (op & 0xc6) == 0x02 || (op & 0xc6) == 0x08 ||
                         (op & 0xc6) == 0x0a || (op & 0xc6) == 0x10 || (op & 0xc6) == 0x12 ||
                         (op & 0xc6) == 0x18 || (op & 0xc6) == 0x1a || (op & 0xc6) == 0x20 ||
                         (op & 0xc6) == 0x22 || (op & 0xc6) == 0x28 || (op & 0xc6) == 0x2a ||
                         (op & 0xc6) == 0x30 || (op & 0xc6) == 0x32 || (op & 0xc6) == 0x38 ||
                         (op & 0xc6) == 0x3a || op == 0x84 || op == 0x85 || op == 0x86 || op == 0x87))
    {
        int reg = (ip[i+1] >> 3) & 7;
        int width = (op & 1) ? ((opsz == 2) ? 2 : 4) : 1;
        unsigned int cur, val, res;
        n = hwbp_modrm_len( ip + i + 1 );
        if (((ip[i+1] >> 6) & 3) == 3) return 0;      /* register form cannot fault */
        cur = (width == 1) ? *(unsigned char *)(ULONG_PTR)fa
            : (width == 2) ? *(unsigned short *)(ULONG_PTR)fa : *(unsigned int *)(ULONG_PTR)fa;
        val = (unsigned int)hwbp_getreg( uc, reg );
        if (width == 1) val &= 0xff;
        else if (width == 2) val &= 0xffff;
        if (op == 0x84 || op == 0x85) res = cur & val;
        else if (op == 0x86 || op == 0x87)
        {
            unsigned int old = cur;
            if (width == 1) { *(unsigned char *)(ULONG_PTR)fa = (unsigned char)val;
                              hwbp_setreg( uc, reg, (hwbp_getreg(uc,reg) & ~0xffu) | old ); }
            else if (width == 2) { *(unsigned short *)(ULONG_PTR)fa = (unsigned short)val;
                                   hwbp_setreg( uc, reg, (hwbp_getreg(uc,reg) & ~0xffffu) | old ); }
            else { *(unsigned int *)(ULONG_PTR)fa = val; hwbp_setreg( uc, reg, old ); }
            RIP_sig(uc) += i + 1 + n; return 1;
        }
        else
        {
            switch ((op >> 3) & 7)
            {
            case 0: res = cur + val; break;      /* add */
            case 1: res = cur | val; break;      /* or */
            case 2: res = cur + val; break;      /* adc (approx) */
            case 3: res = cur - val; break;      /* sbb (approx) */
            case 4: res = cur & val; break;      /* and */
            case 5: res = cur - val; break;      /* sub */
            case 6: res = cur ^ val; break;      /* xor */
            default: res = cur - val; break;     /* cmp: no store */
            }
        }
        if (!(op == 0x38 || op == 0x39 || op == 0x3a || op == 0x3b || op == 0x84 || op == 0x85))
        {
            if (width == 1) *(unsigned char *)(ULONG_PTR)fa = (unsigned char)res;
            else if (width == 2) *(unsigned short *)(ULONG_PTR)fa = (unsigned short)res;
            else *(unsigned int *)(ULONG_PTR)fa = res;
        }
        RIP_sig(uc) += i + 1 + n;
        return 1;
    }
    return 0;
}
'''
s = s.replace(anchor, alu, 1)
io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('ALU r/m forms added')
