NL = chr(10)
BS = chr(92)
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = open(p, encoding='utf-8', errors='replace').read()
if 'GCTX2' in s:
    print('already')
else:
    start = s.index('NTSTATUS get_thread_wow64_context(')
    end = s.index('return STATUS_SUCCESS;', start)
    ins = NL.join([
'    if (needed_flags & CONTEXT_I386_DEBUG_REGISTERS)',
'    {',
'        char b[192]; int n = snprintf( b, sizeof(b),',
'            "GCTX2 dr0=%lx dr1=%lx dr2=%lx dr3=%lx dr6=%lx dr7=%lx eip=%lx' + BS + 'n",',
'            (unsigned long)context->Dr0, (unsigned long)context->Dr1,',
'            (unsigned long)context->Dr2, (unsigned long)context->Dr3,',
'            (unsigned long)context->Dr6, (unsigned long)context->Dr7,',
'            (unsigned long)context->Eip );',
'        write( 2, b, n );',
'    }',
''])
    s = s[:end] + ins + s[end:]
    open(p, 'w', encoding='utf-8', newline=NL).write(s)
    print('patched GCTX2 at', end)
