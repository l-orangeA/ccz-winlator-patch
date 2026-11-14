import io
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = io.open(p, encoding='utf-8', errors='replace', newline='').read()
start = s.find('    /* ALU r/m forms: 0x00..0x3B')
end = s.find('    return 0;\n}\n\nstatic void segv_handler')
assert start > 0 and end > start, 'range'
new = '''    /* ALU forms: r/m,reg (0x00..0x39 even) and reg,r/m (0x02..0x3B) + test/xchg */
    if (op <= 0x3b && ((op & 0xe0) == 0x00) &&
        (((op & 7) <= 3) || ((op & 7) >= 8 && (op & 7) <= 9) ||
         ((op & 7) >= 0x10 && (op & 7) <= 0x13) || ((op & 7) >= 0x18 && (op & 7) <= 0x1b) ||
         ((op & 7) >= 0x20 && (op & 7) <= 0x23) || ((op & 7) >= 0x28 && (op & 7) <= 0x2b) ||
         ((op & 7) >= 0x30 && (op & 7) <= 0x33) || ((op & 7) >= 0x38 && (op & 7) <= 0x3b)) ||
        op == 0x84 || op == 0x85 || op == 0x86 || op == 0x87)
    {
        int reg = (ip[i+1] >> 3) & 7;
        int reg_form = ((op & 0xc6) == 0x02);        /* e.g. 0x03 add r32,r/m32 */
        int width = (op & 1) ? ((opsz == 2) ? 2 : 4) : 1;
        int aluop = (op >> 3) & 7;
        unsigned int cur, val, res, mask, sbit;
        int cf = 0, of = 0;
        n = hwbp_modrm_len( ip + i + 1 );
        if (((ip[i+1] >> 6) & 3) == 3) return 0;      /* register form cannot fault */
        mask = (width == 1) ? 0xffu : (width == 2) ? 0xffffu : 0xffffffffu;
        sbit = (width == 1) ? 0x80u : (width == 2) ? 0x8000u : 0x80000000u;
        cur = (width == 1) ? *(unsigned char *)(ULONG_PTR)fa
            : (width == 2) ? *(unsigned short *)(ULONG_PTR)fa : *(unsigned int *)(ULONG_PTR)fa;
        cur &= mask;
        val = (unsigned int)hwbp_getreg( uc, reg ) & mask;

        if (op == 0x86 || op == 0x87)                /* xchg r/m, r */
        {
            if (width == 1) *(unsigned char *)(ULONG_PTR)fa = (unsigned char)val;
            else if (width == 2) *(unsigned short *)(ULONG_PTR)fa = (unsigned short)val;
            else *(unsigned int *)(ULONG_PTR)fa = val;
            hwbp_setreg( uc, reg, (hwbp_getreg(uc,reg) & ~(ULONG_PTR)mask) | cur );
            RIP_sig(uc) += i + 1 + n;
            return 1;
        }
        if (op == 0x84 || op == 0x85) aluop = 4;     /* test == and without store */

        if (reg_form)
        {
            unsigned int dst = val;                  /* dst = register */
            switch (aluop)
            {
            case 0: res = dst + cur; cf = res < dst; of = ((dst ^ res) & (cur ^ res)) >> 31; break;
            case 1: res = dst | cur; break;
            case 2: res = dst + cur; cf = res < dst; of = ((dst ^ res) & (cur ^ res)) >> 31; break;
            case 3: res = dst - cur; cf = dst < cur; of = ((dst ^ cur) & (dst ^ res)) >> 31; break;
            case 4: res = dst & cur; break;
            case 5: res = dst - cur; cf = dst < cur; of = ((dst ^ cur) & (dst ^ res)) >> 31; break;
            case 6: res = dst ^ cur; break;
            default: res = dst - cur; cf = dst < cur; of = ((dst ^ cur) & (dst ^ res)) >> 31; break;
            }
            hwbp_setflags( uc, res, width, cf, of );
            if (aluop != 7) hwbp_setreg( uc, reg, (hwbp_getreg(uc,reg) & ~(ULONG_PTR)mask) | (res & mask) );
        }
        else
        {
            unsigned int dst = cur;                  /* dst = memory */
            switch (aluop)
            {
            case 0: res = dst + val; cf = res < dst; of = ((dst ^ res) & (val ^ res)) >> 31; break;
            case 1: res = dst | val; break;
            case 2: res = dst + val; cf = res < dst; of = ((dst ^ res) & (val ^ res)) >> 31; break;
            case 3: res = dst - val; cf = dst < val; of = ((dst ^ val) & (dst ^ res)) >> 31; break;
            case 4: res = dst & val; break;
            case 5: res = dst - val; cf = dst < val; of = ((dst ^ val) & (dst ^ res)) >> 31; break;
            case 6: res = dst ^ val; break;
            default: res = dst - val; cf = dst < val; of = ((dst ^ val) & (dst ^ res)) >> 31; break;
            }
            hwbp_setflags( uc, res, width, cf, of );
            if (aluop != 7)
            {
                if (width == 1) *(unsigned char *)(ULONG_PTR)fa = (unsigned char)(res & mask);
                else if (width == 2) *(unsigned short *)(ULONG_PTR)fa = (unsigned short)(res & mask);
                else *(unsigned int *)(ULONG_PTR)fa = res & mask;
            }
        }
        RIP_sig(uc) += i + 1 + n;
        return 1;
    }
'''
s = s[:start] + new + s[end:]
io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('ALU forms rewritten (reg vs mem direction fixed)')
