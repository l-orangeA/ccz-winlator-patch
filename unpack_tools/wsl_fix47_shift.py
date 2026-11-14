NL = chr(10)
BS = chr(92)

# 1) virtual.c: DIRECT semantics again
p = '/root/wine0905/dlls/ntdll/unix/virtual.c'
s = open(p, encoding='utf-8', errors='replace').read()
old = NL.join([
'    int idx = hwbp_match_write( addr );',
'    hwbp_pending = idx ? idx : 9;',
'    hwbp_step_flag = 1;   /* STEP: host TF first, moved to guest in trap handler */'])
new = NL.join([
'    int idx = hwbp_match_write( addr );',
'    hwbp_pending = idx ? idx : 9;',
'    hwbp_step_flag = 0;   /* DIRECT */'])
if new in s:
    print('virt already DIRECT')
else:
    assert old in s, 'virt step line'
    s = s.replace(old, new, 1)
    open(p, 'w', encoding='utf-8', newline=NL).write(s)
    print('virt -> DIRECT')

# 2) signal_x86_64.c: direct delivery
p2 = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s2 = open(p2, encoding='utf-8', errors='replace').read()
old2 = NL.join([
'                hwbp_handle_rw_fault( rwpage, siginfo->si_addr );',
'                EFL_sig(ucontext) |= 0x100;   /* STEP: host TF, moved to guest on first trap */',
'                leave_handler( ucontext );',
'                return;'])
new2 = NL.join([
'                {',
'                    int idx = hwbp_handle_rw_fault( rwpage, siginfo->si_addr );',
'                    if (idx >= 1 && idx <= 4)',
'                    {',
'                        EXCEPTION_RECORD srec = { 0 };',
'                        srec.ExceptionCode = EXCEPTION_SINGLE_STEP;',
'                        srec.ExceptionAddress = (void *)RIP_sig(ucontext);',
'                        amd64_thread_data()->dr6 = 0xffff0ff0u | (1u << idx);',
'                        setup_exception( ucontext, &srec );',
'                        return;',
'                    }',
'                }',
'                leave_handler( ucontext );',
'                return;'])
if new2 in s2:
    print('signal already DIRECT')
else:
    assert old2 in s2, 'signal step branch'
    s2 = s2.replace(old2, new2, 1)
    open(p2, 'w', encoding='utf-8', newline=NL).write(s2)
    print('signal -> DIRECT')

# 3) thread.c: shift wow64 stack base so the packer's watched byte lands on a page start
p3 = '/root/wine0905/dlls/ntdll/unix/thread.c'
s3 = open(p3, encoding='utf-8', errors='replace').read()
old3 = '        wow_teb->Tib.StackBase = PtrToUlong( stack.StackBase );'
new3 = '        wow_teb->Tib.StackBase = PtrToUlong( stack.StackBase ) - 0xE14;  /* STACKSHIFT */'
if new3 in s3:
    print('thread already')
else:
    assert old3 in s3, 'thread stackbase'
    s3 = s3.replace(old3, new3, 1)
    open(p3, 'w', encoding='utf-8', newline=NL).write(s3)
    print('thread -> STACKSHIFT')
