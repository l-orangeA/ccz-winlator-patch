import io
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = io.open(p, encoding='utf-8', errors='replace', newline='').read()
old = '''            char wb[160]; int wn = snprintf( wb, sizeof(wb),
                "WCTX eip=%lx esp=%lx ef=%lx flags=%lx\\n",
                (unsigned long)context->Eip, (unsigned long)context->Esp,
                (unsigned long)context->EFlags, (unsigned long)context->ContextFlags );'''
new = '''            char wb[256]; int wn = snprintf( wb, sizeof(wb),
                "WCTX ctx=%p eip=%lx esp=%lx ef=%lx flags=%lx dr0=%lx dr1=%lx dr7=%lx eax=%lx\\n",
                (void *)context, (unsigned long)context->Eip, (unsigned long)context->Esp,
                (unsigned long)context->EFlags, (unsigned long)context->ContextFlags,
                (unsigned long)context->Dr0, (unsigned long)context->Dr1,
                (unsigned long)context->Dr7, (unsigned long)context->Eax );'''
assert old in s, 'wctx'
s = s.replace(old, new, 1)
io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('wctx ctx added')
