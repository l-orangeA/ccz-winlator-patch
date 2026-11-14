import io
NL = '\n'
vp = '/root/wine0905/dlls/ntdll/unix/virtual.c'
s = io.open(vp, encoding='utf-8', errors='replace', newline='').read()

# 1) keep the page watched while stepping; add step helpers
old = '''int hwbp_handle_rw_fault( char *page, const void *addr )   /* DIRECT: return matched index */
{
    int idx = hwbp_match_write( addr );
    hwbp_pending = idx ? idx : 9;
    hwbp_step_flag = 0;   /* DIRECT */
    hwbp_rw_unwatch( page );
    set_page_vprot_bits( page, page_size, VPROT_READ | VPROT_WRITE, VPROT_WRITEWATCH );
    mprotect_range( page, page_size, 0, 0 );
    return idx;
}'''
new = '''static char *hwbp_step_page;

int hwbp_handle_rw_fault( char *page, const void *addr )   /* DIRECT: return matched index */
{
    int idx = hwbp_match_write( addr );
    hwbp_pending = idx ? idx : 9;
    hwbp_step_flag = 0;   /* DIRECT */
    hwbp_step_page = page;
    set_page_vprot_bits( page, page_size, VPROT_READ | VPROT_WRITE, VPROT_WRITEWATCH );
    mprotect_range( page, page_size, 0, 0 );
    return idx;
}

void hwbp_step_begin( char *page ) { hwbp_step_page = page; hwbp_step_flag = 1; }
int hwbp_step_active( void ) { return hwbp_step_flag; }
void hwbp_step_end( void )
{
    char *pg = hwbp_step_page;
    hwbp_step_flag = 0;
    hwbp_step_page = 0;
    if (!pg) return;
    if (!hwbp_rw_watched( pg )) return;
    set_page_vprot_bits( pg, page_size, 0, VPROT_READ | VPROT_WRITE | VPROT_WRITEWATCH );
    mprotect_range( pg, page_size, 0, 0 );
}'''
assert old in s, 'fault fn'
s = s.replace(old, new, 1)
io.open(vp, 'w', encoding='utf-8', newline=NL).write(s)

# 2) signal side
sp = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s2 = io.open(sp, encoding='utf-8', errors='replace', newline='').read()

old2 = '''extern int hwbp_perf_owner( int fd );  /* HWBP_PERF_TRAP */'''
new2 = '''extern int hwbp_perf_owner( int fd );  /* HWBP_PERF_TRAP */
extern void hwbp_step_begin( char *page );  /* HWBP_STEP */
extern int hwbp_step_active( void );
extern void hwbp_step_end( void );'''
assert old2 in s2, 'extern2'
s2 = s2.replace(old2, new2, 1)

# 3) non-match fault: single-step one instruction with host TF
old3 = '''                leave_handler( ucontext );
                return;
            }
        }
        if ((ULONG_PTR)RIP_sig(ucontext) < 0x10000000 || (ULONG_PTR)siginfo->si_addr < 0x10000000)'''
new3 = '''                hwbp_step_begin( rwpage );   /* HWBP_STEP: let one insn through, keep page armed */
                EFL_sig(ucontext) |= 0x100;
                leave_handler( ucontext );
                return;
            }
        }
        if ((ULONG_PTR)RIP_sig(ucontext) < 0x10000000 || (ULONG_PTR)siginfo->si_addr < 0x10000000)'''
assert old3 in s2, 'else branch'
s2 = s2.replace(old3, new3, 1)

# 4) trap_handler: handle our step traps before anything else
old4 = '''    ucontext_t *ucontext = init_handler( sigcontext );

    if (siginfo->si_code == SI_SIGIO)   /* HWBP_PERF_TRAP: hardware data breakpoint */'''
new4 = '''    ucontext_t *ucontext = init_handler( sigcontext );

    if (TRAP_sig(ucontext) == TRAP_x86_TRCTRAP && hwbp_step_active())   /* HWBP_STEP */
    {
        if ((ULONG_PTR)RIP_sig(ucontext) >= 0x10000000)
        {   /* still inside 64-bit bridge code: keep TF and continue */
            leave_handler( ucontext );
            return;
        }
        EFL_sig(ucontext) &= ~0x100;
        hwbp_step_end();
        leave_handler( ucontext );
        return;
    }

    if (siginfo->si_code == SI_SIGIO)   /* HWBP_PERF_TRAP: hardware data breakpoint */'''
assert old4 in s2, 'trap entry'
s2 = s2.replace(old4, new4, 1)
io.open(sp, 'w', encoding='utf-8', newline=NL).write(s2)
print('step patch applied')
