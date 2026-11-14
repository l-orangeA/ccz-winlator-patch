NL = chr(10)
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = open(p, encoding='utf-8', errors='replace').read()
if 'DR_EXPERIMENT' in s:
    print('already')
else:
    old = NL.join([
'    if (!self)',
'    {',
'        NTSTATUS ret = set_thread_context( handle, context, &self, IMAGE_FILE_MACHINE_I386 );',
'        if (ret || !self) return ret;',
'        if (flags & CONTEXT_I386_DEBUG_REGISTERS)',
'        {',
'            amd64_thread_data()->dr0 = context->Dr0;',
'            amd64_thread_data()->dr1 = context->Dr1;',
'            amd64_thread_data()->dr2 = context->Dr2;',
'            amd64_thread_data()->dr3 = context->Dr3;',
'            amd64_thread_data()->dr6 = context->Dr6;',
'            amd64_thread_data()->dr7 = context->Dr7;',
'        }',
'        if (!(flags & ~CONTEXT_I386_DEBUG_REGISTERS)) return ret;',
'    }'])
    new = NL.join([
'    /* DR_EXPERIMENT: cache debug registers but never program them via ptrace */',
'    if (flags & CONTEXT_I386_DEBUG_REGISTERS)',
'    {',
'        amd64_thread_data()->dr0 = context->Dr0;',
'        amd64_thread_data()->dr1 = context->Dr1;',
'        amd64_thread_data()->dr2 = context->Dr2;',
'        amd64_thread_data()->dr3 = context->Dr3;',
'        amd64_thread_data()->dr6 = context->Dr6;',
'        amd64_thread_data()->dr7 = context->Dr7;',
'        flags &= ~CONTEXT_I386_DEBUG_REGISTERS;',
'        if (!flags) return STATUS_SUCCESS;',
'    }',
'    if (!self)',
'    {',
'        NTSTATUS ret = set_thread_context( handle, context, &self, IMAGE_FILE_MACHINE_I386 );',
'        if (ret || !self) return ret;',
'        if (!(flags & ~CONTEXT_I386_DEBUG_REGISTERS)) return ret;',
'    }'])
    assert old in s, 'wow64 set context block'
    s = s.replace(old, new, 1)
    open(p, 'w', encoding='utf-8', newline=NL).write(s)
    print('patched DR_EXPERIMENT')
