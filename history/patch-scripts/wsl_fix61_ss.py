import io
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = io.open(p, encoding='utf-8', errors='replace', newline='').read()

old = '''    case TRAP_x86_STKFLT:  /* Stack fault */
        /* box64 reports faults on 32-bit stack accesses as #SS;'''
new = '''    case TRAP_x86_STKFLT:  /* Stack fault */
        /* HWBP_SS: a watched page inside the stack is reported as #SS by box64 */
        if (hwbp_rw_page_for( siginfo->si_addr )) goto hwbp_pagefault;
        /* box64 reports faults on 32-bit stack accesses as #SS;'''
assert old in s, 'stkflt'
s = s.replace(old, new, 1)

old2 = '''    case TRAP_x86_PAGEFLT:  /* Page fault */
        {
            char *rwpage = hwbp_rw_page_for( siginfo->si_addr );'''
new2 = '''    case TRAP_x86_PAGEFLT:  /* Page fault */
    hwbp_pagefault:
        {
            char *rwpage = hwbp_rw_page_for( siginfo->si_addr );'''
assert old2 in s, 'pageflt'
s = s.replace(old2, new2, 1)

io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('ss routing applied')
