NL = chr(10)
p2 = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s2 = open(p2, encoding='utf-8', errors='replace').read()
old = NL.join([
'                        amd64_thread_data()->dr6 = 0xffff0ff0u | (1u << idx);',
'                        setup_exception( ucontext, &srec );',
'                        return;'])
new = NL.join([
'                        amd64_thread_data()->dr6 = 0xffff0ff0u | (1u << idx);',
'                        {   /* deliver with our own ExceptionAddress (after the insn) */',
'                            struct xcontext xctx;',
'                            save_context( &xctx, ucontext );',
'                            setup_raise_exception( ucontext, &srec, &xctx );',
'                        }',
'                        return;'])
if new in s2:
    print('already')
else:
    assert old in s2, 'setup_exception call'
    s2 = s2.replace(old, new, 1)
    open(p2, 'w', encoding='utf-8', newline=NL).write(s2)
    print('patched setup_raise_exception direct')
