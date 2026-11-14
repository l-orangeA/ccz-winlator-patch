import subprocess
subprocess.run(['tar','-xzf','/mnt/d/unpack_tools/wine-20240905.tar.gz','-C','/tmp','wine-9.2-custom-a48ee6df4a77/dlls/ntdll/unix/virtual.c'],check=True)
p='/root/wine0905/dlls/ntdll/unix/virtual.c'
src='/tmp/wine-9.2-custom-a48ee6df4a77/dlls/ntdll/unix/virtual.c'
s=open(src,encoding='utf-8',errors='replace').read()
bs=chr(92); nl=chr(10)
old=nl.join([
'        else ret = grow_thread_stack( page, &stack_info );',
'    }',
'    else if (err & EXCEPTION_WRITE_FAULT)'])
new=nl.join([
'        else {',
'            char b[160]; int n = snprintf( b, sizeof(b),',
'                "GUARD page=%lx vprot=%x is_wow=%d start=%lx limit=%lx end=%lx guar=%lx stack=%lx err=%x' + bs + 'n",',
'                (unsigned long)page, vprot, stack_info.is_wow,',
'                (unsigned long)stack_info.start, (unsigned long)stack_info.limit,',
'                (unsigned long)stack_info.end, (unsigned long)stack_info.guaranteed,',
'                (unsigned long)stack, err );',
'            write( 2, b, n );',
'            ret = grow_thread_stack( page, &stack_info );',
'        }',
'    }',
'    else if (err & EXCEPTION_WRITE_FAULT)'])
assert old in s
s=s.replace(old,new,1)
open(p,'w',encoding='utf-8',newline=nl).write(s)
print('fix8 debug')
