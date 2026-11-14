NL = chr(10)
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = open(p, encoding='utf-8', errors='replace').read()
marker = 'HWBP_HOOK2'
if marker in s:
    print('already')
else:
    line = '        amd64_thread_data()->dr7 = context->Dr7;'
    line8 = '            amd64_thread_data()->dr7 = context->Dr7;'
    call = NL.join([
'            hwbp_update( (unsigned long)context->Dr0, (unsigned long)context->Dr1,',
'                         (unsigned long)context->Dr2, (unsigned long)context->Dr3,',
'                         (unsigned long)context->Dr7 );  /* HWBP_HOOK2 */'])
    # 8-space indented occurrences (NtSetContextThread !self branch and set_thread_info)
    n1 = s.count(line8)
    s = s.replace(line8, line8 + NL + call, n1)
    n2 = s.count(line) - s.count(line8)
    # replace 4-space ones only where not already followed by a hook
    s = s.replace(line, line + NL + call, n2)
    open(p, 'w', encoding='utf-8', newline=NL).write(s)
    print('hooks added: 8s=%d 4s=%d' % (n1, n2))
