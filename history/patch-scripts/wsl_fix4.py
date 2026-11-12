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
'        size_t sz = end - page;',
'        if (mprotect( (void *)page, sz, PROT_READ | PROT_WRITE ) != 0)',
'        {',
'            static char tmp[16384];',
'            if (sz <= sizeof(tmp))',
'            {',
'                static const char m2[] = "MPROT_FALLBACK' + bs + 'n";',
'                write( 2, m2, sizeof(m2) - 1 );',
'                memcpy( tmp, (void *)page, sz );',
'                void *p = mmap( (void *)page, sz, PROT_READ | PROT_WRITE,',
'                                MAP_PRIVATE | MAP_ANONYMOUS | MAP_FIXED, -1, 0 );',
'                if (p != MAP_FAILED) memcpy( p, tmp, sz );',
'            }',
'        }',
'    }',
'    stack->rec               = *rec;'])
assert old in s
s=s.replace(old,new,1)
if '#include <sys/mman.h>' not in s:
    s=s.replace('#include <sys/types.h>','#include <sys/types.h>'+nl+'#include <sys/mman.h>',1)
if '#include <errno.h>' not in s:
    s=s.replace('#include <sys/types.h>','#include <sys/types.h>'+nl+'#include <errno.h>',1)
open(p,'w',encoding='utf-8',newline=nl).write(s)
print('patched fix4')
