NL = chr(10)
BS = chr(92)
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = open(p, encoding='utf-8', errors='replace').read()

# revert DR_EXPERIMENT
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
if new in s:
    s = s.replace(new, old, 1)
    print('reverted DR_EXPERIMENT')
else:
    print('DR_EXPERIMENT block not found (ok)')

# add WCTX probe
if 'WCTX' in s:
    print('already WCTX')
else:
    old2 = '    if (size != sizeof(I386_CONTEXT)) return STATUS_INFO_LENGTH_MISMATCH;'
    new2 = NL.join([
'    if (size != sizeof(I386_CONTEXT)) return STATUS_INFO_LENGTH_MISMATCH;',
'    {',
'        static int wcn;',
'        if (wcn++ < 200 && (context->ContextFlags & 1))',
'        {',
'            char wb[160]; int wn = snprintf( wb, sizeof(wb),',
'                "WCTX eip=%lx esp=%lx ef=%lx flags=%lx' + BS + 'n",',
'                (unsigned long)context->Eip, (unsigned long)context->Esp,',
'                (unsigned long)context->EFlags, (unsigned long)context->ContextFlags );',
'            write( 2, wb, wn );',
'        }',
'    }'])
    assert old2 in s
    s = s.replace(old2, new2, 1)
    print('patched WCTX')

open(p, 'w', encoding='utf-8', newline=NL).write(s)
