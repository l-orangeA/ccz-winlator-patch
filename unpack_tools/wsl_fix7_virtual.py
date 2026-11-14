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
'        else if (stack_info.is_wow && !(vprot & VPROT_WRITE))',
'        {',
'            static const char m[] = "WOW_AV_PASS' + bs + 'n";',
'            write( 2, m, sizeof(m) - 1 );',
'            ret = STATUS_ACCESS_VIOLATION;',
'        }',
'        else ret = grow_thread_stack( page, &stack_info );',
'    }',
'    else if (err & EXCEPTION_WRITE_FAULT)'])
assert old in s, 'pattern'
s=s.replace(old,new,1)
open(p,'w',encoding='utf-8',newline=nl).write(s)
print('virtual.c fix7')
