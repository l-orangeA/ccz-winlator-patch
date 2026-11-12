import io
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = io.open(p, encoding='utf-8', errors='replace', newline='').read()

# 1) mov with operand-size awareness
old = '''        if (op == 0x8b) { hwbp_setreg( uc, reg, *(unsigned int *)(ULONG_PTR)ea ); }
        else if (op == 0x8a) { hwbp_setreg( uc, reg, (hwbp_getreg(uc,reg) & ~0xffu) | *(unsigned char *)(ULONG_PTR)ea ); }
        else if (op == 0x89) { *(unsigned int *)(ULONG_PTR)ea = (unsigned int)hwbp_getreg( uc, reg );
                               { char b[128]; int n2 = snprintf(b,sizeof(b),"EMU store rip=%lx ea=%lx val=%lx eax=%lx\\n",(unsigned long)RIP_sig(uc),(unsigned long)ea,(unsigned long)hwbp_getreg(uc,reg),(unsigned long)hwbp_getreg(uc,0)); write(2,b,n2); } }
        else if (op == 0x88) { *(unsigned char *)(ULONG_PTR)ea = (unsigned char)hwbp_getreg( uc, reg ); }'''
new = '''        if (op == 0x8b) { unsigned int v = (opsz == 2) ? *(unsigned short *)(ULONG_PTR)ea : *(unsigned int *)(ULONG_PTR)ea;
                          if (opsz == 2) hwbp_setreg( uc, reg, (hwbp_getreg(uc,reg) & ~0xffffu) | v ); else hwbp_setreg( uc, reg, v ); }
        else if (op == 0x8a) { hwbp_setreg( uc, reg, (hwbp_getreg(uc,reg) & ~0xffu) | *(unsigned char *)(ULONG_PTR)ea ); }
        else if (op == 0x89) { if (opsz == 2) *(unsigned short *)(ULONG_PTR)ea = (unsigned short)hwbp_getreg( uc, reg );
                               else *(unsigned int *)(ULONG_PTR)ea = (unsigned int)hwbp_getreg( uc, reg );
                               { char b[128]; int n2 = snprintf(b,sizeof(b),"EMU store rip=%lx ea=%lx val=%lx sz=%d eax=%lx\\n",(unsigned long)RIP_sig(uc),(unsigned long)ea,(unsigned long)hwbp_getreg(uc,reg),opsz,(unsigned long)hwbp_getreg(uc,0)); write(2,b,n2); } }
        else if (op == 0x88) { *(unsigned char *)(ULONG_PTR)ea = (unsigned char)hwbp_getreg( uc, reg ); }'''
assert old in s, 'mov block'
s = s.replace(old, new, 1)

# 2) add 0xc7 / 0x80 / 0x81 / 0x83 before the 0xff group
anchor = '''    if (op == 0xff)                        /* group5: push/call/jmp r/m */'''
add = '''    if (op == 0xc7 || op == 0x80 || op == 0x81 || op == 0x83)
    {
        ULONG_PTR ea = 0; int reg, is_mem;
        int imm;
        n = hwbp_emu_modrm( uc, ip + i + 1, &ea, &reg, &is_mem );
        if (!is_mem) return 0;
        if (op == 0xc7)
        {
            imm = (int)(ip[i+1+n] | (ip[i+2+n] << 8) | (ip[i+3+n] << 16) | ((unsigned)ip[i+4+n] << 24));
            if (opsz == 2) *(unsigned short *)(ULONG_PTR)ea = (unsigned short)imm;
            else *(unsigned int *)(ULONG_PTR)ea = (unsigned int)imm;
            RIP_sig(uc) += i + 1 + n + (opsz == 2 ? 2 : 4);
            return 1;
        }
        if (op == 0x83) { imm = (int)(signed char)ip[i+1+n]; }
        else if (op == 0x81) { imm = (int)(ip[i+1+n] | (ip[i+2+n] << 8) | (ip[i+3+n] << 16) | ((unsigned)ip[i+4+n] << 24)); }
        else { imm = (int)(signed char)ip[i+1+n]; }
        {
            unsigned int cur = (opsz == 2) ? *(unsigned short *)(ULONG_PTR)ea : *(unsigned int *)(ULONG_PTR)ea;
            unsigned int res;
            switch (reg)
            {
            case 0: res = cur + imm; break;              /* add */
            case 1: res = cur | imm; break;              /* or  */
            case 4: res = cur & imm; break;              /* and */
            case 5: res = cur - imm; break;              /* sub */
            case 6: res = cur ^ imm; break;              /* xor */
            default: return 0;
            }
            if (opsz == 2) *(unsigned short *)(ULONG_PTR)ea = (unsigned short)res;
            else *(unsigned int *)(ULONG_PTR)ea = res;
            EFL_sig(uc) = (EFL_sig(uc) & ~0x8d5ull) | (res == 0 ? 0x40 : 0) | (res & 0x80000000u ? 0x80 : 0);
        }
        RIP_sig(uc) += i + 1 + n + (op == 0x81 ? (opsz == 2 ? 2 : 4) : 1);
        return 1;
    }

'''
s = s.replace(anchor, add + anchor, 1)

# 3) fix jcc signed comparisons
old3 = '''        case 12: take = (ef & 0x80) != ((ef & 0x800) ? 1u : 0u); break;
        case 13: take = (ef & 0x80) == ((ef & 0x800) ? 1u : 0u); break;'''
new3 = '''        case 12: take = ((ef & 0x80) != 0) != ((ef & 0x800) != 0); break;
        case 13: take = ((ef & 0x80) != 0) == ((ef & 0x800) != 0); break;'''
assert old3 in s, 'jcc'
s = s.replace(old3, new3, 1)

# fix LLE/JG (14/15) too: 14 = JLE = ZF || SF!=OF ; 15 = JG = !ZF && SF==OF
old4 = '''        case 14: take = ef & 0x400; break; default: take = !(ef & 0x400); break; }'''
new4 = '''        case 14: take = (ef & 0x40) || (((ef & 0x80) != 0) != ((ef & 0x800) != 0)); break;
        default: take = !(ef & 0x40) && (((ef & 0x80) != 0) == ((ef & 0x800) != 0)); break; }'''
assert old4 in s, 'jle/jg'
s = s.replace(old4, new4, 1)

# 4) pushad saved ESP should be the pre-push ESP
old5 = '        ((unsigned int *)(ULONG_PTR)esp)[3] = (unsigned)(RSP_sig(uc) + 32);'
new5 = '        ((unsigned int *)(ULONG_PTR)esp)[3] = (unsigned)RSP_sig(uc);'
assert old5 in s, 'pushad esp'
s = s.replace(old5, new5, 1)

# 5) FF/6 push r/m operand size
old6 = '''        if (reg == 6)   /* push r/m */
        {
            unsigned int v = is_mem ? *(unsigned int *)(ULONG_PTR)ea : (unsigned int)hwbp_getreg( uc, ea & 7 );
            esp -= 4; *(unsigned int *)(ULONG_PTR)esp = v;
            hwbp_setreg( uc, 4, esp ); RIP_sig(uc) += i + 1 + n; return 1;
        }'''
new6 = '''        if (reg == 6)   /* push r/m */
        {
            unsigned int v = is_mem ? ((opsz == 2) ? *(unsigned short *)(ULONG_PTR)ea : *(unsigned int *)(ULONG_PTR)ea)
                                    : (unsigned int)hwbp_getreg( uc, ea & 7 );
            if (opsz == 2) { esp -= 2; *(unsigned short *)(ULONG_PTR)esp = (unsigned short)v; }
            else { esp -= 4; *(unsigned int *)(ULONG_PTR)esp = v; }
            hwbp_setreg( uc, 4, esp ); RIP_sig(uc) += i + 1 + n; return 1;
        }'''
assert old6 in s, 'ff push'
s = s.replace(old6, new6, 1)

io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('emulator fixes applied')
