import sys, subprocess, os
# restore pristine file from tarball
subprocess.run(['tar','-xzf','/mnt/d/unpack_tools/wine-20240905.tar.gz','-C','/tmp','wine-9.2-custom-a48ee6df4a77/dlls/ntdll/unix/signal_x86_64.c'],check=True)
p='/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
src='/tmp/wine-9.2-custom-a48ee6df4a77/dlls/ntdll/unix/signal_x86_64.c'
s=open(src,encoding='utf-8',errors='replace').read()
bs=chr(92)
nl=chr(10)
if '#include <sys/mman.h>' not in s:
    s=s.replace('#include <sys/types.h>','#include <sys/types.h>'+nl+'#include <sys/mman.h>',1)
old=nl.join([
'    if (status) return status;',
'    stack = (struct exc_stack_layout *)((context->Rsp - sizeof(*stack)) & ~(ULONG_PTR)63);',
'    memmove( &stack->context, context, sizeof(*context) );'])
new=nl.join([
'    if (status) return status;',
'    stack = (struct exc_stack_layout *)((context->Rsp - sizeof(*stack)) & ~(ULONG_PTR)63);',
'    {',
'        ULONG_PTR page_size = getpagesize();',
'        ULONG_PTR page = (ULONG_PTR)stack & ~(page_size - 1);',
'        ULONG_PTR end  = ((ULONG_PTR)stack + sizeof(*stack) + page_size - 1) & ~(page_size - 1);',
'        static const char m[] = "WINE_MPROTECT_STACK' + bs + 'n";',
'        write( 2, m, sizeof(m) - 1 );',
'        mprotect( (void *)page, end - page, PROT_READ | PROT_WRITE );',
'    }',
'    memmove( &stack->context, context, sizeof(*context) );'])
assert old in s, 'pattern'
s=s.replace(old,new,1)
open(p,'w',encoding='utf-8',newline=nl).write(s)
print('written', 'WINE_MPROTECT_STACK' in s)
