import subprocess
subprocess.run(['tar','-xzf','/mnt/d/unpack_tools/wine-20240905.tar.gz','-C','/tmp','wine-9.2-custom-a48ee6df4a77/dlls/ntdll/unix/signal_x86_64.c'],check=True)
p='/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
src='/tmp/wine-9.2-custom-a48ee6df4a77/dlls/ntdll/unix/signal_x86_64.c'
s=open(src,encoding='utf-8',errors='replace').read()
bs=chr(92); nl=chr(10)
old=nl.join([
'    stack = virtual_setup_exception( stack_ptr, stack_size, rec );',
'    stack->rec               = *rec;'])
new=nl.join([
'    stack = virtual_setup_exception( stack_ptr, stack_size, rec );',
'    {',
'        ULONG_PTR page_size = getpagesize();',
'        ULONG_PTR page = (ULONG_PTR)stack & ~(page_size - 1);',
'        ULONG_PTR end  = ((ULONG_PTR)stack + sizeof(*stack) + page_size - 1) & ~(page_size - 1);',
'        int rc = mprotect( (void *)page, end - page, PROT_READ | PROT_WRITE );',
'        char b[96]; int n = snprintf( b, sizeof(b), "MPROT rc=%d errno=%d page=%lx size=%lx' + bs + 'n", rc, errno, (unsigned long)page, (unsigned long)(end-page) );',
'        write( 2, b, n );',
'    }',
'    stack->rec               = *rec;'])
assert old in s
s=s.replace(old,new,1)
if '#include <errno.h>' not in s:
    s=s.replace('#include <sys/types.h>','#include <sys/types.h>'+nl+'#include <errno.h>',1)
if '#include <sys/mman.h>' not in s:
    s=s.replace('#include <sys/types.h>','#include <sys/types.h>'+nl+'#include <sys/mman.h>',1)
open(p,'w',encoding='utf-8',newline=nl).write(s)
print('patched with errno')
