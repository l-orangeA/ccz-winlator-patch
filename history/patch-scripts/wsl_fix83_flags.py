import io
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = io.open(p, encoding='utf-8', errors='replace', newline='').read()
if 'hwbp_setflags' in s:
    print('already'); raise SystemExit

# helper before hwbp_emu_insn
anchor = 'static int hwbp_emu_insn( ucontext_t *uc, const unsigned char *ip, ULONG_PTR fa )'
helper = '''static void hwbp_setflags( ucontext_t *uc, unsigned int res, int width, int cf, int of )
{
    unsigned int mask = (width == 1) ? 0xffu : (width == 2) ? 0xffffu : 0xffffffffu;
    unsigned int sbit = (width == 1) ? 0x80u : (width == 2) ? 0x8000u : 0x80000000u;
    unsigned long ef = EFL_sig(uc) & ~0x8d5ull;
    unsigned int par = (res & 0xff);
    par ^= par >> 4; par ^= par >> 2; par ^= par >> 1;
    if (cf) ef |= 0x1;
    if ((res & mask) == 0) ef |= 0x40;
    if (res & sbit) ef |= 0x80;
    if (of) ef |= 0x800;
    if (!(par & 1)) ef |= 0x4;
    EFL_sig(uc) = ef;
}

'''
assert anchor in s
s = s.replace(anchor, helper + anchor, 1)

# cmp/test/xchg and ALU: set flags
old = '''            case 0: res = cur + val; break;      /* add */
            case 1: res = cur | val; break;      /* or */
            case 2: res = cur + val; break;      /* adc (approx) */
            case 3: res = cur - val; break;      /* sbb (approx) */
            case 4: res = cur & val; break;      /* and */
            case 5: res = cur - val; break;      /* sub */
            case 6: res = cur ^ val; break;      /* xor */
            default: res = cur - val; break;     /* cmp: no store */
            }
        }'''
new = '''            case 0: res = cur + val; hwbp_setflags( uc, res, width, res < cur, ((cur ^ res) & (val ^ res)) >> 31 ); break;
            case 1: res = cur | val; hwbp_setflags( uc, res, width, 0, 0 ); break;
            case 2: res = cur + val; hwbp_setflags( uc, res, width, res < cur, ((cur ^ res) & (val ^ res)) >> 31 ); break;
            case 3: res = cur - val; hwbp_setflags( uc, res, width, cur < val, ((cur ^ val) & (cur ^ res)) >> 31 ); break;
            case 4: res = cur & val; hwbp_setflags( uc, res, width, 0, 0 ); break;
            case 5: res = cur - val; hwbp_setflags( uc, res, width, cur < val, ((cur ^ val) & (cur ^ res)) >> 31 ); break;
            case 6: res = cur ^ val; hwbp_setflags( uc, res, width, 0, 0 ); break;
            default: res = cur - val; hwbp_setflags( uc, res, width, cur < val, ((cur ^ val) & (cur ^ res)) >> 31 ); break;  /* cmp */
            }
        }
        if (op == 0x84 || op == 0x85) hwbp_setflags( uc, cur & val, width, 0, 0 );   /* test */'''
assert old in s, 'alu switch'
s = s.replace(old, new, 1)
io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('flags added')
