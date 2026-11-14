NL = chr(10)
BS = chr(92)
p = '/root/wine0905/dlls/ntdll/signal_i386.c'
s = open(p, encoding='utf-8', errors='replace').read()
if 'BP_EIP_FIX' in s:
    print('already')
else:
    old = NL.join([
'    TRACE( "code=%lx flags=%lx addr=%p ip=%08lx' + BS + 'n",',
'           rec->ExceptionCode, rec->ExceptionFlags, rec->ExceptionAddress, context->Eip );'])
    new = NL.join([
'    TRACE( "code=%lx flags=%lx addr=%p ip=%08lx' + BS + 'n",',
'           rec->ExceptionCode, rec->ExceptionFlags, rec->ExceptionAddress, context->Eip );',
'    /* BP_EIP_FIX: box64 reports breakpoint context Eip one byte below the',
'     * int3; Windows exposes Eip == ExceptionAddress for #BP. */',
'    if (rec->ExceptionCode == EXCEPTION_BREAKPOINT &&',
'        context->Eip == (DWORD)(ULONG_PTR)rec->ExceptionAddress - 1)',
'        context->Eip = (DWORD)(ULONG_PTR)rec->ExceptionAddress;'])
    assert old in s, 'dispatch trace'
    s = s.replace(old, new, 1)
    open(p, 'w', encoding='utf-8', newline=NL).write(s)
    print('patched BP_EIP_FIX')
